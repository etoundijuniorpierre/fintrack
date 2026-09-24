// Service metier : coordonne les operations du domaine super-administration.

package com.fintrack.reporting.service.impl;

import com.fintrack.reporting.client.audit.AuditServiceClientService;
import com.fintrack.reporting.client.audit.constant.AuditAction;
import com.fintrack.reporting.client.incident.IncidentServiceClientService;
import com.fintrack.reporting.client.superadmin.SuperAdminHealthClient;
import com.fintrack.reporting.client.superadmin.SuperAdminStatsClientService;
import com.fintrack.reporting.client.superadmin.SuperAdminUserClient;
import com.fintrack.reporting.config.CacheConfig;
import com.fintrack.reporting.model.entity.GeneratedReport;
import com.fintrack.reporting.model.readmodel.superadmin.BackupOutcome;
import com.fintrack.reporting.model.readmodel.superadmin.HealthProbe;
import com.fintrack.reporting.model.readmodel.superadmin.SuperAdminOverviewMeta;
import com.fintrack.reporting.model.readmodel.superadmin.SuperAdminSectionMetadata;
import com.fintrack.reporting.model.readmodel.superadmin.SuperAdminSnapshot;
import com.fintrack.reporting.model.readmodel.superadmin.SystemThresholds;
import com.fintrack.reporting.repository.GeneratedReportRepository;
import com.fintrack.reporting.security.UserDetailsImpl;
import com.fintrack.reporting.service.AuditExportService;
import com.fintrack.reporting.service.BackupService;
import com.fintrack.reporting.service.SuperAdminService;
import com.fintrack.reporting.service.SystemConfigService;
import com.fintrack.reporting.model.dto.response.superadmin.BackupTriggerResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

// Implemente les regles metier du domaine super-administration.

@Service
@Slf4j
public class SuperAdminServiceImpl implements SuperAdminService {

  @Value("${fintrack.superadmin.reports-fetch-size:1000}")
  private int reportsFetchSize;

  @Value("${fintrack.superadmin.stats-sample-size:20}")
  private int statsSampleSize;

  private final IncidentServiceClientService incidentService;
  private final SuperAdminUserClient userClient;
  private final SuperAdminStatsClientService statsClientService;
  private final GeneratedReportRepository reportRepository;
  private final SystemConfigService systemConfigService;
  private final AuditExportService auditExportService;
  private final AuditServiceClientService auditService;
  private final SuperAdminHealthClient healthClient;
  private final BackupService backupService;
  private final MessageSource messageSource;
  private final Map<String, String> serviceUrls;

  @Autowired
  // Initialise le composant avec ses dependances obligatoires.
  public SuperAdminServiceImpl(
    IncidentServiceClientService incidentService,
    SuperAdminUserClient userClient,
    SuperAdminStatsClientService statsClientService,
    GeneratedReportRepository reportRepository,
    SystemConfigService systemConfigService,
    AuditExportService auditExportService,
    AuditServiceClientService auditService,
    SuperAdminHealthClient healthClient,
    BackupService backupService,
    MessageSource messageSource,
    @Value("${user.service.url}") String userServiceUrl,
    @Value("${incident.service.url}") String incidentServiceUrl,
    @Value("${document.service.url}") String documentServiceUrl,
    @Value("${notification.service.url}") String notificationServiceUrl,
    @Value("${spring.application.name}") String applicationName,
    @Value("${audit.service.url}") String auditServiceUrl
  ) {
    this.incidentService = incidentService;
    this.userClient = userClient;
    this.statsClientService = statsClientService;
    this.reportRepository = reportRepository;
    this.systemConfigService = systemConfigService;
    this.auditExportService = auditExportService;
    this.auditService = auditService;
    this.healthClient = healthClient;
    this.backupService = backupService;
    this.messageSource = messageSource;
    LinkedHashMap<String, String> urls = new LinkedHashMap<>();
    urls.put("user", userServiceUrl);
    urls.put("incident", incidentServiceUrl);
    urls.put("document", documentServiceUrl);
    urls.put("notification", notificationServiceUrl);
    urls.put("reporting", "self:" + applicationName);
    urls.put("audit", auditServiceUrl);
    this.serviceUrls = Collections.unmodifiableMap(urls);
  }

  // Fournit gouvernance a la couche appelante.

