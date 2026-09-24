// Service metier : orchestre la generation, l'acces, l'envoi et les metriques des rapports.

package com.fintrack.reporting.service.impl;

import com.fintrack.reporting.client.audit.AuditServiceClientService;
import com.fintrack.reporting.client.audit.constant.AuditAction;
import com.fintrack.reporting.client.audit.constant.AuditStatus;
import com.fintrack.reporting.client.incident.IncidentServiceClientService;
import com.fintrack.reporting.client.notification.BilingualText;
import com.fintrack.reporting.client.notification.NotificationClientService;
import com.fintrack.reporting.client.user.UserServiceClientService;
import com.fintrack.reporting.client.user.dto.AgencyClientResponse;
import com.fintrack.reporting.client.user.dto.ServiceClientResponse;
import com.fintrack.reporting.client.user.dto.UserClientResponse;
import com.fintrack.reporting.exception.BusinessRuleViolationException;
import com.fintrack.reporting.exception.EntityNotFoundException;
import com.fintrack.reporting.exception.ErrorCode;
import com.fintrack.reporting.model.constant.ReportFormat;
import com.fintrack.reporting.model.constant.ReportContentType;
import com.fintrack.reporting.model.constant.ReportGenerationType;
import com.fintrack.reporting.model.constant.ReportType;
import com.fintrack.reporting.model.entity.GeneratedReport;
import com.fintrack.reporting.model.entity.ReportDownload;
import com.fintrack.reporting.report.ReportDocumentGenerator;
import com.fintrack.reporting.repository.GeneratedReportRepository;
import com.fintrack.reporting.security.UserDetailsImpl;
import com.fintrack.reporting.service.ReportService;
import jakarta.persistence.criteria.Predicate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

