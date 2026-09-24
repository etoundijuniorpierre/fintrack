// Service metier : execute les sauvegardes PostgreSQL / MongoDB et la synchronisation B2.

package com.fintrack.reporting.service.impl;

import com.fintrack.reporting.model.readmodel.superadmin.BackupOutcome;
import com.fintrack.reporting.service.BackupService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Le dump passe par le reseau (pg_dumpall / mongodump vers les conteneurs de bases),
 * puis rclone pousse le repertoire vers Backblaze B2. Aucun socket Docker n'est
 * requis : le service n'a aucun pouvoir sur l'hote.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {

  private final MessageSource messageSource;

  private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern(
    "yyyyMMdd_HHmmss"
  );

  /** Au-dela, l'outil est considere bloque et le processus est tue. */
  private static final long COMMAND_TIMEOUT_MINUTES = 30;

  @Value("${fintrack.backup.directory:${BACKUP_DIR:/data/fintrack/backups}}")
  private String backupDirectory;

  @Value("${fintrack.backup.db.host:${BACKUP_DB_HOST:postgresql}}")
  private String dbHost;

  @Value("${fintrack.backup.db.port:${BACKUP_DB_PORT:5434}}")
  private String dbPort;

  @Value("${fintrack.backup.db.user:${BACKUP_DB_USER:postgres}}")
  private String dbUser;

  @Value("${fintrack.backup.db.password:${BACKUP_DB_PASSWORD:}}")
  private String dbPassword;

  @Value("${fintrack.backup.mongo.host:${BACKUP_MONGO_HOST:mongodb}}")
  private String mongoHost;

  @Value("${fintrack.backup.mongo.port:${BACKUP_MONGO_PORT:27019}}")
  private String mongoPort;

  @Value("${fintrack.backup.mongo.user:${BACKUP_MONGO_USER:}}")
  private String mongoUser;

  @Value("${fintrack.backup.mongo.password:${BACKUP_MONGO_PASSWORD:}}")
  private String mongoPassword;

  @Value("${fintrack.backup.b2.bucket:${B2_BUCKET_NAME:}}")
  private String b2Bucket;

  @Value("${fintrack.backup.b2.key-id:${B2_KEY_ID:}}")
  private String b2KeyId;

  @Value("${fintrack.backup.b2.application-key:${B2_APPLICATION_KEY:}}")
  private String b2ApplicationKey;

  @Override
  public BackupOutcome run() {
    LocalDateTime now = LocalDateTime.now();
    String stamp = now.format(STAMP);
    List<String> failures = new ArrayList<>();

    Path directory = Paths.get(backupDirectory);
    try {
      Files.createDirectories(directory);
    } catch (IOException ex) {
      log.error("Répertoire de sauvegarde inaccessible : {}", backupDirectory, ex);
      return BackupOutcome.builder()
        .success(false)
        .message(
          msg("superadmin.backup.directory_error", ex.getMessage())
        )
        .timestamp(now)
        .postgresStatus("FAILED")
        .mongoStatus("FAILED")
        .b2SyncStatus("SKIPPED")
        .backupDirectory(backupDirectory)
        .build();
    }

    String postgresStatus = dumpPostgres(directory, stamp, failures);
    String mongoStatus = dumpMongo(directory, stamp, failures);
    String b2Status = syncToB2(directory, failures);

    boolean success = failures.isEmpty();
    return BackupOutcome.builder()
      .success(success)
      .message(
        success
          ? msg("superadmin.backup.success")
          : msg("superadmin.backup.incomplete", String.join(" | ", failures))
      )
      .timestamp(now)
      .postgresStatus(postgresStatus)
      .mongoStatus(mongoStatus)
      .b2SyncStatus(b2Status)
      .backupDirectory(backupDirectory)
      .build();
  }

  // pg_dumpall couvre les quatre bases applicatives en une passe.
  private String dumpPostgres(Path directory, String stamp, List<String> failures) {
    Path target = directory.resolve("postgres_" + stamp + ".sql");
    List<String> command = List.of(
      "pg_dumpall",
      "-h", dbHost,
      "-p", dbPort,
      "-U", dbUser
    );
    return execute(
      "PostgreSQL",
      command,
      Map.of("PGPASSWORD", dbPassword == null ? "" : dbPassword),
      target,
      failures
    );
  }

  // mongodump archive les deux bases documentaires en un flux gzip.
  private String dumpMongo(Path directory, String stamp, List<String> failures) {
    Path target = directory.resolve("mongo_" + stamp + ".archive.gz");
    List<String> command = new ArrayList<>(
      List.of("mongodump", "--host", mongoHost, "--port", mongoPort, "--archive", "--gzip")
    );
    if (StringUtils.hasText(mongoUser) && StringUtils.hasText(mongoPassword)) {
      command.add("--username");
      command.add(mongoUser);
      command.add("--password");
      command.add(mongoPassword);
      command.add("--authenticationDatabase");
      command.add("admin");
    }
    return execute("MongoDB", command, Map.of(), target, failures);
  }

  // rclone pousse le repertoire complet ; sans bucket configure, l'etape est ignoree.
  private String syncToB2(Path directory, List<String> failures) {
    if (!StringUtils.hasText(b2Bucket)) {
      return "NOT_CONFIGURED";
    }
    if (!StringUtils.hasText(b2KeyId) || !StringUtils.hasText(b2ApplicationKey)) {
      failures.add(msg("superadmin.backup.b2_credentials_missing"));
      return "NOT_CONFIGURED";
    }

    // Le profil est fourni en ligne : rien n'est ecrit dans un fichier de config.
    List<String> command = List.of(
      "rclone",
      "sync",
      directory.toAbsolutePath().toString(),
      ":b2:" + b2Bucket,
      "--b2-account", b2KeyId,
      "--b2-key", b2ApplicationKey
    );
    return execute("Backblaze B2", command, Map.of(), null, failures)
        .equals("SUCCESS")
      ? "SYNCHRONIZED"
      : "FAILED";
  }

  /**
   * Lance une commande externe. {@code output} non nul redirige la sortie standard
   * vers ce fichier (cas des dumps qui ecrivent sur stdout).
   */
  private String execute(
    String label,
    List<String> command,
    Map<String, String> environment,
    Path output,
    List<String> failures
  ) {
    ProcessBuilder builder = new ProcessBuilder(command);
    builder.environment().putAll(environment);
    if (output != null) {
      builder.redirectOutput(output.toFile());
      builder.redirectErrorStream(false);
    } else {
      builder.redirectErrorStream(true);
    }

    Process process = null;
    try {
      process = builder.start();
      if (!process.waitFor(COMMAND_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
        process.destroyForcibly();
        failures.add(msg("superadmin.backup.step_timeout", label));
        return "FAILED";
      }
      int exitCode = process.exitValue();
      if (exitCode != 0) {
        failures.add(msg("superadmin.backup.step_exit_code", label, exitCode));
        return "FAILED";
      }
      log.info("Sauvegarde {} terminée avec succès.", label);
      return "SUCCESS";
    } catch (IOException ex) {
      // Binaire absent de l'image : on le dit, au lieu de laisser croire au succes.
      log.error("Outil de sauvegarde {} indisponible : {}", label, ex.getMessage());
      failures.add(msg("superadmin.backup.step_tool_missing", label));
      return "UNAVAILABLE";
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      if (process != null) {
        process.destroyForcibly();
      }
      failures.add(msg("superadmin.backup.step_interrupted", label));
      return "FAILED";
    }
  }

  // Tous les libelles passent par MessageSource : rien n'est fige en dur.
  private String msg(String key, Object... args) {
    return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
  }
}
