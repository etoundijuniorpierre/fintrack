// Service metier : coordonne les operations du domaine configuration systeme.

package com.fintrack.reporting.service.impl;

import com.fintrack.reporting.client.audit.AuditServiceClientService;
import com.fintrack.common.notification.EmailNotificationEvent;
import com.fintrack.reporting.model.entity.SystemSetting;
import com.fintrack.reporting.model.readmodel.superadmin.EmailNotificationEventSetting;
import com.fintrack.reporting.model.readmodel.superadmin.EmailNotificationSettings;
import com.fintrack.reporting.model.readmodel.superadmin.SystemThresholds;
import com.fintrack.reporting.repository.SystemSettingRepository;
import com.fintrack.reporting.security.UserDetailsImpl;
import com.fintrack.reporting.service.SystemConfigService;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Implemente les regles metier du domaine configuration systeme.

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

  public static final String CACHE_NAME = "systemConfig";

  private static final Map<String, Object> DEFAULT_THRESHOLDS;

  static {
    // Stable UI order.
    Map<String, Object> defaults = new LinkedHashMap<>();
    defaults.put("defaultSlaHours", 24);
    defaults.put("criticalIncidentHours", 4);
    defaults.put("slaReminderIntervalHours", 24);
    defaults.put("maxTransfersBeforeAlert", 2);
    defaults.put("notificationMaxRetryCount", 5);
    defaults.put("loginMaxFailedAttempts", 5);
    defaults.put("tempPasswordValidityMinutes", 30);
    defaults.put("escalationScanIntervalMinutes", 15);
    defaults.put("maxReopenCount", 2);
    defaults.put("reopenTimeLimitHours", 48);
    defaults.put("autoBlockOverdueWorkingDays", 7);
    defaults.put("blockedReminderIntervalDays", 7);
    defaults.put("criticalReminderIntervalHours", 4);
    defaults.put("prolongedWaitDays", 30);
    defaults.put("validationDelayHours", 48);
    defaults.put("validationReminderEnabled", 1);
    defaults.put("serviceManagerSelfValidationEnabled", 0);
    defaults.put("pendingActionReminderIntervalHours", 4);
    defaults.put("pendingActionInternalEnabled", 1);
    defaults.put("reportRetentionDays", 20);
    defaults.put("backupScheduleEnabled", 1);
    defaults.put("backupScheduleHour", 20);
    DEFAULT_THRESHOLDS = Collections.unmodifiableMap(defaults);
  }

  // Bounds applied to every editable threshold.
  private static final Map<String, long[]> THRESHOLD_BOUNDS;

  static {
    Map<String, long[]> bounds = new LinkedHashMap<>();
    bounds.put("defaultSlaHours", new long[] { 1, 720 });
    bounds.put("criticalIncidentHours", new long[] { 1, 168 });
    bounds.put("slaReminderIntervalHours", new long[] { 1, 168 });
    bounds.put("maxTransfersBeforeAlert", new long[] { 1, 20 });
    bounds.put("notificationMaxRetryCount", new long[] { 1, 20 });
    bounds.put("loginMaxFailedAttempts", new long[] { 1, 20 });
    bounds.put("tempPasswordValidityMinutes", new long[] { 1, 1440 });
    bounds.put("escalationScanIntervalMinutes", new long[] { 1, 1440 });
    bounds.put("maxReopenCount", new long[] { 0, 10 });
    bounds.put("reopenTimeLimitHours", new long[] { 1, 720 });
    bounds.put("autoBlockOverdueWorkingDays", new long[] { 1, 60 });
    bounds.put("blockedReminderIntervalDays", new long[] { 1, 90 });
    bounds.put("criticalReminderIntervalHours", new long[] { 1, 168 });
    bounds.put("prolongedWaitDays", new long[] { 1, 365 });
    bounds.put("validationDelayHours", new long[] { 1, 720 });
    bounds.put("validationReminderEnabled", new long[] { 0, 1 });
    bounds.put("serviceManagerSelfValidationEnabled", new long[] { 0, 1 });
    bounds.put("pendingActionReminderIntervalHours", new long[] { 1, 720 });
    bounds.put("pendingActionInternalEnabled", new long[] { 0, 1 });
    bounds.put("reportRetentionDays", new long[] { 1, 3650 });
    bounds.put("backupScheduleEnabled", new long[] { 0, 1 });
    bounds.put("backupScheduleHour", new long[] { 0, 23 });
    THRESHOLD_BOUNDS = Collections.unmodifiableMap(bounds);
  }

  private static final Set<String> ALLOWED_THRESHOLD_KEYS =
    DEFAULT_THRESHOLDS.keySet();

  private final SystemSettingRepository repository;
  private final AuditServiceClientService auditService;
  private final MessageSource messageSource;
  private final ObjectMapper objectMapper;

  private String t(String key, Object... args) {
    return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
  }

  // Demarre ou initialise le traitement applicatif attendu.

  @PostConstruct
  public void seedDefaults() {
    try {
      DEFAULT_THRESHOLDS.forEach((key, value) ->
        seed(key, String.valueOf(value), SystemSetting.CATEGORY_THRESHOLD)
      );
      // Reglages e-mail : par defaut tout est actif (comportement inchange),
      // aucune exclusion. Le detail par evenement reste pilotable dans l'UI.
      for (EmailNotificationEvent event : EmailNotificationEvent.values()) {
        seed(
          event.enabledKey(),
          "true",
          SystemSetting.CATEGORY_EMAIL_NOTIFICATION
        );
        if (event.isSupportsExclusion()) {
          seed(
            event.excludedKey(),
            "[]",
            SystemSetting.CATEGORY_EMAIL_NOTIFICATION
          );
        }
      }
      // Cleanup idempotent des seuils devenus orphelins (borne a la categorie
      // THRESHOLD pour ne jamais toucher les reglages e-mail ni d'autres familles).
      List<SystemSetting> orphans = repository
        .findAll()
        .stream()
        .filter(setting ->
          SystemSetting.CATEGORY_THRESHOLD.equals(setting.getCategory())
        )
        .filter(setting -> {
          String k = setting.getSettingKey();
          return !ALLOWED_THRESHOLD_KEYS.contains(k);
        })
        .toList();
      if (!orphans.isEmpty()) {
        log.info(
          "Nettoyage des paramètres système : suppression de {} clé(s) orpheline(s) : {}",
          orphans.size(),
          orphans.stream().map(SystemSetting::getSettingKey).toList()
        );
        repository.deleteAll(orphans);
      }
    } catch (RuntimeException ex) {
      log.warn(
        "Initialisation des paramètres système ignorée : {}",
        ex.getMessage()
      );
    }
  }

  // Fournit seuils a la couche appelante.

  @Override
  @Cacheable(value = CACHE_NAME, key = "'thresholds'")
  public SystemThresholds getThresholds() {
    SystemThresholds out = new SystemThresholds();
    out.setDefaultSlaHours(getThresholdLongInternal("defaultSlaHours", 24));
    out.setCriticalIncidentHours(
      getThresholdLongInternal("criticalIncidentHours", 4)
    );
    out.setSlaReminderIntervalHours(
      getThresholdLongInternal("slaReminderIntervalHours", 24)
    );
    out.setMaxTransfersBeforeAlert(
      getThresholdLongInternal("maxTransfersBeforeAlert", 2)
    );
    out.setNotificationMaxRetryCount(
      getThresholdLongInternal("notificationMaxRetryCount", 5)
    );
    out.setLoginMaxFailedAttempts(
      getThresholdLongInternal("loginMaxFailedAttempts", 5)
    );
    out.setTempPasswordValidityMinutes(
      getThresholdLongInternal("tempPasswordValidityMinutes", 30)
    );
    out.setEscalationScanIntervalMinutes(
      getThresholdLongInternal("escalationScanIntervalMinutes", 15)
    );
    out.setMaxReopenCount(getThresholdLongInternal("maxReopenCount", 2));
    out.setReopenTimeLimitHours(
      getThresholdLongInternal("reopenTimeLimitHours", 48)
    );
    out.setAutoBlockOverdueWorkingDays(
      getThresholdLongInternal("autoBlockOverdueWorkingDays", 7)
    );
    out.setBlockedReminderIntervalDays(
      getThresholdLongInternal("blockedReminderIntervalDays", 7)
    );
    out.setCriticalReminderIntervalHours(
      getThresholdLongInternal("criticalReminderIntervalHours", 4)
    );
    out.setProlongedWaitDays(
      getThresholdLongInternal("prolongedWaitDays", 30)
    );
    out.setValidationDelayHours(
      getThresholdLongInternal("validationDelayHours", 48)
    );
    out.setValidationReminderEnabled(
      getThresholdLongInternal("validationReminderEnabled", 1)
    );
    out.setServiceManagerSelfValidationEnabled(
      getThresholdLongInternal("serviceManagerSelfValidationEnabled", 0)
    );
    out.setPendingActionReminderIntervalHours(
      getThresholdLongInternal("pendingActionReminderIntervalHours", 4)
    );
    out.setPendingActionInternalEnabled(
      getThresholdLongInternal("pendingActionInternalEnabled", 1)
    );
    out.setReportRetentionDays(
      getThresholdLongInternal("reportRetentionDays", 20)
    );
    out.setBackupScheduleEnabled(
      getThresholdLongInternal("backupScheduleEnabled", 1)
    );
    out.setBackupScheduleHour(
      getThresholdLongInternal("backupScheduleHour", 20)
    );
    return out;
  }

  // Fournit seuil long interne a la couche appelante.

  private Long getThresholdLongInternal(String key, long fallback) {
    return repository
      .findBySettingKey(key)
      .map(setting -> {
        Object parsed = parseThreshold(setting.getSettingValue(), fallback);
        return parsed instanceof Number
          ? ((Number) parsed).longValue()
          : fallback;
      })
      .orElse(fallback);
  }

  // Fournit seuil long a la couche appelante.

  @Override
  public long getThresholdLong(String key, long fallback) {
    return getThresholdLongInternal(key, fallback);
  }

  // Applique le changement demande apres validation metier.

  @Override
  @Transactional
  @CacheEvict(value = CACHE_NAME, allEntries = true)
  public SystemThresholds updateThresholds(
    SystemThresholds thresholds,
    UserDetailsImpl actor
  ) {
    requireSuperAdmin(actor);
    if (thresholds == null) {
      return getThresholds();
    }
    Map<String, Long> thresholdsMap = new LinkedHashMap<>();
    if (thresholds.getDefaultSlaHours() != null) thresholdsMap.put(
      "defaultSlaHours",
      thresholds.getDefaultSlaHours()
    );
    if (thresholds.getCriticalIncidentHours() != null) thresholdsMap.put(
      "criticalIncidentHours",
      thresholds.getCriticalIncidentHours()
    );
    if (thresholds.getSlaReminderIntervalHours() != null) thresholdsMap.put(
      "slaReminderIntervalHours",
      thresholds.getSlaReminderIntervalHours()
    );
    if (thresholds.getMaxTransfersBeforeAlert() != null) thresholdsMap.put(
      "maxTransfersBeforeAlert",
      thresholds.getMaxTransfersBeforeAlert()
    );
    if (thresholds.getNotificationMaxRetryCount() != null) thresholdsMap.put(
      "notificationMaxRetryCount",
      thresholds.getNotificationMaxRetryCount()
    );
    if (thresholds.getLoginMaxFailedAttempts() != null) thresholdsMap.put(
      "loginMaxFailedAttempts",
      thresholds.getLoginMaxFailedAttempts()
    );
    if (thresholds.getTempPasswordValidityMinutes() != null) thresholdsMap.put(
      "tempPasswordValidityMinutes",
      thresholds.getTempPasswordValidityMinutes()
    );
    if (
      thresholds.getEscalationScanIntervalMinutes() != null
    ) thresholdsMap.put(
      "escalationScanIntervalMinutes",
      thresholds.getEscalationScanIntervalMinutes()
    );
    if (thresholds.getMaxReopenCount() != null) thresholdsMap.put(
      "maxReopenCount",
      thresholds.getMaxReopenCount()
    );
    if (thresholds.getReopenTimeLimitHours() != null) thresholdsMap.put(
      "reopenTimeLimitHours",
      thresholds.getReopenTimeLimitHours()
    );
    if (thresholds.getAutoBlockOverdueWorkingDays() != null) thresholdsMap.put(
      "autoBlockOverdueWorkingDays",
      thresholds.getAutoBlockOverdueWorkingDays()
    );
    if (thresholds.getBlockedReminderIntervalDays() != null) thresholdsMap.put(
      "blockedReminderIntervalDays",
      thresholds.getBlockedReminderIntervalDays()
    );
    if (thresholds.getCriticalReminderIntervalHours() != null) thresholdsMap.put(
      "criticalReminderIntervalHours",
      thresholds.getCriticalReminderIntervalHours()
    );
    if (thresholds.getProlongedWaitDays() != null) thresholdsMap.put(
      "prolongedWaitDays",
      thresholds.getProlongedWaitDays()
    );
    if (thresholds.getReportRetentionDays() != null) thresholdsMap.put(
      "reportRetentionDays",
      thresholds.getReportRetentionDays()
    );
    if (thresholds.getBackupScheduleEnabled() != null) thresholdsMap.put(
      "backupScheduleEnabled",
      thresholds.getBackupScheduleEnabled()
    );
    if (thresholds.getBackupScheduleHour() != null) thresholdsMap.put(
      "backupScheduleHour",
      thresholds.getBackupScheduleHour()
    );

    if (thresholds.getServiceManagerSelfValidationEnabled() != null) thresholdsMap.put(
      "serviceManagerSelfValidationEnabled",
      thresholds.getServiceManagerSelfValidationEnabled()
    );

    Map<String, Object> applied = new LinkedHashMap<>();
    Map<String, Map<String, Object>> delta = new LinkedHashMap<>();

    thresholdsMap.forEach((key, longValue) -> {
      if (!ALLOWED_THRESHOLD_KEYS.contains(key)) {
        throw new IllegalArgumentException(t("reporting.error.unknown_threshold_key", key));
      }
      if (longValue < 0) {
        throw new IllegalArgumentException(t("reporting.error.threshold_must_be_positive", key));
      }
      long[] bounds = THRESHOLD_BOUNDS.get(key);
      if (bounds != null && (longValue < bounds[0] || longValue > bounds[1])) {
        throw new IllegalArgumentException(
          t("reporting.error.threshold_out_of_bounds", key, bounds[0], bounds[1])
        );
      }

      // Seuil jamais enregistre : la valeur en vigueur etait celle par defaut, pas
      // un sentinelle. L'audit doit nommer ce que l'utilisateur voyait reellement.
      Object defaultValue = DEFAULT_THRESHOLDS.get(key);
      long prevLongValue = getThresholdLongInternal(
        key,
        defaultValue instanceof Number number ? number.longValue() : 0L
      );

      if (prevLongValue != longValue) {
        delta.put(key, Map.of("from", prevLongValue, "to", longValue));

        SystemSetting setting = repository
          .findBySettingKey(key)
          .orElseGet(SystemSetting::new);
        setting.setSettingKey(key);
        setting.setCategory(SystemSetting.CATEGORY_THRESHOLD);
        setting.setSettingValue(String.valueOf(longValue));
        setting.setModifiedBy(actor.getId());
        if (setting.getCreatedAt() == null) {
          setting.setCreatedAt(LocalDateTime.now());
        }
        setting.setUpdatedAt(LocalDateTime.now());
        repository.save(setting);
      }
      applied.put(key, longValue);
    });

    if (!delta.isEmpty()) {
      audit(
        actor,
        "SETTINGS_CHANGE",
        "system_settings",
        "thresholds",
        Map.of("applied", applied, "delta", delta)
      );
    }
    return getThresholds();
  }

  // Fournit les reglages e-mail par evenement a la couche appelante.
  @Override
  @Cacheable(value = CACHE_NAME, key = "'emailNotifications'")
  public EmailNotificationSettings getEmailNotificationSettings() {
    List<EmailNotificationEventSetting> events = new ArrayList<>();
    for (EmailNotificationEvent event : EmailNotificationEvent.values()) {
      events.add(
        EmailNotificationEventSetting.builder()
          .event(event.name())
          .category(event.getCategory().name())
          .enabled(readEnabled(event))
          .supportsExclusion(event.isSupportsExclusion())
          .excludedUserIds(
            event.isSupportsExclusion() ? readExcluded(event) : List.of()
          )
          .build()
      );
    }
    return EmailNotificationSettings.builder().events(events).build();
  }

  // Applique la mise a jour des reglages e-mail apres validation metier.
  @Override
  @Transactional
  @CacheEvict(value = CACHE_NAME, allEntries = true)
  public EmailNotificationSettings updateEmailNotificationSettings(
    EmailNotificationSettings settings,
    UserDetailsImpl actor
  ) {
    requireSuperAdmin(actor);
    if (settings == null || settings.getEvents() == null) {
      return getEmailNotificationSettings();
    }
    Map<String, Object> delta = new LinkedHashMap<>();
    for (EmailNotificationEventSetting incoming : settings.getEvents()) {
      EmailNotificationEvent event = EmailNotificationEvent.fromName(
        incoming.getEvent()
      );
      if (event == null) {
        throw new IllegalArgumentException(
          t("reporting.error.unknown_email_event", incoming.getEvent())
        );
      }
      boolean prevEnabled = readEnabled(event);
      if (prevEnabled != incoming.isEnabled()) {
        saveSetting(
          event.enabledKey(),
          Boolean.toString(incoming.isEnabled()),
          actor
        );
        delta.put(
          event.enabledKey(),
          Map.of("from", prevEnabled, "to", incoming.isEnabled())
        );
      }
      if (event.isSupportsExclusion()) {
        List<UUID> prevExcluded = readExcluded(event);
        List<UUID> nextExcluded = sanitizeExcluded(incoming.getExcludedUserIds());
        if (!prevExcluded.equals(nextExcluded)) {
          saveSetting(event.excludedKey(), writeUuids(nextExcluded), actor);
          delta.put(
            event.excludedKey(),
            Map.of("from", prevExcluded, "to", nextExcluded)
          );
        }
      }
    }
    if (!delta.isEmpty()) {
      audit(
        actor,
        "SETTINGS_CHANGE",
        "system_settings",
        "emailNotifications",
        Map.of("delta", delta)
      );
    }
    return getEmailNotificationSettings();
  }

  // Lit l'interrupteur "enabled" d'un evenement (defaut porte par l'evenement).
  private boolean readEnabled(EmailNotificationEvent event) {
    return repository
      .findBySettingKey(event.enabledKey())
      .map(setting -> !"false".equalsIgnoreCase(setting.getSettingValue()))
      .orElseGet(event::isEnabledByDefault);
  }

  // Lit la liste d'exclusion (JSON) d'un evenement ; liste vide si absente/illisible.
  private List<UUID> readExcluded(EmailNotificationEvent event) {
    return repository
      .findBySettingKey(event.excludedKey())
      .map(setting -> parseUuids(setting.getSettingValue()))
      .orElseGet(List::of);
  }

  // Deserialise une liste d'UUID depuis le JSON stocke.
  private List<UUID> parseUuids(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    try {
      List<UUID> ids = objectMapper.readValue(
        raw,
        new TypeReference<List<UUID>>() {}
      );
      return ids != null ? ids : List.of();
    } catch (RuntimeException ex) {
      log.warn(
        "Liste d'exclusion e-mail illisible ({}), repli sur vide : {}",
        raw,
        ex.getMessage()
      );
      return List.of();
    }
  }

  // Normalise une liste d'exclusion : sans doublon ni valeur nulle, ordre stable.
  private List<UUID> sanitizeExcluded(List<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    List<UUID> cleaned = new ArrayList<>();
    for (UUID id : ids) {
      if (id != null && !cleaned.contains(id)) {
        cleaned.add(id);
      }
    }
    return cleaned;
  }

  // Serialise une liste d'UUID pour stockage.
  private String writeUuids(List<UUID> ids) {
    try {
      return objectMapper.writeValueAsString(ids);
    } catch (RuntimeException ex) {
      return "[]";
    }
  }

  // Cree ou met a jour un parametre e-mail.
  private void saveSetting(String key, String value, UserDetailsImpl actor) {
    SystemSetting setting = repository
      .findBySettingKey(key)
      .orElseGet(SystemSetting::new);
    setting.setSettingKey(key);
    setting.setCategory(SystemSetting.CATEGORY_EMAIL_NOTIFICATION);
    setting.setSettingValue(value);
    setting.setModifiedBy(actor != null ? actor.getId() : null);
    if (setting.getCreatedAt() == null) {
      setting.setCreatedAt(LocalDateTime.now());
    }
    setting.setUpdatedAt(LocalDateTime.now());
    repository.save(setting);
  }

  // Demarre ou initialise le traitement applicatif attendu.

  private void seed(String key, String value, String category) {
    repository.findBySettingKey(key).orElseGet(() -> {
      SystemSetting setting = new SystemSetting();
      setting.setSettingKey(key);
      setting.setCategory(category);
      setting.setSettingValue(value);
      setting.setCreatedAt(LocalDateTime.now());
      setting.setUpdatedAt(LocalDateTime.now());
      return repository.save(setting);
    });
  }

  // Exige super admin.

  private void requireSuperAdmin(UserDetailsImpl actor) {
    if (actor == null) {
      throw new AccessDeniedException(t("reporting.error.super_admin_authenticated_required"));
    }
    boolean isSuper = actor
      .getAuthorities()
      .stream()
      .map(GrantedAuthority::getAuthority)
      .anyMatch(
        role -> "ROLE_SUPER_ADMIN".equals(role) || "SUPER_ADMIN".equals(role)
      );
    if (!isSuper) {
      throw new AccessDeniedException(t("reporting.error.super_admin_role_required"));
    }
  }

  // Trace l'action metier realisee sur le domaine configuration systeme.

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

  // Lit les donnees brutes du domaine configuration systeme dans un format exploitable.

  private Object parseThreshold(String raw, Object fallback) {
    if (raw == null || raw.isBlank()) return fallback;
    try {
      return Long.parseLong(raw);
    } catch (NumberFormatException ex) {
      return raw;
    }
  }

  // Realise l'intention metier coerce long.

  private long coerceLong(Object value) {
    if (value instanceof Number number) return number.longValue();
    if (value == null) throw new IllegalArgumentException(
      t("reporting.error.threshold_value_required")
    );
    try {
      return Long.parseLong(value.toString());
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(
        t("reporting.error.threshold_must_be_number", value)
      );
    }
  }
}