// Applique les regles metier des rapports et coordonne les clients necessaires a leur generation.

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

  private final GeneratedReportRepository repository;
  private final AuditServiceClientService auditServiceClientService;
  private final IncidentServiceClientService incidentServiceClientService;
  private final AutomaticReportGroupingService automaticReportGroupingService;
  private final IncidentTypeReportBuilder incidentTypeReportBuilder;
  private final IncidentStatusReportBuilder incidentStatusReportBuilder;
  private final UserServiceClientService userServiceClientService;
  private final ReportDocumentGenerator documentGenerator;
  private final AsyncReportGenerator asyncReportGenerator;
  private final ObjectMapper objectMapper;
  private final NotificationClientService notificationClientService;
  private final MessageSource messageSource;

  @Value("${fintrack.frontend.url:http://localhost:5173}")
  private String frontendUrl;

  private static final String REPORT_VIEW_ALL = "REPORT_VIEW_ALL";
  private static final String REPORT_GENERATE_ALL_SCOPES =
    "REPORT_GENERATE_ALL_SCOPES";
  private static final String REPORT_VIEW_SERVICE = "REPORT_VIEW_SERVICE";
  private static final String REPORT_VIEW_AGENCY = "REPORT_VIEW_AGENCY";
  private static final Pattern EMAIL_PATTERN = Pattern.compile(
    "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
  );

  @Override
  // Liste les rapports que l'utilisateur courant est autorise a consulter.
  public Page<GeneratedReport> findAll(
    UserDetailsImpl currentUser,
    String scope,
    String keyword,
    String type,
    String generationType,
    String format,
    String status,
    Boolean missingFile,
    Pageable pageable
  ) {
    UserDetailsImpl authenticatedUser = requireCurrentUser(currentUser);
    Specification<GeneratedReport> spec = (root, query, cb) -> {
      if (hasAuthority(authenticatedUser, REPORT_VIEW_ALL)) {
        return cb.conjunction();
      }
      if (
        hasAuthority(authenticatedUser, REPORT_VIEW_SERVICE) &&
        authenticatedUser.getServiceId() != null
      ) {
        return cb.equal(
          root.get("serviceId"),
          authenticatedUser.getServiceId()
        );
      }
      if (
        hasAuthority(authenticatedUser, REPORT_VIEW_AGENCY) &&
        authenticatedUser.getAgencyId() != null
      ) {
        return cb.equal(root.get("agencyId"), authenticatedUser.getAgencyId());
      }
      return cb.equal(root.get("createdBy"), authenticatedUser.getId());
    };

    return repository.findAll(
      spec
        .and(scopeSpecification(scope, authenticatedUser))
        .and(
          reportFilterSpecification(
            keyword,
            type,
            generationType,
            format,
            status
          )
        )
        .and(missingFileSpecification(missingFile)),
      pageable
    );
  }

  // Qualite des donnees : rapports disponibles dont le fichier est manquant (lien Super Admin).
  private Specification<GeneratedReport> missingFileSpecification(
    Boolean missingFile
  ) {
    return (root, query, cb) -> {
      if (!Boolean.TRUE.equals(missingFile)) {
        return cb.conjunction();
      }
      return cb.and(
        cb.equal(cb.upper(root.get("status")), "AVAILABLE"),
        cb.or(
          cb.isNull(root.get("filePath")),
          cb.equal(cb.trim(root.get("filePath")), "")
        )
      );
    };
  }

  // Applique les filtres de table au listing pagine des rapports.
  private Specification<GeneratedReport> reportFilterSpecification(
    String keyword,
    String type,
    String generationType,
    String format,
    String status
  ) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (keyword != null && !keyword.isBlank()) {
        String like = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        predicates.add(
          cb.or(
            cb.like(cb.lower(root.get("name")), like),
            cb.like(cb.lower(root.get("status")), like),
            cb.like(cb.lower(root.get("filters")), like)
          )
        );
      }
      parseEnum(ReportType.class, type).ifPresent(value ->
        predicates.add(cb.equal(root.get("type"), value))
      );
      parseEnum(ReportGenerationType.class, generationType).ifPresent(value ->
        predicates.add(cb.equal(root.get("generationType"), value))
      );
      parseEnum(ReportFormat.class, format).ifPresent(value ->
        predicates.add(cb.equal(root.get("format"), value))
      );
      if (status != null && !status.isBlank()) {
        predicates.add(
          cb.equal(
            cb.upper(root.get("status")),
            status.trim().toUpperCase(Locale.ROOT)
          )
        );
      }
      return predicates.isEmpty()
        ? cb.conjunction()
        : cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  // Convertit un filtre texte en enum sans rendre la requete fragile.
  private <E extends Enum<E>> Optional<E> parseEnum(
    Class<E> type,
    String value
  ) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(
        Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT))
      );
    } catch (IllegalArgumentException ignored) {
      return Optional.empty();
    }
  }

  // Restreint la liste aux vues demandees sans elargir les permissions deja appliquees.
  private Specification<GeneratedReport> scopeSpecification(
    String scope,
    UserDetailsImpl user
  ) {
    return (root, query, cb) -> {
      if (scope == null || scope.isBlank()) {
        return cb.conjunction();
      }
      return switch (scope.toLowerCase()) {
        case "own" -> cb.equal(root.get("createdBy"), user.getId());
        case "agency" -> user.getAgencyId() != null
          ? cb.equal(root.get("agencyId"), user.getAgencyId())
          : cb.disjunction();
        case "service" -> user.getServiceId() != null
          ? cb.equal(root.get("serviceId"), user.getServiceId())
          : cb.disjunction();
        default -> cb.conjunction();
      };
    };
  }

  @Override
  // Cree une demande de generation manuelle et declenche le traitement asynchrone.
  public GeneratedReport generate(
    GeneratedReport report,
    UserDetailsImpl currentUser
  ) {
    UserDetailsImpl authenticatedUser = requireCurrentUser(currentUser);
    String requestedScope = extractView(report);
    validateScopeAccess(requestedScope, authenticatedUser);

    report.setStatus("PENDING");
    report.setContentType(ReportContentType.orDefault(report.getContentType()));
    report.setGenerationType(ReportGenerationType.MANUAL);
    report.setCreatedBy(authenticatedUser.getId());
    report.setAgencyId(authenticatedUser.getAgencyId());
    report.setServiceId(authenticatedUser.getServiceId());

    String scope = normalizeView(requestedScope);
    String agencyId = extractFilter(report, "agencyId", "agencies");
    String serviceId = extractFilter(report, "serviceId", "services");
    String subjectUserId = extractFilter(report, "subjectUserId", null);
    boolean global = hasAuthority(
      authenticatedUser,
      REPORT_GENERATE_ALL_SCOPES
    );

    // Libelle de perimetre (nom + page de garde) pour differencier les rapports.
    String scopeLabel = computeScopeLabel(
      scope,
      agencyId,
      serviceId,
      subjectUserId
    );
    storeScopeLabel(report, scopeLabel);
    // Titre explicite (nature du rapport + perimetre + periode) sauf si un nom a
    // deja ete fourni ; sert de sous-titre du document et de nom de fichier.
    if (report.getName() == null || report.getName().isBlank()) {
      report.setName(
        buildReportName(
          contentTypeTitle(report.getContentType()),
          scopeLabel,
          namePeriodLabel(report.getPeriodStart(), report.getPeriodEnd())
        )
      );
    }
    // Multi-selection (>= 2 IDs) : rapport groupe par entite selectionnee.
    int agencyCount = extractFilterListSize(report, "agencyIds");
    int serviceCount = extractFilterListSize(report, "serviceIds");
    int userCount = extractFilterListSize(report, "subjectUserIds");

    if (agencyCount >= 2) {
      automaticReportGroupingService.populate(report, "agency");
    } else if (serviceCount >= 2) {
      automaticReportGroupingService.populate(report, "service");
    } else if (userCount >= 2) {
      automaticReportGroupingService.populate(report, "user");
    } else if ("agency".equals(scope) && agencyId == null && global) {
      automaticReportGroupingService.populate(report, "agency");
    } else if ("service".equals(scope) && serviceId == null && global) {
      automaticReportGroupingService.populate(report, "service");
    } else if ("user".equals(scope) && subjectUserId == null) {
      automaticReportGroupingService.populate(report, "user");
    } else {
      hydrateReportMetrics(report, authenticatedUser);
    }

    GeneratedReport saved = repository.save(report);

    // Generation asynchrone : deleguee a un bean distinct pour que @Async
    // passe bien par le proxy Spring (pas d'auto-invocation).
    asyncReportGenerator.generate(saved.getId());

    return saved;
  }

  @Override
  // Prepare le telechargement d'un rapport genere.
  public ReportDownload download(UUID id, UserDetailsImpl currentUser) {
    UserDetailsImpl authenticatedUser = requireCurrentUser(currentUser);
    GeneratedReport report = repository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException(
          ErrorCode.ENTITY_NOT_FOUND,
          t("error.report.not_found", id)
        )
      );
    assertCanAccess(report, authenticatedUser);

    ReportFormat format =
      report.getFormat() != null ? report.getFormat() : ReportFormat.PDF;
    byte[] content = documentGenerator.generate(report);
    String filename =
      sanitizeFilename(report.getName(), id) +
      "." +
      documentGenerator.fileExtension(format);

    return new ReportDownload(content, filename, format);
  }

  // Produit un nom de fichier stable pour le rapport telecharge.

  private String sanitizeFilename(String name, UUID id) {
    if (name == null || name.isBlank()) {
      return "rapport-" + id;
    }
    // Reutilise la normalisation "slug" (sans accents ni ponctuation) pour un
    // nom de fichier propre a partir du titre lisible du rapport.
    String cleaned = slug(name);
    return "rapport".equals(cleaned) ? "rapport-" + id : cleaned;
  }

  @Override
  // Envoie un rapport genere aux destinataires autorises.
  public void sendEmail(
    UUID id,
    List<String> recipients,
    UserDetailsImpl currentUser
  ) {
    if (currentUser != null) {
      requireCurrentUser(currentUser);
    }

    List<String> normalizedRecipients = normalizeRecipients(recipients);
    if (normalizedRecipients.isEmpty()) {
      throw new BusinessRuleViolationException(
        ErrorCode.BUSINESS_RULE_VIOLATION,
        t("error.report_email.recipient_required")
      );
    }

    GeneratedReport report = repository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException(
          ErrorCode.ENTITY_NOT_FOUND,
          t("error.report.not_found", id)
        )
      );

    if (currentUser != null) {
      assertCanAccess(report, currentUser);
      if (
        !currentUser.getId().equals(report.getCreatedBy()) &&
        !hasAuthority(currentUser, "REPORT_SEND_EMAIL")
      ) {
        throw new AccessDeniedException(
          t("error.report.permission.send_email")
        );
      }
    }

    int failedDeliveries = 0;
    ReportDownload attachment = buildEmailAttachment(report, id);
    String reportVersion = report.getUpdatedAt() != null
      ? report.getUpdatedAt().toString()
      : report.getCreatedAt() != null ? report.getCreatedAt().toString() : "";
    String dispatchKey =
      "reporting-service:REPORT_EMAIL:" +
      id +
      ":" +
      reportVersion +
      ":" +
      String.join(",", normalizedRecipients);
    for (String recipient : normalizedRecipients) {
      BilingualText subject = lmsg(
        "notification.report.created.subject",
        report.getName()
      );
      BilingualText content = lmsg(
        "notification.report.created.content",
        report.getName()
      );

      Map<String, Object> templateParams = new HashMap<>();
      templateParams.put("email_template", "generated_report");
      // Le gabarit e-mail est rendu dans la langue de reference de la plateforme.
      templateParams.put(
        "kind",
        messageSource.getMessage(
          "notification.report.kind",
          null,
          "notification.report.kind",
          Locale.FRENCH
        )
      );
      templateParams.put("subject", subject.fr());
      templateParams.put("subject_color", "#caa22e");
      templateParams.put("message", content.fr());
      templateParams.put("report_name", report.getName());
      templateParams.put("report_format", attachment.getFormat().name());
      templateParams.put("report_url", frontendUrl + "/reports");
      templateParams.put("incident_url", frontendUrl + "/reports");
      templateParams.put(
        "cta_label",
        messageSource.getMessage(
          "notification.report.cta",
          null,
          "notification.report.cta",
          Locale.FRENCH
        )
      );
      templateParams.put("display_incident_details", "none");
      templateParams.put("display_action_details", "none");
      templateParams.put(
        "date",
        LocalDateTime.now().format(
          DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        )
      );
      templateParams.put("attachment_name", attachment.getFilename());
      templateParams.put(
        "attachment_content",
        Base64.getEncoder().encodeToString(attachment.getContent())
      );

      try {
        notificationClientService.sendEmail(
          recipient,
          subject,
          content,
          templateParams,
          dispatchKey + ":EMAIL:" + recipient
        );
        log.info(
          "Notification e-mail envoyée pour le rapport {} à {}",
          id,
          recipient
        );
      } catch (Exception e) {
        log.error(
          "Échec d'envoi de la notification e-mail du rapport {} à {} : {}",
          id,
          recipient,
          e.getMessage()
        );
        failedDeliveries++;
      }
    }
    if (failedDeliveries > 0) {
      throw new BusinessRuleViolationException(
        ErrorCode.BUSINESS_RULE_VIOLATION,
        t("error.report_email.delivery_failed", failedDeliveries)
      );
    }
    rememberEmailRecipients(report, normalizedRecipients);
  }

  // Construit la piece jointe utilisee par l'e-mail de rapport.

  private ReportDownload buildEmailAttachment(GeneratedReport report, UUID id) {
    ReportFormat format =
      report.getFormat() != null ? report.getFormat() : ReportFormat.PDF;
    byte[] content = documentGenerator.generate(report);
    String filename =
      sanitizeFilename(report.getName(), id) +
      "." +
      documentGenerator.fileExtension(format);
    return new ReportDownload(content, filename, format);
  }

  // Memorise les destinataires retenus pour tracer l'envoi du rapport.

  private void rememberEmailRecipients(
    GeneratedReport report,
    List<String> recipients
  ) {
    try {
      report.setEmailRecipients(objectMapper.writeValueAsString(recipients));
      repository.save(report);
    } catch (Exception e) {
      log.warn(
        "Impossible d'enregistrer les destinataires e-mail du rapport {} : {}",
        report.getId(),
        e.getMessage()
      );
    }
  }

  // Normalise et valide la liste de destinataires fournie par l'appelant.

  private List<String> normalizeRecipients(List<String> recipients) {
    if (recipients == null || recipients.isEmpty()) {
      return List.of();
    }

    List<String> normalized = recipients
      .stream()
      .filter(item -> item != null && !item.isBlank())
      .flatMap(item -> Arrays.stream(item.split("[,;\\s]+")))
      .map(String::trim)
      .filter(item -> !item.isBlank())
      .distinct()
      .toList();

    List<String> invalidRecipients = normalized
      .stream()
      .filter(email -> !EMAIL_PATTERN.matcher(email).matches())
      .toList();
    if (!invalidRecipients.isEmpty()) {
      throw new BusinessRuleViolationException(
        ErrorCode.BUSINESS_RULE_VIOLATION,
        t(
          "error.report_email.invalid_recipients",
          String.join(", ", invalidRecipients)
        )
      );
    }
    return normalized;
  }

  // Relance un rapport existant en le replacant dans la file de generation.

  @Override
  public GeneratedReport rerun(UUID id, UserDetailsImpl currentUser) {
    UserDetailsImpl authenticatedUser = requireCurrentUser(currentUser);
    if (
      !hasAuthority(authenticatedUser, "REPORT_VIEW_ALL") &&
      !authenticatedUser
        .getAuthorities()
        .stream()
        .anyMatch(
          a ->
            "ROLE_SUPER_ADMIN".equals(a.getAuthority()) ||
            "SUPER_ADMIN".equals(a.getAuthority())
        )
    ) {
      throw new AccessDeniedException(t("error.report.permission.rerun"));
    }
    GeneratedReport report = repository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException(
          ErrorCode.ENTITY_NOT_FOUND,
          t("error.report.not_found", id)
        )
      );

    report.setStatus("PENDING");
    report.setFileSize(null);
    report.setFilePath(null);
    report.setDownloadUrl(null);
    GeneratedReport saved = repository.save(report);
    asyncReportGenerator.generate(saved.getId());

    auditServiceClientService.audit(
      authenticatedUser.getId(),
      authenticatedUser.getUsername(),
      null,
      AuditAction.REPORT_RERUN.getName(),
      "REPORT",
      id.toString(),
      AuditStatus.SUCCESS.getName(),
      Map.of(
        "previousStatus",
        String.valueOf(report.getStatus()),
        "generationType",
        report.getGenerationType() == null
          ? "MANUAL"
          : report.getGenerationType().name()
      )
    );
    return saved;
  }

  @Override
  // Supprime un rapport (manuel ou automatique) apres controle du proprietaire ou du droit dedie.
  public void delete(UUID id, UserDetailsImpl currentUser) {
    UserDetailsImpl authenticatedUser = requireCurrentUser(currentUser);
    GeneratedReport report = repository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException(
          ErrorCode.ENTITY_NOT_FOUND,
          t("error.report.not_found", id)
        )
      );

    assertCanAccess(report, authenticatedUser);
    if (
      !authenticatedUser.getId().equals(report.getCreatedBy()) &&
      !hasAuthority(authenticatedUser, "REPORT_DELETE")
    ) {
      throw new AccessDeniedException(t("error.report.permission.delete"));
    }

    repository.delete(report);
    log.info(
      "Rapport {} supprimé par l'utilisateur {}",
      id,
      authenticatedUser.getId()
    );

    auditServiceClientService.audit(
      authenticatedUser.getId(),
      authenticatedUser.getUsername(),
      null,
      AuditAction.REPORT_DELETE.getName(),
      "REPORT",
      id.toString(),
      AuditStatus.SUCCESS.getName(),
      Map.of("createdBy", String.valueOf(report.getCreatedBy()))
    );
  }

  // Garantit qu'une operation protegee dispose d'un utilisateur authentifie.

  private UserDetailsImpl requireCurrentUser(UserDetailsImpl currentUser) {
    if (currentUser == null) {
      throw new AccessDeniedException(t("error.authenticated_user_required"));
    }
    return currentUser;
  }

  /**
   * Applique sur un rapport précis la même portée que {@link #findAll} :
   * vue globale, périmètre service/agence correspondant, ou créateur.
   * Empêche le téléchargement/suppression d'un rapport hors périmètre.
   */
  // Verifie que les regles metier autorisent l'operation sur service interne.
  private void assertCanAccess(GeneratedReport report, UserDetailsImpl user) {
    if (hasAuthority(user, REPORT_VIEW_ALL)) {
      return;
    }
    if (
      hasAuthority(user, REPORT_VIEW_SERVICE) &&
      user.getServiceId() != null &&
      user.getServiceId().equals(report.getServiceId())
    ) {
      return;
    }
    if (
      hasAuthority(user, REPORT_VIEW_AGENCY) &&
      user.getAgencyId() != null &&
      user.getAgencyId().equals(report.getAgencyId())
    ) {
      return;
    }
    if (user.getId() != null && user.getId().equals(report.getCreatedBy())) {
      return;
    }
    throw new AccessDeniedException(t("error.report.permission.access"));
  }

  // Determine si l'utilisateur porte l'autorite demandee.

  private boolean hasAuthority(UserDetailsImpl user, String authority) {
    return user
      .getAuthorities()
      .stream()
      .anyMatch(a -> authority.equalsIgnoreCase(a.getAuthority()));
  }

  // Refuse la generation d'un rapport hors du perimetre autorise.

  private void validateScopeAccess(String scope, UserDetailsImpl user) {
    String normalizedScope = normalizeView(scope);
    if (
      normalizedScope == null ||
      normalizedScope.isBlank() ||
      "own".equalsIgnoreCase(normalizedScope)
    ) {
      return;
    }
    if ("all".equalsIgnoreCase(normalizedScope)) {
      if (!hasAuthority(user, REPORT_GENERATE_ALL_SCOPES)) {
        throw new AccessDeniedException(
          t("error.report.permission.generate_global")
        );
      }
      return;
    }
    if ("agency".equalsIgnoreCase(normalizedScope)) {
      if (
        !hasAuthority(user, REPORT_GENERATE_ALL_SCOPES) &&
        !hasAuthority(user, REPORT_VIEW_AGENCY)
      ) {
        throw new AccessDeniedException(
          t("error.report.permission.generate_agency")
        );
      }
      return;
    }
    if ("service".equalsIgnoreCase(normalizedScope)) {
      if (
        !hasAuthority(user, REPORT_GENERATE_ALL_SCOPES) &&
        !hasAuthority(user, REPORT_VIEW_AGENCY) &&
        !hasAuthority(user, REPORT_VIEW_SERVICE)
      ) {
        throw new AccessDeniedException(
          t("error.report.permission.generate_service")
        );
      }
      return;
    }
  }

  // Lit une valeur de filtre (cle principale ou repli) depuis le payload du rapport.
  private String extractFilter(
    GeneratedReport report,
    String primaryKey,
    String fallbackKey
  ) {
    Map<String, Object> payload = readJson(report.getFilters());
    if (!(payload.get("filters") instanceof Map<?, ?> filters)) {
      return null;
    }
    String value = firstString(filters.get(primaryKey));
    if (value == null && fallbackKey != null) {
      value = firstString(filters.get(fallbackKey));
    }
    return value;
  }

  // Compte les elements d'une liste multi-selection dans les filtres du rapport.
  private int extractFilterListSize(GeneratedReport report, String key) {
    Map<String, Object> payload = readJson(report.getFilters());
    if (!(payload.get("filters") instanceof Map<?, ?> filters)) return 0;
    Object value = filters.get(key);
    return value instanceof List<?> list ? list.size() : 0;
  }

  // Libelle lisible du perimetre du rapport (entite nommee ou portee generique).
  private String computeScopeLabel(
    String scope,
    String agencyId,
    String serviceId,
    String subjectUserId
  ) {
    return switch (scope == null ? "" : scope.toLowerCase(Locale.ROOT)) {
      case "agency" -> agencyId != null
        ? agencyName(agencyId)
        : t("report.scope.byAgency");
      case "service" -> serviceId != null
        ? serviceName(serviceId)
        : t("report.scope.byService");
      case "user" -> subjectUserId != null
        ? userName(subjectUserId)
        : t("report.scope.byUser");
      case "all" -> t("report.scope.all");
      default -> t("report.scope.own");
    };
  }

  private String agencyName(String id) {
    return userServiceClientService
      .getAgencies()
      .stream()
      .filter(agency -> id.equals(String.valueOf(agency.getId())))
      .map(AgencyClientResponse::getName)
      .findFirst()
      .orElseGet(() -> t("report.scope.byAgency"));
  }

  private String serviceName(String id) {
    return userServiceClientService
      .getServices()
      .stream()
      .filter(service -> id.equals(String.valueOf(service.getId())))
      .map(ServiceClientResponse::getName)
      .findFirst()
      .orElseGet(() -> t("report.scope.byService"));
  }

  private String userName(String id) {
    return userServiceClientService
      .getReportSubjects()
      .stream()
      .filter(user -> id.equals(String.valueOf(user.getId())))
      .map(this::userDisplayName)
      .findFirst()
      .orElseGet(() -> t("report.scope.byUser"));
  }

  private String userDisplayName(UserClientResponse user) {
    String first = user.getFirstName() == null ? "" : user.getFirstName();
    String last = user.getLastName() == null ? "" : user.getLastName();
    String full = (first + " " + last).trim();
    return full.isBlank() ? user.getUsername() : full;
  }

  // Stocke le libelle de perimetre dans le payload pour la page de garde du PDF.
  private void storeScopeLabel(GeneratedReport report, String scopeLabel) {
    Map<String, Object> payload = readJson(report.getFilters());
    payload.put("scopeLabel", scopeLabel);
    payload.put(
      "documentLanguage",
      LocaleContextHolder.getLocale().toLanguageTag()
    );
    report.setFilters(writeJson(payload));
  }

  // Construit un titre de rapport explicite : nature du rapport + perimetre +
  // periode. Ex. "Situation des incidents - Global - du 01/07/2026 au 10/08/2026".
  static String buildReportName(
    String contentLabel,
    String scopeLabel,
    String periodLabel
  ) {
    StringBuilder name = new StringBuilder(
      contentLabel != null && !contentLabel.isBlank() ? contentLabel : "Rapport"
    );
    if (scopeLabel != null && !scopeLabel.isBlank()) {
      name.append(" - ").append(scopeLabel);
    }
    if (periodLabel != null && !periodLabel.isBlank()) {
      name.append(" - ").append(periodLabel);
    }
    return name.toString();
  }

  // Libelle de nature du rapport, aligne sur le titre de section du document.
  private String contentTypeTitle(ReportContentType contentType) {
    return switch (ReportContentType.orDefault(contentType)) {
      case INCIDENT_TYPE_ANALYSIS -> t("report.typeAnalysis.title");
      case INCIDENT_STATUS_OVERVIEW -> t("report.statusOverview.title");
      default -> t("report.summary.title");
    };
  }

  // Periode lisible pour le titre (dates seules), traduite selon la locale.
  private String namePeriodLabel(
    LocalDateTime periodStart,
    LocalDateTime periodEnd
  ) {
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    String start = periodStart != null ? periodStart.format(fmt) : null;
    String end = periodEnd != null ? periodEnd.format(fmt) : null;
    if (start != null && end != null) {
      return t("report.name.period", start, end);
    }
    if (start != null) {
      return t("report.name.period.from", start);
    }
    if (end != null) {
      return t("report.name.period.to", end);
    }
    return "";
  }

  // Normalise un libelle en fragment de nom de fichier (sans accents ni espaces).
  static String slug(String value) {
    if (value == null || value.isBlank()) {
      return "rapport";
    }
    String normalized = java.text.Normalizer
      .normalize(value, java.text.Normalizer.Form.NFD)
      .replaceAll("\\p{M}", "");
    String slug = normalized
      .toLowerCase(Locale.ROOT)
      .replaceAll("[^a-z0-9]+", "-")
      .replaceAll("(^-|-$)", "");
    return slug.isBlank() ? "rapport" : slug;
  }

  // Enrichit le rapport avec les metriques et incidents fournis par le service incident.

  private void hydrateReportMetrics(
    GeneratedReport report,
    UserDetailsImpl currentUser
  ) {
    ReportIncidentCriteria criteria = buildIncidentCriteria(report, currentUser);
    List<String> requestedMetrics = extractRequestedMetrics(report);
    List<Map<String, Object>> incidents =
      incidentServiceClientService.getIncidentsAsMaps(
        criteria.getView(),
        criteria.getStatuses(),
        criteria.getTypes(),
        criteria.getCriticalities(),
        criteria.getAssignedTo(),
        criteria.getCreatedBy(),
        criteria.getSubjectUserId(),
        criteria.getAgencyId(),
        criteria.getServiceId(),
        criteria.getSubjectUserIds(),
        criteria.getAgencyIds(),
        criteria.getServiceIds(),
        criteria.getStartDate(),
        criteria.getEndDate()
      );

    if (
      ReportContentType.orDefault(report.getContentType()) ==
      ReportContentType.INCIDENT_TYPE_ANALYSIS
    ) {
      report.setMetrics(writeJson(incidentTypeReportBuilder.build(incidents)));
      return;
    }

    if (
      ReportContentType.orDefault(report.getContentType()) ==
      ReportContentType.INCIDENT_STATUS_OVERVIEW
    ) {
      Map<String, Object> statusMetrics = incidentStatusReportBuilder.build(
        incidents
      );
      statusMetrics.put(
        "globalSituation",
        buildGlobalSituation(report, criteria)
      );
      report.setMetrics(writeJson(statusMetrics));
      return;
    }

    Map<String, Object> dashboardMetrics =
      incidentServiceClientService.getDashboardMetricsRaw(
        criteria.getView(),
        criteria.getAgencyId(),
        criteria.getServiceId(),
        criteria.getSubjectUserId(),
        report.getPeriodStart(),
        report.getPeriodEnd()
      );
    Map<String, Object> selectedMetrics = new LinkedHashMap<>();
    Set<String> allowedMetrics = ReportMetricCatalog.allowedFor(
      criteria.getView(),
      criteria.hasUserFilter()
    );

    if (dashboardMetrics != null && !dashboardMetrics.isEmpty()) {
      if (requestedMetrics.isEmpty()) {
        dashboardMetrics.forEach((metric, value) -> {
          if (allowedMetrics.contains(metric)) {
            selectedMetrics.put(metric, value);
          }
        });
      } else {
        requestedMetrics.forEach(metric -> {
          if (
            allowedMetrics.contains(metric) &&
            dashboardMetrics.containsKey(metric)
          ) {
            selectedMetrics.put(metric, dashboardMetrics.get(metric));
          }
        });
      }
    }

    if (criteria.hasUserFilter()) {
      Map<String, Object> filteredMetrics = summarizeIncidents(incidents);
      if (requestedMetrics.isEmpty()) {
        filteredMetrics.forEach((metric, value) -> {
          if (allowedMetrics.contains(metric)) {
            selectedMetrics.put(metric, value);
          }
        });
      } else {
        requestedMetrics.forEach(metric -> {
          if (
            allowedMetrics.contains(metric) &&
            filteredMetrics.containsKey(metric)
          ) {
            selectedMetrics.put(metric, filteredMetrics.get(metric));
          }
        });
      }
    }
    constrainMonthlySeriesToReportPeriod(selectedMetrics, report);
    selectedMetrics.put("incidentsList", incidents);

    report.setMetrics(writeJson(selectedMetrics));
  }

  // Rappelle la situation globale (non traites + retard) et le flux de la periode,
  // au-dela des seuls incidents crees : reprend les agregats du tableau de bord,
  // dans la meme portee (agence/service) et la meme periode que le rapport.
  private Map<String, Object> buildGlobalSituation(
    GeneratedReport report,
    ReportIncidentCriteria criteria
  ) {
    Map<String, Object> dashboard =
      incidentServiceClientService.getDashboardMetricsRaw(
        criteria.getView(),
        criteria.getAgencyId(),
        criteria.getServiceId(),
        criteria.getSubjectUserId(),
        report.getPeriodStart(),
        report.getPeriodEnd()
      );
    // Backlog courant (instantane, hors periode) mis en forme par familles/retards,
    // pour que "reste a traiter" et "en retard" soient adosses a la liste des incidents.
    List<Map<String, Object>> backlog =
      incidentServiceClientService.getIncidentsAsMaps(
        criteria.getView(),
        GlobalSituationMetrics.BACKLOG_STATUSES,
        List.of(),
        List.of(),
        null,
        null,
        criteria.getSubjectUserId(),
        criteria.getAgencyId(),
        criteria.getServiceId(),
        null,
        null
      );
    // Listes adossant les compteurs de flux (traites/resolus/clotures) de la periode,
    // enrichies du drapeau "en retard" comme le backlog.
    Map<String, List<Map<String, Object>>> periodActivity = enrichPeriodActivity(
      incidentServiceClientService.getPeriodActivityAsMaps(
        criteria.getView(),
        criteria.getAgencyId(),
        criteria.getServiceId(),
        criteria.getSubjectUserId(),
        report.getPeriodStart(),
        report.getPeriodEnd()
      )
    );
    return GlobalSituationMetrics.merge(
      dashboard,
      incidentStatusReportBuilder.build(backlog),
      periodActivity
    );
  }

  // Enrichit chaque liste de flux du drapeau "en retard" (meme logique que le backlog).
  private Map<String, List<Map<String, Object>>> enrichPeriodActivity(
    Map<String, List<Map<String, Object>>> periodActivity
  ) {
    Map<String, List<Map<String, Object>>> enriched = new LinkedHashMap<>();
    periodActivity.forEach((key, list) ->
      enriched.put(key, incidentStatusReportBuilder.enrich(list))
    );
    return enriched;
  }

  // Limite les series mensuelles aux mois couverts par une periode d'une meme annee.
  private void constrainMonthlySeriesToReportPeriod(
    Map<String, Object> metrics,
    GeneratedReport report
  ) {
    if (
      report.getPeriodStart() == null ||
      report.getPeriodEnd() == null ||
      report.getPeriodStart().getYear() != report.getPeriodEnd().getYear()
    ) {
      return;
    }
    int firstMonth = report.getPeriodStart().getMonthValue();
    int lastMonth = report.getPeriodEnd().getMonthValue();
    List.of("monthlyClosures", "monthlyAvgClosureHours").forEach(key -> {
      Object value = metrics.get(key);
      if (!(value instanceof Collection<?> rows)) {
        return;
      }
      List<?> filtered = rows
        .stream()
        .filter(row -> {
          if (!(row instanceof Map<?, ?> entry)) {
            return false;
          }
          Object monthValue = entry.get("month");
          if (!(monthValue instanceof Number month)) {
            return false;
          }
          return month.intValue() >= firstMonth && month.intValue() <= lastMonth;
        })
        .toList();
      metrics.put(key, filtered);
    });
  }

  // Calcule les agregats synthetiques a partir de la liste d'incidents filtree.

  private Map<String, Object> summarizeIncidents(
    List<Map<String, Object>> incidents
  ) {
    Map<String, Object> metrics = new LinkedHashMap<>();
    long closed = incidents
      .stream()
      .filter(row -> "CLOSED".equalsIgnoreCase(asText(row.get("status"))))
      .count();
    long rejected = incidents
      .stream()
      .filter(row -> "REJECTED".equalsIgnoreCase(asText(row.get("status"))))
      .count();
    long active = incidents.size() - closed - rejected;

    metrics.put("totalIncidents", incidents.size());
    metrics.put("activeIncidents", Math.max(active, 0));
    metrics.put("closedIncidents", closed);
    metrics.put("rejectedIncidents", rejected);
    metrics.put("avgClosureHours", averageHours(incidents, "closedAt"));
    metrics.put("avgResolutionHours", averageHours(incidents, "resolvedAt"));
    metrics.put("distributionByType", groupCount(incidents, "type"));
    metrics.put(
      "distributionByCriticality",
      groupCount(incidents, "criticality")
    );
    metrics.put("monthlyClosures", monthlyClosureCounts(incidents));
    metrics.put(
      "monthlyAvgClosureHours",
      monthlyAverageClosureHours(incidents)
    );
    return metrics;
  }

  // Regroupe les incidents clotures par mois de cloture.

  private Map<String, Long> monthlyClosureCounts(
    List<Map<String, Object>> incidents
  ) {
    Map<String, Long> counts = new LinkedHashMap<>();
    incidents.forEach(row -> {
      LocalDateTime closedAt = parseDateTime(row.get("closedAt"));
      if (
        closedAt != null &&
        "CLOSED".equalsIgnoreCase(asText(row.get("status")))
      ) {
        counts.merge(
          closedAt.getYear() +
            "-" +
            "%02d".formatted(closedAt.getMonthValue()),
          1L,
          Long::sum
        );
      }
    });
    return counts;
  }

  // Calcule le delai moyen de cloture pour chaque mois de cloture.

  private Map<String, Double> monthlyAverageClosureHours(
    List<Map<String, Object>> incidents
  ) {
    Map<String, List<Double>> durationsByMonth = new LinkedHashMap<>();
    incidents.forEach(row -> {
      LocalDateTime closedAt = parseDateTime(row.get("closedAt"));
      double duration = durationHours(
        row.get("createdAt"),
        row.get("closedAt")
      );
      if (closedAt != null && duration >= 0) {
        String month =
          closedAt.getYear() +
          "-" +
          "%02d".formatted(closedAt.getMonthValue());
        durationsByMonth
          .computeIfAbsent(month, ignored -> new ArrayList<>())
          .add(duration);
      }
    });
    Map<String, Double> averages = new LinkedHashMap<>();
    durationsByMonth.forEach((month, durations) -> {
      double average = durations
        .stream()
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(0);
      averages.put(month, Math.round(average * 10.0) / 10.0);
    });
    return averages;
  }

  // Calcule le delai moyen creation -> jalon demande (closedAt ou resolvedAt).

  private double averageHours(
    List<Map<String, Object>> incidents,
    String milestoneField
  ) {
    List<Double> durations = incidents
      .stream()
      .map(row -> durationHours(row.get("createdAt"), row.get(milestoneField)))
      .filter(value -> value >= 0)
      .toList();
    if (durations.isEmpty()) {
      return 0;
    }
    double average = durations
      .stream()
      .mapToDouble(Double::doubleValue)
      .average()
      .orElse(0);
    return Math.round(average * 10.0) / 10.0;
  }

  // Mesure la duree en heures entre deux dates d'incident exploitables.

  private double durationHours(Object start, Object end) {
    LocalDateTime startedAt = parseDateTime(start);
    LocalDateTime endedAt = parseDateTime(end);
    if (startedAt == null || endedAt == null || endedAt.isBefore(startedAt)) {
      return -1;
    }
    return Duration.between(startedAt, endedAt).toMinutes() / 60.0;
  }

  // Convertit une valeur brute en date exploitable par les metriques.

  private LocalDateTime parseDateTime(Object value) {
    if (value instanceof LocalDateTime localDateTime) {
      return localDateTime;
    }
    if (value instanceof String text && !text.isBlank()) {
      try {
        return LocalDateTime.parse(text.replace("Z", ""));
      } catch (Exception ignored) {
        return null;
      }
    }
    return null;
  }

  // Compte les occurrences d'un champ incident pour les repartitions.

  private Map<String, Long> groupCount(
    List<Map<String, Object>> incidents,
    String key
  ) {
    Map<String, Long> counts = new LinkedHashMap<>();
    incidents.forEach(row -> {
      String label = labelFor(row.get(key));
      counts.merge(label, 1L, Long::sum);
    });
    return counts;
  }

  // Deduit un libelle lisible pour une valeur issue d'un incident.

  @SuppressWarnings("unchecked")
  private String labelFor(Object value) {
    if (value instanceof Map<?, ?> map) {
      Object label = map.get("name");
      if (label == null) {
        label = map.get("displayName");
      }
      if (label == null) {
        label = map.get("typeName");
      }
      if (label == null) {
        label = map.get("id");
      }
      return asText(label);
    }
    return asText(value);
  }

  // Fournit une representation texte stable pour les valeurs absentes ou heterogenes.

  private String asText(Object value) {
    return value == null ? "N/A" : String.valueOf(value);
  }

  // Construit les criteres d'interrogation incident a partir des filtres du rapport.

  // Pour le perimetre "own" sans filtre utilisateur explicite, ancre la liste sur le createur
  // du rapport : la liste et les agregats partagent alors le meme perimetre (et "own" devient
  // deterministe meme hors thread de requete, ou le JWT n'est plus disponible).
  static String resolveOwnScopeCreatedBy(
    String view,
    String createdBy,
    String assignedTo,
    UUID reportCreator
  ) {
    boolean ownScope = "own".equalsIgnoreCase(view);
    boolean noUserFilter =
      (createdBy == null || createdBy.isBlank()) &&
      (assignedTo == null || assignedTo.isBlank());
    if (ownScope && noUserFilter && reportCreator != null) {
      return reportCreator.toString();
    }
    return createdBy;
  }

  // Construit le critere de recuperation des incidents d'un rapport.

  private ReportIncidentCriteria buildIncidentCriteria(
    GeneratedReport report,
    UserDetailsImpl currentUser
  ) {
    String view = normalizeView(extractView(report));
    Map<String, Object> payload = readJson(report.getFilters());
    Map<?, ?> filterMap =
      payload.get("filters") instanceof Map<?, ?> map ? map : Map.of();

    String agencyId = firstString(filterMap.get("agencies"));
    if (agencyId == null) {
      agencyId = firstString(filterMap.get("agencyId"));
    }

    String serviceId = firstString(filterMap.get("services"));
    if (serviceId == null) {
      serviceId = firstString(filterMap.get("serviceId"));
    }

    String assignedTo = firstString(filterMap.get("assignedUsers"));
    if (assignedTo == null) {
      assignedTo = firstString(filterMap.get("assignedTo"));
    }
    String createdBy = firstString(filterMap.get("createdBy"));
    String subjectUserId = firstString(filterMap.get("subjectUserId"));

    // Entite precise (utilisateur / agence / service) : incident-service ne connait
    // pas la vue "user" et la vue "agency/service" se restreint a l'entite du chef.
    // On ouvre sur le perimetre reellement autorise au createur, l'entite ciblee
    // (subjectUserId / agencyId / serviceId) servant ensuite de filtre explicite.
    boolean targetsEntity =
      "user".equalsIgnoreCase(view) ||
      ("agency".equalsIgnoreCase(view) && agencyId != null) ||
      ("service".equalsIgnoreCase(view) && serviceId != null);
    if (targetsEntity) {
      view = resolveCreatorScopeView(currentUser);
    }

    createdBy = resolveOwnScopeCreatedBy(
      view,
      createdBy,
      assignedTo,
      report.getCreatedBy()
    );

    return new ReportIncidentCriteria(
      view,
      stringList(filterMap.get("statuses")),
      stringList(filterMap.get("incidentTypes")),
      stringList(filterMap.get("criticalities")),
      assignedTo,
      createdBy,
      subjectUserId,
      agencyId,
      serviceId,
      stringList(filterMap.get("subjectUserIds")),
      stringList(filterMap.get("agencyIds")),
      stringList(filterMap.get("serviceIds")),
      report.getPeriodStart() != null
        ? report.getPeriodStart().toLocalDate().toString()
        : null,
      report.getPeriodEnd() != null
        ? report.getPeriodEnd().toLocalDate().toString()
        : null
    );
  }

  // Resout le perimetre d'acces aux incidents pour un rapport cible sur une entite
  // precise (utilisateur / agence / service) : la vue la plus large que le createur
  // du rapport est autorise a consulter.
  private String resolveCreatorScopeView(UserDetailsImpl user) {
    if (user == null) {
      return "own";
    }
    if (hasAuthority(user, "INCIDENT_VIEW_ALL")) {
      return "all";
    }
    if (hasAuthority(user, "INCIDENT_VIEW_AGENCY")) {
      return "agency";
    }
    if (hasAuthority(user, "INCIDENT_VIEW_SERVICE")) {
      return "service";
    }
    return "own";
  }

  // Convertit un filtre mono ou multi-valeur en liste de chaines.

  private List<String> stringList(Object value) {
    if (value instanceof List<?> list) {
      return list
        .stream()
        .map(Object::toString)
        .filter(item -> !item.isBlank())
        .toList();
    }
    if (value instanceof String text && !text.isBlank()) {
      return List.of(text);
    }
    return List.of();
  }

  // Extrait la premiere valeur textuelle utile d'un filtre.

  private String firstString(Object value) {
    if (value instanceof List<?> list) {
      return list
        .stream()
        .map(Object::toString)
        .filter(item -> !item.isBlank())
        .findFirst()
        .orElse(null);
    }
    if (value instanceof String text && !text.isBlank()) {
      return text;
    }
    return null;
  }

  // Normalise les variantes historiques de perimetre de rapport.

  private String normalizeView(String view) {
    if (view == null || view.isBlank()) {
      return "own";
    }
    return switch (view.toLowerCase()) {
      case "global", "byglobal" -> "all";
      case "byagency" -> "agency";
      case "byservice" -> "service";
      default -> view;
    };
  }

  // Realise l'intention metier allowed metrics for.

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  // Porte les filtres normalises necessaires a la recuperation des incidents d'un rapport.
  private static class ReportIncidentCriteria {

    private String view;
    private List<String> statuses;
    private List<String> types;
    private List<String> criticalities;
    private String assignedTo;
    private String createdBy;
    // Utilisateur cible : union createur OU assigne, resolue cote incident-service.
    private String subjectUserId;
    private String agencyId;
    private String serviceId;
    // Variantes multi-valeurs (rapport filtre sur plusieurs entites).
    private List<String> subjectUserIds;
    private List<String> agencyIds;
    private List<String> serviceIds;
    private String startDate;
    private String endDate;

    // Indique si le rapport cible un utilisateur precis.

    private boolean hasUserFilter() {
      return (
        (assignedTo != null && !assignedTo.isBlank()) ||
        (createdBy != null && !createdBy.isBlank()) ||
        (subjectUserId != null && !subjectUserId.isBlank()) ||
        (subjectUserIds != null && !subjectUserIds.isEmpty())
      );
    }

  }

  // Extrait le perimetre demande depuis les filtres serialises du rapport.

  private String extractView(GeneratedReport report) {
    Map<String, Object> payload = readJson(report.getFilters());
    Object filters = payload.get("filters");
    if (filters instanceof Map<?, ?> filterMap) {
      Object view = filterMap.get("view");
      if (view instanceof String value && !value.isBlank()) {
        return value;
      }
    }

    Object view = payload.get("view");
    return view instanceof String value && !value.isBlank() ? value : "own";
  }

  // Extrait les metriques explicitement demandees dans les filtres.

  @SuppressWarnings("unchecked")
  private List<String> extractRequestedMetrics(GeneratedReport report) {
    Map<String, Object> payload = readJson(report.getFilters());
    Object metrics = payload.get("requestedMetrics");
    if (metrics instanceof List<?> values) {
      return values
        .stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .toList();
    }
    return List.of();
  }

  // Lit un objet JSON de filtre en conservant un resultat vide en cas d'erreur.

  private Map<String, Object> readJson(String json) {
    if (json == null || json.isBlank()) {
      return new LinkedHashMap<>();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<>() {});
    } catch (Exception e) {
      log.warn(
        "Impossible d'analyser les filtres JSON du rapport : {}",
        e.getMessage()
      );
      return new LinkedHashMap<>();
    }
  }

  // Serialise une valeur de rapport en JSON pour stockage.

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      log.warn(
        "Impossible de sérialiser le contenu du rapport : {}",
        e.getMessage()
      );
      return null;
    }
  }

  // Resout un message localise pour les erreurs metier portees par ce service.
  private String t(String key, Object... args) {
    return messageSource.getMessage(
      key,
      args,
      key,
      LocaleContextHolder.getLocale()
    );
  }

  // Rend une cle dans les deux langues de l'interface ; la cle sert de repli.
  private BilingualText lmsg(String key, Object... args) {
    return new BilingualText(
      messageSource.getMessage(key, args, key, Locale.FRENCH),
      messageSource.getMessage(key, args, key, Locale.ENGLISH)
    );
  }
}
