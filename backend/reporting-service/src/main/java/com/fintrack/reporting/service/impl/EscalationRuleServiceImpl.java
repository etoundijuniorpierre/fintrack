// Service metier : coordonne les operations du domaine escalation rule.

package com.fintrack.reporting.service.impl;

import com.fintrack.reporting.client.audit.AuditServiceClientService;
import com.fintrack.reporting.client.incident.IncidentServiceClientService;
import com.fintrack.reporting.client.notification.BilingualText;
import com.fintrack.reporting.client.notification.NotificationClientService;
import com.fintrack.reporting.client.superadmin.SuperAdminNotificationClientService;
import com.fintrack.reporting.client.superadmin.SuperAdminUserClientService;
import com.fintrack.reporting.model.entity.EscalationRule;
import com.fintrack.reporting.model.entity.EscalationRuleEvent;
import com.fintrack.reporting.model.entity.GeneratedReport;
import com.fintrack.reporting.model.readmodel.EscalationIncident;
import com.fintrack.reporting.repository.EscalationRuleEventRepository;
import com.fintrack.reporting.repository.EscalationRuleRepository;
import com.fintrack.reporting.repository.GeneratedReportRepository;
import com.fintrack.reporting.security.UserDetailsImpl;
import com.fintrack.reporting.service.EscalationRuleService;
import com.fintrack.reporting.service.SystemConfigService;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Implemente les regles metier du domaine escalation rule.

@Slf4j
@Service
@RequiredArgsConstructor
public class EscalationRuleServiceImpl implements EscalationRuleService {

  private static final Set<String> ACTIVE_STATUSES = Set.of(
    "PENDING",
    "VALIDATED",
    "TRANSFERRED",
    "ASSIGNED",
    "IN_PROGRESS"
  );

  public static final String RULE_CRITICAL_UNTREATED = "critical-untreated";
  public static final String RULE_MULTI_TRANSFER = "multi-transfer";
  public static final String RULE_REPORT_FAILED = "automatic-report-failed";
  public static final String RULE_EMAIL_FAILED = "email-failed";
  private static final String RULE_LABEL_PREFIX =
    "notification.escalation.rule.";

  private static final String LEGACY_RULE_REOPENED = "reopened-incident";
  private static final String SECONDARY_OWNER_CRITICAL_UNTREATED =
    "SUPER_ADMIN";

  // Au-dela, on nomme les premiers et on annonce le reste : l'alerte reste lisible.
  private static final int MAX_NAMED_INCIDENTS = 10;

  private final EscalationRuleRepository ruleRepository;
  private final EscalationRuleEventRepository eventRepository;
  private final IncidentServiceClientService incidentService;
  private final SuperAdminNotificationClientService notificationClientService;
  private final SuperAdminUserClientService userClientService;
  private final GeneratedReportRepository reportRepository;
  private final NotificationClientService outboundNotificationClientService;
  private final AuditServiceClientService auditService;
  private final SystemConfigService systemConfigService;
  private final MessageSource messageSource;

  // Demarre ou initialise le traitement applicatif attendu.

  @PostConstruct
  @Transactional
  public void seedRules() {
    try {
      // Indicateur seulement : le mail critique part par incident depuis incident-service.
      seedRule(
        RULE_CRITICAL_UNTREATED,
        "SERVICE_MANAGER",
        false,
        "ESCALATION_CRITICAL_UNTREATED"
      );
      seedRule(
        RULE_MULTI_TRANSFER,
        "SUPER_ADMIN",
        true,
        "ESCALATION_MULTI_TRANSFER"
      );
      seedRule(
        RULE_REPORT_FAILED,
        "SUPER_ADMIN",
        true,
        "ESCALATION_REPORT_FAILED"
      );
      seedRule(
        RULE_EMAIL_FAILED,
        "SUPER_ADMIN",
        true,
        "ESCALATION_EMAIL_FAILED"
      );
      ruleRepository
        .findByRuleKey(LEGACY_RULE_REOPENED)
        .ifPresent(ruleRepository::delete);
    } catch (RuntimeException ex) {
      log.warn(
        "Initialisation des règles d'escalade ignorée : {}",
        ex.getMessage()
      );
    }
  }