  @Override
  @Cacheable(
    value = CacheConfig.SUPER_ADMIN_GOVERNANCE_CACHE,
    key = "'governance'"
  )
  public SuperAdminSnapshot getGovernance() {
    long startedAt = System.nanoTime();
    List<String> degraded = new ArrayList<>();
    return SuperAdminSnapshot.builder()
      .incidentMetrics(
        safeCall(
          "incidentMetrics",
          () -> incidentService.getDashboardMetricsOrThrow("all", null, null),
          null,
          degraded
        )
      )
      .incidents(loadIncidents(degraded))
      .users(safeList("users", userClient::getUsers, degraded))
      .userStats(
        safeCall(
          "userStats",
          statsClientService::loadUserStatsOrThrow,
          null,
          degraded
        )
      )
      .agencies(safeList("agencies", userClient::getAgencies, degraded))
      .services(safeList("services", userClient::getServices, degraded))
      .notificationStats(
        safeCall(
          "notificationStats",
          () ->
            statsClientService.loadNotificationStatsOrThrow(statsSampleSize),
          null,
          degraded
        )
      )
      .reports(loadReports(degraded))
      .incidentTypeConfigs(
        safeCall(
          "incidentTypeConfigs",
          incidentService::getIncidentTypeConfigsOrThrow,
          List.of(),
          degraded
        )
      )
      .metadata(sectionMetadata("governance", startedAt, degraded))
      .build();
  }

  // Fournit systeme sante a la couche appelante.

  @Override
  @Cacheable(value = CacheConfig.SUPER_ADMIN_HEALTH_CACHE, key = "'health'")
  public SuperAdminSnapshot getSystemHealth() {
    long startedAt = System.nanoTime();
    List<String> degraded = Collections.synchronizedList(new ArrayList<>());
    Map<String, HealthProbe> probes = probeServices(degraded);

    return SuperAdminSnapshot.builder()
      .auditStats(
        safeCall(
          "auditStats",
          () -> statsClientService.loadAuditStatsOrThrow(statsSampleSize),
          null,
          degraded
        )
      )
      .healthProbes(probes)
      .metadata(sectionMetadata("health", startedAt, degraded))
      .build();
  }

  // Fournit controles qualite a la couche appelante.

  @Override
  @Cacheable(
    value = CacheConfig.SUPER_ADMIN_CONTROLS_CACHE,
    key = "'controls-quality'"
  )
  public SuperAdminSnapshot getControlsQuality() {
    long startedAt = System.nanoTime();
    List<String> degraded = new ArrayList<>();
    return SuperAdminSnapshot.builder()
      .roles(safeList("roles", userClient::getRoles, degraded))
      .permissions(
        safeList("permissions", userClient::getPermissions, degraded)
      )
      .auditStats(
        safeCall(
          "auditStats",
          () -> statsClientService.loadAuditStatsOrThrow(statsSampleSize),
          null,
          degraded
        )
      )
      .incidents(loadIncidents(degraded))
      .users(safeList("users", userClient::getUsers, degraded))
      .agencies(safeList("agencies", userClient::getAgencies, degraded))
      .services(safeList("services", userClient::getServices, degraded))
      .notificationStats(
        safeCall(
          "notificationStats",
          () ->
            statsClientService.loadNotificationStatsOrThrow(statsSampleSize),
          null,
          degraded
        )
      )
      .reports(loadReports(degraded))
      .incidentTypeConfigs(
        safeCall(
          "incidentTypeConfigs",
          incidentService::getIncidentTypeConfigsOrThrow,
          List.of(),
          degraded
        )
      )
      .metadata(sectionMetadata("controls-quality", startedAt, degraded))
      .build();
  }

  // Fournit operations a la couche appelante.

  @Override
  @Cacheable(
    value = CacheConfig.SUPER_ADMIN_OPERATIONS_CACHE,
    key = "'operations'"
  )
  public SuperAdminSnapshot getOperations() {
    long startedAt = System.nanoTime();
    List<String> degraded = new ArrayList<>();
    return SuperAdminSnapshot.builder()
      .notificationStats(
        safeCall(
          "notificationStats",
          () ->
            statsClientService.loadNotificationStatsOrThrow(statsSampleSize),
          null,
          degraded
        )
      )
      .metadata(sectionMetadata("operations", startedAt, degraded))
      .build();
  }

