// Tests unitaires : verifie que les migrations Flyway restent purement additives.

package com.fintrack.document.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlywayMigrationSafetyTest {

  private static final Path MIGRATIONS = Paths.get(
    "src/main/resources/db/migration"
  );


  private static final Map<Pattern, String> FORBIDDEN = Map.of(
    Pattern.compile("\\bDROP\\s+TABLE\\b", Pattern.CASE_INSENSITIVE),
    "DROP TABLE supprime les lignes",
    Pattern.compile("\\bDROP\\s+COLUMN\\b", Pattern.CASE_INSENSITIVE),
    "DROP COLUMN supprime le contenu de la colonne",
    Pattern.compile("\\bTRUNCATE\\b", Pattern.CASE_INSENSITIVE),
    "TRUNCATE vide la table",
    Pattern.compile("\\bDELETE\\s+FROM\\b", Pattern.CASE_INSENSITIVE),
    "DELETE FROM supprime des lignes",
    Pattern.compile("\\bRENAME\\s+TO\\b", Pattern.CASE_INSENSITIVE),
    "RENAME TO coupe l'application de ses donnees",
    Pattern.compile("\\bDROP\\s+(DATABASE|SCHEMA)\\b", Pattern.CASE_INSENSITIVE),
    "DROP DATABASE/SCHEMA detruit tout"
  );

  @Test
  @DisplayName(
    "Migrations - Contain no statement able to destroy existing data"
  )
  void migrations_ContainNoDestructiveStatement() throws IOException {
    List<String> violations = new ArrayList<>();

    for (Path migration : migrationFiles()) {
      String sql = stripComments(
        Files.readString(migration, StandardCharsets.UTF_8)
      );
      FORBIDDEN.forEach((pattern, reason) -> {
        if (pattern.matcher(sql).find()) {
          violations.add(migration.getFileName() + " : " + reason);
        }
      });
    }

    assertThat(violations)
      .as(
        "Une migration doit pouvoir etre appliquee en production sans perdre de donnees"
      )
      .isEmpty();
  }

  @Test
  @DisplayName("Migrations - Every ADD COLUMN is replayable and non-blocking")
  void migrations_AddColumnsAreIdempotentAndNullable() throws IOException {
    Pattern addColumn = Pattern.compile(
      "ADD\\s+COLUMN\\s+(?!IF\\s+NOT\\s+EXISTS)",
      Pattern.CASE_INSENSITIVE
    );
    // NOT NULL sans DEFAULT echoue des que la table contient deja des lignes.
    Pattern unsafeNotNull = Pattern.compile(
      "ADD\\s+COLUMN[^;]*?NOT\\s+NULL(?![^;]*DEFAULT)",
      Pattern.CASE_INSENSITIVE
    );
    List<String> violations = new ArrayList<>();

    for (Path migration : migrationFiles()) {
      String sql = stripComments(
        Files.readString(migration, StandardCharsets.UTF_8)
      );
      if (addColumn.matcher(sql).find()) {
        violations.add(
          migration.getFileName() + " : ADD COLUMN sans IF NOT EXISTS"
        );
      }
      if (unsafeNotNull.matcher(sql).find()) {
        violations.add(
          migration.getFileName() + " : ADD COLUMN NOT NULL sans DEFAULT"
        );
      }
    }

    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("Migrations - At least one migration is present and versioned")
  void migrations_ArePresentAndVersioned() throws IOException {
    List<Path> files = migrationFiles();
    assertThat(files).isNotEmpty();
    assertThat(files)
      .allSatisfy(file ->
        assertThat(file.getFileName().toString()).matches("V\\d+_\\d+__.+\\.sql")
      );
  }

  // Liste les migrations du service, triees pour un diagnostic stable.
  private List<Path> migrationFiles() throws IOException {
    try (Stream<Path> files = Files.list(MIGRATIONS)) {
      return files
        .filter(path -> path.getFileName().toString().endsWith(".sql"))
        .sorted()
        .toList();
    }
  }

  // Les mots-cles cites en commentaire ne doivent pas declencher le garde-fou.
  private String stripComments(String sql) {
    return sql.replaceAll("(?m)--.*$", "");
  }
}