  // Demarre ou initialise le traitement applicatif attendu.

  private void seedRule(
    String key,
    String owner,
    boolean automated,
    String notificationType
  ) {
    Locale locale = Locale.ENGLISH;
    EscalationRule rule = ruleRepository.findByRuleKey(key).orElseGet(() -> {
      EscalationRule createdRule = new EscalationRule();
      createdRule.setRuleKey(key);
      createdRule.setOwnerRole(owner);
      createdRule.setEnabled(true);
      createdRule.setAutomated(automated);
      createdRule.setNotificationType(notificationType);
      createdRule.setCreatedAt(LocalDateTime.now());
      createdRule.setUpdatedAt(LocalDateTime.now());
      return createdRule;
    });
    rule.setTriggerLabel(t(ruleTriggerKey(key), null, locale));
    rule.setActionLabel(t(ruleActionKey(key), null, locale));
    rule.setOwnerRole(owner);
    rule.setAutomated(automated);
    rule.setNotificationType(notificationType);
    rule.setUpdatedAt(LocalDateTime.now());
    ruleRepository.save(rule);
  }

  // Fournit rules a la couche appelante.

  @Override
  public List<EscalationRule> getRules() {
    return ruleRepository.findAll(Sort.by(Sort.Direction.ASC, "ruleKey"));
  }

  // Applique le changement demande apres validation metier.

  @Override
  @Transactional
  public EscalationRule toggleRule(
    String key,
    boolean enabled,
    UserDetailsImpl actor
  ) {
    requireSuperAdmin(actor);
    EscalationRule rule = ruleRepository
      .findByRuleKey(key)
      .orElseThrow(() ->
        new IllegalArgumentException(
          t(
            "error.escalation_rule.unknown",
            new Object[] { key },
            LocaleContextHolder.getLocale()
          )
        )
      );
    rule.setEnabled(enabled);
    rule.setUpdatedAt(LocalDateTime.now());
    rule.setModifiedBy(actor.getId());
    EscalationRule saved = ruleRepository.save(rule);
    audit(
      actor,
      "ESCALATION_RULE_TOGGLE",
      "escalation_rule",
      key,
      Map.of("enabled", enabled)
    );
    return saved;
  }

  // Realise l'intention metier evaluate and dispatch.

  @Override
  @Transactional
  public Map<String, Object> evaluateAndDispatch(
    boolean dispatchNotifications
  ) {
    Map<String, Object> outcome = new LinkedHashMap<>();
    long total = 0;
    long emitted = 0;

    List<EscalationIncident> incidents =
      incidentService.getEscalationIncidents();
    List<GeneratedReport> failedAutoReports = reportRepository
      .findAll()
      .stream()
      .filter(report -> "FAILED".equalsIgnoreCase(report.getStatus()))
      .filter(
        report ->
          report.getGenerationType() != null &&
          "AUTOMATIC".equalsIgnoreCase(report.getGenerationType().name())
      )
      .toList();

    long criticalIncidentHours = systemConfigService.getThresholdLong(
      "criticalIncidentHours",
      4
    );
    LocalDateTime criticalCutoff = LocalDateTime.now().minusHours(
      criticalIncidentHours
    );
    List<EscalationIncident> criticalUntreatedIncidents = incidents
      .stream()
      .filter(incident ->
        "CRITICAL".equalsIgnoreCase(incident.getCriticality())
      )
      .filter(
        incident ->
          incident.getStatus() != null &&
          ACTIVE_STATUSES.contains(
            incident.getStatus().toUpperCase(Locale.ROOT)
          )
      )
      .filter(incident -> createdBefore(incident, criticalCutoff))
      .toList();
    long criticalUntreated = criticalUntreatedIncidents.size();

    long maxTransfersBeforeAlert = systemConfigService.getThresholdLong(
      "maxTransfersBeforeAlert",
      2
    );
    List<EscalationIncident> multiTransferredIncidents = incidents
      .stream()
      .filter(
        incident -> incident.getTransferCount() >= maxTransfersBeforeAlert
      )
      .toList();
    long multiTransferred = multiTransferredIncidents.size();
    long failedReports = failedAutoReports.size();
    long failedEmails = notificationClientService.countFailedNotifications();

    emitted += runRule(
      RULE_CRITICAL_UNTREATED,
      criticalUntreated,
      namedIncidents(criticalUntreatedIncidents),
      dispatchNotifications,
      SECONDARY_OWNER_CRITICAL_UNTREATED,
      outcome
    );
    emitted += runRule(
      RULE_MULTI_TRANSFER,
      multiTransferred,
      namedIncidents(multiTransferredIncidents),
      dispatchNotifications,
      null,
      outcome
    );
    emitted += runRule(
      RULE_REPORT_FAILED,
      failedReports,
      List.of(),
      dispatchNotifications,
      null,
      outcome
    );
    emitted += runRule(
      RULE_EMAIL_FAILED,
      failedEmails,
      List.of(),
      dispatchNotifications,
      null,
      outcome
    );

    total = criticalUntreated + multiTransferred + failedReports + failedEmails;
    outcome.put("totalHits", total);
    outcome.put("notificationsEmitted", emitted);
    outcome.put("dispatchedAt", LocalDateTime.now().toString());
    return outcome;
  }