  // Fournit audit vue d ensemble a la couche appelante.

  @Override
  @Cacheable(value = CacheConfig.SUPER_ADMIN_AUDIT_CACHE, key = "'audit'")
  public SuperAdminSnapshot getAuditOverview() {
    long startedAt = System.nanoTime();
    List<String> degraded = new ArrayList<>();
    return SuperAdminSnapshot.builder()
      .auditStats(
        safeCall(
          "auditStats",
          () -> statsClientService.loadAuditStatsOrThrow(statsSampleSize),
          null,
          degraded
        )
      )
      .metadata(sectionMetadata("audit-overview", startedAt, degraded))
      .build();
  }

  // Fournit configuration systeme a la couche appelante.

  @Override
  @Cacheable(value = CacheConfig.SUPER_ADMIN_CONFIG_CACHE, key = "'config'")
  public SuperAdminSnapshot getSystemConfig() {
    long startedAt = System.nanoTime();
    List<String> degraded = new ArrayList<>();
    return SuperAdminSnapshot.builder()
      .thresholds(
        safeCall(
          "thresholds",
          systemConfigService::getThresholds,
          new SystemThresholds(),
          degraded
        )
      )
      .metadata(sectionMetadata("system-config", startedAt, degraded))
      .build();
  }

  // Fournit reporting vue d ensemble a la couche appelante.

  @Override
  @Cacheable(
    value = CacheConfig.SUPER_ADMIN_REPORTING_CACHE,
    key = "'reporting'"
  )
  public SuperAdminSnapshot getReportingOverview() {
    long startedAt = System.nanoTime();
    List<String> degraded = new ArrayList<>();
    return SuperAdminSnapshot.builder()
      .reports(loadReports(degraded))
      .metadata(sectionMetadata("reporting", startedAt, degraded))
      .build();
  }

  // Fournit vue d ensemble a la couche appelante.

  @Override
  @Cacheable(value = CacheConfig.SUPER_ADMIN_OVERVIEW_CACHE, key = "'overview'")
  public SuperAdminSnapshot getOverview() {
    SuperAdminOverviewMeta meta = new SuperAdminOverviewMeta();
    meta.setGeneratedAt(Instant.now().toString());

    long startedAt = System.nanoTime();
    List<String> degraded = Collections.synchronizedList(new ArrayList<>());
    Map<String, HealthProbe> probes = probeServices(degraded);

    return SuperAdminSnapshot.builder()
      .incidentMetrics(
        safeCall(
          "incidentMetrics",
          () -> incidentService.getDashboardMetricsOrThrow("all", null, null),
          null,
          degraded
        )
      )
      .incidents(loadIncidents(degraded))
      .incidentTypeConfigs(
        safeCall(
          "incidentTypeConfigs",
          incidentService::getIncidentTypeConfigsOrThrow,
          List.of(),
          degraded
        )
      )
      .users(safeList("users", userClient::getUsers, degraded))
      .userStats(
        safeCall(
          "userStats",
          statsClientService::loadUserStatsOrThrow,
          null,
          degraded
        )
      )
      .roles(safeList("roles", userClient::getRoles, degraded))
      .agencies(safeList("agencies", userClient::getAgencies, degraded))
      .services(safeList("services", userClient::getServices, degraded))
      .permissions(
        safeList("permissions", userClient::getPermissions, degraded)
      )
      .notificationStats(
        safeCall(
          "notificationStats",
          () ->
            statsClientService.loadNotificationStatsOrThrow(statsSampleSize),
          null,
          degraded
        )
      )
      .auditStats(
        safeCall(
          "auditStats",
          () -> statsClientService.loadAuditStatsOrThrow(statsSampleSize),
          null,
          degraded
        )
      )
      .reports(loadReports(degraded))
      .thresholds(
        safeCall(
          "thresholds",
          systemConfigService::getThresholds,
          new SystemThresholds(),
          degraded
        )
      )
      .healthProbes(probes)
      .overviewMeta(meta)
      .metadata(sectionMetadata("overview", startedAt, degraded))
      .build();
  }

