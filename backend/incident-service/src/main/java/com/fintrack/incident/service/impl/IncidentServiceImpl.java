// Service metier : coordonne les operations du domaine incident.

package com.fintrack.incident.service.impl;

import com.fintrack.incident.client.audit.AuditServiceClientService;
import com.fintrack.incident.client.audit.constant.AuditAction;
import com.fintrack.incident.client.audit.constant.AuditStatus;
import com.fintrack.incident.client.notification.NotificationClientService;
import com.fintrack.incident.client.reporting.ReportingSystemConfigClientService;
import com.fintrack.incident.client.user.UserClientService;
import com.fintrack.incident.exception.BusinessRuleViolationException;
import com.fintrack.incident.exception.EntityNotFoundException;
import com.fintrack.incident.exception.ErrorCode;
import com.fintrack.incident.exception.InvalidStatusTransitionException;
import com.fintrack.incident.model.constant.ActionType;
import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentAction;
import com.fintrack.incident.model.constant.IncidentCause;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.constant.ResolutionCycleOutcome;
import com.fintrack.incident.model.constant.IncidentValidatorScope;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentHistory;
import com.fintrack.incident.model.entity.IncidentSearchCriteria;
import com.fintrack.incident.model.entity.IncidentTypeConfig;
import com.fintrack.incident.model.readmodel.ExternalUser;
import com.fintrack.incident.repository.IncidentRepository;
import com.fintrack.incident.repository.IncidentTypeConfigRepository;
import com.fintrack.incident.repository.specification.IncidentSpecification;
import com.fintrack.incident.security.IncidentWorkflowGuard;
import com.fintrack.incident.security.IncidentValidationPolicy;
import com.fintrack.incident.security.UserDetailsImpl;
import com.fintrack.incident.service.IncidentReferenceGenerator;
import com.fintrack.incident.service.IncidentHistoryService;
import com.fintrack.incident.service.IncidentService;
import com.fintrack.incident.service.ResolutionCycleRecorder;
import io.micrometer.core.annotation.Timed;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