  // Fournit recent events a la couche appelante.

  @Override
  public List<Map<String, Object>> getRecentEvents(int limit) {
    return eventRepository
      .findTop50ByOrderByEvaluatedAtDesc()
      .stream()
      .limit(limit)
      .map(this::toEventRow)
      .toList();
  }

  // Construit la representation attendue pour le domaine escalation rule.

  @Override
  public List<Map<String, Object>> buildRuleSummary() {
    return ruleRepository
      .findAll(Sort.by(Sort.Direction.ASC, "ruleKey"))
      .stream()
      .map(rule -> {
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", rule.getRuleKey());
        row.put("trigger", translatedRuleTrigger(rule, locale));
        row.put("action", translatedRuleAction(rule, locale));
        row.put("owner", rule.getOwnerRole());
        row.put("enabled", rule.isEnabled());
        row.put("automated", rule.isAutomated());
        row.put("lastTriggeredAt", rule.getLastTriggeredAt());
        row.put(
          "currentHits",
          rule.getLastEvaluationCount() == null
            ? 0L
            : rule.getLastEvaluationCount()
        );
        row.put(
          "status",
          rule.isEnabled() && rule.isAutomated() ? "ready" : "needsAutomation"
        );
        row.put("notificationType", rule.getNotificationType());
        return row;
      })
      .toList();
  }

  // Demarre ou initialise le traitement applicatif attendu.

  private long runRule(
    String key,
    long hits,
    List<String> namedIncidents,
    boolean dispatchNotifications,
    String secondaryOwnerRole,
    Map<String, Object> outcome
  ) {
    EscalationRule rule = ruleRepository.findByRuleKey(key).orElse(null);
    if (rule == null) {
      outcome.put(key, Map.of("hits", hits, "status", "unknown"));
      return 0L;
    }
    long emitted = 0;
    rule.setLastEvaluationCount(hits);
    if (hits > 0 && rule.isEnabled() && rule.isAutomated()) {
      rule.setLastTriggeredAt(LocalDateTime.now());
      if (dispatchNotifications) {
        emitted = dispatchEscalation(
          rule,
          hits,
          namedIncidents,
          secondaryOwnerRole
        );
      }
      persistEvent(rule, hits, emitted, "TRIGGERED");
      try {
        auditService.audit(
          null,
          "system",
          List.of("ROLE_SYSTEM"),
          "ESCALATION_RULE_TRIGGER",
          "escalation_rule",
          rule.getRuleKey(),
          "SUCCESS",
          Map.of("hits", hits, "notifications", emitted)
        );
      } catch (RuntimeException ignored) {
        /* best-effort */
      }
    } else if (hits == 0) {
      persistEvent(rule, hits, 0, "CLEAN");
    } else {
      persistEvent(rule, hits, 0, rule.isEnabled() ? "MANUAL" : "DISABLED");
    }
    ruleRepository.save(rule);
    outcome.put(
      key,
      Map.of("hits", hits, "emitted", emitted, "enabled", rule.isEnabled())
    );
    return emitted;
  }