  // Sonde les services en parallele pour que l'onglet Super Admin ne cumule pas les timeouts.
  private Map<String, HealthProbe> probeServices(List<String> degraded) {
    List<CompletableFuture<Map.Entry<String, HealthProbe>>> futures =
      serviceUrls
        .entrySet()
        .stream()
        .map(entry ->
          CompletableFuture.supplyAsync(() -> {
            HealthProbe fallback = new HealthProbe();
            fallback.setStatus("DOWN");
            HealthProbe probe = safeCall(
              "probe_" + entry.getKey(),
              () -> healthClient.probe(entry.getKey(), entry.getValue()),
              fallback,
              degraded
            );
            return Map.entry(entry.getKey(), probe);
          })
        )
        .toList();

    return futures
      .stream()
      .map(CompletableFuture::join)
      .collect(
        Collectors.toMap(
          Map.Entry::getKey,
          Map.Entry::getValue,
          (left, right) -> left,
          LinkedHashMap::new
        )
      );
  }

  @Override
  @Caching(
    evict = {
      @CacheEvict(
        value = CacheConfig.SUPER_ADMIN_OVERVIEW_CACHE,
        allEntries = true
      ),
      @CacheEvict(
        value = CacheConfig.SUPER_ADMIN_CONFIG_CACHE,
        allEntries = true
      ),
    }
  )
  // Applique le changement demande apres validation metier.
  public SuperAdminSnapshot updateThresholds(
    SystemThresholds thresholds,
    UserDetailsImpl actor
  ) {
    SystemThresholds applied = systemConfigService.updateThresholds(
      thresholds,
      actor
    );
    return SuperAdminSnapshot.builder().appliedThresholds(applied).build();
  }

  @Override
  // Fournit les reglages e-mail par evenement (delegue au service de config).
  public com.fintrack.reporting.model.readmodel.superadmin.EmailNotificationSettings getEmailNotificationSettings() {
    return systemConfigService.getEmailNotificationSettings();
  }

  @Override
  // Applique la mise a jour des reglages e-mail (delegue au service de config).
  public com.fintrack.reporting.model.readmodel.superadmin.EmailNotificationSettings updateEmailNotificationSettings(
    com.fintrack.reporting.model.readmodel.superadmin.EmailNotificationSettings settings,
    UserDetailsImpl actor
  ) {
    return systemConfigService.updateEmailNotificationSettings(settings, actor);
  }

  @Override
  // Realise l'intention metier export audit csv.
  public byte[] exportAuditCsv(
    String action,
    String status,
    String from,
    String to,
    Integer limit,
    UserDetailsImpl actor
  ) {
    return auditExportService.exportCsv(action, status, from, to, limit, actor);
  }

  @Override
  @Caching(
    evict = {
      @CacheEvict(
        value = CacheConfig.SUPER_ADMIN_OVERVIEW_CACHE,
        allEntries = true
      ),
      @CacheEvict(
        value = CacheConfig.SUPER_ADMIN_GOVERNANCE_CACHE,
        allEntries = true
      ),
      @CacheEvict(
        value = CacheConfig.SUPER_ADMIN_HEALTH_CACHE,
        allEntries = true
      ),
      @CacheEvict(
        value = CacheConfig.SUPER_ADMIN_CONTROLS_CACHE,
        allEntries = true
      ),
      @CacheEvict(
        value = CacheConfig.SUPER_ADMIN_OPERATIONS_CACHE,
        allEntries = true
      ),
      @CacheEvict(
        value = CacheConfig.SUPER_ADMIN_AUDIT_CACHE,
        allEntries = true
      ),
      @CacheEvict(
        value = CacheConfig.SUPER_ADMIN_CONFIG_CACHE,
        allEntries = true
      ),
      @CacheEvict(
        value = CacheConfig.SUPER_ADMIN_REPORTING_CACHE,
        allEntries = true
      ),
    }
  )
  // Invalide cache.
  public void invalidateCache(UserDetailsImpl actor) {
    log.info(
      "Memoires locales des onglets Super Admin invalidees par {}",
      actor != null ? actor.getUsername() : "system"
    );
    audit(
      actor,
      AuditAction.CACHE_INVALIDATION.getName(),
      "super_admin_cache",
      "all",
      Map.of("action", "invalidate_all")
    );
  }

  // Trace l'action metier realisee sur le domaine super-administration.