// Implementation du service de gestion des incidents et de leur workflow metier.

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {

  private final IncidentRepository incidentRepository;
  private final IncidentTypeConfigRepository incidentTypeConfigRepository;
  private final IncidentHistoryService historyService;
  private final AuditServiceClientService auditServiceClientService;
  private final NotificationClientService notificationClientService;
  private final UserClientService userClientService;
  private final ReportingSystemConfigClientService reportingSystemConfigClientService;
  private final IncidentReferenceGenerator referenceGenerator;
  private final IncidentWorkflowGuard incidentWorkflowGuard;
  private final IncidentValidationPolicy validationPolicy;
  private final ResolutionCycleRecorder resolutionCycleRecorder;
  private final MessageSource messageSource;
  private final com.fintrack.incident.service.AttachmentCleanupService attachmentCleanup;
  private static final Set<IncidentStatus> TERMINAL_STATUSES =
    IncidentStatus.TERMINAL_STATUSES;
  // Annuler et rejeter sont deux portes disjointes : tant qu'il n'est pas valide,
  // un incident se rejette (OPEN, PENDING_VALIDATION) ; une fois entre dans le
  // circuit, il s'annule. Aucun statut ne doit figurer dans les deux ensembles.
  private static final Set<IncidentStatus> CANCELLABLE_STATUSES = Set.of(
    IncidentStatus.VALIDATED,
    IncidentStatus.TRANSFERRED,
    IncidentStatus.ASSIGNED,
    IncidentStatus.BLOCKED,
    IncidentStatus.UNRESOLVED_PROLONGED_WAIT
  );
  private static final Set<IncidentStatus> UPDATABLE_STATUSES = Set.of(
    IncidentStatus.OPEN,
    IncidentStatus.PENDING_VALIDATION,
    IncidentStatus.REOPENED
  );
  private static final Set<IncidentStatus> DELETABLE_STATUSES = Set.of(
    IncidentStatus.OPEN,
    IncidentStatus.PENDING_VALIDATION,
    IncidentStatus.REOPENED
  );

  // Valeur par defaut SLA 48h
  @Value("${system.defaults.sla-hours:48}")
  private Integer defaultSlaHours;

  // Rend caduque la proposition de solution en cours si le contexte de
  // traitement change de main.
  private void voidPendingSolution(Incident incident) {
    incident.setProposedSolution(null);
    incident.setDirectionRejectionReason(null);
    incident.setEstimatedResolutionHours(null);
  }

  // Temps pendant lequel l'horloge etait arretee : 0 si le debut n'est pas connu.
  private long pausedMinutesSince(LocalDateTime pausedAt, LocalDateTime until) {
    if (pausedAt == null || until == null || until.isBefore(pausedAt)) {
      return 0L;
    }
    return Duration.between(pausedAt, until).toMinutes();
  }

  // Le traitant peut estimer le temps de resolution (en heures) a la prise en charge :
  // s'il est renseigne (> 0) il devient l'estimation de reference et recalcule l'echeance
  // SLA depuis maintenant ; sinon le SLA par defaut deja porte par dueDate prime.
  private void applyEstimatedResolutionHours(
    Incident incident,
    Integer estimatedResolutionHours
  ) {
    if (estimatedResolutionHours == null || estimatedResolutionHours <= 0) {
      return;
    }
    incident.setEstimatedResolutionHours(estimatedResolutionHours);
    incident.setDueDate(LocalDateTime.now().plusHours(estimatedResolutionHours));
  }

  // Resout type configuration a partir du contexte disponible.
  private IncidentTypeConfig resolveTypeConfig(UUID typeId) {
    return incidentTypeConfigRepository
      .findById(typeId)
      .orElseThrow(() ->
        new EntityNotFoundException(
          ErrorCode.INCIDENT_TYPE_NOT_FOUND,
          msg("incident.error.type_not_found", typeId)
        )
      );
  }

  // Protege la chronologie metier meme lors d'un appel interne sans validation HTTP.
  private void validateBusinessDates(Incident incident) {
    LocalDate today = LocalDate.now();
    if (
      (incident.getIncidentDate() != null &&
        incident.getIncidentDate().isAfter(today)) ||
      (incident.getObservationDate() != null &&
        incident.getObservationDate().isAfter(today))
    ) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        msg("incident.error.date_not_future")
      );
    }
    if (
      incident.getIncidentDate() != null &&
      incident.getObservationDate() != null &&
      incident.getIncidentDate().isAfter(incident.getObservationDate())
    ) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        msg("incident.error.date_order")
      );
    }
  }

  @Override
  @Transactional(readOnly = true)
  @Timed(
    value = "incident.service.findAll",
    description = "Temps de recherche de tous les incidents"
  )
  // Liste les elements du domaine incident.
  public Page<Incident> findAll(Pageable pageable) {
    return incidentRepository.findAll(pageable);
  }

  @Override
  @Transactional(readOnly = true)
  @Timed(
    value = "incident.service.findFiltered",
    description = "Temps de recherche des incidents filtrés"
  )
  // Recherche les incidents pour filtered.
  public Page<Incident> findFiltered(
    IncidentSearchCriteria searchCriteria,
    UserDetailsImpl currentUser,
    Pageable pageable
  ) {
    // Filtre qualite "assigne a un utilisateur inactif" : on resout les IDs
    // inactifs depuis user-service (thread requete -> JWT propage) avant le filtrage.
    if (Boolean.TRUE.equals(searchCriteria.getAssignedToInactive())) {
      searchCriteria.setInactiveAssigneeIds(
        Set.copyOf(userClientService.getInactiveUserIds())
      );
    }
    return incidentRepository.findAll(
      IncidentSpecification.getFilterSpecification(searchCriteria, currentUser),
      pageable
    );
  }

  @Override
  @Transactional(readOnly = true)
  @Timed(
    value = "incident.service.findByAgencyId",
    description = "Temps de recherche des incidents par agence"
  )
  // Recherche les incidents par agence identifiant.
  public List<Incident> findByAgencyId(UUID agencyId) {
    return incidentRepository.findByAgencyId(agencyId);
  }

  @Override
  @Transactional(readOnly = true)
  @Timed(
    value = "incident.service.findByCreatedBy",
    description = "Temps de recherche des incidents par créateur"
  )
  // Recherche les incidents par creation by.
  public List<Incident> findByCreatedBy(UUID userId) {
    return incidentRepository.findByCreatedBy(userId);
  }

  @Override
  @Transactional(readOnly = true)
  @Timed(
    value = "incident.service.findByAssignedTo",
    description = "Temps de recherche des incidents par assignation"
  )
  // Recherche les incidents par assigne fin.
  public List<Incident> findByAssignedTo(UUID userId) {
    return incidentRepository.findByAssignedTo(userId);
  }

  @Override
  @Transactional(readOnly = true)
  @Timed(
    value = "incident.service.findById",
    description = "Temps de recherche d'un incident par ID"
  )
  // Recherche les incidents par identifiant.
  public Incident findById(UUID id) {
    return incidentRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException(
          ErrorCode.INCIDENT_NOT_FOUND,
          msg("incident.error.incident_not_found", id)
        )
      );
  }

  @Override
  @Transactional(readOnly = true)
  @Timed(
    value = "incident.service.findByReference",
    description = "Temps de recherche d'un incident par reference"
  )
  // Recherche un incident par son code metier lisible (reference).
  public Incident findByReference(String reference) {
    return incidentRepository
      .findByReference(reference)
      .orElseThrow(() ->
        new EntityNotFoundException(
          ErrorCode.INCIDENT_NOT_FOUND,
          msg("incident.error.incident_not_found", reference)
        )
      );
  }

  @Override
  @Transactional
  @Timed(
    value = "incident.service.create",
    description = "Temps de création d'un incident"
  )
  // Cree un element du domaine incident apres validation metier.
  public Incident create(
    Incident incident,
    UUID createdBy,
    UUID agencyId,
    boolean requiresCreatorValidation,
    UUID requestedTargetServiceId,
    boolean assignToSelf
  ) {
    if (incident.getTypeId() == null) {
      throw new BusinessRuleViolationException(
        ErrorCode.INCIDENT_TYPE_NOT_FOUND,
        msg("incident.error.type_required")
      );
    }

    // Un incident appartient toujours a une agence : un utilisateur sans agence
    // (compte global/admin) ne peut pas en creer message metier explicite.
    if (agencyId == null) {
      throw new BusinessRuleViolationException(
        ErrorCode.INCIDENT_AGENCY_REQUIRED,
        msg("incident.error.agency_required")
      );
    }

    IncidentTypeConfig typeConfig = resolveTypeConfig(incident.getTypeId());

    if (!typeConfig.isActive()) {
      throw new BusinessRuleViolationException(
        ErrorCode.INCIDENT_TYPE_INACTIVE,
        msg("incident.error.type_inactive", typeConfig.getName())
      );
    }

    if (incident.getCriticality() == null) {
      incident.setCriticality(Criticality.MEDIUM);
    }

    incident.setCreatedBy(createdBy);
    boolean ownServiceValidation = validationPolicy.isCreatedByTargetServiceHead(incident, typeConfig);
    IncidentStatus initialStatus =
      ownServiceValidation || (requiresCreatorValidation && typeConfig.isRequiresValidation())
        ? IncidentStatus.PENDING_VALIDATION
        : IncidentStatus.OPEN;

    incident.setStatus(initialStatus);
    if (initialStatus == IncidentStatus.PENDING_VALIDATION) {
      startDecisionWait(incident);
    }
    incident.setAgencyId(agencyId);

    // la date de constatation par defaut est la date de creation.
    if (incident.getObservationDate() == null) {
      incident.setObservationDate(LocalDate.now());
    }
    if (incident.getIncidentDate() == null) {
      incident.setIncidentDate(incident.getObservationDate());
    }
    validateBusinessDates(incident);

    // L'echeance est toujours pilotee par le SLA, jamais par la requete cliente.
    applySlaDueDate(incident, typeConfig);

    if (assignToSelf) {
      incident.setAssignedTo(createdBy);
    }

    // Un service par defaut configure sur le type prime sur le choix de
    // l'utilisateur sauf en cas d'auto-attribution, qui annule tout routage.
    UUID effectiveServiceId = assignToSelf
      ? null
      : typeConfig.getDefaultTargetServiceId() != null
        ? typeConfig.getDefaultTargetServiceId()
        : requestedTargetServiceId;

    // Type ciblant un responsable precis sans service : affectation directe
    // (ignoree si l'utilisateur s'est auto-attribue l'incident).
    if (
      !assignToSelf &&
      typeConfig.getDefaultTargetServiceId() == null &&
      typeConfig.getDefaultTargetUserId() != null &&
      incident.getAssignedTo() == null
    ) {
      incident.setAssignedTo(typeConfig.getDefaultTargetUserId());
    }

    // Rattachement au service cible (s'il y a un service cible par défaut ou demandé)
    if (effectiveServiceId != null && incident.getTransferredToService() == null) {
      incident.setTransferredToService(effectiveServiceId);
    }
    // Point de depart du routage : ce service est la reference face a laquelle un
    // transfert ulterieur sera juge « reroutage » plutot que routage initial.
    if (incident.getInitialTargetServiceId() == null) {
      incident.setInitialTargetServiceId(incident.getTransferredToService());
    }

    // Reference attribuee une seule fois, a la creation : elle ne bouge plus ensuite,
    // y compris apres transfert, reouverture ou clonage.
    if (incident.getReference() == null) {
      incident.setReference(referenceGenerator.next());
    }

    Incident saved = incidentRepository.save(incident);
    resolutionCycleRecorder.openFirstCycle(saved);

    historyService.record(
      saved,
      createdBy,
      ActionType.CREATION,
      null,
      initialStatus.getName(),
      null
    );

    auditServiceClientService.audit(
      createdBy,
      null,
      null,
      AuditAction.INCIDENT_CREATE.getName(),
      "INCIDENT",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName(),
      incidentSnapshot(saved)
    );

    notifyAfterCommit(
      () ->
        notificationClientService.notifyIncidentSubmitted(
          saved,
          typeConfig.getValidatorScope(),
          createdBy,
          null
        )
    );

    if (!requiresCreatorValidation && !ownServiceValidation && effectiveServiceId != null) {
      log.debug(
        "Transfert automatique de l'incident {} vers le service {}",
        saved.getId(),
        effectiveServiceId
      );
      return doTransfer(
        saved,
        createdBy,
        effectiveServiceId,
        null,
        "Transfert automatique vers le service cible lors de la création",
        true
      );
    }

    return saved;
  }

  @Override
  @Transactional
  @Timed(
    value = "incident.service.update",
    description = "Temps de mise à jour d'un incident"
  )
  // Met a jour un element du domaine incident avec les donnees validees.
  public Incident update(UUID id, Incident details, UUID requestingUserId) {
    Incident incident = findById(id);

    if (!UPDATABLE_STATUSES.contains(incident.getStatus())) {
      throw new InvalidStatusTransitionException(
        msg("incident.error.invalid_update_status", incident.getStatus().getName())
      );
    }

    if (!requestingUserId.equals(incident.getCreatedBy())) {
      throw new AccessDeniedException(
        msg("incident.error.unauthorized_update")
      );
    }

    Map<String, Object> diff = new LinkedHashMap<>();
    // Les changements de qualification sont historises axe par axe (type, criticite)
    // pour rester lisibles dans la chronologie : on retient les valeurs de depart.
    UUID previousTypeId = null;
    Criticality previousCriticality = null;

    if (
      details.getTitle() != null &&
      !details.getTitle().equals(incident.getTitle())
    ) {
      diff.put(
        "title",
        Map.of(
          "from",
          Objects.toString(incident.getTitle(), ""),
          "to",
          details.getTitle()
        )
      );
      incident.setTitle(details.getTitle());
    }

    if (
      details.getDescription() != null &&
      !details.getDescription().equals(incident.getDescription())
    ) {
      diff.put(
        "description",
        Map.of(
          "from",
          Objects.toString(incident.getDescription(), ""),
          "to",
          details.getDescription()
        )
      );
      incident.setDescription(details.getDescription());
    }

    // Seuls le motif (typeId) et la severite (criticality) peuvent etre modifies
    if (
      details.getTypeId() != null &&
      !details.getTypeId().equals(incident.getTypeId())
    ) {
      IncidentTypeConfig newType = resolveTypeConfig(details.getTypeId());
      if (!newType.isActive()) {
        throw new BusinessRuleViolationException(
          ErrorCode.INCIDENT_TYPE_INACTIVE,
          msg("incident.error.type_inactive", newType.getName())
        );
      }
      diff.put(
        "typeId",
        Map.of(
          "from",
          asString(incident.getTypeId()),
          "to",
          asString(newType.getId())
        )
      );
      previousTypeId = incident.getTypeId();
      incident.setTypeId(newType.getId());
      if (incident.getStatus() == IncidentStatus.PENDING_VALIDATION) {
        UUID validationServiceId =
          newType.getValidatorScope() ==
              IncidentValidatorScope.TARGET_SERVICE_MANAGER
            ? newType.getDefaultTargetServiceId()
            : null;
        incident.setTransferredToService(validationServiceId);
      }
      // Le SLA appartient au type : requalifier repart de l'echeance du nouveau type,
      // comme la reclassification lors d'un transfert.
      applySlaDueDate(incident, newType);
    }

    if (
      details.getCriticality() != null &&
      details.getCriticality() != incident.getCriticality()
    ) {
      String oldCriticality =
        incident.getCriticality() != null
          ? incident.getCriticality().getName()
          : "";
      diff.put(
        "criticality",
        Map.of("from", oldCriticality, "to", details.getCriticality().getName())
      );
      previousCriticality = incident.getCriticality();
      incident.setCriticality(details.getCriticality());
    }

    if (
      details.getIncidentDate() != null &&
      !details.getIncidentDate().equals(incident.getIncidentDate())
    ) {
      diff.put(
        "incidentDate",
        Map.of(
          "from",
          Objects.toString(incident.getIncidentDate(), ""),
          "to",
          details.getIncidentDate().toString()
        )
      );
      incident.setIncidentDate(details.getIncidentDate());
    }

    if (
      details.getObservationDate() != null &&
      !details.getObservationDate().equals(incident.getObservationDate())
    ) {
      diff.put(
        "observationDate",
        Map.of(
          "from",
          Objects.toString(incident.getObservationDate(), ""),
          "to",
          details.getObservationDate().toString()
        )
      );
      incident.setObservationDate(details.getObservationDate());
    }

    if (
      details.getCause() != null && details.getCause() != incident.getCause()
    ) {
      diff.put(
        "cause",
        Map.of(
          "from",
          incident.getCause() != null ? incident.getCause().name() : "",
          "to",
          details.getCause().name()
        )
      );
      incident.setCause(details.getCause());
    }

    if (
      details.getCauseDetail() != null &&
      !details.getCauseDetail().equals(incident.getCauseDetail())
    ) {
      diff.put(
        "causeDetail",
        Map.of(
          "from",
          Objects.toString(incident.getCauseDetail(), ""),
          "to",
          details.getCauseDetail()
        )
      );
      incident.setCauseDetail(details.getCauseDetail());
    }

    validateBusinessDates(incident);

    if (diff.isEmpty()) {
      return incident;
    }

    incident.setUpdatedAt(LocalDateTime.now());

    Incident saved = incidentRepository.saveAndFlush(incident);

    // Une entree par axe requalifie : le mapper resout les UUID de type en libelles
    // et le frontend dispose deja d'une phrase dediee pour chaque action.
    if (previousTypeId != null) {
      historyService.record(
        saved,
        requestingUserId,
        ActionType.TYPE_CHANGE,
        asString(previousTypeId),
        asString(saved.getTypeId()),
        null
      );
    }
    if (previousCriticality != null) {
      historyService.record(
        saved,
        requestingUserId,
        ActionType.CRITICALITY_CHANGE,
        previousCriticality.getName(),
        saved.getCriticality().getName(),
        null
      );
    }

    if (previousTypeId != null || previousCriticality != null) {
      String changeSummary = qualificationSummary(
        previousTypeId,
        previousCriticality,
        saved
      );
      notifyAfterCommit(
        () ->
          notificationClientService.notifyQualificationChanged(
            saved,
            changeSummary,
            requestingUserId
          )
      );
    }

    auditServiceClientService.audit(
      requestingUserId,
      null,
      null,
      AuditAction.INCIDENT_UPDATE.getName(),
      "INCIDENT",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName(),
      diff
    );

    return saved;
  }

  @Override
  @Transactional
  @Caching(
    evict = {
      @CacheEvict(value = "dashboardMetrics", allEntries = true),
      @CacheEvict(value = "dashboardComparisons", allEntries = true),
    }
  )
  // Supprime l'incident et conserve un instantane complet dans l'audit.
  public void delete(UUID id, UUID deletedBy) {
    Incident incident = findById(id);
    if (!DELETABLE_STATUSES.contains(incident.getStatus())) {
      throw new InvalidStatusTransitionException(
        msg("incident.error.invalid_delete_status", incident.getStatus().getName())
      );
    }
    // L'incident disparait de la base : on fige un instantane complet dans
    // l'audit pour qu'il reste consultable apres suppression.
    Map<String, Object> snapshot = incidentSnapshot(incident);
    incidentRepository.delete(incident);
    attachmentCleanup.enqueue(id, null);
    auditServiceClientService.audit(
      deletedBy,
      null,
      null,
      AuditAction.INCIDENT_DELETE.getName(),
      "INCIDENT",
      id.toString(),
      AuditStatus.SUCCESS.getName(),
      snapshot
    );
  }

  private Map<String, Object> incidentSnapshotWithComment(
    Incident incident,
    String comment
  ) {
    Map<String, Object> snapshot = incidentSnapshot(incident);
    if (comment != null && !comment.trim().isEmpty()) {
      snapshot.put("comment", comment);
    }
    return snapshot;
  }

  /** Instantané des champs métier d'un incident, conservé dans l'audit après suppression. */
  // Realise l'intention metier incident instantane.
  private Map<String, Object> incidentSnapshot(Incident incident) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("title", incident.getTitle());
    snapshot.put("description", incident.getDescription());
    snapshot.put("typeId", asString(incident.getTypeId()));
    snapshot.put(
      "criticality",
      incident.getCriticality() != null
        ? incident.getCriticality().name()
        : null
    );
    snapshot.put(
      "status",
      incident.getStatus() != null ? incident.getStatus().name() : null
    );
    snapshot.put("agencyId", asString(incident.getAgencyId()));
    snapshot.put("createdBy", asString(incident.getCreatedBy()));
    snapshot.put("assignedTo", asString(incident.getAssignedTo()));
    snapshot.put("creatorServiceId", asString(incident.getCreatorServiceId()));
    snapshot.put("blockedBy", asString(incident.getBlockedBy()));
    snapshot.put("reopenedBy", asString(incident.getReopenedBy()));
    snapshot.put("reopenCount", incident.getReopenCount());

    // Additional details for richer audit logs
    if (incident.getIncidentDate() != null) snapshot.put(
      "incidentDate",
      incident.getIncidentDate().toString()
    );
    if (incident.getObservationDate() != null) snapshot.put(
      "observationDate",
      incident.getObservationDate().toString()
    );
    if (incident.getDueDate() != null) snapshot.put(
      "dueDate",
      incident.getDueDate().toString()
    );
    if (incident.getValidatedAt() != null) snapshot.put(
      "validatedAt",
      incident.getValidatedAt().toString()
    );
    if (incident.getResolvedAt() != null) snapshot.put(
      "resolvedAt",
      incident.getResolvedAt().toString()
    );
    if (incident.getClosedAt() != null) snapshot.put(
      "closedAt",
      incident.getClosedAt().toString()
    );
    if (incident.getBlockedAt() != null) snapshot.put(
      "blockedAt",
      incident.getBlockedAt().toString()
    );
    if (incident.getUnblockedAt() != null) snapshot.put(
      "unblockedAt",
      incident.getUnblockedAt().toString()
    );
    if (incident.getReopenedAt() != null) snapshot.put(
      "reopenedAt",
      incident.getReopenedAt().toString()
    );

    if (incident.getCause() != null) snapshot.put(
      "cause",
      incident.getCause().name()
    );
    if (incident.getCauseDetail() != null) snapshot.put(
      "causeDetail",
      incident.getCauseDetail()
    );
    if (incident.getTreatmentDescription() != null) snapshot.put(
      "treatmentDescription",
      incident.getTreatmentDescription()
    );
    if (incident.getResolutionDescription() != null) snapshot.put(
      "resolutionDescription",
      incident.getResolutionDescription()
    );
    if (incident.getRejectReason() != null) snapshot.put(
      "rejectReason",
      incident.getRejectReason()
    );
    if (incident.getCancelReason() != null) snapshot.put(
      "cancelReason",
      incident.getCancelReason()
    );
    if (incident.getBlockedReason() != null) snapshot.put(
      "blockedReason",
      incident.getBlockedReason()
    );
    if (incident.getTransferReason() != null) snapshot.put(
      "transferReason",
      incident.getTransferReason()
    );
    if (incident.getReopenReason() != null) snapshot.put(
      "reopenReason",
      incident.getReopenReason()
    );

    return snapshot;
  }

  // Convertit chaine.

  private String asString(UUID value) {
    return value != null ? value.toString() : null;
  }

  @Override
  @Transactional
  // Valide un incident soumis selon les regles de workflow.
  public Incident validate(
    UUID id,
    UUID validatedBy,
    String comment,
    UUID requestedTargetServiceId,
    UUID requestedTargetUserId,
    Set<String> validatorPermissions
  ) {
    Incident incident = findById(id);
    // Un incident peut etre valide qu'il ait ete cree par un agent (PENDING_VALIDATION)
    // ou par un non-agent (OPEN).
    requireStatusIn(
      incident,
      Set.of(IncidentStatus.OPEN, IncidentStatus.PENDING_VALIDATION)
    );

    IncidentTypeConfig typeConfig = resolveTypeConfig(incident.getTypeId());
    UUID effectiveServiceId =
      typeConfig.getDefaultTargetServiceId() != null
        ? typeConfig.getDefaultTargetServiceId()
        : requestedTargetServiceId;
    UUID effectiveUserId =
      requestedTargetUserId != null
        ? requestedTargetUserId
        : typeConfig.getDefaultTargetServiceId() == null
          ? typeConfig.getDefaultTargetUserId()
          : null;

    if (
      incident.getAssignedTo() == null &&
      typeConfig.getDefaultTargetServiceId() == null &&
      typeConfig.getDefaultTargetUserId() == null &&
      requestedTargetServiceId == null &&
      requestedTargetUserId == null
    ) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        msg("incident.error.validation_requires_assignee")
      );
    }

    if (effectiveUserId != null) {
      validateExplicitAssignee(
        effectiveUserId,
        effectiveServiceId,
        incident.getAgencyId()
      );
    }

    IncidentStatus previousStatus = incident.getStatus();
    String oldStatus = previousStatus.getName();
    incident.setValidatedBy(validatedBy);
    incident.setValidatedAt(LocalDateTime.now());
    // L'incident entre dans le circuit maintenant : son delai part d'ici. Depuis OPEN,
    // il etait deja traitable — l'echeance de la declaration tient.
    if (previousStatus == IncidentStatus.PENDING_VALIDATION) {
      applySlaDueDate(incident, typeConfig);
    }
    // La validation confirme l'incident. La prise en charge reste une
    // action explicite du responsable, comme dans un flux ticketing.
    IncidentStatus targetStatus =
      incident.getAssignedTo() != null
        ? IncidentStatus.ASSIGNED
        : IncidentStatus.VALIDATED;
    incident.setStatus(targetStatus);

    Incident saved = incidentRepository.saveAndFlush(incident);
    resolutionCycleRecorder.markValidated(saved, saved.getValidatedAt());

    historyService.record(
      saved,
      validatedBy,
      ActionType.VALIDATION,
      oldStatus,
      targetStatus.getName(),
      comment
    );

    auditServiceClientService.audit(
      validatedBy,
      null,
      null,
      AuditAction.INCIDENT_VALIDATE.getName(),
      "INCIDENT",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName(),
      incidentSnapshotWithComment(saved, comment)
    );

    Incident validatedIncident = saved;
    notifyAfterCommit(
      () ->
        notificationClientService.notifyStatusChanged(
          validatedIncident,
          msg("incident.status.validated"),
          validatedBy,
          comment
        )
    );

    // Une auto-attribution explicite (assignToSelf) prime sur le routage vers le
    // service par defaut : a la validation on ne re-route que si l'incident n'est PAS
    // deja assigne, ou si le valideur demande explicitement une autre cible. Sinon
    // doTransfer() effacerait l'assignation du createur qui s'etait auto-attribue.
    boolean validatorRequestedRouting =
      requestedTargetServiceId != null || requestedTargetUserId != null;
    boolean autoRouteToDefault =
      typeConfig.getDefaultTargetServiceId() != null &&
      validatorPermissions.contains("INCIDENT_AUTO_TRANSFER") &&
      saved.getAssignedTo() == null;
    if (
      effectiveServiceId != null &&
      !effectiveServiceId.equals(saved.getTransferredToService()) &&
      (autoRouteToDefault || validatorRequestedRouting)
    ) {
      log.debug(
        "Transfert automatique de l'incident {} après validation vers le service {} (INCIDENT_AUTO_TRANSFER)",
        saved.getId(),
        effectiveServiceId
      );
      return doTransfer(
        saved,
        validatedBy,
        effectiveServiceId,
        null,
        "Transfert automatique apres validation",
        effectiveUserId,
        true
      );
    }

    if (
      effectiveServiceId != null &&
      effectiveServiceId.equals(saved.getTransferredToService()) &&
      (autoRouteToDefault || validatorRequestedRouting) &&
      saved.getAssignedTo() == null
    ) {
      log.debug(
        "Auto-assignation de l'incident {} déjà rattaché au service {}",
        saved.getId(),
        effectiveServiceId
      );
      saved = autoAssignToResponsible(
        saved,
        validatedBy,
        effectiveServiceId,
        effectiveUserId
      );
    }

    if (effectiveUserId != null && saved.getAssignedTo() == null) {
      return assign(saved.getId(), validatedBy, effectiveUserId, comment);
    }

    return saved;
  }

  @Override
  @Transactional
  // Rejette un incident avec le motif metier attendu.
  public Incident reject(
    UUID id,
    UUID rejectedBy,
    String reason,
    String comment
  ) {
    Incident incident = findById(id);

    if (
      incident.getStatus() != IncidentStatus.PENDING_VALIDATION &&
      incident.getStatus() != IncidentStatus.OPEN
    ) {
      throw new InvalidStatusTransitionException(
        msg("incident.error.invalid_reject_status", incident.getStatus().getName())
      );
    }

    String oldStatus = incident.getStatus().getName();
    incident.setStatus(IncidentStatus.REJECTED);
    incident.setRejectReason(reason);

    Incident saved = incidentRepository.saveAndFlush(incident);
    resolutionCycleRecorder.closeCycle(
      saved,
      ResolutionCycleOutcome.REJECTED,
      null
    );

    historyService.record(
      saved,
      rejectedBy,
      ActionType.STATUS_CHANGE,
      oldStatus,
      IncidentStatus.REJECTED.getName(),
      comment != null ? reason + " — " + comment : reason
    );

    auditServiceClientService.audit(
      rejectedBy,
      null,
      null,
      AuditAction.INCIDENT_STATUS_CHANGE.getName(),
      "INCIDENT",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName(),
      incidentSnapshotWithComment(saved, comment)
    );

    notifyAfterCommit(
      () ->
        notificationClientService.notifyStatusChanged(
          saved,
          msg("incident.status.rejected"),
          rejectedBy,
          reason
        )
    );

    return saved;
  }

  @Override
  @Transactional
  // Transfere un incident vers un service ou l'agence du declarant.
  public Incident transfer(
    UUID id,
    UUID transferredBy,
    UUID targetServiceId,
    UUID targetAgencyId,
    UUID newTypeId,
    String reason,
    String comment,
    Set<String> requesterPermissions
  ) {
    Incident incident = findById(id);
    requireStatusIn(
      incident,
      Set.of(
        IncidentStatus.VALIDATED,
        IncidentStatus.TRANSFERRED,
        IncidentStatus.ASSIGNED,
        IncidentStatus.IN_PROGRESS,
        IncidentStatus.BLOCKED
      )
    );

    if ((targetServiceId == null) == (targetAgencyId == null)) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        msg("validation.incident.transfer_target_required")
      );
    }
    if (
      targetAgencyId != null &&
      (incident.getAgencyId() == null ||
        incident.getCreatorServiceId() != null ||
        !incident.getAgencyId().equals(targetAgencyId))
    ) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        msg("incident.error.transfer_agency_not_allowed")
      );
    }

    if (
      requesterPermissions.contains("INCIDENT_TRANSFER_WITH_REASON") &&
      (reason == null || reason.isBlank())
    ) {
      throw new BusinessRuleViolationException(
        ErrorCode.INCIDENT_TRANSFER_REASON_REQUIRED,
        msg("incident.error.transfer_reason_required")
      );
    }

    // Garde-fou : transferer vers le service courant est un no-op qui
    // pollue l'historique avec des transitions "TRANSFERRED TRANSFERRED"
    // illisibles. On rejette explicitement plutot que d'autoriser.
    UUID currentServiceId = incident.getTransferredToService();
    if (targetServiceId != null && targetServiceId.equals(currentServiceId)) {
      throw new BusinessRuleViolationException(
        ErrorCode.INCIDENT_TRANSFER_SAME_SERVICE,
        msg("incident.error.transfer_same_service")
      );
    }

    if (
      targetAgencyId != null &&
      currentServiceId == null &&
      incident.getTransferredAt() != null
    ) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        msg("incident.error.transfer_same_agency")
      );
    }

    return targetAgencyId != null
      ? doTransferToAgency(
        incident,
        transferredBy,
        targetAgencyId,
        reason,
        comment
      )
      : doTransfer(
        incident,
        transferredBy,
        targetServiceId,
        newTypeId,
        reason,
        comment,
        null,
        false
      );
  }

  @Override
  @Transactional
  // Assigne un incident au responsable designe.
  public Incident assign(
    UUID id,
    UUID assignedBy,
    UUID assignedTo,
    String comment
  ) {
    Incident incident = findById(id);
    requireStatusIn(
      incident,
      Set.of(
        IncidentStatus.OPEN,
        IncidentStatus.VALIDATED,
        IncidentStatus.TRANSFERRED,
        IncidentStatus.ASSIGNED,
        IncidentStatus.IN_PROGRESS
      )
    );

    ExternalUser assignee =
      incidentWorkflowGuard.resolveValidAssigneeForIncident(
        incident,
        assignedTo
      );

    if (incident.getTransferredToService() == null) {
      // Mettre a jour le service de l'incident si l'assigne appartient a un service
      incident.setTransferredToService(assignee.getServiceId());
    }

    String oldAssignee =
      incident.getAssignedTo() != null
        ? incident.getAssignedTo().toString()
        : null;

    incident.setAssignedTo(assignedTo);
    String oldStatus = incident.getStatus().getName();
    incident.setStatus(IncidentStatus.ASSIGNED);
    incident.setTransferReason(null);
    voidPendingSolution(incident);

    Incident saved = incidentRepository.saveAndFlush(incident);

    historyService.record(
      saved,
      assignedBy,
      ActionType.ASSIGNMENT,
      oldAssignee,
      assignedTo.toString(),
      comment
    );
    if (!oldStatus.equals(IncidentStatus.ASSIGNED.getName())) {
      historyService.record(
        saved,
        assignedBy,
        ActionType.STATUS_CHANGE,
        oldStatus,
        IncidentStatus.ASSIGNED.getName(),
        comment
      );
    }
    notifyAfterCommit(() ->
      notificationClientService.notifyIncidentAssigned(saved, assignedTo)
    );

    return saved;
  }

  @Override
  @Transactional
  // Execute la prise en charge explicite avant resolution.
  public Incident startProgress(
    UUID id,
    UUID startedBy,
    String comment,
    Integer estimatedResolutionHours
  ) {
    if (startedBy == null) {
      throw new InvalidStatusTransitionException(
        msg("incident.error.start_requires_user")
      );
    }
    Incident incident = findById(id);
    requireStatusIn(incident, Set.of(IncidentStatus.ASSIGNED));

    // Un incident non assigne ne peut pas etre demarre : on leve une erreur
    // metier 422 explicite plutot que de laisser un assignedTo null filer
    // (ce qui aurait provoque un 500 plus loin). Coherent avec le guard.
    if (incident.getAssignedTo() == null) {
      throw new BusinessRuleViolationException(
        ErrorCode.INCIDENT_START_REQUIRES_ASSIGNEE,
        msg("incident.error.start_requires_assignee")
      );
    }

    String oldStatus = incident.getStatus().getName();
    incident.setStatus(IncidentStatus.IN_PROGRESS);
    applyEstimatedResolutionHours(incident, estimatedResolutionHours);

    Incident saved = incidentRepository.saveAndFlush(incident);

    historyService.record(
      saved,
      startedBy,
      ActionType.STATUS_CHANGE,
      oldStatus,
      IncidentStatus.IN_PROGRESS.getName(),
      comment
    );

    auditServiceClientService.audit(
      startedBy,
      null,
      null,
      AuditAction.INCIDENT_STATUS_CHANGE.getName(),
      "INCIDENT",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName(),
      incidentSnapshotWithComment(saved, comment)
    );

    notifyAfterCommit(
      () ->
        notificationClientService.notifyStatusChanged(
          saved,
          msg("incident.status.in_progress"),
          startedBy,
          null
        )
    );

    return saved;
  }

  @Override
  @Transactional
  public Incident submitSolution(
    UUID id,
    String proposedSolution,
    Integer estimatedResolutionHours,
    UserDetailsImpl actor
  ) {
    Incident incident = findById(id);
    incidentWorkflowGuard.assertCanAct(
      incident,
      actor,
      IncidentAction.SUBMIT_SOLUTION
    );

    if (!StringUtils.hasText(proposedSolution)) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        msg("incident.error.proposed_solution_required")
      );
    }

    String oldStatus = incident.getStatus().getName();
    incident.setProposedSolution(proposedSolution);
    incident.setDirectionRejectionReason(null);
    // Estimation saisie a la prise en charge : conservee ici et appliquee a l'echeance
    // quand la Direction valide (passage effectif en IN_PROGRESS).
    if (estimatedResolutionHours != null && estimatedResolutionHours > 0) {
      incident.setEstimatedResolutionHours(estimatedResolutionHours);
    }
    incident.setStatus(IncidentStatus.DRAFT);
    startDecisionWait(incident);

    Incident saved = incidentRepository.saveAndFlush(incident);

    historyService.record(
      saved,
      actor.getId(),
      ActionType.STATUS_CHANGE,
      oldStatus,
      IncidentStatus.DRAFT.getName(),
      "Soumission de la proposition de solution pour validation préalable par la Direction"
    );

    auditServiceClientService.audit(
      actor.getId(),
      null,
      null,
      AuditAction.INCIDENT_SUBMIT_SOLUTION.getName(),
      "INCIDENT",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName(),
      incidentSnapshotWithComment(
        saved,
        "Solution proposée : " + proposedSolution
      )
    );

    IncidentTypeConfig config = incident.getTypeId() != null
      ? incidentTypeConfigRepository.findById(incident.getTypeId()).orElse(null)
      : null;
    Set<UUID> directionValidatorIds = config != null
      ? config.getDirectionValidatorIds()
      : null;

    notifyAfterCommit(
      () ->
        notificationClientService.notifySolutionProposed(
          saved,
          directionValidatorIds,
          actor.getId()
        )
    );

    return saved;
  }

  @Override
  @Transactional
  public Incident validateDirection(
    UUID id,
    UserDetailsImpl actor
  ) {
    Incident incident = findById(id);
    incidentWorkflowGuard.assertCanAct(
      incident,
      actor,
      IncidentAction.DIRECTION_VALIDATE
    );

    String oldStatus = incident.getStatus().getName();
    incident.setStatus(IncidentStatus.IN_PROGRESS);
    incident.setDirectionRejectionReason(null);
    // L'attente de la Direction n'est pas du temps de traitement.
    restartTreatmentDeadline(incident);
    // Le traitement demarre effectivement : si le traitant a estime un temps de
    // resolution a la soumission, il pilote desormais l'echeance SLA.
    applyEstimatedResolutionHours(
      incident,
      incident.getEstimatedResolutionHours()
    );

    Incident saved = incidentRepository.saveAndFlush(incident);

    historyService.record(
      saved,
      actor.getId(),
      ActionType.STATUS_CHANGE,
      oldStatus,
      IncidentStatus.IN_PROGRESS.getName(),
      "Validation préalable accordée par la Direction"
    );

    auditServiceClientService.audit(
      actor.getId(),
      null,
      null,
      AuditAction.INCIDENT_DIRECTION_VALIDATE.getName(),
      "INCIDENT",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName(),
      incidentSnapshotWithComment(
        saved,
        "Validation de la Direction accordée"
      )
    );

    notifyAfterCommit(
      () ->
        notificationClientService.notifyDirectionValidated(
          saved,
          actor.getId()
        )
    );

    return saved;
  }

  @Override
  @Transactional
  public Incident rejectDirection(
    UUID id,
    String rejectionReason,
    UserDetailsImpl actor
  ) {
    Incident incident = findById(id);
    incidentWorkflowGuard.assertCanAct(
      incident,
      actor,
      IncidentAction.DIRECTION_REJECT
    );

    if (!StringUtils.hasText(rejectionReason)) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        msg("incident.error.direction_reject_reason_required")
      );
    }

    String oldStatus = incident.getStatus().getName();
    String previousSolution = incident.getProposedSolution();
    incident.setDirectionRejectionReason(rejectionReason);
    incident.setStatus(IncidentStatus.DRAFT);
    // L'attente change de main : ce n'est plus la Direction qu'on attend mais une
    // nouvelle solution du traitant. Elle repart d'ici, et sera relancee aupres de lui.
    startDecisionWait(incident);

    Incident saved = incidentRepository.saveAndFlush(incident);

    String historyComment = StringUtils.hasText(previousSolution)
      ? msg("incident.history.direction_rejected_with_solution", rejectionReason, previousSolution)
      : msg("incident.history.direction_rejected", rejectionReason);

    historyService.record(
      saved,
      actor.getId(),
      ActionType.STATUS_CHANGE,
      oldStatus,
      IncidentStatus.DRAFT.getName(),
      historyComment
    );

    String auditComment = StringUtils.hasText(previousSolution)
      ? msg("incident.history.direction_reject_audit_with_solution", rejectionReason, previousSolution)
      : msg("incident.history.direction_reject_audit", rejectionReason);

    auditServiceClientService.audit(
      actor.getId(),
      null,
      null,
      AuditAction.INCIDENT_DIRECTION_REJECT.getName(),
      "INCIDENT",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName(),
      incidentSnapshotWithComment(saved, auditComment)
    );

    notifyAfterCommit(
      () ->
        notificationClientService.notifyDirectionRejected(
          saved,
          actor.getId(),
          rejectionReason
        )
    );

    return saved;
  }

  @Override
  @Transactional
  // Marque un incident comme resolu apres traitement.
  public Incident block(
    UUID id,
    UUID blockedBy,
    String reason,
    String comment
  ) {
    Incident incident = findById(id);
    requireStatus(incident, IncidentStatus.IN_PROGRESS);
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        msg("incident.error.block_reason_required")
      );
    }

    String oldStatus = incident.getStatus().getName();
    incident.setPreBlockStatus(oldStatus);
    incident.setStatus(IncidentStatus.BLOCKED);
    incident.setBlockedReason(reason);
    incident.setBlockedAt(LocalDateTime.now());
    incident.setBlockedBy(blockedBy);
    incident.setUnblockedAt(null);

    Incident saved = incidentRepository.saveAndFlush(incident);

    String historyComment =
      comment != null && !comment.isBlank() ? reason + " - " + comment : reason;
    historyService.record(
      saved,
      blockedBy,
      ActionType.STATUS_CHANGE,
      oldStatus,
      IncidentStatus.BLOCKED.getName(),
      historyComment
    );

    auditServiceClientService.audit(
      blockedBy,
      null,
      null,
      AuditAction.INCIDENT_STATUS_CHANGE.getName(),
      "INCIDENT",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName(),
      incidentSnapshotWithComment(saved, comment)
    );

    notifyAfterCommit(
      () ->
        notificationClientService.notifyStatusChanged(
          saved,
          msg("incident.status.blocked"),
          blockedBy,
          reason
        )
    );

    return saved;
  }

  // Realise l'intention metier resume.

  @Override
  @Transactional
  public Incident resume(UUID id, UUID resumedBy, String comment) {
    Incident incident = findById(id);
    requireStatus(incident, IncidentStatus.BLOCKED);

    IncidentTypeConfig typeConfig = resolveTypeConfig(incident.getTypeId());
    Integer effectiveSlaHours = resolveSlaHours(typeConfig);
    LocalDateTime resumedAt = LocalDateTime.now();
    // L'horloge etait arretee pendant le blocage : on la solde avant d'effacer le contexte.
    long pausedMinutes = pausedMinutesSince(incident.getBlockedAt(), resumedAt);
    String oldStatus = incident.getStatus().getName();
    // Reprise : on restaure le statut d'origine memorise au blocage (le blocage SLA peut
    // survenir depuis n'importe quel statut avant Traite), avec repli sur IN_PROGRESS.
    IncidentStatus target = IncidentStatus.IN_PROGRESS;
    if (incident.getPreBlockStatus() != null) {
      try {
        target = IncidentStatus.valueOf(incident.getPreBlockStatus());
      } catch (IllegalArgumentException ex) {
        target = IncidentStatus.IN_PROGRESS;
      }
    }
    incident.setStatus(target);
    incident.setPreBlockStatus(null);
    // Reprise : le blocage est soldé (conservé dans l'historique). On efface le contexte
    // de blocage affiché à l'état courant ; unblockedAt garde la trace de la reprise.
    incident.setBlockedAt(null);
    incident.setBlockedBy(null);
    incident.setBlockedReason(null);
    incident.setUnblockedAt(resumedAt);
    incident.setLastSlaReminderSentAt(null);
    incident.setDueDate(
      effectiveSlaHours != null && effectiveSlaHours > 0
        ? resumedAt.plusHours(effectiveSlaHours)
        : null
    );

    Incident saved = incidentRepository.saveAndFlush(incident);
    resolutionCycleRecorder.addPausedMinutes(saved, pausedMinutes);

    historyService.record(
      saved,
      resumedBy,
      ActionType.STATUS_CHANGE,
      oldStatus,
      target.getName(),
      comment
    );

    auditServiceClientService.audit(
      resumedBy,
      null,
      null,
      AuditAction.INCIDENT_STATUS_CHANGE.getName(),
      "INCIDENT",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName(),
      incidentSnapshotWithComment(saved, comment)
    );

    final IncidentStatus resumedStatus = target;
    notifyAfterCommit(
      () ->
        notificationClientService.notifyStatusChanged(
          saved,
          msg(resumedStatus.getNameKey()),
          resumedBy,
          comment
        )
    );

    return saved;
  }

  // Attente prolongee, etape 1 : le traitant reprend l'incident, mais il ne redemarre
  // pas pour autant. L'entite source est interrogee sur son actualite ; le statut ne
  // bouge pas tant qu'elle n'a pas repondu.
  @Override
  @Transactional
  public Incident requestConfirmation(UUID id, UUID requestedBy, String comment) {
    Incident incident = findById(id);
    requireStatus(incident, IncidentStatus.UNRESOLVED_PROLONGED_WAIT);

    LocalDateTime requestedAt = LocalDateTime.now();
    incident.setConfirmationRequestedAt(requestedAt);
    incident.setConfirmationRequestedBy(requestedBy);
    incident.setLastConfirmationReminderSentAt(null);

    Incident saved = incidentRepository.saveAndFlush(incident);

    historyService.record(
      saved,
      requestedBy,
      ActionType.CONFIRMATION_REQUEST,
      null,
      null,
      comment != null ? comment : msg("incident.history.confirmation_requested")
    );

    auditServiceClientService.audit(
      requestedBy,
      null,
      null,
      AuditAction.INCIDENT_STATUS_CHANGE.getName(),
      "INCIDENT",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName(),
      incidentSnapshotWithComment(saved, comment)
    );

    notifyAfterCommit(
      () ->
        notificationClientService.notifyConfirmationRequested(
          saved,
          requestedBy,
          comment
        )
    );

    return saved;
  }

  // Attente prolongee, etape 2 : l'entite source confirme. L'incident reprend son cours
  // avec TOUTES ses donnees (solution proposee validee comprise) ; seule l'echeance
  // repart a neuf. L'echeance de reference, elle, ne bouge pas : le retard deja pris
  // reste lisible dans les metriques de conformite.
  @Override
  @Transactional
  public Incident confirmRelevance(UUID id, UUID confirmedBy, String comment) {
    Incident incident = findById(id);
    requireStatus(incident, IncidentStatus.UNRESOLVED_PROLONGED_WAIT);

    if (incident.getConfirmationRequestedAt() == null) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_STATUS_TRANSITION,
        msg("incident.error.confirmation_not_requested")
      );
    }

    // Le traitant d'origine a pu quitter le circuit pendant l'attente. Refuser la reprise
    // reproduirait l'immobilisation que ce circuit corrige : l'incident revient alors au
    // chef du service traitant, a charge pour lui de le redistribuer.
    UUID effectiveAssignee = resolveConfirmationAssignee(incident);
    boolean reassigned =
      effectiveAssignee != null &&
      !effectiveAssignee.equals(incident.getAssignedTo());

    IncidentTypeConfig typeConfig = resolveTypeConfig(incident.getTypeId());
    Integer effectiveSlaHours = resolveSlaHours(typeConfig);
    LocalDateTime confirmedAt = LocalDateTime.now();
    // Meme regle qu'a la reprise : l'attente prolongee n'est pas du temps de traitement.
    long pausedMinutes = pausedMinutesSince(
      incident.getBlockedAt(),
      confirmedAt
    );
    String oldStatus = incident.getStatus().getName();

    // Sans traitant disponible, l'incident ne peut pas etre "en cours de traitement" :
    // il repart au stade "arrive au traitant, pas encore assigne", d'ou son chef pourra
    // l'assigner. Meme sortie que autoAssignToResponsible quand il ne trouve personne.
    IncidentStatus resumedStatus = effectiveAssignee != null
      ? IncidentStatus.IN_PROGRESS
      : IncidentStatus.TRANSFERRED;
    incident.setStatus(resumedStatus);
    incident.setAssignedTo(effectiveAssignee);
    // Le contexte de blocage est solde (l'historique en garde la trace) et le compteur
    // de relances repart, sans quoi l'incident serait relance sur un blocage revolu.
    incident.setPreBlockStatus(null);
    incident.setBlockedAt(null);
    incident.setBlockedBy(null);
    incident.setBlockedReason(null);
    incident.setUnblockedAt(confirmedAt);
    incident.setLastSlaReminderSentAt(null);
    incident.setConfirmationRequestedAt(null);
    incident.setConfirmationRequestedBy(null);
    incident.setLastConfirmationReminderSentAt(null);
    incident.setDueDate(
      effectiveSlaHours != null && effectiveSlaHours > 0
        ? confirmedAt.plusHours(effectiveSlaHours)
        : null
    );

    Incident saved = incidentRepository.saveAndFlush(incident);
    resolutionCycleRecorder.addPausedMinutes(saved, pausedMinutes);

    if (reassigned) {
      // Reassignation derivee de la confirmation, pas choisie par celui qui confirme :
      // elle est signee par le systeme (cf. historyAuthor).
      historyService.record(
        saved,
        null,
        ActionType.ASSIGNMENT,
        null,
        effectiveAssignee.toString(),
        msg("incident.history.confirmation_reassigned_to_service_head")
      );
      notifyAfterCommit(() ->
        notificationClientService.notifyIncidentAssigned(
          saved,
          effectiveAssignee
        )
      );
    }

    if (effectiveAssignee == null) {
      log.warn(
        "ALERTE: aucun traitant disponible pour l'incident {} dont l'actualité vient d'être confirmée — laissé non assigné",
        saved.getId()
      );
      notifyAfterCommit(() -> notifyResumedWithoutAssignee(saved));
    }

    historyService.record(
      saved,
      confirmedBy,
      ActionType.STATUS_CHANGE,
      oldStatus,
      resumedStatus.getName(),
      comment != null ? comment : msg("incident.history.relevance_confirmed")
    );

    auditServiceClientService.audit(
      confirmedBy,
      null,
      null,
      AuditAction.INCIDENT_STATUS_CHANGE.getName(),
      "INCIDENT",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName(),
      incidentSnapshotWithComment(saved, comment)
    );

    notifyAfterCommit(
      () ->
        notificationClientService.notifyStatusChanged(
          saved,
          msg(resumedStatus.getNameKey()),
          confirmedBy,
          comment != null
            ? comment
            : msg("incident.history.relevance_confirmed")
        )
    );

    return saved;
  }

  // Le traitant clot son traitement : IN_PROGRESS -> TREATED. Description de traitement
  // et piece jointe obligatoires ; analyse de cause imposee selon le type. L'incident part
  // ensuite en validation de resolution aupres du valideur.
  @Override
  @Transactional
  public Incident treat(
    UUID id,
    UUID treatedBy,
    String treatmentDescription,
    IncidentCause cause,
    String causeDetail
  ) {
    Incident incident = findById(id);
    requireStatus(incident, IncidentStatus.IN_PROGRESS);

    if (!StringUtils.hasText(treatmentDescription)) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        msg("incident.error.treatment_description_required")
      );
    }

    if (cause != null) {
      incident.setCause(cause);
    }
    if (causeDetail != null && !causeDetail.isBlank()) {
      incident.setCauseDetail(causeDetail);
    }

    IncidentTypeConfig typeConfig = resolveTypeConfig(incident.getTypeId());
    if (typeConfig.isRequiresCauseAnalysis()) {
      if (
        incident.getCause() == null ||
        incident.getCauseDetail() == null ||
        incident.getCauseDetail().isBlank()
      ) {
        throw new BusinessRuleViolationException(
          ErrorCode.INVALID_INPUT,
          msg("incident.error.resolve_cause_required")
        );
      }
    }

    incident.setTreatmentDescription(treatmentDescription);

    String oldStatus = incident.getStatus().getName();
    incident.setStatus(IncidentStatus.TREATED);
    startDecisionWait(incident);

    Incident saved = incidentRepository.saveAndFlush(incident);
    resolutionCycleRecorder.markTreated(saved, LocalDateTime.now());

    historyService.record(
      saved,
      treatedBy,
      ActionType.STATUS_CHANGE,
      oldStatus,
      IncidentStatus.TREATED.getName(),
      treatmentDescription
    );

    auditServiceClientService.audit(
      treatedBy,
      null,
      null,
      AuditAction.INCIDENT_STATUS_CHANGE.getName(),
      "INCIDENT",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName(),
      incidentSnapshot(saved)
    );

    notifyAfterCommit(
      () ->
        notificationClientService.notifyStatusChanged(
          saved,
          msg("incident.status.treated"),
          treatedBy,
          treatmentDescription
        )
    );

    return saved;
  }

  // Le valideur confirme la resolution : TREATED -> RESOLVED (note de validation obligatoire).
  @Override
  @Transactional
  public Incident resolve(UUID id, UUID validatedBy, String resolutionNote) {
    Incident incident = findById(id);
    requireStatus(incident, IncidentStatus.TREATED);

    if (!StringUtils.hasText(resolutionNote)) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        msg("incident.error.resolution_note_required")
      );
    }

    String oldStatus = incident.getStatus().getName();
    incident.setStatus(IncidentStatus.RESOLVED);
    incident.setResolvedAt(LocalDateTime.now());
    startDecisionWait(incident);
    if (StringUtils.hasText(resolutionNote)) {
      incident.setResolutionDescription(resolutionNote);
    }

    Incident saved = incidentRepository.saveAndFlush(incident);
    resolutionCycleRecorder.markResolved(saved, saved.getResolvedAt());

    historyService.record(
      saved,
      validatedBy,
      ActionType.STATUS_CHANGE,
      oldStatus,
      IncidentStatus.RESOLVED.getName(),
      resolutionNote
    );

    auditServiceClientService.audit(
      validatedBy,
      null,
      null,
      AuditAction.INCIDENT_RESOLVE.getName(),
      "INCIDENT",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName(),
      incidentSnapshot(saved)
    );

    notifyAfterCommit(
      () ->
        notificationClientService.notifyStatusChanged(
          saved,
          msg("incident.status.resolved"),
          validatedBy,
          resolutionNote
        )
    );

    return saved;
  }

  // Le valideur juge la resolution insuffisante : TREATED -> IN_PROGRESS (motif obligatoire),
  // renvoi au traitant pour retravail.
  @Override
  @Transactional
  public Incident markUnresolved(UUID id, UUID validatedBy, String reason) {
    Incident incident = findById(id);
    requireStatus(incident, IncidentStatus.TREATED);

    if (!StringUtils.hasText(reason)) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        msg("incident.error.unresolved_reason_required")
      );
    }

    String oldStatus = incident.getStatus().getName();
    incident.setStatus(IncidentStatus.IN_PROGRESS);

    Incident saved = incidentRepository.saveAndFlush(incident);
    resolutionCycleRecorder.clearResolved(saved);

    historyService.record(
      saved,
      validatedBy,
      ActionType.RESOLUTION_REJECTED,
      oldStatus,
      IncidentStatus.IN_PROGRESS.getName(),
      msg("incident.history.marked_unresolved") + " : " + reason
    );

    auditServiceClientService.audit(
      validatedBy,
      null,
      null,
      AuditAction.INCIDENT_STATUS_CHANGE.getName(),
      "INCIDENT",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName(),
      incidentSnapshot(saved)
    );

    notifyAfterCommit(
      () ->
        notificationClientService.notifyStatusChanged(
          saved,
          msg("incident.status.in_progress"),
          validatedBy,
          reason
        )
    );

    return saved;
  }

  @Override
  @Transactional
  // Cloture un incident resolu apres validation finale.
  public Incident close(
    UUID id,
    UUID closedBy,
    String closureDescription,
    String comment
  ) {
    Incident incident = findById(id);
    requireStatus(incident, IncidentStatus.RESOLVED);

    String oldStatus = incident.getStatus().getName();
    incident.setStatus(IncidentStatus.CLOSED);
    incident.setClosedAt(LocalDateTime.now());
    incident.setClosureDescription(closureDescription);

    Incident saved = incidentRepository.saveAndFlush(incident);
    resolutionCycleRecorder.closeCycle(
      saved,
      ResolutionCycleOutcome.CLOSED,
      saved.getClosedAt()
    );

    String historyComment =
      comment != null && !comment.isBlank()
        ? closureDescription + " — " + comment
        : closureDescription;
    historyService.record(
      saved,
      closedBy,
      ActionType.STATUS_CHANGE,
      oldStatus,
      IncidentStatus.CLOSED.getName(),
      historyComment
    );

    auditServiceClientService.audit(
      closedBy,
      null,
      null,
      AuditAction.INCIDENT_CLOSE.getName(),
      "INCIDENT",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName(),
      incidentSnapshotWithComment(saved, comment)
    );

    notifyAfterCommit(
      () ->
        notificationClientService.notifyStatusChanged(
          saved,
          msg("incident.status.closed"),
          closedBy,
          comment != null ? comment : closureDescription
        )
    );

    return saved;
  }

  @Override
  @Transactional
  // Annule un incident lorsque son cycle metier doit s'arreter.
  public Incident cancel(UUID id, UUID canceledBy, String reason) {
    Incident incident = findById(id);

    if (!CANCELLABLE_STATUSES.contains(incident.getStatus())) {
      throw new InvalidStatusTransitionException(
        msg("incident.error.action_not_allowed_for_status")
      );
    }

    String oldStatus = incident.getStatus().getName();
    incident.setStatus(IncidentStatus.CANCELLED);
    incident.setCancelReason(reason);
    // Infirmer une demande d'actualite la solde : plus rien n'est attendu de personne.
    incident.setConfirmationRequestedAt(null);
    incident.setConfirmationRequestedBy(null);
    incident.setLastConfirmationReminderSentAt(null);

    Incident saved = incidentRepository.saveAndFlush(incident);
    resolutionCycleRecorder.closeCycle(
      saved,
      ResolutionCycleOutcome.CANCELLED,
      null
    );

    historyService.record(
      saved,
      canceledBy,
      ActionType.STATUS_CHANGE,
      oldStatus,
      IncidentStatus.CANCELLED.getName(),
      msg("incident.history.cancelled") + " : " + reason
    );

    auditServiceClientService.audit(
      canceledBy,
      null,
      null,
      AuditAction.INCIDENT_STATUS_CHANGE.getName(),
      "INCIDENT",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName(),
      incidentSnapshot(saved)
    );

    notifyAfterCommit(
      () ->
        notificationClientService.notifyStatusChanged(
          saved,
          msg("incident.status.cancelled"),
          canceledBy,
          reason
        )
    );

    return saved;
  }

  @Override
  @Transactional
  // Rouvre un incident lorsque les regles metier l'autorisent.
  public Incident reopen(
    UUID id,
    UUID reopenedBy,
    String reason,
    String comment
  ) {
    Incident incident = findById(id);
    requireStatusIn(
      incident,
      Set.of(
        IncidentStatus.RESOLVED,
        IncidentStatus.REJECTED,
        IncidentStatus.UNRESOLVED_PROLONGED_WAIT
      )
    );

    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleViolationException(
        ErrorCode.INCIDENT_REOPEN_REASON_REQUIRED,
        msg("incident.error.reopen_reason_required")
      );
    }

    long maxReopenCount = reportingSystemConfigClientService.getThresholdLong(
      "maxReopenCount",
      2
    );
    if (incident.getReopenCount() >= maxReopenCount) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_STATUS_TRANSITION,
        msg("incident.error.max_reopen_count_reached", maxReopenCount)
      );
    }

    String oldStatus = incident.getStatus().getName();
    IncidentStatus newStatus;

    if (incident.getStatus() == IncidentStatus.REJECTED) {
      long limitHours = reportingSystemConfigClientService.getThresholdLong(
        "reopenTimeLimitHours",
        48
      );
      LocalDateTime referenceTime = resolveReopenReferenceTime(incident);
      if (
        referenceTime != null &&
        LocalDateTime.now().isAfter(referenceTime.plusHours(limitHours))
      ) {
        throw new BusinessRuleViolationException(
          ErrorCode.INVALID_STATUS_TRANSITION,
          msg("incident.error.reopen_time_limit_exceeded", limitHours)
        );
      }

      UUID rejectorId = null;
      if (incident.getHistory() != null) {
        rejectorId = incident
          .getHistory()
          .stream()
          .filter(h ->
            IncidentStatus.REJECTED.getName().equals(h.getNewValue())
          )
          .max(Comparator.comparing(IncidentHistory::getCreatedAt))
          .map(IncidentHistory::getUserId)
          .orElse(null);
      }
      boolean isCreator = reopenedBy.equals(incident.getCreatedBy());
      boolean isRejector = reopenedBy.equals(rejectorId);
      if (!isCreator && !isRejector) {
        throw new AccessDeniedException(
          msg("incident.error.unauthorized_reopen")
        );
      }
      newStatus = IncidentStatus.REOPENED;
    } else if (
      incident.getStatus() == IncidentStatus.UNRESOLVED_PROLONGED_WAIT
    ) {
      // Attente prolongee : relance complete du cycle (le createur resoumettra).
      newStatus = IncidentStatus.REOPENED;
    } else {
      // RESOLVED
      if (incident.getAssignedTo() == null) {
        throw new BusinessRuleViolationException(
          ErrorCode.INVALID_STATUS_TRANSITION,
          msg("incident.error.reopen_needs_assignee")
        );
      }
      try {
        ExternalUser assignee = userClientService.getUser(
          incident.getAssignedTo()
        );
        if (!assignee.isActive()) {
          throw new BusinessRuleViolationException(
            ErrorCode.INVALID_STATUS_TRANSITION,
            msg("incident.error.reopen_assignee_inactive")
          );
        }
      } catch (Exception e) {
        throw new BusinessRuleViolationException(
          ErrorCode.INVALID_STATUS_TRANSITION,
          msg("incident.error.reopen_assignee_not_found")
        );
      }
      newStatus = IncidentStatus.IN_PROGRESS;
    }

    incident.setStatus(newStatus);
    if (newStatus == IncidentStatus.PENDING_VALIDATION) {
      startDecisionWait(incident);
    }

    // Capture avant remise a zero : le traitant du cycle precedent est detache mais
    // reste le premier concerne par la reouverture.
    UUID previousAssignee = incident.getAssignedTo();
    UUID previousValidator = incident.getValidatedBy();

    // Les comptes rendus du cycle precedent sont remis a zero pour le nouveau cycle ;
    // ils restent consultables dans l'historique et via les PJ, datees par cycle.
    if (newStatus == IncidentStatus.REOPENED) {
      incident.setValidatedBy(null);
      incident.setValidatedAt(null);
      incident.setResolvedAt(null);
      incident.setClosedAt(null);
      incident.setTreatmentDescription(null);
      incident.setResolutionDescription(null);
      incident.setClosureDescription(null);
      incident.setRejectReason(null);
      incident.setCancelReason(null);
      incident.setAssignedTo(null);
    } else {
      // newStatus == IN_PROGRESS
      incident.setResolvedAt(null);
      incident.setTreatmentDescription(null);
      incident.setResolutionDescription(null);
    }

    // Reouverture = nouveau cycle : on solde le contexte de blocage du cycle precedent
    // (conserve dans l'historique) pour ne pas afficher un motif/date de blocage perimes.
    incident.setBlockedAt(null);
    incident.setBlockedBy(null);
    incident.setBlockedReason(null);
    incident.setUnblockedAt(null);
    incident.setPreBlockStatus(null);

    incident.setReopenReason(reason);
    incident.setReopenedAt(LocalDateTime.now());
    incident.setReopenedBy(reopenedBy);
    incident.setReopenCount(incident.getReopenCount() + 1);

    Incident saved = incidentRepository.saveAndFlush(incident);
    resolutionCycleRecorder.reopenCycle(saved, saved.getReopenedAt());

    String historyComment =
      comment != null && !comment.isBlank() ? reason + " — " + comment : reason;
    historyService.record(
      saved,
      reopenedBy,
      ActionType.REOPENING,
      oldStatus,
      newStatus.getName(),
      historyComment
    );

    auditServiceClientService.audit(
      reopenedBy,
      null,
      null,
      AuditAction.INCIDENT_STATUS_CHANGE.getName(),
      "INCIDENT",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName(),
      incidentSnapshotWithComment(saved, comment)
    );

    notifyAfterCommit(
      () ->
        notificationClientService.notifyStatusChanged(
          saved,
          msg("incident.history.reopened"),
          reopenedBy,
          reason,
          Stream.of(previousAssignee, previousValidator)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet())
        )
    );

    return saved;
  }

  // Realise l'intention metier clone incident.

  @Transactional
  public Incident cloneIncident(UUID id, UUID clonedBy) {
    Incident original = findById(id);

    Incident clone = new Incident();
    clone.setTitle(original.getTitle());
    clone.setDescription(original.getDescription());
    clone.setTypeId(original.getTypeId());
    clone.setCriticality(original.getCriticality());
    clone.setCause(original.getCause());
    clone.setCauseDetail(original.getCauseDetail());
    clone.setIncidentDate(original.getIncidentDate());
    clone.setObservationDate(original.getObservationDate());
    clone.setAgencyId(original.getAgencyId());
    clone.setCreatorServiceId(original.getCreatorServiceId());
    clone.setInitialTargetServiceId(null);

    clone.setSourceIncidentId(original.getId());
    clone.setCreatedBy(clonedBy);

    ExternalUser user = userClientService.getUser(clonedBy);
    boolean hasPrivilege =
      user.getPermissions() != null &&
      user.getPermissions().contains("INCIDENT_CREATE_OPEN");
    IncidentTypeConfig typeConfig = resolveTypeConfig(clone.getTypeId());
    boolean ownServiceValidation = validationPolicy.isCreatedByTargetServiceHead(clone, typeConfig);
    IncidentStatus initialStatus = hasPrivilege && !ownServiceValidation
      ? IncidentStatus.OPEN
      : IncidentStatus.PENDING_VALIDATION;
    clone.setStatus(initialStatus);
    if (initialStatus == IncidentStatus.PENDING_VALIDATION) {
      startDecisionWait(clone);
    }

    if (!typeConfig.isActive()) {
      throw new BusinessRuleViolationException(
        ErrorCode.INCIDENT_TYPE_INACTIVE,
        msg("incident.error.type_inactive", typeConfig.getName())
      );
    }

    // Le clone est une declaration neuve : son echeance repart du SLA du type,
    // sinon il herite d'un delai deja consomme (voire depasse) par l'original.
    applySlaDueDate(clone, typeConfig);

    UUID effectiveServiceId = typeConfig.getDefaultTargetServiceId();

    if (
      initialStatus == IncidentStatus.PENDING_VALIDATION &&
      typeConfig.getValidatorScope() ==
        IncidentValidatorScope.TARGET_SERVICE_MANAGER
    ) {
      clone.setTransferredToService(effectiveServiceId);
    }
    clone.setInitialTargetServiceId(clone.getTransferredToService());

    if (
      typeConfig.getDefaultTargetServiceId() == null &&
      typeConfig.getDefaultTargetUserId() != null &&
      clone.getAssignedTo() == null
    ) {
      clone.setAssignedTo(typeConfig.getDefaultTargetUserId());
    }

    clone.setReference(referenceGenerator.next());

    Incident saved = incidentRepository.saveAndFlush(clone);
    resolutionCycleRecorder.openFirstCycle(saved);

    String clonedFromDetail =
      msg("incident.history.cloned") + " « " + original.getTitle() + " »";

    historyService.record(
      saved,
      clonedBy,
      ActionType.CREATION,
      null,
      clone.getStatus().getName(),
      clonedFromDetail
    );

    auditServiceClientService.audit(
      clonedBy,
      null,
      null,
      "CREATE",
      "INCIDENT",
      saved.getId().toString(),
      "SUCCESS",
      incidentSnapshot(saved)
    );

    notifyAfterCommit(
      () ->
        notificationClientService.notifyIncidentSubmitted(
          saved,
          typeConfig.getValidatorScope(),
          clonedBy,
          clonedFromDetail
        )
    );

    if (hasPrivilege && !ownServiceValidation && effectiveServiceId != null) {
      log.debug(
        "Transfert automatique de l'incident cloné {} vers le service {}",
        saved.getId(),
        effectiveServiceId
      );
      return doTransfer(
        saved,
        clonedBy,
        effectiveServiceId,
        null,
        "Transfert automatique vers le service cible lors du clonage",
        true
      );
    }

    return saved;
  }

  // Realise l'intention metier resubmit.

  @Override
  @Transactional
  public Incident resubmit(
    UUID id,
    UUID submittedBy,
    String comment,
    boolean requiresCreatorValidation
  ) {
    Incident incident = findById(id);
    requireStatus(incident, IncidentStatus.REOPENED);

    boolean isCreator = submittedBy.equals(incident.getCreatedBy());
    if (!isCreator) {
      throw new AccessDeniedException(
        msg("incident.error.unauthorized_resubmit")
      );
    }

    IncidentTypeConfig typeConfig = resolveTypeConfig(incident.getTypeId());

    if (!typeConfig.isActive()) {
      throw new BusinessRuleViolationException(
        ErrorCode.INCIDENT_TYPE_INACTIVE,
        msg("incident.error.type_inactive", typeConfig.getName())
      );
    }

    String oldStatus = incident.getStatus().getName();

    boolean ownServiceValidation = validationPolicy.isCreatedByTargetServiceHead(incident, typeConfig);
    IncidentStatus newStatus =
      ownServiceValidation || (requiresCreatorValidation && typeConfig.isRequiresValidation())
        ? IncidentStatus.PENDING_VALIDATION
        : IncidentStatus.OPEN;

    incident.setStatus(newStatus);
    if (newStatus == IncidentStatus.PENDING_VALIDATION) {
      startDecisionWait(incident);
    }

    UUID effectiveServiceId = typeConfig.getDefaultTargetServiceId() != null
      ? typeConfig.getDefaultTargetServiceId()
      : incident.getTransferredToService();

    if (
      newStatus == IncidentStatus.PENDING_VALIDATION &&
      typeConfig.getValidatorScope() ==
        IncidentValidatorScope.TARGET_SERVICE_MANAGER
    ) {
      incident.setTransferredToService(effectiveServiceId);
    }

    if (
      typeConfig.getDefaultTargetServiceId() == null &&
      typeConfig.getDefaultTargetUserId() != null &&
      incident.getAssignedTo() == null
    ) {
      incident.setAssignedTo(typeConfig.getDefaultTargetUserId());
    }

    // Aucun cycle n'est ouvert ici : la reouverture qui a mene a REOUVERT l'a deja
    // fait. En ouvrir un second laissait un cycle fantome, sans aucun jalon, entre
    // la reouverture et la resoumission.
    Incident saved = incidentRepository.saveAndFlush(incident);

    historyService.record(
      saved,
      submittedBy,
      ActionType.STATUS_CHANGE,
      oldStatus,
      newStatus.getName(),
      comment
    );

    auditServiceClientService.audit(
      submittedBy,
      null,
      null,
      AuditAction.INCIDENT_STATUS_CHANGE.getName(),
      "INCIDENT",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName(),
      incidentSnapshotWithComment(saved, comment)
    );

    Incident submittedIncident = saved;
    notifyAfterCommit(
      () ->
        notificationClientService.notifyIncidentSubmitted(
          submittedIncident,
          typeConfig.getValidatorScope(),
          submittedBy,
          comment
        )
    );

    if (!requiresCreatorValidation && !ownServiceValidation && effectiveServiceId != null) {
      if (!effectiveServiceId.equals(saved.getTransferredToService())) {
        log.debug(
          "Transfert automatique de l'incident {} vers le service {}",
          saved.getId(),
          effectiveServiceId
        );
        return doTransfer(
          saved,
          submittedBy,
          effectiveServiceId,
          null,
          "Transfert automatique vers le service cible lors de la resoumission",
          true
        );
      } else if (saved.getAssignedTo() == null) {
        log.debug(
          "Auto-assignation de l'incident {} déjà rattaché au service {} lors de la resoumission",
          saved.getId(),
          effectiveServiceId
        );
        saved = autoAssignToResponsible(
          saved,
          submittedBy,
          effectiveServiceId,
          null
        );
      }
    }

    return saved;
  }

  // Realise l'intention metier do transfer.

  private Incident doTransfer(
    Incident incident,
    UUID transferredBy,
    UUID targetServiceId,
    String reason,
    String comment,
    boolean automatic
  ) {
    return doTransfer(
      incident,
      transferredBy,
      targetServiceId,
      null,
      reason,
      comment,
      null,
      automatic
    );
  }

  // Surcharge : transfert avec responsable explicite (auto-transfert), sans
  // reclassification de type.
  private Incident doTransfer(
    Incident incident,
    UUID transferredBy,
    UUID targetServiceId,
    String reason,
    String comment,
    UUID explicitResponsibleId,
    boolean automatic
  ) {
    return doTransfer(
      incident,
      transferredBy,
      targetServiceId,
      null,
      reason,
      comment,
      explicitResponsibleId,
      automatic
    );
  }

  // Realise l'intention metier do transfer avec reclassification optionnelle du type.
  // `automatic` distingue le transfert decide par un utilisateur de celui derive par le
  // workflow : le second est historise au nom du systeme, jamais de l'utilisateur qui a
  // lance l'action amont (cf. historyAuthor).
  private Incident doTransfer(
    Incident incident,
    UUID transferredBy,
    UUID targetServiceId,
    UUID newTypeId,
    String reason,
    String comment,
    UUID explicitResponsibleId,
    boolean automatic
  ) {
    String oldStatus = incident.getStatus().getName();
    // Un transfert vers un autre service rompt la relation avec l'ancien
    // assigne : l'incident n'est plus traite dans le perimetre du
    // transferant, on libere donc l'affectation pour la reattribuer au
    // chef du service cible juste apres.
    UUID previousAssignee = incident.getAssignedTo();
    UUID oldServiceId = incident.getTransferredToService();
    incident.setAssignedTo(null);
    incident.setStatus(IncidentStatus.TRANSFERRED);
    incident.setTransferredToService(targetServiceId);
    incident.setTransferredAt(LocalDateTime.now());
    incident.setTransferReason(reason);
    incident.setBlockedReason(null);
    incident.setBlockedBy(null);
    incident.setBlockedAt(null);
    voidPendingSolution(incident);

    // Reclassification du type d'incident lors du transfert : si un nouveau
    // type est specifie, on met a jour le typeId et on recalcule le SLA
    // (dueDate) a partir du moment du transfert pour que les metriques du
    // service destinataire soient fideles.
    if (newTypeId != null && !newTypeId.equals(incident.getTypeId())) {
      UUID oldTypeId = incident.getTypeId();
      IncidentTypeConfig newTypeConfig = resolveTypeConfig(newTypeId);
      if (!newTypeConfig.isActive()) {
        throw new BusinessRuleViolationException(
          ErrorCode.INVALID_INPUT,
          msg("incident.error.reclassify_type_inactive")
        );
      }
      if (
        !targetServiceId.equals(newTypeConfig.getDefaultTargetServiceId())
      ) {
        throw new BusinessRuleViolationException(
          ErrorCode.INVALID_INPUT,
          msg("incident.error.reclassify_type_wrong_service")
        );
      }

      incident.setTypeId(newTypeId);

      // Recalcul du SLA a partir de maintenant en utilisant le slaHours du nouveau type.
      applySlaDueDate(incident, newTypeConfig);

      historyService.record(
        incident,
        transferredBy,
        ActionType.TYPE_CHANGE,
        asString(oldTypeId),
        asString(newTypeId),
        msg("incident.history.type_reclassified_on_transfer")
      );
    }

    // Un incident cree sans service cible (auto-attribution, type sans defaut) prend
    // ici son point de depart : ce premier routage n'est pas un reroutage.
    if (incident.getInitialTargetServiceId() == null) {
      incident.setInitialTargetServiceId(targetServiceId);
    }

    Incident saved = incidentRepository.saveAndFlush(incident);

    String targetServiceName = userClientService.resolveServiceName(targetServiceId);
    String actionDetails =
      msg("incident.history.transferred_to_service", targetServiceName) +
      (reason != null && !reason.isBlank()
        ? " - " + msg("incident.history.reason") + ": " + reason
        : "");

    historyService.record(
      saved,
      historyAuthor(transferredBy, automatic),
      ActionType.TRANSFER,
      oldStatus,
      IncidentStatus.TRANSFERRED.getName(),
      comment != null ? comment : actionDetails
    );

    // Trace le deplacement de service lui-meme, avec les identifiants en clair :
    // la ligne TRANSFER ne porte que des statuts, la chaine des services traverses
    // n'etait donc conservee nulle part. ActionType.ROUTING existait pour cela.
    historyService.record(
      saved,
      historyAuthor(transferredBy, automatic),
      ActionType.ROUTING,
      asString(oldServiceId),
      asString(targetServiceId),
      actionDetails
    );

    // Trace la liberation explicite de l'ancien assigne pour audit.
    if (previousAssignee != null) {
      historyService.record(
        saved,
        historyAuthor(transferredBy, automatic),
        ActionType.ASSIGNMENT,
        previousAssignee.toString(),
        null,
        msg("incident.history.unassigned_after_transfer")
      );
    }

    auditServiceClientService.audit(
      transferredBy,
      null,
      null,
      AuditAction.INCIDENT_TRANSFER.getName(),
      "INCIDENT",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName(),
      incidentSnapshot(saved)
    );

    notifyAfterCommit(
      () ->
        notificationClientService.notifyIncidentTransferred(
          saved,
          oldServiceId,
          targetServiceId,
          transferredBy
        )
    );

    // Reaffectation automatique au responsable du service cible.
    return autoAssignToResponsible(
      saved,
      transferredBy,
      targetServiceId,
      explicitResponsibleId
    );
  }

  // Transfere l'incident vers l'agence d'origine et l'assigne a son responsable.
  private Incident doTransferToAgency(
    Incident incident,
    UUID transferredBy,
    UUID targetAgencyId,
    String reason,
    String comment
  ) {
    UUID agencyHeadId = userClientService.resolveAgencyHead(targetAgencyId);
    if (agencyHeadId == null) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        msg("incident.error.transfer_agency_head_required")
      );
    }

    String oldStatus = incident.getStatus().getName();
    UUID previousAssignee = incident.getAssignedTo();
    UUID oldServiceId = incident.getTransferredToService();
    incident.setAssignedTo(null);
    incident.setStatus(IncidentStatus.TRANSFERRED);
    incident.setTransferredToService(null);
    incident.setTransferredAt(LocalDateTime.now());
    incident.setTransferReason(reason);
    incident.setBlockedReason(null);
    incident.setBlockedBy(null);
    incident.setBlockedAt(null);

    Incident saved = incidentRepository.saveAndFlush(incident);
    String targetAgencyName = userClientService.resolveAgencyName(targetAgencyId);
    String actionDetails =
      msg("incident.history.transferred_to_agency", targetAgencyName) +
      (reason != null && !reason.isBlank()
        ? " - " + msg("incident.history.reason") + ": " + reason
        : "");

    historyService.record(
      saved,
      transferredBy,
      ActionType.TRANSFER,
      oldStatus,
      IncidentStatus.TRANSFERRED.getName(),
      comment != null ? comment : actionDetails
    );
    if (previousAssignee != null) {
      historyService.record(
        saved,
        transferredBy,
        ActionType.ASSIGNMENT,
        previousAssignee.toString(),
        null,
        msg("incident.history.unassigned_after_transfer")
      );
    }

    auditServiceClientService.audit(
      transferredBy,
      null,
      null,
      AuditAction.INCIDENT_TRANSFER.getName(),
      "INCIDENT",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName(),
      incidentSnapshot(saved)
    );
    notifyAfterCommit(
      () ->
        notificationClientService.notifyIncidentTransferredToAgency(
          saved,
          oldServiceId,
          targetAgencyId,
          transferredBy
        )
    );
    return autoAssignToAgencyHead(saved, agencyHeadId);
  }

  // Assigne un incident transfere a l'agence au chef de cette agence.
  private Incident autoAssignToAgencyHead(
    Incident incident,
    UUID agencyHeadId
  ) {
    incident.setAssignedTo(agencyHeadId);
    String oldStatus = incident.getStatus().getName();
    incident.setStatus(IncidentStatus.ASSIGNED);
    Incident saved = incidentRepository.saveAndFlush(incident);
    // Assignation derivee du transfert, pas choisie par le transferant : elle est
    // signee par le systeme (miroir de autoAssignToResponsible).
    historyService.record(
      saved,
      null,
      ActionType.ASSIGNMENT,
      null,
      agencyHeadId.toString(),
      msg("incident.history.auto_assigned_to_agency_head")
    );
    historyService.record(
      saved,
      null,
      ActionType.STATUS_CHANGE,
      oldStatus,
      IncidentStatus.ASSIGNED.getName(),
      msg("incident.history.auto_assigned_to_agency_head")
    );
    notifyAfterCommit(() ->
      notificationClientService.notifyIncidentAssigned(saved, agencyHeadId)
    );
    return saved;
  }

  /**
   * Affecte l'incident au responsable par défaut du type, sinon au chef du service cible.
   * À utiliser après une libération d'affectation (transfert) ou à la création
   * d'un incident non auto-attribué. Si l'incident est déjà assigné, l'appel
   * est un no-op pour conserver l'affectation explicite.
   * Si aucun responsable n'est trouvé, l'incident reste non assigné et une alerte est levée.
   */
  // Realise l'intention metier auto assign to responsible.
  private Incident autoAssignToResponsible(
    Incident incident,
    UUID actor,
    UUID targetServiceId,
    UUID explicitResponsibleId
  ) {
    if (incident.getAssignedTo() != null) {
      return incident;
    }
    IncidentTypeConfig typeConfig = resolveTypeConfig(incident.getTypeId());
    UUID responsible = explicitResponsibleId;

    // Utilise l'utilisateur cible par defaut uniquement lorsque le service cible par defaut est retenu.
    if (
      responsible == null &&
      targetServiceId.equals(typeConfig.getDefaultTargetServiceId())
    ) {
      responsible = typeConfig.getDefaultTargetUserId();
    }

    if (responsible == null) {
      responsible = userClientService.resolveServiceHead(targetServiceId);
    }
    if (responsible == null) {
      log.warn(
        "ALERTE: aucun responsable trouvé pour le service {} — incident {} laissé non assigné",
        targetServiceId,
        incident.getId()
      );
      notifyAfterCommit(
        () ->
          notificationClientService.notifyIncidentUnassigned(
            incident,
            targetServiceId
          )
      );
      Map<String, Object> snapshot = incidentSnapshot(incident);
      snapshot.put(
        "anomalyReason",
        msg(
          "incident.anomaly.transferred_without_assignee",
          userClientService.resolveServiceName(targetServiceId)
        )
      );

      auditServiceClientService.audit(
        actor,
        null,
        null,
        "DATA_QUALITY_ANOMALY",
        "INCIDENT",
        incident.getId().toString(),
        "WARNING",
        snapshot
      );
      return incident;
    }
    incident.setAssignedTo(responsible);
    String oldStatus = incident.getStatus().getName();
    incident.setStatus(IncidentStatus.ASSIGNED);
    Incident saved = incidentRepository.saveAndFlush(incident);
    // C'est le workflow qui designe le responsable, pas l'utilisateur en amont :
    // ces deux entrees sont signees par le systeme. L'audit, lui, garde `actor`.
    historyService.record(
      saved,
      null,
      ActionType.ASSIGNMENT,
      null,
      responsible.toString(),
      msg("incident.history.auto_assigned_to_service_head")
    );
    historyService.record(
      saved,
      null,
      ActionType.STATUS_CHANGE,
      oldStatus,
      IncidentStatus.ASSIGNED.getName(),
      msg("incident.history.auto_assigned_to_service_head")
    );
    UUID notifiedResponsible = responsible;
    notifyAfterCommit(() ->
      notificationClientService.notifyIncidentAssigned(saved, notifiedResponsible)
    );
    return saved;
  }

  // Verifie que les regles metier autorisent l operation sur incident.

  private void validateExplicitAssignee(
    UUID userId,
    UUID targetServiceId,
    UUID agencyId
  ) {
    ExternalUser user = userClientService.getUser(userId);
    if (user == null || !user.isActive()) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        msg("incident.error.assignee_inactive_or_not_found")
      );
    }
    Set<String> permissions = user.getPermissions();
    if (
      permissions == null ||
      (!permissions.contains("INCIDENT_TREAT") &&
        !permissions.contains("INCIDENT_RESOLVE"))
    ) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        msg("incident.error.assignee_lacks_permission")
      );
    }
    if (targetServiceId != null) {
      boolean inService =
        targetServiceId.equals(user.getServiceId()) ||
        (user.getManagedServiceIds() != null &&
          user.getManagedServiceIds().contains(targetServiceId));
      if (!inService) {
        throw new BusinessRuleViolationException(
          ErrorCode.INVALID_INPUT,
          msg("incident.error.assignee_outside_service")
        );
      }
      return;
    }
    if (
      agencyId != null &&
      user.getAgencyId() != null &&
      !agencyId.equals(user.getAgencyId())
    ) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        msg("incident.error.assignee_outside_agency")
      );
    }
  }

  // Determine l'instant de reference pour la fenetre de reouverture :
  // resolvedAt pour un incident RESOLVED, sinon l'horodatage de la derniere
  // transition REJECTED dans l'historique (pas de champ rejectedAt dedie).
  private LocalDateTime resolveReopenReferenceTime(Incident incident) {
    if (incident.getStatus() == IncidentStatus.RESOLVED) {
      return incident.getResolvedAt();
    }
    if (incident.getHistory() == null) {
      return incident.getUpdatedAt();
    }
    return incident
      .getHistory()
      .stream()
      .filter(h -> IncidentStatus.REJECTED.getName().equals(h.getNewValue()))
      .map(IncidentHistory::getCreatedAt)
      .filter(Objects::nonNull)
      .max(Comparator.naturalOrder())
      .orElse(incident.getUpdatedAt());
  }

  // Diffuse l'evenement seulement lorsque la transaction metier est validee.
  private void notifyAfterCommit(Runnable notification) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      notification.run();
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
      new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          notification.run();
        }
      }
    );
  }

  // Exige status.

  private void requireStatus(Incident incident, IncidentStatus required) {
    if (incident.getStatus() != required) {
      throw new InvalidStatusTransitionException(
        msg("incident.error.invalid_expected_status", required.getName(), incident.getStatus().getName())
      );
    }
  }

  // Exige status in.

  private void requireStatusIn(Incident incident, Set<IncidentStatus> allowed) {
    if (!allowed.contains(incident.getStatus())) {
      throw new InvalidStatusTransitionException(
        msg("incident.error.invalid_expected_status_in", allowed.stream().map(IncidentStatus::getName).toList(), incident.getStatus().getName())
      );
    }
  }

  // Traitant sur lequel l'incident repart apres confirmation : l'assigne s'il est
  // toujours actif, sinon le chef du cote traitant (service, ou agence quand c'est elle
  // qui porte l'incident). null si aucun des deux n'est disponible : la reprise n'est
  // jamais refusee, l'incident repart non assigne et l'alerte part (miroir de
  // autoAssignToResponsible lors d'un transfert).
  private UUID resolveConfirmationAssignee(Incident incident) {
    UUID assigneeId = incident.getAssignedTo();
    if (assigneeId != null && isActiveUser(assigneeId)) {
      return assigneeId;
    }
    UUID handlingService = incident.getTransferredToService();
    UUID headId = handlingService != null
      ? userClientService.resolveServiceHead(handlingService)
      : userClientService.resolveAgencyHead(incident.getAgencyId());
    return headId != null && isActiveUser(headId) ? headId : null;
  }

  // Alerte "reprise sans traitant" : on previent l'entite qui porte l'incident,
  // service ou agence, par les canaux deja utilises pour une desassignation.
  private void notifyResumedWithoutAssignee(Incident incident) {
    UUID handlingService = incident.getTransferredToService();
    if (handlingService != null) {
      notificationClientService.notifyIncidentUnassigned(
        incident,
        handlingService
      );
      return;
    }
    notificationClientService.notifyIncidentUnassignedAtAgency(
      incident,
      incident.getAgencyId()
    );
  }

  // Un utilisateur injoignable est traite comme indisponible : le repli doit jouer,
  // pas remonter une panne de l'annuaire au valideur.
  private boolean isActiveUser(UUID userId) {
    try {
      ExternalUser user = userClientService.getUser(userId);
      return user != null && user.isActive();
    } catch (RuntimeException ex) {
      return false;
    }
  }

  // Auteur d'une entree d'historique. Une transition derivee par le workflow n'est pas
  // signee par l'utilisateur qui a lance l'action amont : sans cela, valider un incident
  // faisait apparaitre le valideur comme ayant aussi transfere puis assigne a la main.
  private static UUID historyAuthor(UUID actor, boolean automatic) {
    return automatic ? null : actor;
  }

  // Resume lisible d'une requalification, pour le corps de la notification.
  private String qualificationSummary(
    UUID previousTypeId,
    Criticality previousCriticality,
    Incident saved
  ) {
    List<String> lines = new ArrayList<>();
    if (previousTypeId != null) {
      lines.add(
        msg(
          "incident.qualification.change.type",
          typeDisplayName(previousTypeId),
          typeDisplayName(saved.getTypeId())
        )
      );
    }
    if (previousCriticality != null) {
      lines.add(
        msg(
          "incident.qualification.change.criticality",
          msg(previousCriticality.getNameKey()),
          msg(saved.getCriticality().getNameKey())
        )
      );
    }
    return String.join(" ; ", lines);
  }

  // Libelle affichable d'un type d'incident, avec repli sur l'identifiant brut.
  private String typeDisplayName(UUID typeId) {
    if (typeId == null) return "";
    return incidentTypeConfigRepository
      .findById(typeId)
      .map(IncidentTypeConfig::getDisplayName)
      .orElseGet(() -> typeId.toString());
  }

  // Ouvre une attente de decision : le compteur de relance repart avec elle.
  private void startDecisionWait(Incident incident) {
    incident.setDecisionAwaitedSince(LocalDateTime.now());
    incident.setLastDecisionReminderSentAt(null);
  }

  // Heures de SLA en vigueur pour un type : la valeur du type, sinon le seuil global.
  private Integer resolveSlaHours(IncidentTypeConfig typeConfig) {
    return typeConfig.getSlaHours() != null
      ? typeConfig.getSlaHours()
      : (int) reportingSystemConfigClientService.getThresholdLong(
        "defaultSlaHours",
        defaultSlaHours != null ? defaultSlaHours : 48
      );
  }

  // Redonne du temps de traitement sans toucher a l'engagement : l'attente d'une
  // validation ne doit pas consommer le delai du traitant.
  private void restartTreatmentDeadline(Incident incident) {
    Integer slaHours = resolveSlaHours(resolveTypeConfig(incident.getTypeId()));
    incident.setDueDate(
      slaHours != null && slaHours > 0
        ? LocalDateTime.now().plusHours(slaHours)
        : null
    );
  }

  // Repositionne l'echeance sur le SLA du type, decompte a partir de maintenant.
  // Seul point de calcul de dueDate : creation, clonage, requalification et entree dans
  // le circuit (validation). L'echeance de reference suit ici, et ici seulement : ces
  // gestes (re)negocient l'engagement, une reprise ne fait que redonner du temps sur un
  // engagement deja pris.
  private void applySlaDueDate(Incident incident, IncidentTypeConfig typeConfig) {
    Integer effectiveSlaHours = resolveSlaHours(typeConfig);
    LocalDateTime dueDate = effectiveSlaHours != null && effectiveSlaHours > 0
      ? LocalDateTime.now().plusHours(effectiveSlaHours)
      : null;
    incident.setDueDate(dueDate);
    incident.setInitialDueDate(dueDate);
  }

  /** Résout une clé i18n en utilisant la locale du contexte courant. */
  private String msg(String key, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(key, args, key, locale);
  }
}