  // Diffuse l'information du domaine escalation rule aux destinataires concernes.

  private long dispatchEscalation(
    EscalationRule rule,
    long hits,
    List<String> namedIncidents,
    String secondaryOwnerRole
  ) {
    List<String> recipients = userClientService.resolveUsernamesByRoles(
      rule.getOwnerRole(),
      secondaryOwnerRole
    );
    long emitted = 0;
    String evaluatedAt = LocalDateTime.now().toString();
    // Les libelles de regle sont traduits : chaque langue recoit les siens.
    BilingualText subject = lmsgPer(
      "notification.escalation.internal.subject",
      locale -> new Object[] { translatedRuleTrigger(rule, locale) }
    );
    BilingualText baseContent = lmsgPer(
      "notification.escalation.internal.content",
      locale -> new Object[] { hits, translatedRuleAction(rule, locale) }
    );
    // Nommer les incidents evite au destinataire d'aller les retrouver un par un.
    BilingualText content = namedIncidents.isEmpty()
      ? baseContent
      : new BilingualText(
        baseContent.fr() +
        " " +
        t(
          "notification.escalation.internal.incidents",
          new Object[] { incidentsLabel(namedIncidents, hits, Locale.FRENCH) },
          Locale.FRENCH
        ),
        baseContent.en() +
        " " +
        t(
          "notification.escalation.internal.incidents",
          new Object[] { incidentsLabel(namedIncidents, hits, Locale.ENGLISH) },
          Locale.ENGLISH
        )
      );

    // Details enrichis exposes au tableau de bord du destinataire.
    Map<String, Object> details = new LinkedHashMap<>();
    details.put("display_escalation_details", "block");
    details.put("ruleKey", rule.getRuleKey());
    details.put("trigger", translatedRuleTrigger(rule, Locale.FRENCH));
    details.put("action", translatedRuleAction(rule, Locale.FRENCH));
    details.put("hits", hits);
    details.put("owner", rule.getOwnerRole());
    details.put("evaluatedAt", evaluatedAt);
    if (!namedIncidents.isEmpty()) {
      details.put("incidents", namedIncidents);
    }

    String dispatchKey =
      "reporting-service:ESCALATION:" +
      rule.getRuleKey() +
      ":" +
      hits +
      ":" +
      rule.getOwnerRole() +
      ":" +
      (secondaryOwnerRole != null ? secondaryOwnerRole : "");
    for (String username : recipients) {
      try {
        outboundNotificationClientService.sendInternal(
          username,
          subject,
          content,
          details,
          dispatchKey + ":INTERNAL:" + username
        );
        emitted++;
      } catch (RuntimeException ex) {
        log.warn(
          "Notification interne d escalade a echoue pour {} vers {}: {}",
          rule.getRuleKey(),
          username,
          ex.toString()
        );
      }
    }
    return emitted;
  }

  // Prepare l'enregistrement de la ressource selon les regles metier.

  private void persistEvent(
    EscalationRule rule,
    long hits,
    long emitted,
    String outcome
  ) {
    EscalationRuleEvent event = new EscalationRuleEvent();
    event.setRuleKey(rule.getRuleKey());
    event.setEvaluatedAt(LocalDateTime.now());
    event.setHitCount(hits);
    event.setNotificationsEmitted(emitted);
    event.setOutcome(outcome);
    event.setCreatedAt(LocalDateTime.now());
    event.setUpdatedAt(LocalDateTime.now());
    eventRepository.save(event);
  }

  // Convertit les donnees du domaine escalation rule entre les modeles utilises.