  private void audit(
    UserDetailsImpl actor,
    String action,
    String resourceType,
    String resourceId,
    Map<String, Object> details
  ) {
    try {
      UUID userId = actor != null ? actor.getId() : null;
      String username = actor != null ? actor.getUsername() : "system";
      List<String> roles =
        actor != null
          ? actor
              .getAuthorities()
              .stream()
              .map(GrantedAuthority::getAuthority)
              .toList()
          : List.of();
      auditService.audit(
        userId,
        username,
        roles,
        action,
        resourceType,
        resourceId,
        "SUCCESS",
        details
      );
    } catch (RuntimeException ex) {
      log.warn(
        "Échec d'émission de l'audit pour {} sur {} : {}",
        action,
        resourceId,
        ex.toString()
      );
    }
  }

  // Realise l'intention metier section metadata.

  private SuperAdminSectionMetadata sectionMetadata(
    String section,
    long startedAtNanos,
    List<String> degraded
  ) {
    return SuperAdminSectionMetadata.builder()
      .section(section)
      .generatedAt(Instant.now().toString())
      .durationMs(
        Duration.ofNanos(System.nanoTime() - startedAtNanos).toMillis()
      )
      .degraded(degraded)
      .build();
  }

  // Charge reports.

  private List<GeneratedReport> loadReports(List<String> degraded) {
    return safeCall(
      "reports",
      () ->
        reportRepository
          .findAll(
            PageRequest.of(
              0,
              reportsFetchSize,
              Sort.by(Sort.Direction.DESC, "createdAt")
            )
          )
          .getContent(),
      List.of(),
      degraded
    );
  }

  // Charge incidents.

  private List<?> loadIncidents(List<String> degraded) {
    return safeCall(
      "incidents",
      () ->
        incidentService.getIncidentsOrThrow(
          "all",
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null
        ),
      List.of(),
      degraded
    );
  }

  // Fournit une lecture tolerante pour les donnees du domaine super-administration.

  private <T> T safeCall(
    String label,
    Supplier<T> supplier,
    T fallback,
    List<String> degraded
  ) {
    try {
      T result = supplier.get();
      return result == null ? fallback : result;
    } catch (RuntimeException ex) {
      log.warn(
        "Impossible de charger {} pour la vue d'ensemble Super Admin : {}",
        label,
        ex.toString()
      );
      if (degraded != null) {
        degraded.add(label);
      }
      return fallback;
    }
  }

  // Fournit une lecture tolerante pour les donnees du domaine super-administration.

  private <T> List<T> safeList(
    String label,
    Supplier<List<T>> supplier,
    List<String> degraded
  ) {
    return safeCall(label, supplier, List.of(), degraded);
  }

  @Override
  public BackupTriggerResponse triggerBackup(UserDetailsImpl actor) {
    UUID userId = actor != null ? actor.getId() : null;
    log.info("SuperAdmin [{}] a déclenché une sauvegarde manuelle du système.", userId);

    // Les statuts proviennent des codes de sortie reels des outils : un echec
    // remonte comme un echec, jamais comme un succes silencieux.
    BackupOutcome outcome = backupService.run();

    try {
      String auditDetails = getMessage("superadmin.backup.audit_details", null, "Déclenchement manuel de la sauvegarde système et de la synchronisation B2");
      auditService.audit(
        userId,
        null,
        null,
        AuditAction.SYSTEM_BACKUP.getName(),
        "SYSTEM",
        "BACKUP_" + outcome.getTimestamp(),
        outcome.isSuccess() ? "SUCCESS" : "FAILURE",
        Map.of("description", auditDetails)
      );
    } catch (Exception ex) {
      log.warn("Impossible d'enregistrer le log d'audit pour le backup : {}", ex.getMessage());
    }

    return BackupTriggerResponse.builder()
      .success(outcome.isSuccess())
      .message(outcome.getMessage())
      .timestamp(outcome.getTimestamp())
      .postgresStatus(outcome.getPostgresStatus())
      .mongoStatus(outcome.getMongoStatus())
      .b2SyncStatus(outcome.getB2SyncStatus())
      .backupDirectory(outcome.getBackupDirectory())
      .build();
  }

  private String getMessage(String key, Object[] args, String defaultMessage) {
    if (messageSource == null) {
      return defaultMessage;
    }
    try {
      return messageSource.getMessage(key, args, defaultMessage, LocaleContextHolder.getLocale());
    } catch (Exception ex) {
      return defaultMessage;
    }
  }
}
