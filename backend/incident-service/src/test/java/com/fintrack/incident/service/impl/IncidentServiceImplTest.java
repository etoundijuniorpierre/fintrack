package com.fintrack.incident.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fintrack.incident.client.audit.AuditServiceClientService;
import com.fintrack.incident.client.notification.NotificationClientService;
import com.fintrack.incident.client.reporting.ReportingSystemConfigClientService;
import com.fintrack.incident.client.user.UserClientService;
import com.fintrack.incident.exception.BusinessRuleViolationException;
import com.fintrack.incident.exception.EntityNotFoundException;
import com.fintrack.incident.exception.InvalidStatusTransitionException;
import com.fintrack.incident.model.constant.ActionType;
import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentCause;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.constant.IncidentValidatorScope;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentTypeConfig;
import com.fintrack.incident.model.readmodel.ExternalUser;
import com.fintrack.incident.repository.IncidentRepository;
import com.fintrack.incident.repository.IncidentTypeConfigRepository;
import com.fintrack.incident.security.IncidentWorkflowGuard;
import com.fintrack.incident.security.IncidentValidationPolicy;
import com.fintrack.incident.security.UserDetailsImpl;
import com.fintrack.incident.service.IncidentHistoryService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class IncidentServiceImplTest {

  @Mock
  private IncidentRepository incidentRepository;

  @Mock
  private IncidentTypeConfigRepository incidentTypeConfigRepository;

  @Mock
  private IncidentHistoryService historyService;

  @Mock
  private AuditServiceClientService auditServiceClientService;

  @Mock
  private NotificationClientService notificationClientService;

  @Mock
  private MessageSource messageSource;
  @Mock private com.fintrack.incident.service.AttachmentCleanupService attachmentCleanup;

  @Mock
  private UserClientService userClientService;

  @Mock
  private IncidentWorkflowGuard incidentWorkflowGuard;

  @Mock
  private IncidentValidationPolicy validationPolicy;

  @Mock
  private ReportingSystemConfigClientService reportingSystemConfigClientService;

  @Mock
  private com.fintrack.incident.service.IncidentReferenceGenerator referenceGenerator;

  @Mock
  private com.fintrack.incident.service.ResolutionCycleRecorder resolutionCycleRecorder;

  @InjectMocks
  private IncidentServiceImpl incidentService;

  private UUID incidentId;
  private UUID userId;
  private UUID agencyId;
  private UUID serviceId;
  private UUID typeConfigId;
  private Incident incident;
  private IncidentTypeConfig typeConfig;

  @BeforeEach
  void setUp() {
    incidentId = UUID.randomUUID();
    userId = UUID.randomUUID();
    agencyId = UUID.randomUUID();
    serviceId = UUID.randomUUID();
    typeConfigId = UUID.randomUUID();

    typeConfig = new IncidentTypeConfig();
    typeConfig.setId(typeConfigId);
    typeConfig.setName("IT");
    typeConfig.setDisplayName("Informatique");
    typeConfig.setActive(true);
    typeConfig.setSlaHours(48);
    typeConfig.setRequiresValidation(true);
    typeConfig.setValidatorScope(IncidentValidatorScope.AGENCY_MANAGER);

    incident = new Incident();
    incident.setId(incidentId);
    incident.setTitle("Test incident");
    incident.setDescription("Description");
    incident.setTypeId(typeConfigId);
    incident.setCriticality(Criticality.MEDIUM);
    incident.setStatus(IncidentStatus.OPEN);
    incident.setCreatedBy(userId);
    incident.setAgencyId(agencyId);
  }

  // ── findById ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName("findById - Should return incident when it exists")
  void findById_Exists_ReturnsIncident() {
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    assertThat(incidentService.findById(incidentId)).isEqualTo(incident);
  }

  @Test
  @DisplayName("findById - Should throw EntityNotFoundException when not found")
  void findById_NotFound_ThrowsEntityNotFoundException() {
    when(incidentRepository.findById(incidentId)).thenReturn(Optional.empty());
    assertThrows(EntityNotFoundException.class, () ->
      incidentService.findById(incidentId)
    );
  }

  @Test
  @DisplayName("findByReference - Should return incident when the code exists")
  void findByReference_Exists_ReturnsIncident() {
    when(incidentRepository.findByReference("FT-I-2026-0001")).thenReturn(
      Optional.of(incident)
    );
    assertThat(incidentService.findByReference("FT-I-2026-0001")).isEqualTo(
      incident
    );
  }

  @Test
  @DisplayName("findByReference - Should throw EntityNotFoundException when not found")
  void findByReference_NotFound_ThrowsEntityNotFoundException() {
    when(incidentRepository.findByReference("FT-I-2026-9999")).thenReturn(
      Optional.empty()
    );
    assertThrows(EntityNotFoundException.class, () ->
      incidentService.findByReference("FT-I-2026-9999")
    );
  }

  // ── create ────────────────────────────────────────────────────────────────

  @Test
  void create_OwnServiceHeadMustWaitDespiteDirectOpenPermissionAndTypeBypass() {
    typeConfig.setDefaultTargetServiceId(serviceId);
    typeConfig.setRequiresValidation(false);
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(Optional.of(typeConfig));
    when(validationPolicy.isCreatedByTargetServiceHead(incident, typeConfig)).thenReturn(true);
    when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    Incident result = incidentService.create(incident, userId, agencyId, false, serviceId, true);
    assertThat(result.getStatus()).isEqualTo(IncidentStatus.PENDING_VALIDATION);
    assertThat(result.getDecisionAwaitedSince()).isNotNull();
    verify(incidentRepository, never()).saveAndFlush(any());
  }

  @Test
  void resubmit_OwnServiceHeadCannotBypassValidation() {
    incident.setStatus(IncidentStatus.REOPENED);
    typeConfig.setDefaultTargetServiceId(serviceId);
    when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(Optional.of(typeConfig));
    when(validationPolicy.isCreatedByTargetServiceHead(incident, typeConfig)).thenReturn(true);
    when(incidentRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    Incident result = incidentService.resubmit(incidentId, userId, "Correction", false);
    assertThat(result.getStatus()).isEqualTo(IncidentStatus.PENDING_VALIDATION);
    assertThat(result.getAssignedTo()).isNull();
    assertThat(result.getDecisionAwaitedSince()).isNotNull();
  }

  @Test
  void clone_OwnServiceHeadCannotBypassValidation() {
    typeConfig.setDefaultTargetServiceId(serviceId);
    when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(Optional.of(typeConfig));
    when(userClientService.getUser(userId)).thenReturn(ExternalUser.builder()
      .id(userId).permissions(Set.of("INCIDENT_CREATE_OPEN")).build());
    when(validationPolicy.isCreatedByTargetServiceHead(any(Incident.class), eq(typeConfig))).thenReturn(true);
    when(incidentRepository.saveAndFlush(any())).thenAnswer(inv -> {
      Incident saved = inv.getArgument(0);
      saved.setId(UUID.randomUUID());
      return saved;
    });
    Incident result = incidentService.cloneIncident(incidentId, userId);
    assertThat(result.getStatus()).isEqualTo(IncidentStatus.PENDING_VALIDATION);
    assertThat(result.getDecisionAwaitedSince()).isNotNull();
    assertThat(result.getAssignedTo()).isNull();
  }

  @Test
  @DisplayName(
    "create - Creator without direct-open permission sets status to PENDING_VALIDATION"
  )
  void create_CreatorRequiringValidation_SetsPendingValidation() {
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(incidentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.create(
      incident,
      userId,
      agencyId,
      true,
      null,
      false
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.PENDING_VALIDATION);
    assertThat(result.getCreatedBy()).isEqualTo(userId);
    assertThat(result.getAgencyId()).isEqualTo(agencyId);
    verify(notificationClientService).notifyIncidentSubmitted(
      result,
      IncidentValidatorScope.AGENCY_MANAGER,
      userId,
      null
    );
  }

  @Test
  @DisplayName(
    "create - Creator with direct-open permission sets status to OPEN"
  )
  void create_CreatorAllowedToOpen_SetsOpen() {
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(incidentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.create(
      incident,
      userId,
      agencyId,
      false,
      null,
      false
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.OPEN);
    verify(notificationClientService).notifyIncidentSubmitted(
      result,
      IncidentValidatorScope.AGENCY_MANAGER,
      userId,
      null
    );
  }

  @Test
  @DisplayName(
    "create - Agent creator skips validation when type requiresValidation is false"
  )
  void create_AgentCreator_RequiresValidationFalse_SetsOpen() {
    typeConfig.setRequiresValidation(false);
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(incidentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.create(
      incident,
      userId,
      agencyId,
      true,
      null,
      false
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.OPEN);
  }

  @Test
  @DisplayName("create - Default criticality MEDIUM when not provided")
  void create_NoCriticality_DefaultsMedium() {
    incident.setCriticality(null);
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(incidentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.create(
      incident,
      userId,
      agencyId,
      false,
      null,
      false
    );

    assertThat(result.getCriticality()).isEqualTo(Criticality.MEDIUM);
  }

  @Test
  @DisplayName("create - SLA due date computed from type config slaHours")
  void create_SlaHoursSet_ComputesDueDate() {
    typeConfig.setSlaHours(48);
    incident.setDueDate(null);
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(incidentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.create(
      incident,
      userId,
      agencyId,
      false,
      null,
      false
    );

    assertThat(result.getDueDate()).isNotNull();
    // Le SLA est horaire : l'echeance tombe 48 h apres la creation, pas en fin de jour.
    assertThat(result.getDueDate()).isCloseTo(
      LocalDateTime.now().plusHours(48),
      within(1, ChronoUnit.MINUTES)
    );
  }

  @Test
  @DisplayName("create - SLA due date falls back to system configuration when type config has null slaHours")
  void create_SlaHoursNull_ComputesDueDateFromSystemConfig() {
    typeConfig.setSlaHours(null);
    incident.setDueDate(null);
    
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(reportingSystemConfigClientService.getThresholdLong("defaultSlaHours", 48))
      .thenReturn(72L);
    when(incidentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.create(
      incident,
      userId,
      agencyId,
      false,
      null,
      false
    );

    assertThat(result.getDueDate()).isNotNull();
    assertThat(result.getDueDate()).isCloseTo(
      LocalDateTime.now().plusHours(72),
      within(1, ChronoUnit.MINUTES)
    );
  }

  @Test
  @DisplayName(
    "create - Auto-transfer assigns service head without starting processing"
  )
  void create_AutoTransferServiceId_TransfersAfterCreate() {
    UUID responsibleId = UUID.randomUUID();
    typeConfig.setDefaultTargetServiceId(serviceId);
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(userClientService.resolveServiceHead(serviceId)).thenReturn(
      responsibleId
    );
    when(incidentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.create(
      incident,
      userId,
      agencyId,
      false,
      null,
      false
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
    assertThat(result.getTransferredToService()).isEqualTo(serviceId);
    assertThat(result.getAssignedTo()).isEqualTo(responsibleId);
  }

  @Test
  @DisplayName(
    "create - Self-assignment overrides the type's default target service"
  )
  void create_AssignToSelf_OverridesTypeTargetService() {
    typeConfig.setDefaultTargetServiceId(serviceId);
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(incidentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    // assignToSelf=true : l'incident est attribué au créateur, sans transfert vers le service du type.
    Incident result = incidentService.create(
      incident,
      userId,
      agencyId,
      false,
      null,
      true
    );

    assertThat(result.getAssignedTo()).isEqualTo(userId);
    assertThat(result.getStatus()).isEqualTo(IncidentStatus.OPEN);
    assertThat(result.getTransferredToService()).isNull();
  }

  @Test
  @DisplayName("create - No auto-transfer for agent (PENDING_VALIDATION)")
  void create_AgentWithAutoTransfer_NoAutoTransfer() {
    typeConfig.setDefaultTargetServiceId(serviceId);
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(incidentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.create(
      incident,
      userId,
      agencyId,
      true,
      null,
      false
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.PENDING_VALIDATION);
    verify(incidentRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName(
    "create - Target service manager receives a pending incident without automatic transfer"
  )
  void create_TargetServiceValidation_RoutesToManagerWithoutTransfer() {
    typeConfig.setDefaultTargetServiceId(serviceId);
    typeConfig.setValidatorScope(IncidentValidatorScope.TARGET_SERVICE_MANAGER);
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(incidentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.create(
      incident,
      userId,
      agencyId,
      true,
      null,
      false
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.PENDING_VALIDATION);
    assertThat(result.getTransferredToService()).isEqualTo(serviceId);
    verify(notificationClientService).notifyIncidentSubmitted(
      result,
      IncidentValidatorScope.TARGET_SERVICE_MANAGER,
      userId,
      null
    );
    verify(incidentRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("create - Throws when type is inactive")
  void create_InactiveType_ThrowsBusinessRuleViolation() {
    typeConfig.setActive(false);
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );

    assertThrows(BusinessRuleViolationException.class, () ->
      incidentService.create(incident, userId, agencyId, false, null, false)
    );
    verify(incidentRepository, never()).save(any());
  }

  @Test
  @DisplayName("create - Throws when typeId is null")
  void create_NullTypeId_ThrowsBusinessRuleViolation() {
    incident.setTypeId(null);

    assertThrows(BusinessRuleViolationException.class, () ->
      incidentService.create(incident, userId, agencyId, false, null, false)
    );
    verify(incidentRepository, never()).save(any());
  }

  @Test
  @DisplayName("create - Throws when creator has no agency")
  void create_NullAgencyId_ThrowsBusinessRuleViolation() {
    assertThrows(BusinessRuleViolationException.class, () ->
      incidentService.create(incident, userId, null, false, null, false)
    );
    verify(incidentRepository, never()).save(any());
  }

  @Test
  @DisplayName("create - Rejects a future incident date")
  void create_FutureIncidentDate_ThrowsBusinessRuleViolation() {
    incident.setIncidentDate(LocalDate.now().plusDays(1));
    incident.setObservationDate(LocalDate.now());
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );

    assertThrows(BusinessRuleViolationException.class, () ->
      incidentService.create(incident, userId, agencyId, false, null, false)
    );
    verify(incidentRepository, never()).save(any());
  }

  @Test
  @DisplayName("create - Ignores a client dueDate and computes it from the type SLA")
  void create_DueDateAlreadySet_ComputesFromSla() {
    LocalDateTime fixedDate = LocalDate.of(2025, 12, 31).atTime(LocalTime.MAX);
    incident.setDueDate(fixedDate);
    typeConfig.setSlaHours(24);
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(incidentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.create(
      incident,
      userId,
      agencyId,
      false,
      null,
      false
    );

    assertThat(result.getDueDate()).isNotEqualTo(fixedDate);
    assertThat(result.getDueDate()).isCloseTo(
      LocalDateTime.now().plusHours(24),
      within(1, ChronoUnit.MINUTES)
    );
  }

  // ── validate ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName(
    "validate - PENDING_VALIDATION to VALIDATED sets validatedBy and validatedAt"
  )
  void validate_PendingValidation_SetsValidated() {
    typeConfig.setDefaultTargetServiceId(serviceId);
    incident.setStatus(IncidentStatus.PENDING_VALIDATION);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.validate(
      incidentId,
      userId,
      "ok",
      null,
      null,
      Set.of()
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.VALIDATED);
    assertThat(result.getValidatedBy()).isEqualTo(userId);
    assertThat(result.getValidatedAt()).isNotNull();
  }

  @Test
  @DisplayName(
    "validate - The treatment deadline starts when the incident enters the circuit"
  )
  void validate_FromPendingValidation_StartsTheTreatmentDeadline() {
    typeConfig.setDefaultTargetServiceId(serviceId);
    incident.setStatus(IncidentStatus.PENDING_VALIDATION);
    // Declare il y a longtemps : sans remise a zero, l'incident serait deja en retard
    // au moment meme ou quelqu'un peut enfin le traiter.
    incident.setDueDate(LocalDateTime.now().minusDays(10));
    incident.setInitialDueDate(LocalDateTime.now().minusDays(10));
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );

    Incident result = incidentService.validate(
      incidentId,
      userId,
      "ok",
      null,
      null,
      Set.of()
    );

    assertThat(result.getDueDate()).isAfter(LocalDateTime.now());
    // L'engagement de traitement se prend ici aussi : avant, personne ne pouvait agir.
    assertThat(result.getInitialDueDate()).isAfter(LocalDateTime.now());
  }

  @Test
  @DisplayName(
    "validate - An incident that needed no validation keeps the deadline set at declaration"
  )
  void validate_FromOpen_KeepsTheDeclarationDeadline() {
    typeConfig.setDefaultTargetServiceId(serviceId);
    // OPEN signale une validation NON requise : l'incident etait traitable des le
    // depart, son echeance ne doit pas repartir.
    LocalDateTime declaredDueDate = LocalDateTime.now().plusHours(2);
    incident.setStatus(IncidentStatus.OPEN);
    incident.setDueDate(declaredDueDate);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );

    Incident result = incidentService.validate(
      incidentId,
      userId,
      "ok",
      null,
      null,
      Set.of()
    );

    assertThat(result.getDueDate()).isEqualTo(declaredDueDate);
  }

  @Test
  @DisplayName(
    "validate - Auto-transfer after validation when INCIDENT_AUTO_TRANSFER permission and defaultTargetServiceId set"
  )
  void validate_AutoTransfer_WithPermissionAndDefaultTarget_TransfersAfterValidation() {
    UUID responsibleId = UUID.randomUUID();
    typeConfig.setDefaultTargetServiceId(serviceId);
    incident.setStatus(IncidentStatus.PENDING_VALIDATION);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(userClientService.resolveServiceHead(serviceId)).thenReturn(
      responsibleId
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.validate(
      incidentId,
      userId,
      null,
      null,
      null,
      Set.of("INCIDENT_AUTO_TRANSFER")
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
    assertThat(result.getTransferredToService()).isEqualTo(serviceId);
    assertThat(result.getAssignedTo()).isEqualTo(responsibleId);
  }

  @Test
  @DisplayName(
    "validate - Attributes derived transitions to the system, not to the validator"
  )
  void validate_AutoTransfer_AttributesDerivedStepsToTheSystem() {
    UUID responsibleId = UUID.randomUUID();
    typeConfig.setDefaultTargetServiceId(serviceId);
    incident.setStatus(IncidentStatus.PENDING_VALIDATION);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(userClientService.resolveServiceHead(serviceId)).thenReturn(
      responsibleId
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    incidentService.validate(
      incidentId,
      userId,
      null,
      null,
      null,
      Set.of("INCIDENT_AUTO_TRANSFER")
    );

    // Le valideur signe ce qu'il a decide...
    verify(historyService).record(
      any(),
      eq(userId),
      eq(ActionType.VALIDATION),
      any(),
      any(),
      any()
    );
    // ...mais pas le transfert ni l'assignation que le workflow en a derives.
    verify(historyService).record(
      any(),
      isNull(),
      eq(ActionType.TRANSFER),
      any(),
      any(),
      any()
    );
    verify(historyService).record(
      any(),
      isNull(),
      eq(ActionType.ASSIGNMENT),
      any(),
      any(),
      any()
    );
    verify(historyService, never()).record(
      any(),
      eq(userId),
      eq(ActionType.TRANSFER),
      any(),
      any(),
      any()
    );
    verify(historyService, never()).record(
      any(),
      eq(userId),
      eq(ActionType.ASSIGNMENT),
      any(),
      any(),
      any()
    );
  }

  @Test
  @DisplayName(
    "validate - Preserves self-assignment instead of auto-transferring it away"
  )
  void validate_SelfAssigned_KeepsAssigneeOnValidation() {
    UUID selfAssigneeId = UUID.randomUUID();
    typeConfig.setDefaultTargetServiceId(serviceId);
    incident.setStatus(IncidentStatus.PENDING_VALIDATION);
    incident.setAssignedTo(selfAssigneeId);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.validate(
      incidentId,
      userId,
      null,
      null,
      null,
      Set.of("INCIDENT_AUTO_TRANSFER")
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
    assertThat(result.getAssignedTo()).isEqualTo(selfAssigneeId);
  }

  @Test
  @DisplayName("validate - No auto-transfer when INCIDENT_AUTO_TRANSFER absent")
  void validate_NoAutoTransfer_WithoutPermission_StaysValidated() {
    typeConfig.setDefaultTargetServiceId(serviceId);
    incident.setStatus(IncidentStatus.PENDING_VALIDATION);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.validate(
      incidentId,
      userId,
      null,
      null,
      null,
      Set.of("INCIDENT_VALIDATE")
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.VALIDATED);
  }

  @Test
  @DisplayName("validate - Throws when status is not OPEN/PENDING_VALIDATION")
  void validate_WrongStatus_ThrowsInvalidStatusTransition() {
    incident.setStatus(IncidentStatus.VALIDATED);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );

    assertThrows(InvalidStatusTransitionException.class, () ->
      incidentService.validate(incidentId, userId, null, null, null, Set.of())
    );
  }

  // ── reject ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("reject - PENDING_VALIDATION to REJECTED")
  void reject_PendingValidation_SetsRejected() {
    incident.setStatus(IncidentStatus.PENDING_VALIDATION);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.reject(
      incidentId,
      userId,
      "invalid",
      null
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.REJECTED);
    assertThat(result.getRejectReason()).isEqualTo("invalid");
  }

  @Test
  @DisplayName("reject - OPEN to REJECTED")
  void reject_Open_SetsRejected() {
    incident.setStatus(IncidentStatus.OPEN);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.reject(
      incidentId,
      userId,
      "not relevant",
      null
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.REJECTED);
  }

  @Test
  @DisplayName("reject - Throws when status is IN_PROGRESS")
  void reject_InProgress_ThrowsInvalidStatusTransition() {
    incident.setStatus(IncidentStatus.IN_PROGRESS);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );

    assertThrows(InvalidStatusTransitionException.class, () ->
      incidentService.reject(incidentId, userId, "reason", null)
    );
  }

  // ── transfer ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName("transfer - VALIDATED to TRANSFERRED")
  void transfer_Validated_SetsTransferred() {
    incident.setStatus(IncidentStatus.VALIDATED);
    // Une proposition + estimation portees par le traitant precedent ne doivent pas
    // survivre au transfert (elles deviennent caduques pour le service cible).
    incident.setProposedSolution("Proposition du service source");
    incident.setDirectionRejectionReason("Motif source");
    incident.setEstimatedResolutionHours(12);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.transfer(
      incidentId,
      userId,
      serviceId,
      null,
      null,
      "raison du transfert",
      "manual",
      Set.of("INCIDENT_TRANSFER")
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.TRANSFERRED);
    assertThat(result.getTransferredToService()).isEqualTo(serviceId);
    assertThat(result.getTransferredAt()).isNotNull();
    assertThat(result.getTransferReason()).isEqualTo("raison du transfert");
    assertThat(result.getEstimatedResolutionHours()).isNull();
    assertThat(result.getProposedSolution()).isNull();
    assertThat(result.getDirectionRejectionReason()).isNull();
  }

  @Test
  @DisplayName("transfer - Assigns the agency head when targeting the creator agency")
  void transfer_ToCreatorAgency_AssignsAgencyHead() {
    UUID agencyHeadId = UUID.randomUUID();
    incident.setStatus(IncidentStatus.VALIDATED);
    incident.setCreatorServiceId(null);
    incident.setTransferredToService(serviceId);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(userClientService.resolveAgencyHead(agencyId)).thenReturn(
      agencyHeadId
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.transfer(
      incidentId,
      userId,
      null,
      agencyId,
      null,
      "requires local handling",
      "manual",
      Set.of("INCIDENT_TRANSFER")
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
    assertThat(result.getTransferredToService()).isNull();
    assertThat(result.getAssignedTo()).isEqualTo(agencyHeadId);
    assertThat(result.getTransferredAt()).isNotNull();
    verify(notificationClientService).notifyIncidentTransferredToAgency(
      any(),
      eq(serviceId),
      eq(agencyId),
      eq(userId)
    );
    verify(notificationClientService).notifyIncidentAssigned(
      any(),
      eq(agencyHeadId)
    );
  }

  @Test
  @DisplayName(
    "transfer - Rejects an agency transfer when no agency head can be resolved"
  )
  void transfer_ToCreatorAgency_RejectsWhenNoAgencyHeadExists() {
    incident.setStatus(IncidentStatus.VALIDATED);
    incident.setCreatorServiceId(null);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(userClientService.resolveAgencyHead(agencyId)).thenReturn(null);

    assertThrows(BusinessRuleViolationException.class, () ->
      incidentService.transfer(
        incidentId,
        userId,
        null,
        agencyId,
        null,
        "requires local handling",
        "manual",
        Set.of("INCIDENT_TRANSFER")
      )
    );

    assertThat(incident.getStatus()).isEqualTo(IncidentStatus.VALIDATED);
    assertThat(incident.getAssignedTo()).isNull();
    verify(incidentRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("transfer - Throws when status is OPEN")
  void transfer_Open_ThrowsInvalidStatusTransition() {
    incident.setStatus(IncidentStatus.OPEN);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );

    assertThrows(InvalidStatusTransitionException.class, () ->
      incidentService.transfer(
        incidentId,
        userId,
        serviceId,
        null,
        null,
        null,
        null,
        Set.of()
      )
    );
  }

  @Test
  @DisplayName(
    "transfer - Reclassifies to an active type of the target service and recomputes dueDate"
  )
  void transfer_WithValidNewType_ReclassifiesAndRecomputesSla() {
    incident.setStatus(IncidentStatus.VALIDATED);
    UUID newTypeId = UUID.randomUUID();
    IncidentTypeConfig newType = new IncidentTypeConfig();
    newType.setId(newTypeId);
    newType.setActive(true);
    newType.setValidatorScope(
      IncidentValidatorScope.TARGET_SERVICE_MANAGER
    );
    newType.setDefaultTargetServiceId(serviceId);
    newType.setDefaultTargetServiceId(serviceId);
    newType.setSlaHours(24);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentTypeConfigRepository.findById(newTypeId)).thenReturn(
      Optional.of(newType)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.transfer(
      incidentId,
      userId,
      serviceId,
      null,
      newTypeId,
      "raison",
      "manual",
      Set.of("INCIDENT_TRANSFER")
    );

    assertThat(result.getTypeId()).isEqualTo(newTypeId);
    assertThat(result.getDueDate()).isAfter(LocalDateTime.now().plusHours(23));
  }

  @Test
  @DisplayName("transfer - Rejects reclassification to an inactive type")
  void transfer_WithInactiveNewType_ThrowsBusinessRuleViolation() {
    incident.setStatus(IncidentStatus.VALIDATED);
    UUID newTypeId = UUID.randomUUID();
    IncidentTypeConfig newType = new IncidentTypeConfig();
    newType.setId(newTypeId);
    newType.setActive(false);
    newType.setDefaultTargetServiceId(serviceId);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentTypeConfigRepository.findById(newTypeId)).thenReturn(
      Optional.of(newType)
    );

    assertThrows(BusinessRuleViolationException.class, () ->
      incidentService.transfer(
        incidentId,
        userId,
        serviceId,
        null,
        newTypeId,
        "raison",
        "manual",
        Set.of("INCIDENT_TRANSFER")
      )
    );
  }

  @Test
  @DisplayName("transfer - Rejects reclassification to a type of another service")
  void transfer_WithNewTypeFromOtherService_ThrowsBusinessRuleViolation() {
    incident.setStatus(IncidentStatus.VALIDATED);
    UUID newTypeId = UUID.randomUUID();
    IncidentTypeConfig newType = new IncidentTypeConfig();
    newType.setId(newTypeId);
    newType.setActive(true);
    newType.setDefaultTargetServiceId(UUID.randomUUID());
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentTypeConfigRepository.findById(newTypeId)).thenReturn(
      Optional.of(newType)
    );

    assertThrows(BusinessRuleViolationException.class, () ->
      incidentService.transfer(
        incidentId,
        userId,
        serviceId,
        null,
        newTypeId,
        "raison",
        "manual",
        Set.of("INCIDENT_TRANSFER")
      )
    );
  }

  // ── assign ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("assign - Moves to ASSIGNED and updates assignedTo")
  void assign_Transferred_KeepsStatus() {
    UUID assignee = UUID.randomUUID();
    incident.setStatus(IncidentStatus.TRANSFERRED);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    ExternalUser assigneeUser = new ExternalUser();
    assigneeUser.setId(assignee);
    assigneeUser.setActive(true);
    assigneeUser.setPermissions(Set.of("INCIDENT_TREAT"));
    assigneeUser.setAgencyId(incident.getAgencyId());
    when(
      incidentWorkflowGuard.resolveValidAssigneeForIncident(
        eq(incident),
        eq(assignee)
      )
    ).thenReturn(assigneeUser);

    Incident result = incidentService.assign(
      incidentId,
      userId,
      assignee,
      null
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
    assertThat(result.getAssignedTo()).isEqualTo(assignee);
  }

  @Test
  @DisplayName(
    "assign - Reassignment voids the previous solution proposal (back to ASSIGNED)"
  )
  void assign_Reassignment_VoidsPendingSolution() {
    UUID newAssignee = UUID.randomUUID();
    // Un incident en cours avec une proposition deja soumise + estimation.
    incident.setStatus(IncidentStatus.IN_PROGRESS);
    incident.setAssignedTo(UUID.randomUUID());
    incident.setProposedSolution("Ancienne proposition du traitant precedent");
    incident.setDirectionRejectionReason("Ancien motif");
    incident.setEstimatedResolutionHours(10);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    ExternalUser assigneeUser = new ExternalUser();
    assigneeUser.setId(newAssignee);
    assigneeUser.setActive(true);
    assigneeUser.setPermissions(Set.of("INCIDENT_TREAT"));
    assigneeUser.setAgencyId(incident.getAgencyId());
    when(
      incidentWorkflowGuard.resolveValidAssigneeForIncident(
        eq(incident),
        eq(newAssignee)
      )
    ).thenReturn(assigneeUser);

    Incident result = incidentService.assign(
      incidentId,
      userId,
      newAssignee,
      null
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
    assertThat(result.getAssignedTo()).isEqualTo(newAssignee);
    // La proposition devient caduque : purgee pour le nouvel assigne.
    assertThat(result.getProposedSolution()).isNull();
    assertThat(result.getDirectionRejectionReason()).isNull();
    assertThat(result.getEstimatedResolutionHours()).isNull();
  }

  @Test
  @DisplayName("assign - Throws when status does not allow assignment")
  void assign_WrongStatus_ThrowsInvalidStatusTransition() {
    incident.setStatus(IncidentStatus.CLOSED);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );

    assertThrows(InvalidStatusTransitionException.class, () ->
      incidentService.assign(incidentId, userId, UUID.randomUUID(), null)
    );
  }

  // ── resolve ───────────────────────────────────────────────────────────────

  @Test
  @DisplayName("startProgress - ASSIGNED to IN_PROGRESS")
  void startProgress_Transferred_SetsInProgress() {
    incident.setStatus(IncidentStatus.ASSIGNED);
    incident.setAssignedTo(userId);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.startProgress(
      incidentId,
      userId,
      "starting",
      null
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
  }

  @Test
  @DisplayName(
    "startProgress - Estimated hours entered at handover drive the SLA due date"
  )
  void startProgress_WithEstimate_RecomputesDueDate() {
    incident.setStatus(IncidentStatus.ASSIGNED);
    incident.setAssignedTo(userId);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.startProgress(
      incidentId,
      userId,
      "starting",
      8
    );

    assertThat(result.getEstimatedResolutionHours()).isEqualTo(8);
    assertThat(result.getDueDate()).isAfter(LocalDateTime.now().plusHours(7));
    assertThat(result.getDueDate()).isBefore(LocalDateTime.now().plusHours(9));
  }

  @Test
  @DisplayName(
    "startProgress - Throws business rule when incident is unassigned"
  )
  void startProgress_Unassigned_ThrowsBusinessRule() {
    incident.setStatus(IncidentStatus.ASSIGNED);
    incident.setAssignedTo(null);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );

    // Un incident non assigné ne peut pas être pris en charge : erreur métier
    // explicite (422), jamais un 500. Aucune transition de statut ne survient.
    assertThrows(BusinessRuleViolationException.class, () ->
      incidentService.startProgress(incidentId, userId, null, null)
    );
  }

  @Test
  @DisplayName("startProgress - Throws when status does not allow start")
  void startProgress_WrongStatus_ThrowsInvalidStatusTransition() {
    incident.setStatus(IncidentStatus.RESOLVED);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );

    assertThrows(InvalidStatusTransitionException.class, () ->
      incidentService.startProgress(incidentId, userId, null, null)
    );
  }

  @Test
  @DisplayName("block - IN_PROGRESS to BLOCKED keeps SLA data untouched")
  void block_InProgress_SetsBlockedWithoutChangingDueDate() {
    LocalDateTime dueDate = LocalDateTime.now().plusDays(1);
    incident.setStatus(IncidentStatus.IN_PROGRESS);
    incident.setAssignedTo(userId);
    incident.setDueDate(dueDate);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.block(
      incidentId,
      userId,
      "Waiting for supplier",
      "blocked"
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.BLOCKED);
    assertThat(result.getBlockedReason()).isEqualTo("Waiting for supplier");
    assertThat(result.getBlockedBy()).isEqualTo(userId);
    assertThat(result.getBlockedAt()).isNotNull();
    assertThat(result.getDueDate()).isEqualTo(dueDate);
  }

  @Test
  @DisplayName("resume - BLOCKED to IN_PROGRESS")
  void resume_Blocked_SetsInProgress() {
    LocalDateTime expiredDueDate = LocalDateTime.now().minusDays(8);
    incident.setStatus(IncidentStatus.BLOCKED);
    incident.setAssignedTo(userId);
    incident.setDueDate(expiredDueDate);
    incident.setLastSlaReminderSentAt(LocalDateTime.now().minusHours(1));
    incident.setBlockedAt(LocalDateTime.now().minusDays(2));
    incident.setBlockedBy(userId);
    incident.setBlockedReason("Attente d'un tiers");
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.resume(incidentId, userId, "back");

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
    assertThat(result.getUnblockedAt()).isNotNull();
    assertThat(result.getDueDate())
      .isEqualTo(result.getUnblockedAt().plusHours(typeConfig.getSlaHours()));
    assertThat(result.getDueDate()).isAfter(expiredDueDate);
    assertThat(result.getLastSlaReminderSentAt()).isNull();
    // Le contexte de blocage est soldé à la reprise (reste consultable en historique).
    assertThat(result.getBlockedAt()).isNull();
    assertThat(result.getBlockedBy()).isNull();
    assertThat(result.getBlockedReason()).isNull();
  }

  @Test
  @DisplayName("treat - IN_PROGRESS to TREATED")
  void treat_InProgress_SetsTreated() {
    incident.setStatus(IncidentStatus.IN_PROGRESS);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(
      incidentTypeConfigRepository.findById(incident.getTypeId())
    ).thenReturn(Optional.of(typeConfig));
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.treat(
      incidentId,
      userId,
      "traitement fait",
      null,
      null
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.TREATED);
    // Le compte rendu du traitement a son propre champ : la resolution ne l'ecrase plus.
    assertThat(result.getTreatmentDescription()).isEqualTo("traitement fait");
    assertThat(result.getResolutionDescription()).isNull();
  }

  @Test
  @DisplayName("treat - requiresCauseAnalysis with cause supplied succeeds")
  void treat_RequiresCauseAnalysis_WithCause_Treats() {
    incident.setStatus(IncidentStatus.IN_PROGRESS);
    typeConfig.setRequiresCauseAnalysis(true);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(
      incidentTypeConfigRepository.findById(incident.getTypeId())
    ).thenReturn(Optional.of(typeConfig));
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.treat(
      incidentId,
      userId,
      "traitement fait",
      IncidentCause.TECHNICAL,
      "disque plein"
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.TREATED);
    assertThat(result.getCause()).isEqualTo(IncidentCause.TECHNICAL);
    assertThat(result.getCauseDetail()).isEqualTo("disque plein");
  }

  @Test
  @DisplayName("resolve - TREATED to RESOLVED")
  void resolve_Treated_SetsResolved() {
    incident.setStatus(IncidentStatus.TREATED);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.resolve(incidentId, userId, "fixed");

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
    assertThat(result.getResolvedAt()).isNotNull();
    assertThat(result.getResolutionDescription()).isEqualTo("fixed");
  }

  @Test
  @DisplayName("resolve - Throws when status is not TREATED")
  void resolve_WrongStatus_ThrowsInvalidStatusTransition() {
    incident.setStatus(IncidentStatus.TRANSFERRED);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );

    assertThrows(InvalidStatusTransitionException.class, () ->
      incidentService.resolve(incidentId, userId, "note")
    );
  }

  @Test
  @DisplayName("markUnresolved - TREATED back to IN_PROGRESS")
  void markUnresolved_Treated_SetsInProgress() {
    incident.setStatus(IncidentStatus.TREATED);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.markUnresolved(
      incidentId,
      userId,
      "traitement insuffisant"
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
  }

  // ── close ─────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("close - RESOLVED to CLOSED with closedAt")
  void close_Resolved_SetsClosed() {
    incident.setStatus(IncidentStatus.RESOLVED);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.close(incidentId, userId, null, null);

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.CLOSED);
    assertThat(result.getClosedAt()).isNotNull();
  }

  @Test
  @DisplayName("close - Throws when status is not RESOLVED")
  void close_WrongStatus_ThrowsInvalidStatusTransition() {
    incident.setStatus(IncidentStatus.IN_PROGRESS);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );

    assertThrows(InvalidStatusTransitionException.class, () ->
      incidentService.close(incidentId, userId, null, null)
    );
  }

  // ── cancel ─────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("cancel - ASSIGNED to CANCELLED")
  void cancel_Assigned_SetsCancelled() {
    incident.setStatus(IncidentStatus.ASSIGNED);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.cancel(incidentId, userId, "reason");

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.CANCELLED);
  }

  @Test
  @DisplayName("cancel - Cancels a blocked incident with a preserved solution")
  void cancel_BlockedWithProposedSolution_Succeeds() {
    incident.setStatus(IncidentStatus.BLOCKED);
    incident.setProposedSolution("Proposition deja soumise");
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    Incident result = incidentService.cancel(incidentId, userId, "reason");

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.CANCELLED);
  }

  @Test
  @DisplayName("cancel - Throws when status is CLOSED")
  void cancel_WrongStatus_ThrowsInvalidStatusTransition() {
    incident.setStatus(IncidentStatus.CLOSED);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );

    assertThrows(InvalidStatusTransitionException.class, () ->
      incidentService.cancel(incidentId, userId, "reason")
    );
  }

  @Test
  @DisplayName("cancel - Throws when treatment is IN_PROGRESS")
  void cancel_InProgress_ThrowsInvalidStatusTransition() {
    incident.setStatus(IncidentStatus.IN_PROGRESS);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );

    assertThrows(InvalidStatusTransitionException.class, () ->
      incidentService.cancel(incidentId, userId, "reason")
    );
    verify(incidentRepository, never()).saveAndFlush(any());
  }

  // ── update ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("update - Throws when incident is CLOSED")
  void update_ClosedIncident_ThrowsInvalidStatusTransition() {
    incident.setStatus(IncidentStatus.CLOSED);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );

    assertThrows(InvalidStatusTransitionException.class, () ->
      incidentService.update(incidentId, incident, userId)
    );
  }

  @Test
  @DisplayName("update - Throws when incident is TRANSFERRED")
  void update_TransferredIncident_ThrowsInvalidStatusTransitionException() {
    incident.setStatus(IncidentStatus.TRANSFERRED);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );

    assertThrows(InvalidStatusTransitionException.class, () ->
      incidentService.update(incidentId, incident, userId)
    );
  }

  @Test
  @DisplayName("update - Succeeds when incident is PENDING_VALIDATION")
  void update_OpenIncident_UpdatesFields() {
    incident.setStatus(IncidentStatus.PENDING_VALIDATION);
    incident.setCreatedBy(userId);
    Incident details = new Incident();
    details.setCriticality(Criticality.HIGH);

    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );

    Incident result = incidentService.update(incidentId, details, userId);

    assertThat(result.getCriticality()).isEqualTo(Criticality.HIGH);
  }

  @Test
  @DisplayName("update - Changes typeId when new type is active")
  void update_WithNewActiveType_ChangesTypeId() {
    incident.setStatus(IncidentStatus.PENDING_VALIDATION);
    incident.setCreatedBy(userId);
    UUID newTypeId = UUID.randomUUID();
    IncidentTypeConfig newType = new IncidentTypeConfig();
    newType.setId(newTypeId);
    newType.setActive(true);
    newType.setValidatorScope(IncidentValidatorScope.TARGET_SERVICE_MANAGER);
    newType.setDefaultTargetServiceId(serviceId);

    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentTypeConfigRepository.findById(newTypeId)).thenReturn(
      Optional.of(newType)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );

    Incident details = new Incident();
    details.setTypeId(newTypeId);

    Incident result = incidentService.update(incidentId, details, userId);

    assertThat(result.getTypeId()).isEqualTo(newTypeId);
    assertThat(result.getTransferredToService()).isEqualTo(serviceId);
  }

  @Test
  @DisplayName(
    "update - Records one readable history entry per requalified axis"
  )
  void update_Requalification_RecordsPerAxisHistory() {
    incident.setStatus(IncidentStatus.PENDING_VALIDATION);
    incident.setCreatedBy(userId);
    incident.setCriticality(Criticality.LOW);
    UUID oldTypeId = incident.getTypeId();
    UUID newTypeId = UUID.randomUUID();
    IncidentTypeConfig newType = new IncidentTypeConfig();
    newType.setId(newTypeId);
    newType.setActive(true);
    newType.setSlaHours(24);
    newType.setValidatorScope(IncidentValidatorScope.AGENCY_MANAGER);

    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentTypeConfigRepository.findById(newTypeId)).thenReturn(
      Optional.of(newType)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );

    Incident details = new Incident();
    details.setTypeId(newTypeId);
    details.setCriticality(Criticality.HIGH);

    Incident result = incidentService.update(incidentId, details, userId);

    verify(historyService).record(
      eq(result),
      eq(userId),
      eq(ActionType.TYPE_CHANGE),
      eq(oldTypeId.toString()),
      eq(newTypeId.toString()),
      isNull()
    );
    verify(historyService).record(
      eq(result),
      eq(userId),
      eq(ActionType.CRITICALITY_CHANGE),
      eq(Criticality.LOW.getName()),
      eq(Criticality.HIGH.getName()),
      isNull()
    );
    // L'entree UPDATE opaque ("Qualification modifiee" + diff brut) a disparu.
    verify(historyService, never()).record(
      any(),
      any(),
      eq(ActionType.UPDATE),
      any(),
      any(),
      any()
    );
  }

  @Test
  @DisplayName(
    "update - Requalifying the type restarts the SLA from the new type"
  )
  void update_TypeChange_RestartsSlaDueDate() {
    incident.setStatus(IncidentStatus.PENDING_VALIDATION);
    incident.setCreatedBy(userId);
    incident.setDueDate(LocalDateTime.now().minusDays(10));
    UUID newTypeId = UUID.randomUUID();
    IncidentTypeConfig newType = new IncidentTypeConfig();
    newType.setId(newTypeId);
    newType.setActive(true);
    newType.setSlaHours(24);
    newType.setValidatorScope(IncidentValidatorScope.AGENCY_MANAGER);

    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentTypeConfigRepository.findById(newTypeId)).thenReturn(
      Optional.of(newType)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );

    Incident details = new Incident();
    details.setTypeId(newTypeId);

    Incident result = incidentService.update(incidentId, details, userId);

    assertThat(result.getDueDate()).isAfter(LocalDateTime.now());
    assertThat(result.getDueDate()).isBefore(
      LocalDateTime.now().plusHours(25)
    );
  }

  @Test
  @DisplayName("update - Keeps business dates chronologically consistent")
  void update_InconsistentDates_ThrowsBusinessRuleViolation() {
    incident.setStatus(IncidentStatus.OPEN);
    incident.setCreatedBy(userId);
    Incident details = new Incident();
    details.setIncidentDate(LocalDate.of(2026, 1, 15));
    details.setObservationDate(LocalDate.of(2026, 1, 14));
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );

    assertThrows(BusinessRuleViolationException.class, () ->
      incidentService.update(incidentId, details, userId)
    );
    verify(incidentRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("update - Rejects a user other than the creator")
  void update_NonCreator_ThrowsAccessDenied() {
    UUID otherUserId = UUID.randomUUID();
    incident.setStatus(IncidentStatus.OPEN);
    incident.setCreatedBy(userId);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );

    assertThrows(AccessDeniedException.class, () ->
      incidentService.update(incidentId, new Incident(), otherUserId)
    );
    verify(incidentRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName(
    "update - Throws BusinessRuleViolation when new type is inactive"
  )
  void update_WithInactiveNewType_ThrowsBusinessRuleViolation() {
    incident.setStatus(IncidentStatus.PENDING_VALIDATION);
    incident.setCreatedBy(userId);
    UUID newTypeId = UUID.randomUUID();
    IncidentTypeConfig inactiveType = new IncidentTypeConfig();
    inactiveType.setId(newTypeId);
    inactiveType.setActive(false);
    inactiveType.setName("inactive");
    inactiveType.setDisplayName("Inactive");

    Incident details = new Incident();
    details.setTitle("T");
    details.setDescription("D");
    details.setTypeId(newTypeId);

    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentTypeConfigRepository.findById(newTypeId)).thenReturn(
      Optional.of(inactiveType)
    );

    assertThrows(BusinessRuleViolationException.class, () ->
      incidentService.update(incidentId, details, userId)
    );
  }

  // ── delete ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("delete - Throws when incident is IN_PROGRESS")
  void delete_InProgress_ThrowsInvalidStatusTransition() {
    incident.setStatus(IncidentStatus.IN_PROGRESS);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );

    assertThrows(InvalidStatusTransitionException.class, () ->
      incidentService.delete(incidentId, userId)
    );
    verify(incidentRepository, never()).delete(any(Incident.class));
  }

  @Test
  @DisplayName("delete - Succeeds when incident is OPEN")
  void delete_Open_DeletesIncident() {
    incident.setStatus(IncidentStatus.OPEN);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );

    incidentService.delete(incidentId, userId);

    verify(incidentRepository).delete(incident);
  }

  @Test
  @DisplayName("delete - Succeeds when incident is PENDING_VALIDATION")
  void delete_PendingValidation_DeletesIncident() {
    incident.setStatus(IncidentStatus.PENDING_VALIDATION);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );

    incidentService.delete(incidentId, userId);

    verify(incidentRepository).delete(incident);
  }

  @Test
  @DisplayName(
    "delete - Keeps a snapshot of the deleted incident in the audit log"
  )
  void delete_Open_AuditKeepsSnapshot() {
    incident.setStatus(IncidentStatus.OPEN);
    incident.setTitle("Panne réseau");
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );

    incidentService.delete(incidentId, userId);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(
      Map.class
    );
    verify(auditServiceClientService).audit(
      eq(userId),
      isNull(),
      isNull(),
      eq("INCIDENT_DELETE"),
      eq("INCIDENT"),
      eq(incidentId.toString()),
      eq("SUCCESS"),
      detailsCaptor.capture()
    );
    assertThat(detailsCaptor.getValue()).containsEntry("title", "Panne réseau");
  }

  // ── findAll / findBy* ─────────────────────────────────────────────────────

  @Test
  @DisplayName("findAll - Delegates to repository")
  void findAll_DelegatesToRepository() {
    Pageable pageable = PageRequest.of(0, 10);
    when(incidentRepository.findAll(pageable)).thenReturn(
      new PageImpl<>(List.of(incident))
    );

    var result = incidentService.findAll(pageable);

    assertThat(result.getContent()).hasSize(1);
    verify(incidentRepository).findAll(pageable);
  }

  @Test
  @DisplayName("findByAgencyId - Returns incidents for agency")
  void findByAgencyId_ReturnsIncidents() {
    when(incidentRepository.findByAgencyId(agencyId)).thenReturn(
      List.of(incident)
    );
    assertThat(incidentService.findByAgencyId(agencyId)).hasSize(1);
  }

  @Test
  @DisplayName("findByCreatedBy - Returns incidents created by user")
  void findByCreatedBy_ReturnsIncidents() {
    when(incidentRepository.findByCreatedBy(userId)).thenReturn(
      List.of(incident)
    );
    assertThat(incidentService.findByCreatedBy(userId)).hasSize(1);
  }

  @Test
  @DisplayName("findByAssignedTo - Returns incidents assigned to user")
  void findByAssignedTo_ReturnsIncidents() {
    when(incidentRepository.findByAssignedTo(userId)).thenReturn(
      List.of(incident)
    );
    assertThat(incidentService.findByAssignedTo(userId)).hasSize(1);
  }

  // ── Workflow Actions ──────────────────────────────────────────────────────

  @Test
  @DisplayName("reopen - Successfully reopens an incident")
  void reopen_Succeeds() {
    incident.setStatus(IncidentStatus.REJECTED);
    incident.setCreatedBy(userId);
    incident.setAssignedTo(userId);
    incident.setUpdatedAt(java.time.LocalDateTime.now().minusHours(1));
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(
      reportingSystemConfigClientService.getThresholdLong("maxReopenCount", 2L)
    ).thenReturn(2L);
    when(
      reportingSystemConfigClientService.getThresholdLong(
        "reopenTimeLimitHours",
        48L
      )
    ).thenReturn(48L);
    when(incidentRepository.saveAndFlush(any(Incident.class))).thenAnswer(
      i -> i.getArguments()[0]
    );

    Incident result = incidentService.reopen(
      incidentId,
      userId,
      "reason",
      "comment"
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.REOPENED);
    verify(historyService).record(
      eq(incident),
      eq(userId),
      eq(ActionType.REOPENING),
      eq(IncidentStatus.REJECTED.getName()),
      eq(IncidentStatus.REOPENED.getName()),
      eq("reason — comment")
    );
    // Le cycle precedent est solde et un nouveau s'ouvre : les jalons du cycle
    // rouvert restent mesurables, un rapport passe ne change plus.
    verify(resolutionCycleRecorder).reopenCycle(
      eq(result),
      eq(result.getReopenedAt())
    );
  }

  @Test
  @DisplayName(
    "reopen - From prolonged wait clears the previous cycle block context"
  )
  void reopen_FromProlongedWait_ClearsBlockContext() {
    incident.setStatus(IncidentStatus.UNRESOLVED_PROLONGED_WAIT);
    incident.setCreatedBy(userId);
    incident.setBlockedAt(java.time.LocalDateTime.now().minusDays(31));
    incident.setBlockedBy(userId);
    incident.setBlockedReason("Ancien motif de blocage");
    incident.setUnblockedAt(java.time.LocalDateTime.now().minusDays(30));
    incident.setPreBlockStatus(IncidentStatus.IN_PROGRESS.name());
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(
      reportingSystemConfigClientService.getThresholdLong("maxReopenCount", 2L)
    ).thenReturn(2L);
    when(incidentRepository.saveAndFlush(any(Incident.class))).thenAnswer(
      i -> i.getArguments()[0]
    );

    Incident result = incidentService.reopen(
      incidentId,
      userId,
      "reason",
      "comment"
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.REOPENED);
    assertThat(result.getBlockedAt()).isNull();
    assertThat(result.getBlockedBy()).isNull();
    assertThat(result.getBlockedReason()).isNull();
    assertThat(result.getUnblockedAt()).isNull();
    assertThat(result.getPreBlockStatus()).isNull();
  }

  @Test
  @DisplayName("reopen - Throws when max reopen count reached")
  void reopen_MaxReopenCountReached_Throws() {
    incident.setStatus(IncidentStatus.RESOLVED);
    incident.setResolvedAt(java.time.LocalDateTime.now().minusHours(1));
    incident.setReopenCount(2);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(
      reportingSystemConfigClientService.getThresholdLong("maxReopenCount", 2L)
    ).thenReturn(2L);

    assertThrows(BusinessRuleViolationException.class, () ->
      incidentService.reopen(incidentId, userId, "reason", "comment")
    );
    verify(incidentRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("reopen - Throws when reopen time limit exceeded")
  void reopen_TimeLimitExceeded_Throws() {
    incident.setStatus(IncidentStatus.REJECTED);
    incident.setReopenCount(0);
    incident.setUpdatedAt(java.time.LocalDateTime.now().minusHours(72));
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(
      reportingSystemConfigClientService.getThresholdLong("maxReopenCount", 2L)
    ).thenReturn(2L);
    when(
      reportingSystemConfigClientService.getThresholdLong(
        "reopenTimeLimitHours",
        48L
      )
    ).thenReturn(48L);

    assertThrows(BusinessRuleViolationException.class, () ->
      incidentService.reopen(incidentId, userId, "reason", "comment")
    );
    verify(incidentRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("cloneIncident - Successfully clones an incident")
  void cloneIncident_Succeeds() {
    incident.setStatus(IncidentStatus.CLOSED);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    // cloneIncident resout le type pour router le clone selon validatorScope.
    when(
      incidentTypeConfigRepository.findById(incident.getTypeId())
    ).thenReturn(Optional.of(typeConfig));
    when(incidentRepository.saveAndFlush(any(Incident.class))).thenAnswer(i -> {
      Incident inc = i.getArgument(0);
      inc.setId(UUID.randomUUID());
      return inc;
    });
    ExternalUser mockUser = new ExternalUser();
    mockUser.setPermissions(Set.of("INCIDENT_CREATE"));
    when(userClientService.getUser(userId)).thenReturn(mockUser);
    when(referenceGenerator.next()).thenReturn("FT-I-2026-0042");

    Incident result = incidentService.cloneIncident(incidentId, userId);

    assertThat(result.getId()).isNotEqualTo(incidentId);
    assertThat(result.getTitle()).isEqualTo(incident.getTitle());
    assertThat(result.getReference()).isEqualTo("FT-I-2026-0042");
    assertThat(result.getStatus()).isIn(
      IncidentStatus.OPEN,
      IncidentStatus.PENDING_VALIDATION
    );
    verify(notificationClientService).notifyIncidentSubmitted(
      eq(result),
      eq(IncidentValidatorScope.AGENCY_MANAGER),
      eq(userId),
      any()
    );
  }

  @Test
  @DisplayName(
    "cloneIncident - Opens the validation wait when the clone needs validating"
  )
  void cloneIncident_StartsDecisionWait_WhenPendingValidation() {
    incident.setStatus(IncidentStatus.CLOSED);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(
      incidentTypeConfigRepository.findById(incident.getTypeId())
    ).thenReturn(Optional.of(typeConfig));
    when(incidentRepository.saveAndFlush(any(Incident.class))).thenAnswer(i -> {
      Incident inc = i.getArgument(0);
      inc.setId(UUID.randomUUID());
      return inc;
    });
    // Sans INCIDENT_CREATE_OPEN le clone part en attente de validation.
    ExternalUser mockUser = new ExternalUser();
    mockUser.setPermissions(Set.of("INCIDENT_CREATE"));
    when(userClientService.getUser(userId)).thenReturn(mockUser);
    when(referenceGenerator.next()).thenReturn("FT-I-2026-0043");

    Incident result = incidentService.cloneIncident(incidentId, userId);

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.PENDING_VALIDATION);
    // Une attente de validation sans date de depart echappe a son circuit de relance.
    assertThat(result.getDecisionAwaitedSince()).isNotNull();
  }

  @Test
  @DisplayName(
    "cloneIncident - Restarts the SLA instead of inheriting the original due date"
  )
  void cloneIncident_RestartsSlaDueDate() {
    incident.setStatus(IncidentStatus.CLOSED);
    // Echeance de l'original largement depassee : le clone ne doit pas en heriter.
    incident.setDueDate(LocalDateTime.now().minusDays(30));
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(
      incidentTypeConfigRepository.findById(incident.getTypeId())
    ).thenReturn(Optional.of(typeConfig));
    when(incidentRepository.saveAndFlush(any(Incident.class))).thenAnswer(i -> {
      Incident inc = i.getArgument(0);
      inc.setId(UUID.randomUUID());
      return inc;
    });
    ExternalUser mockUser = new ExternalUser();
    mockUser.setPermissions(Set.of("INCIDENT_CREATE"));
    when(userClientService.getUser(userId)).thenReturn(mockUser);
    when(referenceGenerator.next()).thenReturn("FT-I-2026-0043");

    Incident result = incidentService.cloneIncident(incidentId, userId);

    assertThat(result.getDueDate()).isAfter(LocalDateTime.now());
    assertThat(result.getDueDate()).isBefore(
      LocalDateTime.now().plusHours(typeConfig.getSlaHours() + 1)
    );
  }

  @Test
  @DisplayName("resubmit - Successfully resubmits an incident requiring validation")
  void resubmit_Succeeds_WithValidation() {
    incident.setStatus(IncidentStatus.REOPENED);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(incidentRepository.saveAndFlush(any(Incident.class))).thenAnswer(
      i -> i.getArguments()[0]
    );

    Incident result = incidentService.resubmit(incidentId, userId, "fixed", true);

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.PENDING_VALIDATION);
    verify(notificationClientService).notifyIncidentSubmitted(
      result,
      IncidentValidatorScope.AGENCY_MANAGER,
      userId,
      "fixed"
    );
  }

  @Test
  @DisplayName(
    "resubmit - Restarts the decision wait and opens no second cycle"
  )
  void resubmit_StartsDecisionWait_AndReusesReopenedCycle() {
    incident.setStatus(IncidentStatus.REOPENED);
    incident.setDecisionAwaitedSince(LocalDateTime.now().minusDays(10));
    incident.setLastDecisionReminderSentAt(LocalDateTime.now().minusDays(1));
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(incidentRepository.saveAndFlush(any(Incident.class))).thenAnswer(
      i -> i.getArguments()[0]
    );

    Incident result = incidentService.resubmit(incidentId, userId, "fixed", true);

    // L'attente de validation repart de la resoumission : sans cela la relance serait
    // calculee sur l'attente du cycle precedent, voire ne partirait jamais.
    assertThat(result.getDecisionAwaitedSince()).isAfter(
      LocalDateTime.now().minusMinutes(1)
    );
    assertThat(result.getLastDecisionReminderSentAt()).isNull();
    // La reouverture a deja ouvert le cycle : en ouvrir un second laisserait un cycle
    // fantome, sans aucun jalon.
    verify(resolutionCycleRecorder, never()).reopenCycle(any(), any());
  }

  @Test
  @DisplayName("resubmit - Successfully resubmits an incident bypassing validation")
  void resubmit_Succeeds_BypassValidation() {
    incident.setStatus(IncidentStatus.REOPENED);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(incidentRepository.saveAndFlush(any(Incident.class))).thenAnswer(
      i -> i.getArguments()[0]
    );

    Incident result = incidentService.resubmit(incidentId, userId, "fixed", false);

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.OPEN);
  }

  @Test
  @DisplayName("cancel - Successfully cancels an incident")
  void cancel_Succeeds() {
    incident.setStatus(IncidentStatus.VALIDATED);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentRepository.saveAndFlush(any(Incident.class))).thenAnswer(
      i -> i.getArguments()[0]
    );

    Incident result = incidentService.cancel(incidentId, userId, "reason");

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.CANCELLED);
  }

  @Test
  @DisplayName(
    "cancel - Stores the reason as a cancellation, never as a rejection"
  )
  void cancel_StoresReasonInItsOwnField() {
    incident.setStatus(IncidentStatus.VALIDATED);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentRepository.saveAndFlush(any(Incident.class))).thenAnswer(
      i -> i.getArguments()[0]
    );

    Incident result = incidentService.cancel(
      incidentId,
      userId,
      "doublon avec FT-I-2026-0007"
    );

    assertThat(result.getCancelReason()).isEqualTo(
      "doublon avec FT-I-2026-0007"
    );
    assertThat(result.getRejectReason()).isNull();
  }

  @Test
  @DisplayName(
    "requestConfirmation - Records the pending request without moving the status"
  )
  void requestConfirmation_LeavesTheStatusUntouched() {
    incident.setStatus(IncidentStatus.UNRESOLVED_PROLONGED_WAIT);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentRepository.saveAndFlush(any(Incident.class))).thenAnswer(
      i -> i.getArguments()[0]
    );

    Incident result = incidentService.requestConfirmation(
      incidentId,
      userId,
      "toujours d'actualite ?"
    );

    // L'incident attend une reponse : il ne redemarre pas de lui-meme.
    assertThat(result.getStatus()).isEqualTo(
      IncidentStatus.UNRESOLVED_PROLONGED_WAIT
    );
    assertThat(result.getConfirmationRequestedAt()).isNotNull();
    assertThat(result.getConfirmationRequestedBy()).isEqualTo(userId);
  }

  @Test
  @DisplayName(
    "confirmRelevance - Resumes handling, keeps every field, resets only the deadline"
  )
  void confirmRelevance_ResetsOnlyTheDeadline() {
    UUID assigneeId = UUID.randomUUID();
    LocalDateTime referenceDueDate = LocalDateTime.now().minusDays(40);
    incident.setStatus(IncidentStatus.UNRESOLVED_PROLONGED_WAIT);
    incident.setConfirmationRequestedAt(LocalDateTime.now().minusDays(2));
    incident.setConfirmationRequestedBy(assigneeId);
    incident.setAssignedTo(assigneeId);
    incident.setDueDate(referenceDueDate);
    incident.setInitialDueDate(referenceDueDate);
    incident.setProposedSolution("solution deja validee");
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(
      incidentTypeConfigRepository.findById(incident.getTypeId())
    ).thenReturn(Optional.of(typeConfig));
    when(incidentRepository.saveAndFlush(any(Incident.class))).thenAnswer(
      i -> i.getArguments()[0]
    );
    ExternalUser assignee = new ExternalUser();
    assignee.setActive(true);
    when(userClientService.getUser(assigneeId)).thenReturn(assignee);

    Incident result = incidentService.confirmRelevance(
      incidentId,
      userId,
      null
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
    assertThat(result.getDueDate()).isAfter(LocalDateTime.now());
    // L'echeance de reference ne bouge pas : sans elle, les 40 jours de retard
    // disparaitraient des metriques de conformite.
    assertThat(result.getInitialDueDate()).isEqualTo(referenceDueDate);
    assertThat(result.getProposedSolution()).isEqualTo("solution deja validee");
    assertThat(result.getConfirmationRequestedAt()).isNull();
  }

  @Test
  @DisplayName(
    "confirmRelevance - Refuses to resume when no confirmation was ever requested"
  )
  void confirmRelevance_WithoutRequest_Throws() {
    incident.setStatus(IncidentStatus.UNRESOLVED_PROLONGED_WAIT);
    incident.setConfirmationRequestedAt(null);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );

    assertThrows(BusinessRuleViolationException.class, () ->
      incidentService.confirmRelevance(incidentId, userId, null)
    );
    verify(incidentRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName(
    "confirmRelevance - Hands the incident to the service head when the handler is gone"
  )
  void confirmRelevance_WithInactiveAssignee_FallsBackToTheServiceHead() {
    UUID assigneeId = UUID.randomUUID();
    UUID handlingServiceId = UUID.randomUUID();
    UUID serviceHeadId = UUID.randomUUID();
    incident.setStatus(IncidentStatus.UNRESOLVED_PROLONGED_WAIT);
    incident.setConfirmationRequestedAt(LocalDateTime.now().minusDays(1));
    incident.setAssignedTo(assigneeId);
    incident.setTransferredToService(handlingServiceId);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(
      incidentTypeConfigRepository.findById(incident.getTypeId())
    ).thenReturn(Optional.of(typeConfig));
    when(incidentRepository.saveAndFlush(any(Incident.class))).thenAnswer(
      i -> i.getArguments()[0]
    );
    ExternalUser goneAssignee = new ExternalUser();
    goneAssignee.setActive(false);
    when(userClientService.getUser(assigneeId)).thenReturn(goneAssignee);
    when(userClientService.resolveServiceHead(handlingServiceId)).thenReturn(
      serviceHeadId
    );
    ExternalUser head = new ExternalUser();
    head.setActive(true);
    when(userClientService.getUser(serviceHeadId)).thenReturn(head);

    Incident result = incidentService.confirmRelevance(
      incidentId,
      userId,
      null
    );

    // Refuser aurait reproduit l'immobilisation que ce circuit corrige.
    assertThat(result.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
    assertThat(result.getAssignedTo()).isEqualTo(serviceHeadId);
  }

  @Test
  @DisplayName(
    "confirmRelevance - Resumes unassigned and alerts when nobody can take the incident"
  )
  void confirmRelevance_WithoutAnyAvailableHandler_ResumesUnassigned() {
    UUID handlingServiceId = UUID.randomUUID();
    incident.setStatus(IncidentStatus.UNRESOLVED_PROLONGED_WAIT);
    incident.setConfirmationRequestedAt(LocalDateTime.now().minusDays(1));
    incident.setAssignedTo(null);
    incident.setTransferredToService(handlingServiceId);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(
      incidentTypeConfigRepository.findById(incident.getTypeId())
    ).thenReturn(Optional.of(typeConfig));
    when(incidentRepository.saveAndFlush(any(Incident.class))).thenAnswer(
      i -> i.getArguments()[0]
    );
    when(userClientService.resolveServiceHead(handlingServiceId)).thenReturn(
      null
    );

    Incident result = incidentService.confirmRelevance(
      incidentId,
      userId,
      null
    );

    // La reprise n'est jamais refusee. Mais « en cours de traitement » sans traitant
    // n'a pas de sens : l'incident repart au stade non assigne et l'alerte part.
    assertThat(result.getStatus()).isEqualTo(IncidentStatus.TRANSFERRED);
    assertThat(result.getAssignedTo()).isNull();
    verify(notificationClientService).notifyIncidentUnassigned(
      any(Incident.class),
      eq(handlingServiceId)
    );
  }

  @Test
  @DisplayName(
    "confirmRelevance - Hands the incident to the agency head when the agency handles it"
  )
  void confirmRelevance_AgencyHandled_FallsBackToTheAgencyHead() {
    UUID goneAssigneeId = UUID.randomUUID();
    UUID agencyHeadId = UUID.randomUUID();
    incident.setStatus(IncidentStatus.UNRESOLVED_PROLONGED_WAIT);
    incident.setConfirmationRequestedAt(LocalDateTime.now().minusDays(1));
    incident.setAssignedTo(goneAssigneeId);
    // Pris en charge par l'agence : aucun service traitant.
    incident.setTransferredToService(null);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(
      incidentTypeConfigRepository.findById(incident.getTypeId())
    ).thenReturn(Optional.of(typeConfig));
    when(incidentRepository.saveAndFlush(any(Incident.class))).thenAnswer(
      i -> i.getArguments()[0]
    );
    ExternalUser gone = new ExternalUser();
    gone.setActive(false);
    when(userClientService.getUser(goneAssigneeId)).thenReturn(gone);
    when(userClientService.resolveAgencyHead(incident.getAgencyId())).thenReturn(
      agencyHeadId
    );
    ExternalUser head = new ExternalUser();
    head.setActive(true);
    when(userClientService.getUser(agencyHeadId)).thenReturn(head);

    Incident result = incidentService.confirmRelevance(
      incidentId,
      userId,
      null
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
    assertThat(result.getAssignedTo()).isEqualTo(agencyHeadId);
  }

  @Test
  @DisplayName("cancel - Clears a pending relevance request")
  void cancel_ClearsThePendingConfirmation() {
    UUID assigneeId = UUID.randomUUID();
    incident.setStatus(IncidentStatus.UNRESOLVED_PROLONGED_WAIT);
    incident.setConfirmationRequestedAt(LocalDateTime.now().minusDays(1));
    incident.setConfirmationRequestedBy(assigneeId);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentRepository.saveAndFlush(any(Incident.class))).thenAnswer(
      i -> i.getArguments()[0]
    );

    Incident result = incidentService.cancel(
      incidentId,
      userId,
      "le probleme n'existe plus"
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.CANCELLED);
    assertThat(result.getConfirmationRequestedAt()).isNull();
    assertThat(result.getConfirmationRequestedBy()).isNull();
  }

  @Test
  @DisplayName(
    "cancel - Refuses an incident that is not validated yet: that one is rejected"
  )
  void cancel_BeforeValidation_ThrowsInvalidStatusTransition() {
    // Annuler et rejeter sont deux portes disjointes : OPEN et PENDING_VALIDATION
    // sont exactement les statuts que reject accepte, cancel doit les refuser.
    for (IncidentStatus status : List.of(
      IncidentStatus.OPEN,
      IncidentStatus.PENDING_VALIDATION
    )) {
      incident.setStatus(status);
      when(incidentRepository.findById(incidentId)).thenReturn(
        Optional.of(incident)
      );

      assertThrows(
        InvalidStatusTransitionException.class,
        () -> incidentService.cancel(incidentId, userId, "reason"),
        "annulation ouverte a tort sur " + status
      );
    }
    verify(incidentRepository, never()).saveAndFlush(any());
  }

  // ── validation préalable par la Direction ──────────────────────────────────

  private UserDetailsImpl directionActor() {
    return new UserDetailsImpl(userId, "actor", "", true, List.of());
  }

  @Test
  @DisplayName(
    "submitSolution - Records the solution, moves incident to DRAFT and notifies the validator"
  )
  void submitSolution_ValidSolution_SetsDraftAndNotifies() {
    UUID validatorId = UUID.randomUUID();
    typeConfig.setRequiresDirectionValidation(true);
    typeConfig.setDirectionValidatorIds(Set.of(validatorId));
    incident.setStatus(IncidentStatus.ASSIGNED);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );

    Incident result = incidentService.submitSolution(
      incidentId,
      "Solution de traitement proposée",
      null,
      directionActor()
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.DRAFT);
    assertThat(result.getProposedSolution()).isEqualTo(
      "Solution de traitement proposée"
    );
    assertThat(result.getDirectionRejectionReason()).isNull();
    verify(notificationClientService).notifySolutionProposed(
      result,
      Set.of(validatorId),
      userId
    );
  }

  @Test
  @DisplayName(
    "submitSolution - Estimated hours are kept and applied to the SLA at Direction validation"
  )
  void submitSolution_WithEstimate_AppliedAtDirectionValidation() {
    typeConfig.setRequiresDirectionValidation(true);
    typeConfig.setDirectionValidatorIds(Set.of(UUID.randomUUID()));
    incident.setStatus(IncidentStatus.ASSIGNED);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );

    Incident submitted = incidentService.submitSolution(
      incidentId,
      "Solution proposée",
      6,
      directionActor()
    );
    assertThat(submitted.getEstimatedResolutionHours()).isEqualTo(6);

    Incident validated = incidentService.validateDirection(
      incidentId,
      directionActor()
    );
    assertThat(validated.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
    assertThat(validated.getDueDate()).isAfter(
      LocalDateTime.now().plusHours(5)
    );
    assertThat(validated.getDueDate()).isBefore(
      LocalDateTime.now().plusHours(7)
    );
  }

  @Test
  @DisplayName(
    "submitSolution - Blank solution throws BusinessRuleViolation and does not persist"
  )
  void submitSolution_BlankSolution_ThrowsAndDoesNotSave() {
    incident.setStatus(IncidentStatus.ASSIGNED);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );

    assertThrows(BusinessRuleViolationException.class, () ->
      incidentService.submitSolution(incidentId, "  ", null, directionActor())
    );
    verify(incidentRepository, never()).saveAndFlush(any());
    verify(notificationClientService, never()).notifySolutionProposed(
      any(),
      any(),
      any()
    );
  }

  @Test
  @DisplayName(
    "submitSolution - Resubmission after a rejection clears the previous rejection reason"
  )
  void submitSolution_Resubmission_ClearsRejectionReason() {
    typeConfig.setRequiresDirectionValidation(true);
    typeConfig.setDirectionValidatorIds(Set.of(UUID.randomUUID()));
    incident.setStatus(IncidentStatus.DRAFT);
    incident.setDirectionRejectionReason("Solution incomplète");
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentTypeConfigRepository.findById(typeConfigId)).thenReturn(
      Optional.of(typeConfig)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );

    Incident result = incidentService.submitSolution(
      incidentId,
      "Nouvelle solution",
      null,
      directionActor()
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.DRAFT);
    assertThat(result.getProposedSolution()).isEqualTo("Nouvelle solution");
    assertThat(result.getDirectionRejectionReason()).isNull();
  }

  @Test
  @DisplayName(
    "validateDirection - Approves the solution, moves incident to IN_PROGRESS and notifies"
  )
  void validateDirection_FromDraft_SetsInProgressAndNotifies() {
    incident.setStatus(IncidentStatus.DRAFT);
    incident.setDirectionRejectionReason("Ancien motif");
    // L'attente de la Direction n'est pas du temps de traitement : la sortie de cette
    // attente repositionne l'echeance sur le SLA du type.
    incident.setDueDate(LocalDateTime.now().minusDays(10));
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(
      incidentTypeConfigRepository.findById(incident.getTypeId())
    ).thenReturn(Optional.of(typeConfig));
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );

    Incident result = incidentService.validateDirection(
      incidentId,
      directionActor()
    );

    assertThat(result.getDueDate()).isAfter(LocalDateTime.now());
    assertThat(result.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
    assertThat(result.getDirectionRejectionReason()).isNull();
    verify(notificationClientService).notifyDirectionValidated(result, userId);
  }

  @Test
  @DisplayName(
    "rejectDirection - Keeps incident in DRAFT, stores the reason and notifies"
  )
  void rejectDirection_WithReason_StaysDraftAndNotifies() {
    incident.setStatus(IncidentStatus.DRAFT);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(incidentRepository.saveAndFlush(any())).thenAnswer(i ->
      i.getArgument(0)
    );

    Incident result = incidentService.rejectDirection(
      incidentId,
      "Procédure non conforme",
      directionActor()
    );

    assertThat(result.getStatus()).isEqualTo(IncidentStatus.DRAFT);
    assertThat(result.getDirectionRejectionReason()).isEqualTo(
      "Procédure non conforme"
    );
    verify(notificationClientService).notifyDirectionRejected(
      result,
      userId,
      "Procédure non conforme"
    );
  }

  @Test
  @DisplayName(
    "rejectDirection - Blank reason throws BusinessRuleViolation and does not persist"
  )
  void rejectDirection_BlankReason_ThrowsAndDoesNotSave() {
    incident.setStatus(IncidentStatus.DRAFT);
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );

    assertThrows(BusinessRuleViolationException.class, () ->
      incidentService.rejectDirection(incidentId, "   ", directionActor())
    );
    verify(incidentRepository, never()).saveAndFlush(any());
    verify(notificationClientService, never()).notifyDirectionRejected(
      any(),
      any(),
      any()
    );
  }
}