  private Map<String, Object> toEventRow(EscalationRuleEvent event) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("ruleKey", event.getRuleKey());
    row.put("evaluatedAt", event.getEvaluatedAt());
    row.put("hits", event.getHitCount());
    row.put("notificationsEmitted", event.getNotificationsEmitted());
    row.put("outcome", event.getOutcome());
    return row;
  }

  // Traduit le declencheur technique d'une regle d'escalade.

  private String translatedRuleTrigger(EscalationRule rule, Locale locale) {
    String fallback =
      rule.getTriggerLabel() != null
        ? rule.getTriggerLabel()
        : rule.getRuleKey();
    return messageSource.getMessage(
      ruleTriggerKey(rule.getRuleKey()),
      null,
      fallback,
      locale
    );
  }

  // Traduit l'action technique d'une regle d'escalade.

  private String translatedRuleAction(EscalationRule rule, Locale locale) {
    String fallback =
      rule.getActionLabel() != null ? rule.getActionLabel() : rule.getRuleKey();
    return messageSource.getMessage(
      ruleActionKey(rule.getRuleKey()),
      null,
      fallback,
      locale
    );
  }

  // Reference plus titre : de quoi retrouver l'incident sans ouvrir sa fiche.
  private List<String> namedIncidents(List<EscalationIncident> incidents) {
    return incidents
      .stream()
      .map(incident -> {
        String reference = incident.getReference();
        String title = incident.getTitle();
        if (reference == null || reference.isBlank()) {
          return title == null || title.isBlank() ? null : title;
        }
        return title == null || title.isBlank()
          ? reference
          : reference + " — " + title;
      })
      .filter(Objects::nonNull)
      .toList();
  }

  // Tronquer en silence rendrait l'alerte fausse : le reste est annonce.
  private String incidentsLabel(
    List<String> namedIncidents,
    long hits,
    Locale locale
  ) {
    if (namedIncidents.size() <= MAX_NAMED_INCIDENTS) {
      return String.join(", ", namedIncidents);
    }
    long remaining = hits - MAX_NAMED_INCIDENTS;
    return (
      String.join(", ", namedIncidents.subList(0, MAX_NAMED_INCIDENTS)) +
      " " +
      t(
        "notification.escalation.internal.incidents_more",
        new Object[] { remaining },
        locale
      )
    );
  }

  // Resolut une cle i18n dans la langue demandee.

  private String t(String key, Object[] args, Locale locale) {
    return messageSource.getMessage(key, args, key, locale);
  }

  // Rend une cle avec des arguments propres a chaque langue de l'interface.
  private BilingualText lmsgPer(
    String key,
    Function<Locale, Object[]> argsFor
  ) {
    return new BilingualText(
      t(key, argsFor.apply(Locale.FRENCH), Locale.FRENCH),
      t(key, argsFor.apply(Locale.ENGLISH), Locale.ENGLISH)
    );
  }

  // Realise l'intention metier rule trigger key.

  private String ruleTriggerKey(String ruleKey) {
    return RULE_LABEL_PREFIX + ruleKey + ".trigger";
  }

  // Realise l'intention metier rule action key.

  private String ruleActionKey(String ruleKey) {
    return RULE_LABEL_PREFIX + ruleKey + ".action";
  }

  // Exige super admin.

  private void requireSuperAdmin(UserDetailsImpl actor) {
    if (actor == null) {
      throw new AccessDeniedException(
        t("error.super_admin_required", null, LocaleContextHolder.getLocale())
      );
    }
    boolean ok = actor
      .getAuthorities()
      .stream()
      .map(GrantedAuthority::getAuthority)
      .anyMatch(
        role -> "ROLE_SUPER_ADMIN".equals(role) || "SUPER_ADMIN".equals(role)
      );
    if (!ok) {
      throw new AccessDeniedException(
        t("error.super_admin_required", null, LocaleContextHolder.getLocale())
      );
    }
  }

  // Trace l'action metier realisee sur le domaine escalation rule.

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
      log.warn("Échec de l'audit pour {} : {}", action, ex.toString());
    }
  }

  // Prepare l'enregistrement de la ressource selon les regles metier.

  private boolean createdBefore(
    EscalationIncident incident,
    LocalDateTime cutoff
  ) {
    String raw = incident.getCreatedAt();
    if (raw == null) {
      return true;
    }
    try {
      return LocalDateTime.parse(raw).isBefore(cutoff);
    } catch (RuntimeException ex) {
      try {
        return OffsetDateTime.parse(raw).toLocalDateTime().isBefore(cutoff);
      } catch (RuntimeException e2) {
        return true;
      }
    }
  }
}
