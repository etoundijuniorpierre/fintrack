package com.fintrack.incident.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.fintrack.incident.client.user.UserClientService;
import com.fintrack.incident.client.reporting.ReportingSystemConfigClientService;
import com.fintrack.incident.exception.BusinessRuleViolationException;
import com.fintrack.incident.exception.InvalidStatusTransitionException;
import com.fintrack.incident.model.constant.IncidentAction;
import com.fintrack.incident.model.constant.IncidentActorRole;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.constant.IncidentValidatorRole;
import com.fintrack.incident.model.constant.IncidentValidatorScope;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentTypeConfig;
import com.fintrack.incident.model.readmodel.ExpectedValidator;
import com.fintrack.incident.model.readmodel.ExternalAgency;
import com.fintrack.incident.model.readmodel.ExternalService;
import com.fintrack.incident.model.readmodel.ExternalUser;
import com.fintrack.incident.repository.IncidentTypeConfigRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class IncidentWorkflowGuardTest {

  @Mock
  private UserClientService userClientService;

  @Mock
  private IncidentTypeConfigRepository incidentTypeConfigRepository;

  @Mock
  private ReportingSystemConfigClientService systemConfig;

  private IncidentWorkflowGuard guard;
  private UUID actorId;
  private UUID agencyId;
  private UUID serviceId;
  private Incident incident;

  @BeforeEach
  void setUp() {
    guard = new IncidentWorkflowGuard(
      new IncidentAccessGuard(),
      userClientService,
      incidentTypeConfigRepository,
      new IncidentValidationPolicy(systemConfig, userClientService)
    );
    actorId = UUID.randomUUID();
    agencyId = UUID.randomUUID();
    serviceId = UUID.randomUUID();

    incident = new Incident();
    incident.setId(UUID.randomUUID());
    incident.setTypeId(UUID.randomUUID());
    incident.setCreatedBy(actorId);
    incident.setAgencyId(agencyId);
    incident.setCreatorServiceId(serviceId);
  }

  @Test
  @DisplayName("transfer - rejects unknown target service")
  void transfer_UnknownTargetService_ThrowsBusinessRuleViolation() {
    UUID targetServiceId = UUID.randomUUID();
    incident.setStatus(IncidentStatus.VALIDATED);
    when(userClientService.getService(targetServiceId)).thenThrow(
      new RuntimeException("not found")
    );

    assertThrows(BusinessRuleViolationException.class, () ->
      guard.assertCanTransfer(
        incident,
        user("INCIDENT_VIEW_ALL", "INCIDENT_TRANSFER"),
        targetServiceId,
        null,
        "reason"
      )
    );
  }

  @Test
  @DisplayName(
    "transfer - requires reason when incident is already in progress"
  )
  void transfer_InProgressWithoutReason_ThrowsBusinessRuleViolation() {
    UUID targetServiceId = UUID.randomUUID();
    incident.setStatus(IncidentStatus.IN_PROGRESS);
    ExternalService service = new ExternalService();
    service.setId(targetServiceId);
    when(userClientService.getService(targetServiceId)).thenReturn(service);

    assertThrows(BusinessRuleViolationException.class, () ->
      guard.assertCanTransfer(
        incident,
        user("INCIDENT_VIEW_ALL", "INCIDENT_TRANSFER"),
        targetServiceId,
        null,
        " "
      )
    );
  }

  @Test
  @DisplayName("transfer - allows the creator agency when the creator has no service")
  void transfer_CreatorWithoutServiceToOwnAgency_Succeeds() {
    incident.setStatus(IncidentStatus.VALIDATED);
    incident.setCreatorServiceId(null);
    ExternalAgency agency = new ExternalAgency();
    agency.setId(agencyId);
    agency.setActive(true);
    when(userClientService.getAgency(agencyId)).thenReturn(agency);

    assertDoesNotThrow(() ->
      guard.assertCanTransfer(
        incident,
        user("INCIDENT_VIEW_ALL", "INCIDENT_TRANSFER"),
        null,
        agencyId,
        "reason"
      )
    );
  }

  @Test
  @DisplayName("transfer - rejects an agency target when the creator has a service")
  void transfer_CreatorWithServiceToAgency_ThrowsBusinessRuleViolation() {
    incident.setStatus(IncidentStatus.VALIDATED);

    assertThrows(BusinessRuleViolationException.class, () ->
      guard.assertCanTransfer(
        incident,
        user("INCIDENT_VIEW_ALL", "INCIDENT_TRANSFER"),
        null,
        agencyId,
        "reason"
      )
    );
  }

  @Test
  @DisplayName("transfer - rejects an agency different from the creator agency")
  void transfer_DifferentAgency_ThrowsBusinessRuleViolation() {
    incident.setStatus(IncidentStatus.VALIDATED);
    incident.setCreatorServiceId(null);

    assertThrows(BusinessRuleViolationException.class, () ->
      guard.assertCanTransfer(
        incident,
        user("INCIDENT_VIEW_ALL", "INCIDENT_TRANSFER"),
        null,
        UUID.randomUUID(),
        "reason"
      )
    );
  }

  @Test
  @DisplayName(
    "assign - rejects assignee without incident treatment permission"
  )
  void assign_AssigneeWithoutIncidentPermission_ThrowsBusinessRuleViolation() {
    UUID assigneeId = UUID.randomUUID();
    incident.setStatus(IncidentStatus.VALIDATED);
    incident.setTransferredToService(serviceId);

    ExternalUser assignee = new ExternalUser();
    assignee.setId(assigneeId);
    assignee.setActive(true);
    assignee.setAgencyId(agencyId);
    assignee.setServiceId(serviceId);
    assignee.setPermissions(Set.of("INCIDENT_VIEW_OWN"));
    when(userClientService.getUser(assigneeId)).thenReturn(assignee);

    assertThrows(BusinessRuleViolationException.class, () ->
      guard.assertCanAssign(
        incident,
        user("INCIDENT_VIEW_ALL", "INCIDENT_ASSIGN"),
        assigneeId
      )
    );
  }

  @Test
  @DisplayName("resolve - allows current assignee")
  void resolve_CurrentAssignee_AllowsAction() {
    IncidentTypeConfig config = new IncidentTypeConfig();
    config.setResolverRoles(Set.of(IncidentActorRole.ASSIGNEE));
    when(incidentTypeConfigRepository.findById(any())).thenReturn(Optional.of(config));

    incident.setStatus(IncidentStatus.TREATED);
    incident.setAssignedTo(actorId);

    assertDoesNotThrow(() ->
      guard.assertCanAct(
        incident,
        user("INCIDENT_RESOLVE"),
        IncidentAction.RESOLVE
      )
    );
  }

  @Test
  @DisplayName("resolve - rejects users who are not the current assignee")
  void resolve_NotCurrentAssignee_ThrowsAccessDenied() {
    IncidentTypeConfig config = new IncidentTypeConfig();
    config.setResolverRoles(Set.of(IncidentActorRole.ASSIGNEE));
    when(incidentTypeConfigRepository.findById(any())).thenReturn(Optional.of(config));

    incident.setStatus(IncidentStatus.TREATED);
    incident.setAssignedTo(UUID.randomUUID());

    assertThrows(AccessDeniedException.class, () ->
      guard.assertCanAct(
        incident,
        user("INCIDENT_RESOLVE"),
        IncidentAction.RESOLVE
      )
    );
  }

  @Test
  @DisplayName("close (CREATOR) - le createur peut cloturer")
  void close_Creator_CloserTypeCreator_AllowsAction() {
    incident.setStatus(IncidentStatus.RESOLVED);
    incident.setAssignedTo(UUID.randomUUID());
    configureCloserRoles(IncidentActorRole.CREATOR);

    assertDoesNotThrow(() ->
      guard.assertCanAct(incident, user("INCIDENT_CLOSE"), IncidentAction.CLOSE)
    );
  }

  @Test
  @DisplayName("close (CREATOR) - un non-createur est rejete")
  void close_NonCreator_CloserTypeCreator_ThrowsAccessDenied() {
    incident.setStatus(IncidentStatus.RESOLVED);
    incident.setCreatedBy(UUID.randomUUID());
    incident.setAssignedTo(actorId);
    configureCloserRoles(IncidentActorRole.CREATOR);

    // L'acteur passe la vue en tant qu'assigne, mais la regle CREATOR du type le rejette.
    assertThrows(AccessDeniedException.class, () ->
      guard.assertCanAct(incident, user("INCIDENT_CLOSE"), IncidentAction.CLOSE)
    );
  }

  @Test
  @DisplayName(
    "close (defaut) - sans regle de type, l assigne courant peut cloturer"
  )
  void close_NullCloserType_DefaultsToAssignee() {
    incident.setStatus(IncidentStatus.RESOLVED);
    incident.setCreatedBy(UUID.randomUUID());
    incident.setAssignedTo(actorId);

    assertDoesNotThrow(() ->
      guard.assertCanAct(incident, user("INCIDENT_CLOSE"), IncidentAction.CLOSE)
    );
  }

  @Test
  @DisplayName("close (ASSIGNEE) - l assigne courant peut cloturer")
  void close_Assignee_CloserTypeAssignee_AllowsAction() {
    incident.setStatus(IncidentStatus.RESOLVED);
    incident.setAssignedTo(actorId);
    configureCloserRoles(IncidentActorRole.ASSIGNEE);

    assertDoesNotThrow(() ->
      guard.assertCanAct(incident, user("INCIDENT_CLOSE"), IncidentAction.CLOSE)
    );
  }

  @Test
  @DisplayName("close (ASSIGNEE) - un non-assigne est rejete")
  void close_NonAssignee_CloserTypeAssignee_ThrowsAccessDenied() {
    incident.setStatus(IncidentStatus.RESOLVED);
    incident.setAssignedTo(UUID.randomUUID());
    configureCloserRoles(IncidentActorRole.ASSIGNEE);

    // L'acteur passe la vue en tant que createur, mais la regle ASSIGNEE du type le rejette.
    assertThrows(AccessDeniedException.class, () ->
      guard.assertCanAct(incident, user("INCIDENT_CLOSE"), IncidentAction.CLOSE)
    );
  }

  @Test
  @DisplayName("close (BOTH) - le createur peut cloturer")
  void close_Creator_CloserTypeBoth_AllowsAction() {
    incident.setStatus(IncidentStatus.RESOLVED);
    incident.setAssignedTo(UUID.randomUUID());
    configureCloserRoles(IncidentActorRole.CREATOR, IncidentActorRole.ASSIGNEE);
    // createdBy = actorId (setUp)

    assertDoesNotThrow(() ->
      guard.assertCanAct(incident, user("INCIDENT_CLOSE"), IncidentAction.CLOSE)
    );
  }

  @Test
  @DisplayName("close (BOTH) - l assigne courant peut cloturer")
  void close_Assignee_CloserTypeBoth_AllowsAction() {
    incident.setStatus(IncidentStatus.RESOLVED);
    incident.setCreatedBy(UUID.randomUUID());
    incident.setAssignedTo(actorId);
    configureCloserRoles(IncidentActorRole.CREATOR, IncidentActorRole.ASSIGNEE);

    assertDoesNotThrow(() ->
      guard.assertCanAct(incident, user("INCIDENT_CLOSE"), IncidentAction.CLOSE)
    );
  }

  @Test
  @DisplayName("close (BOTH) - un tiers (ni createur ni assigne) est rejete")
  void close_ThirdParty_CloserTypeBoth_ThrowsAccessDenied() {
    incident.setStatus(IncidentStatus.RESOLVED);
    incident.setCreatedBy(UUID.randomUUID());
    incident.setAssignedTo(UUID.randomUUID());
    configureCloserRoles(IncidentActorRole.CREATOR, IncidentActorRole.ASSIGNEE);

    assertThrows(AccessDeniedException.class, () ->
      guard.assertCanAct(
        incident,
        user("INCIDENT_VIEW_AGENCY", "INCIDENT_CLOSE"),
        IncidentAction.CLOSE
      )
    );
  }

  @Test
  @DisplayName(
    "close - une vue globale (admin) contourne la regle de cloture du type"
  )
  void close_GlobalViewer_CloserTypeAssignee_BypassesCloserType() {
    incident.setStatus(IncidentStatus.RESOLVED);
    incident.setCreatedBy(UUID.randomUUID());
    incident.setAssignedTo(UUID.randomUUID());

    assertDoesNotThrow(() ->
      guard.assertCanAct(
        incident,
        user("INCIDENT_VIEW_ALL", "INCIDENT_CLOSE"),
        IncidentAction.CLOSE
      )
    );
  }

  @Test
  @DisplayName(
    "close - une vue globale (admin) ne consulte pas la regle de cloture du type"
  )
  void close_GlobalViewer_CloserTypeCreator_BypassesCloserType() {
    incident.setStatus(IncidentStatus.RESOLVED);
    incident.setCreatedBy(UUID.randomUUID());
    incident.setAssignedTo(UUID.randomUUID());

    assertDoesNotThrow(() ->
      guard.assertCanAct(
        incident,
        user("INCIDENT_VIEW_ALL", "INCIDENT_CLOSE"),
        IncidentAction.CLOSE
      )
    );
  }

  @Test
  @DisplayName(
    "validate (AGENCY_MANAGER) - un Chef d'Agence (INCIDENT_VIEW_AGENCY) de la meme agence est autorise"
  )
  void validate_AgencyManagerScope_SameAgency_Allows() {
    incident.setStatus(IncidentStatus.OPEN);
    UUID typeId = UUID.randomUUID();
    incident.setTypeId(typeId);
    IncidentTypeConfig type = new IncidentTypeConfig();
    type.setValidatorScope(IncidentValidatorScope.AGENCY_MANAGER);
    when(incidentTypeConfigRepository.findById(typeId)).thenReturn(
      Optional.of(type)
    );

    assertDoesNotThrow(() ->
      guard.assertCanAct(
        incident,
        user("INCIDENT_VALIDATE", "INCIDENT_VIEW_AGENCY"),
        IncidentAction.VALIDATE
      )
    );
  }

  @Test
  @DisplayName(
    "validate (AGENCY_MANAGER) - durcissement : sans INCIDENT_VIEW_AGENCY, refuse meme dans la bonne agence"
  )
  void validate_AgencyManagerScope_WithoutViewAgency_ThrowsAccessDenied() {
    incident.setStatus(IncidentStatus.OPEN);
    UUID typeId = UUID.randomUUID();
    incident.setTypeId(typeId);
    IncidentTypeConfig type = new IncidentTypeConfig();
    type.setValidatorScope(IncidentValidatorScope.AGENCY_MANAGER);
    when(incidentTypeConfigRepository.findById(typeId)).thenReturn(
      Optional.of(type)
    );

    assertThrows(AccessDeniedException.class, () ->
      guard.assertCanAct(
        incident,
        user("INCIDENT_VALIDATE"),
        IncidentAction.VALIDATE
      )
    );
  }

  @Test
  @DisplayName(
    "validate (TARGET_SERVICE_MANAGER) - le responsable du service cible est autorise"
  )
  void validate_TargetServiceScope_Manager_Allows() {
    incident.setStatus(IncidentStatus.OPEN);
    UUID typeId = UUID.randomUUID();
    incident.setTypeId(typeId);
    IncidentTypeConfig type = new IncidentTypeConfig();
    type.setValidatorScope(IncidentValidatorScope.TARGET_SERVICE_MANAGER);
    type.setDefaultTargetServiceId(serviceId); // = service primaire du valideur (helper user())
    when(incidentTypeConfigRepository.findById(typeId)).thenReturn(
      Optional.of(type)
    );

    assertDoesNotThrow(() ->
      guard.assertCanAct(
        incident,
        user("INCIDENT_VALIDATE"),
        IncidentAction.VALIDATE
      )
    );
  }

  @Test
  @DisplayName(
    "validate (TARGET_SERVICE_MANAGER) - un valideur hors service cible est rejete"
  )
  void validate_TargetServiceScope_NotManager_ThrowsAccessDenied() {
    incident.setStatus(IncidentStatus.OPEN);
    UUID typeId = UUID.randomUUID();
    incident.setTypeId(typeId);
    IncidentTypeConfig type = new IncidentTypeConfig();
    type.setValidatorScope(IncidentValidatorScope.TARGET_SERVICE_MANAGER);
    type.setDefaultTargetServiceId(UUID.randomUUID()); // service cible different du valideur
    when(incidentTypeConfigRepository.findById(typeId)).thenReturn(
      Optional.of(type)
    );

    assertThrows(AccessDeniedException.class, () ->
      guard.assertCanAct(
        incident,
        user("INCIDENT_VALIDATE"),
        IncidentAction.VALIDATE
      )
    );
  }

  @Test
  @DisplayName(
    "validate (SOURCE_SERVICE_MANAGER) - auto-validation habituelle sans type cible vers son service"
  )
  void validate_SourceServiceScope_ServiceHead_Allows() {
    incident.setStatus(IncidentStatus.OPEN);
    UUID typeId = UUID.randomUUID();
    incident.setTypeId(typeId);
    IncidentTypeConfig type = new IncidentTypeConfig();
    type.setValidatorScope(IncidentValidatorScope.SOURCE_SERVICE_MANAGER);
    when(incidentTypeConfigRepository.findById(typeId)).thenReturn(
      Optional.of(type)
    );
    // Le chef resolu du service du createur est l'utilisateur courant.
    when(userClientService.resolveServiceHead(serviceId)).thenReturn(actorId);

    assertDoesNotThrow(() ->
      guard.assertCanAct(
        incident,
        user("INCIDENT_VALIDATE"),
        IncidentAction.VALIDATE
      )
    );
  }

  @Test
  @DisplayName(
    "validate (SOURCE_SERVICE_MANAGER) - un valideur qui n'est pas le chef du service source est rejete"
  )
  void validate_SourceServiceScope_NotServiceHead_ThrowsAccessDenied() {
    incident.setStatus(IncidentStatus.OPEN);
    UUID typeId = UUID.randomUUID();
    incident.setTypeId(typeId);
    IncidentTypeConfig type = new IncidentTypeConfig();
    type.setValidatorScope(IncidentValidatorScope.SOURCE_SERVICE_MANAGER);
    when(incidentTypeConfigRepository.findById(typeId)).thenReturn(
      Optional.of(type)
    );
    // Le chef du service source est quelqu'un d'autre que l'utilisateur courant.
    when(userClientService.resolveServiceHead(serviceId)).thenReturn(
      UUID.randomUUID()
    );

    assertThrows(AccessDeniedException.class, () ->
      guard.assertCanAct(
        incident,
        user("INCIDENT_VALIDATE"),
        IncidentAction.VALIDATE
      )
    );
  }

  @Test
  @DisplayName(
    "validate (SOURCE_SERVICE_MANAGER) - createur sans service : repli sur le chef d'agence"
  )
  void validate_SourceServiceScope_NoCreatorService_FallsBackToAgency() {
    incident.setStatus(IncidentStatus.OPEN);
    incident.setCreatorServiceId(null);
    UUID typeId = UUID.randomUUID();
    incident.setTypeId(typeId);
    IncidentTypeConfig type = new IncidentTypeConfig();
    type.setValidatorScope(IncidentValidatorScope.SOURCE_SERVICE_MANAGER);
    when(incidentTypeConfigRepository.findById(typeId)).thenReturn(
      Optional.of(type)
    );

    assertDoesNotThrow(() ->
      guard.assertCanAct(
        incident,
        user("INCIDENT_VALIDATE", "INCIDENT_VIEW_AGENCY"),
        IncidentAction.VALIDATE
      )
    );
  }

  @Test
  @DisplayName(
    "validate (SOURCE_SERVICE_MANAGER) - service source sans chef : repli sur le chef d'agence"
  )
  void validate_SourceServiceScope_ServiceWithoutHead_FallsBackToAgency() {
    incident.setStatus(IncidentStatus.OPEN);
    UUID typeId = UUID.randomUUID();
    incident.setTypeId(typeId);
    IncidentTypeConfig type = new IncidentTypeConfig();
    type.setValidatorScope(IncidentValidatorScope.SOURCE_SERVICE_MANAGER);
    when(incidentTypeConfigRepository.findById(typeId)).thenReturn(
      Optional.of(type)
    );
    when(userClientService.resolveServiceHead(serviceId)).thenReturn(null);

    assertDoesNotThrow(() ->
      guard.assertCanAct(
        incident,
        user("INCIDENT_VALIDATE", "INCIDENT_VIEW_AGENCY"),
        IncidentAction.VALIDATE
      )
    );
  }

  @Test
  @DisplayName(
    "validate - un administrateur (INCIDENT_VIEW_ALL) est toujours autorise, tous scopes confondus"
  )
  void validate_Admin_Allows() {
    incident.setStatus(IncidentStatus.OPEN);

    assertDoesNotThrow(() ->
      guard.assertCanAct(
        incident,
        user("INCIDENT_VIEW_ALL", "INCIDENT_VALIDATE"),
        IncidentAction.VALIDATE
      )
    );
  }

  @Test
  @DisplayName(
    "validate (ADMIN) - tout autre profil que l'administrateur est rejete"
  )
  void validate_AdminScope_NonAdmin_ThrowsAccessDenied() {
    incident.setStatus(IncidentStatus.OPEN);
    UUID typeId = UUID.randomUUID();
    incident.setTypeId(typeId);
    IncidentTypeConfig type = new IncidentTypeConfig();
    type.setValidatorScope(IncidentValidatorScope.ADMIN);
    when(incidentTypeConfigRepository.findById(typeId)).thenReturn(
      Optional.of(type)
    );

    assertThrows(AccessDeniedException.class, () ->
      guard.assertCanAct(
        incident,
        user("INCIDENT_VALIDATE", "INCIDENT_VIEW_AGENCY"),
        IncidentAction.VALIDATE
      )
    );
  }

  @Test
  @DisplayName(
    "canValidate - retourne true quand la portee autorise (verdict UI, sans exception)"
  )
  void canValidate_True_WhenScopeAllows() {
    UUID typeId = UUID.randomUUID();
    incident.setTypeId(typeId);
    IncidentTypeConfig type = new IncidentTypeConfig();
    type.setValidatorScope(IncidentValidatorScope.SOURCE_SERVICE_MANAGER);
    when(incidentTypeConfigRepository.findById(typeId)).thenReturn(
      Optional.of(type)
    );
    when(userClientService.resolveServiceHead(serviceId)).thenReturn(actorId);

    assertTrue(guard.canValidate(incident, user("INCIDENT_VALIDATE")));
  }

  @Test
  @DisplayName("canValidate - retourne false quand la portee refuse")
  void canValidate_False_WhenScopeDenies() {
    UUID typeId = UUID.randomUUID();
    incident.setTypeId(typeId);
    IncidentTypeConfig type = new IncidentTypeConfig();
    type.setValidatorScope(IncidentValidatorScope.ADMIN);
    when(incidentTypeConfigRepository.findById(typeId)).thenReturn(
      Optional.of(type)
    );

    assertFalse(
      guard.canValidate(
        incident,
        user("INCIDENT_VALIDATE", "INCIDENT_VIEW_AGENCY")
      )
    );
  }

  @Test
  @DisplayName("start - allows current assignee")
  void start_CurrentAssignee_AllowsAction() {
    incident.setStatus(IncidentStatus.ASSIGNED);
    incident.setAssignedTo(actorId);

    assertDoesNotThrow(() ->
      guard.assertCanAct(incident, user("INCIDENT_TREAT"), IncidentAction.START)
    );
  }

  @Test
  @DisplayName("start - rejects when assigned to another user")
  void start_AssignedToAnotherUser_ThrowsAccessDenied() {
    incident.setStatus(IncidentStatus.ASSIGNED);
    incident.setAssignedTo(UUID.randomUUID());

    assertThrows(AccessDeniedException.class, () ->
      guard.assertCanAct(incident, user("INCIDENT_TREAT"), IncidentAction.START)
    );
  }

  @Test
  @DisplayName("reopen - requires reason")
  void reopen_WithoutReason_ThrowsBusinessRuleViolation() {
    incident.setStatus(IncidentStatus.RESOLVED);
    // Acteur habilite (chef d'agence source = defaut reouverture) pour isoler
    // l'echec sur le motif manquant.
    assertThrows(BusinessRuleViolationException.class, () ->
      guard.assertCanReopen(
        incident,
        user("INCIDENT_REOPEN", "INCIDENT_VIEW_AGENCY"),
        " "
      )
    );
  }

  @Test
  @DisplayName(
    "reopen apres traitement (defaut) - le chef d'agence source peut rouvrir"
  )
  void reopenResolved_DefaultAgencyManager_AllowsAction() {
    incident.setStatus(IncidentStatus.RESOLVED);
    incident.setAssignedTo(UUID.randomUUID());

    assertDoesNotThrow(() ->
      guard.assertCanReopen(
        incident,
        user("INCIDENT_REOPEN", "INCIDENT_VIEW_AGENCY"),
        "motif"
      )
    );
  }

  @Test
  @DisplayName(
    "reopen apres traitement (defaut) - le traitant (assigne) est rejete"
  )
  void reopenResolved_DefaultReopener_AssigneeRejected() {
    // Le coeur du bug : par defaut, le traitant ne peut PAS rouvrir apres resolution.
    incident.setStatus(IncidentStatus.RESOLVED);
    incident.setAssignedTo(actorId);

    assertThrows(AccessDeniedException.class, () ->
      guard.assertCanReopen(incident, user("INCIDENT_REOPEN"), "motif")
    );
  }

  @Test
  @DisplayName(
    "reopen apres traitement (config ASSIGNEE) - le traitant peut rouvrir"
  )
  void reopenResolved_ConfiguredAssignee_AllowsAction() {
    incident.setStatus(IncidentStatus.RESOLVED);
    incident.setAssignedTo(actorId);
    configureReopenerRoles(IncidentActorRole.ASSIGNEE);

    assertDoesNotThrow(() ->
      guard.assertCanReopen(incident, user("INCIDENT_REOPEN"), "motif")
    );
  }

  @Test
  @DisplayName(
    "reopen apres traitement (config CREATOR) - un non-createur est rejete"
  )
  void reopenResolved_ConfiguredCreator_NonCreatorRejected() {
    incident.setStatus(IncidentStatus.RESOLVED);
    incident.setCreatedBy(UUID.randomUUID());
    incident.setAssignedTo(actorId);
    configureReopenerRoles(IncidentActorRole.CREATOR);

    assertThrows(AccessDeniedException.class, () ->
      guard.assertCanReopen(incident, user("INCIDENT_REOPEN"), "motif")
    );
  }

  @Test
  @DisplayName(
    "reopen apres traitement - une vue globale (admin) contourne la regle"
  )
  void reopenResolved_GlobalViewer_BypassesRoles() {
    incident.setStatus(IncidentStatus.RESOLVED);
    incident.setCreatedBy(UUID.randomUUID());
    incident.setAssignedTo(UUID.randomUUID());

    assertDoesNotThrow(() ->
      guard.assertCanReopen(
        incident,
        user("INCIDENT_VIEW_ALL", "INCIDENT_REOPEN"),
        "motif"
      )
    );
  }

  @Test
  @DisplayName("clone - requires create permission")
  void clone_WithoutCreatePermission_ThrowsAccessDenied() {
    assertThrows(AccessDeniedException.class, () ->
      guard.assertCanClone(incident, user("INCIDENT_VIEW"))
    );
  }

  @Test
  @DisplayName("resubmit - requires creator for REOPENED incident")
  void resubmit_ByNonCreator_ThrowsAccessDenied() {
    incident.setStatus(IncidentStatus.REOPENED);

    // Ensure user is not the creator
    UUID otherCreatorId = UUID.randomUUID();
    incident.setCreatedBy(otherCreatorId);

    assertThrows(AccessDeniedException.class, () ->
      guard.assertCanAct(
        incident,
        user("INCIDENT_CREATE"),
        IncidentAction.RESUBMIT
      )
    );
  }

  @Test
  @DisplayName(
    "assign - accepts assignee who manages the handling service via managedServiceIds"
  )
  void assign_AssigneeManagesService_Succeeds() {
    UUID assigneeId = UUID.randomUUID();
    UUID handlingServiceId = UUID.randomUUID();
    incident.setStatus(IncidentStatus.VALIDATED);
    incident.setTransferredToService(handlingServiceId);

    ExternalUser assignee = new ExternalUser();
    assignee.setId(assigneeId);
    assignee.setActive(true);
    assignee.setAgencyId(agencyId);
    assignee.setServiceId(UUID.randomUUID()); // different service
    assignee.setManagedServiceIds(Set.of(handlingServiceId)); // but manages the target
    assignee.setPermissions(Set.of("INCIDENT_TREAT"));
    when(userClientService.getUser(assigneeId)).thenReturn(assignee);

    assertDoesNotThrow(() ->
      guard.assertCanAssign(
        incident,
        user("INCIDENT_VIEW_ALL", "INCIDENT_ASSIGN"),
        assigneeId
      )
    );
  }

  @Test
  @DisplayName(
    "assign - rejects user with only INCIDENT_TREAT (no INCIDENT_ASSIGN)"
  )
  void assign_OnlyTreatPermission_ThrowsAccessDenied() {
    UUID assigneeId = UUID.randomUUID();
    incident.setStatus(IncidentStatus.VALIDATED);

    assertThrows(AccessDeniedException.class, () ->
      guard.assertCanAssign(
        incident,
        user("INCIDENT_VIEW_ALL", "INCIDENT_TREAT"),
        assigneeId
      )
    );
  }

  @Test
  @DisplayName(
    "assign - allows self-assignment for a handler (TREAT) without INCIDENT_ASSIGN"
  )
  void assign_SelfAssign_HandlerWithoutAssignPermission_Allowed() {
    incident.setStatus(IncidentStatus.VALIDATED);

    ExternalUser self = new ExternalUser();
    self.setId(actorId);
    self.setActive(true);
    self.setAgencyId(agencyId);
    self.setServiceId(serviceId);
    self.setManagedServiceIds(Set.of());
    self.setPermissions(Set.of("INCIDENT_TREAT"));
    when(userClientService.getUser(actorId)).thenReturn(self);

    assertDoesNotThrow(() ->
      guard.assertCanAssign(
        incident,
        user("INCIDENT_VIEW_ALL", "INCIDENT_TREAT"),
        actorId
      )
    );
  }

  @Test
  @DisplayName(
    "assign - rejects assignee outside handling service (no managedServiceIds)"
  )
  void assign_AssigneeOutsideService_ThrowsBusinessRuleViolation() {
    UUID assigneeId = UUID.randomUUID();
    UUID handlingServiceId = UUID.randomUUID();
    incident.setStatus(IncidentStatus.VALIDATED);
    incident.setTransferredToService(handlingServiceId);

    ExternalUser assignee = new ExternalUser();
    assignee.setId(assigneeId);
    assignee.setActive(true);
    assignee.setAgencyId(agencyId);
    assignee.setServiceId(UUID.randomUUID()); // different service
    assignee.setManagedServiceIds(Set.of()); // does not manage the target
    assignee.setPermissions(Set.of("INCIDENT_TREAT"));
    when(userClientService.getUser(assigneeId)).thenReturn(assignee);

    assertThrows(BusinessRuleViolationException.class, () ->
      guard.assertCanAssign(
        incident,
        user("INCIDENT_VIEW_ALL", "INCIDENT_ASSIGN"),
        assigneeId
      )
    );
  }

  @Test
  @DisplayName(
    "assign - accepts assignee in same agency when no handling service"
  )
  void assign_SameAgencyNoService_Succeeds() {
    UUID assigneeId = UUID.randomUUID();
    incident.setStatus(IncidentStatus.VALIDATED);
    incident.setTransferredToService(null);
    incident.setCreatorServiceId(null);

    ExternalUser assignee = new ExternalUser();
    assignee.setId(assigneeId);
    assignee.setActive(true);
    assignee.setAgencyId(agencyId); // same agency as incident
    assignee.setPermissions(Set.of("INCIDENT_TREAT"));
    when(userClientService.getUser(assigneeId)).thenReturn(assignee);

    assertDoesNotThrow(() ->
      guard.assertCanAssign(
        incident,
        user("INCIDENT_VIEW_ALL", "INCIDENT_ASSIGN"),
        assigneeId
      )
    );
  }

  private UserDetailsImpl user(String... authorities) {
    return new UserDetailsImpl(
      actorId,
      "actor",
      null,
      true,
      Arrays.stream(authorities)
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toSet()),
      serviceId,
      agencyId
    );
  }

  // Utilisateur (chef) rattache a une agence donnee, id distinct du soumetteur.
  private UserDetailsImpl userInAgency(UUID agency, String... authorities) {
    return new UserDetailsImpl(
      UUID.randomUUID(),
      "chief",
      null,
      true,
      Arrays.stream(authorities)
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toSet()),
      UUID.randomUUID(),
      agency
    );
  }

  @Test
  @DisplayName(
    "canViewProposedSolution - agency chief of the SUBMITTER's agency sees it, not the incident's agency chief"
  )
  void canViewProposedSolution_UsesSubmitterAgencyNotIncidentAgency() {
    UUID submitterId = UUID.randomUUID();
    UUID submitterAgencyId = UUID.randomUUID();
    incident.setStatus(IncidentStatus.DRAFT);
    incident.setAssignedTo(submitterId); // le soumetteur = l'assigne courant
    incident.setProposedSolution("solution proposee");
    // incident.getAgencyId() reste l'agence de l'incident (agencyId), != submitterAgencyId

    ExternalUser submitter = new ExternalUser();
    submitter.setId(submitterId);
    submitter.setAgencyId(submitterAgencyId);
    when(userClientService.getUser(submitterId)).thenReturn(submitter);

    // Chef d'agence de l'agence DU SOUMETTEUR -> voit la proposition.
    assertTrue(
      guard.canViewProposedSolution(
        incident,
        userInAgency(submitterAgencyId, "INCIDENT_VIEW_AGENCY")
      )
    );

    // Chef d'agence de l'agence DE L'INCIDENT (differente) -> ne voit PAS.
    assertFalse(
      guard.canViewProposedSolution(
        incident,
        userInAgency(agencyId, "INCIDENT_VIEW_AGENCY")
      )
    );
  }

  @Test
  @DisplayName(
    "canViewProposedSolution - a plain member of the handling service does NOT see it, only its head does"
  )
  void canViewProposedSolution_ServiceMemberDenied_HeadAllowed() {
    UUID handlingServiceId = UUID.randomUUID();
    UUID submitterId = UUID.randomUUID();
    incident.setStatus(IncidentStatus.IN_PROGRESS);
    incident.setTransferredToService(handlingServiceId); // service traitant
    incident.setAssignedTo(submitterId);
    incident.setProposedSolution("solution proposee");

    // Le soumetteur appartient au service traitant : "chef de son service" = chef de ce service.
    ExternalUser submitterExt = new ExternalUser();
    submitterExt.setId(submitterId);
    submitterExt.setServiceId(handlingServiceId);
    when(userClientService.getUser(submitterId)).thenReturn(submitterExt);

    // Membre simple du service traitant (serviceId == service traitant) mais PAS chef.
    UUID memberId = UUID.randomUUID();
    UserDetailsImpl member = new UserDetailsImpl(
      memberId,
      "member",
      null,
      true,
      Set.of(),
      handlingServiceId,
      agencyId
    );
    ExternalUser memberExt = new ExternalUser();
    memberExt.setId(memberId);
    memberExt.setManagedServiceIds(Set.of()); // ne gere aucun service
    when(userClientService.getUser(memberId)).thenReturn(memberExt);
    assertFalse(guard.canViewProposedSolution(incident, member));

    // Chef du service traitant declare par le service -> voit, meme si la
    // liste managedServiceIds n'est pas synchronisee.
    UUID headId = UUID.randomUUID();
    UserDetailsImpl head = new UserDetailsImpl(
      headId,
      "head",
      null,
      true,
      Set.of(),
      UUID.randomUUID(),
      agencyId
    );
    when(userClientService.resolveServiceHead(handlingServiceId)).thenReturn(
      headId
    );
    assertTrue(guard.canViewProposedSolution(incident, head));
  }

  @Test
  @DisplayName(
    "cancel - service head can cancel an assigned incident with no solution proposal"
  )
  void cancel_ServiceHeadCanCancelAssignedIncident() {
    UUID handlingServiceId = UUID.randomUUID();
    UUID serviceHeadId = UUID.randomUUID();
    incident.setStatus(IncidentStatus.ASSIGNED);
    incident.setTransferredToService(handlingServiceId);
    incident.setAssignedTo(UUID.randomUUID());
    when(userClientService.resolveServiceHead(handlingServiceId)).thenReturn(
      serviceHeadId
    );

    UserDetailsImpl serviceHead = new UserDetailsImpl(
      serviceHeadId,
      "service-head",
      null,
      true,
      Set.of(new SimpleGrantedAuthority("INCIDENT_VIEW_SERVICE")),
      handlingServiceId,
      agencyId
    );

    assertDoesNotThrow(() ->
      guard.assertCanAct(incident, serviceHead, IncidentAction.CANCEL)
    );
  }

  @Test
  @DisplayName("cancel - rejects an incident whose treatment is in progress")
  void cancel_InProgressIsRejected() {
    incident.setStatus(IncidentStatus.IN_PROGRESS);

    assertThrows(InvalidStatusTransitionException.class, () ->
      guard.assertCanAct(
        incident,
        user("INCIDENT_VIEW_ALL", "INCIDENT_CANCEL"),
        IncidentAction.CANCEL
      )
    );
  }

  @Test
  @DisplayName(
    "cancel - service head can cancel blocked and prolonged-wait incidents"
  )
  void cancel_ServiceHeadCanCancelBlockedOrProlongedWaitIncident() {
    UUID handlingServiceId = UUID.randomUUID();
    UUID serviceHeadId = UUID.randomUUID();
    incident.setTransferredToService(handlingServiceId);
    when(userClientService.resolveServiceHead(handlingServiceId)).thenReturn(
      serviceHeadId
    );

    UserDetailsImpl serviceHead = new UserDetailsImpl(
      serviceHeadId,
      "service-head",
      null,
      true,
      Set.of(new SimpleGrantedAuthority("INCIDENT_VIEW_SERVICE")),
      handlingServiceId,
      agencyId
    );

    incident.setProposedSolution("Proposition conservee");
    for (
      IncidentStatus status :
      Set.of(
        IncidentStatus.BLOCKED,
        IncidentStatus.UNRESOLVED_PROLONGED_WAIT
      )
    ) {
      incident.setStatus(status);
      assertDoesNotThrow(() ->
        guard.assertCanAct(incident, serviceHead, IncidentAction.CANCEL)
      );
    }
  }

  @Test
  @DisplayName(
    "cancel - agency head remains responsible while target service is only predefined"
  )
  void cancel_AgencyHeadCanCancelBeforeServiceAssignment() {
    UUID agencyHeadId = UUID.randomUUID();
    // Valide mais pas encore routé : le service cible n'est qu'une prédéfinition,
    // l'incident reste sous la responsabilité de l'agence.
    incident.setStatus(IncidentStatus.VALIDATED);
    incident.setTransferredToService(UUID.randomUUID());
    when(userClientService.resolveAgencyHead(agencyId)).thenReturn(
      agencyHeadId
    );

    UserDetailsImpl agencyHead = new UserDetailsImpl(
      agencyHeadId,
      "agency-head",
      null,
      true,
      Set.of(new SimpleGrantedAuthority("INCIDENT_VIEW_AGENCY")),
      null,
      agencyId
    );

    assertDoesNotThrow(() ->
      guard.assertCanAct(incident, agencyHead, IncidentAction.CANCEL)
    );
  }

  @Test
  @DisplayName(
    "cancel - agency head is denied once incident is assigned to target service"
  )
  void cancel_AgencyHeadDeniedAfterServiceAssignment() {
    UUID agencyHeadId = UUID.randomUUID();
    UUID handlingServiceId = UUID.randomUUID();
    incident.setStatus(IncidentStatus.ASSIGNED);
    incident.setTransferredToService(handlingServiceId);
    incident.setAssignedTo(UUID.randomUUID());
    when(userClientService.resolveServiceHead(handlingServiceId)).thenReturn(
      UUID.randomUUID()
    );

    UserDetailsImpl agencyHead = new UserDetailsImpl(
      agencyHeadId,
      "agency-head",
      null,
      true,
      Set.of(new SimpleGrantedAuthority("INCIDENT_VIEW_AGENCY")),
      null,
      agencyId
    );

    assertThrows(AccessDeniedException.class, () ->
      guard.assertCanAct(incident, agencyHead, IncidentAction.CANCEL)
    );
  }

  @Test
  @DisplayName(
    "requestConfirmation - only the handler or the handling service head may ask"
  )
  void requestConfirmation_IsReservedToTheHandlingSide() {
    incident.setStatus(IncidentStatus.UNRESOLVED_PROLONGED_WAIT);
    incident.setAssignedTo(actorId);
    UserDetailsImpl assignee = user("INCIDENT_TREAT");
    UserDetailsImpl stranger = userInAgency(agencyId, "INCIDENT_TREAT");

    assertTrue(guard.canRequestConfirmation(incident, assignee));
    assertFalse(guard.canRequestConfirmation(incident, stranger));
    assertThrows(AccessDeniedException.class, () ->
      guard.assertCanAct(
        incident,
        stranger,
        IncidentAction.REQUEST_CONFIRMATION
      )
    );
  }

  @Test
  @DisplayName(
    "requestConfirmation - the agency head asks when the agency itself handles the incident"
  )
  void requestConfirmation_AgencyHandled_OpensToTheAgencyHead() {
    incident.setStatus(IncidentStatus.UNRESOLVED_PROLONGED_WAIT);
    incident.setAssignedTo(UUID.randomUUID());
    // Pris en charge par l'agence : transfere, mais aucun service traitant.
    incident.setTransferredToService(null);
    incident.setTransferredAt(LocalDateTime.now().minusDays(40));
    when(userClientService.resolveAgencyHead(agencyId)).thenReturn(actorId);

    assertTrue(guard.canRequestConfirmation(incident, user("INCIDENT_TREAT")));
  }

  @Test
  @DisplayName(
    "requestConfirmation - the SOURCE agency head stays out while a service handles the incident"
  )
  void requestConfirmation_ServiceHandled_KeepsTheSourceAgencyHeadOut() {
    // Le faux positif a eviter : agencyId est l'agence d'ORIGINE. Tant qu'un service
    // porte l'incident, son chef d'agence n'est pas du cote traitant.
    incident.setStatus(IncidentStatus.UNRESOLVED_PROLONGED_WAIT);
    incident.setAssignedTo(UUID.randomUUID());
    incident.setTransferredToService(UUID.randomUUID());
    incident.setTransferredAt(LocalDateTime.now().minusDays(40));
    lenient()
      .when(userClientService.resolveAgencyHead(agencyId))
      .thenReturn(actorId);

    assertFalse(guard.canRequestConfirmation(incident, user("INCIDENT_TREAT")));
  }

  @Test
  @DisplayName(
    "requestConfirmation - an incident never routed has no handling-side head"
  )
  void requestConfirmation_NeverRouted_HasNoHandlingSideHead() {
    incident.setStatus(IncidentStatus.UNRESOLVED_PROLONGED_WAIT);
    incident.setAssignedTo(UUID.randomUUID());
    incident.setTransferredToService(null);
    incident.setTransferredAt(null);
    lenient()
      .when(userClientService.resolveAgencyHead(agencyId))
      .thenReturn(actorId);

    assertFalse(guard.canRequestConfirmation(incident, user("INCIDENT_TREAT")));
  }

  @Test
  @DisplayName(
    "requestConfirmation - refuses a second request while one is still pending"
  )
  void requestConfirmation_TwiceInARow_IsRefused() {
    incident.setStatus(IncidentStatus.UNRESOLVED_PROLONGED_WAIT);
    incident.setAssignedTo(actorId);
    incident.setConfirmationRequestedAt(LocalDateTime.now().minusDays(1));

    assertFalse(guard.canRequestConfirmation(incident, user("INCIDENT_TREAT")));
  }

  @Test
  @DisplayName(
    "confirmRelevance - only the source entity head answers; the handler cannot answer for it"
  )
  void confirmRelevance_IsReservedToTheSourceEntityHead() {
    incident.setStatus(IncidentStatus.UNRESOLVED_PROLONGED_WAIT);
    incident.setAssignedTo(UUID.randomUUID());
    incident.setConfirmationRequestedAt(LocalDateTime.now().minusDays(1));
    // Le service createur a un chef : c'est lui qui sait si le probleme existe encore.
    when(userClientService.resolveServiceHead(serviceId)).thenReturn(actorId);

    assertTrue(guard.canConfirmRelevance(incident, user("INCIDENT_VALIDATE")));
    assertFalse(
      guard.canConfirmRelevance(
        incident,
        userInAgency(agencyId, "INCIDENT_VALIDATE")
      )
    );
  }

  @Test
  @DisplayName(
    "confirmRelevance - falls back to the agency head when the source service has none"
  )
  void confirmRelevance_WithoutServiceHead_FallsBackToTheAgency() {
    incident.setStatus(IncidentStatus.UNRESOLVED_PROLONGED_WAIT);
    incident.setConfirmationRequestedAt(LocalDateTime.now().minusDays(1));
    when(userClientService.resolveServiceHead(serviceId)).thenReturn(null);
    when(userClientService.resolveAgencyHead(agencyId)).thenReturn(actorId);

    assertTrue(guard.canConfirmRelevance(incident, user("INCIDENT_VALIDATE")));
  }

  @Test
  @DisplayName(
    "relevance confirmation - the administrator keeps his transversal reach on both steps"
  )
  void relevanceConfirmation_KeepsTheAdministratorTransversal() {
    UserDetailsImpl admin = user(
      "INCIDENT_VIEW_ALL",
      "INCIDENT_TREAT",
      "INCIDENT_VALIDATE",
      "INCIDENT_CANCEL"
    );
    incident.setStatus(IncidentStatus.UNRESOLVED_PROLONGED_WAIT);
    // Ni traitant, ni chef de l'entite source : seule sa portee globale le porte.
    incident.setAssignedTo(UUID.randomUUID());
    incident.setTransferredToService(UUID.randomUUID());

    assertTrue(guard.canRequestConfirmation(incident, admin));

    incident.setConfirmationRequestedAt(LocalDateTime.now().minusDays(1));
    assertTrue(guard.canConfirmRelevance(incident, admin));
    assertTrue(guard.canCancel(incident, admin));
    // La demande deja en cours reste une regle d'etat, pas de portee : elle vaut
    // aussi pour l'administrateur.
    assertFalse(guard.canRequestConfirmation(incident, admin));
  }

  @Test
  @DisplayName(
    "resolveRelevanceResponder - names the source service head, so the modal can say who is awaited"
  )
  void resolveRelevanceResponder_NamesTheSourceServiceHead() {
    when(userClientService.resolveServiceHead(serviceId)).thenReturn(
      UUID.randomUUID()
    );
    when(userClientService.resolveServiceName(serviceId)).thenReturn(
      "Comptabilité"
    );

    ExpectedValidator responder = guard.resolveRelevanceResponder(incident);

    assertEquals(IncidentValidatorRole.SERVICE_MANAGER, responder.getRole());
    assertEquals("Comptabilité", responder.getTargetName());
  }

  @Test
  @DisplayName(
    "resolveRelevanceResponder - falls back to the agency head, same tree as the scope check"
  )
  void resolveRelevanceResponder_FallsBackToTheAgencyHead() {
    when(userClientService.resolveServiceHead(serviceId)).thenReturn(null);
    when(userClientService.resolveAgencyName(agencyId)).thenReturn(
      "Agence Centrale"
    );

    ExpectedValidator responder = guard.resolveRelevanceResponder(incident);

    assertEquals(IncidentValidatorRole.AGENCY_MANAGER, responder.getRole());
    assertEquals("Agence Centrale", responder.getTargetName());
  }

  @Test
  @DisplayName(
    "cancel - the source entity head may say no while a confirmation is pending"
  )
  void cancel_IsOpenToTheSourceEntityHeadWhilePending() {
    incident.setStatus(IncidentStatus.UNRESOLVED_PROLONGED_WAIT);
    incident.setTransferredToService(UUID.randomUUID());
    when(userClientService.resolveServiceHead(serviceId)).thenReturn(actorId);
    UserDetailsImpl sourceHead = user("INCIDENT_CANCEL");

    // Sans demande en cours, l'incident appartient au service traitant.
    assertFalse(guard.canCancel(incident, sourceHead));

    incident.setConfirmationRequestedAt(LocalDateTime.now().minusDays(1));
    assertTrue(guard.canCancel(incident, sourceHead));
  }

  @Test
  @DisplayName(
    "cancel - an incident awaiting validation is rejected, never cancelled"
  )
  void cancel_BeforeValidation_IsNeverAllowed() {
    // La sortie d'un incident pas encore valide s'appelle rejet. Personne ne
    // l'annule, pas meme l'admin : le statut ferme la porte avant toute portee.
    UserDetailsImpl admin = user("INCIDENT_VIEW_ALL", "INCIDENT_CANCEL");
    UserDetailsImpl agencyHead = new UserDetailsImpl(
      UUID.randomUUID(),
      "agency-head",
      null,
      true,
      Set.of(new SimpleGrantedAuthority("INCIDENT_VIEW_AGENCY")),
      null,
      agencyId
    );

    for (IncidentStatus status : List.of(
      IncidentStatus.OPEN,
      IncidentStatus.PENDING_VALIDATION
    )) {
      incident.setStatus(status);
      assertThrows(
        InvalidStatusTransitionException.class,
        () -> guard.assertCanAct(incident, admin, IncidentAction.CANCEL),
        "annulation ouverte a tort sur " + status
      );
      assertFalse(guard.canCancel(incident, admin));
      assertFalse(guard.canCancel(incident, agencyHead));
    }
  }

  @Test
  @DisplayName(
    "resolveExpectedValidator - SOURCE scope names the creator's service manager"
  )
  void resolveExpectedValidator_SourceScopeWithHead_ReturnsServiceManager() {
    configuredType().setValidatorScope(
      IncidentValidatorScope.SOURCE_SERVICE_MANAGER
    );
    when(userClientService.resolveServiceHead(serviceId)).thenReturn(
      UUID.randomUUID()
    );
    when(userClientService.resolveServiceName(serviceId)).thenReturn(
      "Comptabilité"
    );

    ExpectedValidator expected = guard.resolveExpectedValidator(incident);

    assertEquals(IncidentValidatorRole.SERVICE_MANAGER, expected.getRole());
    assertEquals("Comptabilité", expected.getTargetName());
  }

  @Test
  @DisplayName(
    "resolveExpectedValidator - SOURCE scope falls back to the agency manager without a service head"
  )
  void resolveExpectedValidator_SourceScopeWithoutHead_FallsBackToAgency() {
    configuredType().setValidatorScope(
      IncidentValidatorScope.SOURCE_SERVICE_MANAGER
    );
    when(userClientService.resolveServiceHead(serviceId)).thenReturn(null);
    when(userClientService.resolveAgencyName(agencyId)).thenReturn("Douala");

    ExpectedValidator expected = guard.resolveExpectedValidator(incident);

    assertEquals(IncidentValidatorRole.AGENCY_MANAGER, expected.getRole());
    assertEquals("Douala", expected.getTargetName());
  }

  @Test
  @DisplayName(
    "resolveExpectedValidator - ADMIN scope names no structure"
  )
  void resolveExpectedValidator_AdminScope_ReturnsAdminWithoutTarget() {
    configuredType().setValidatorScope(IncidentValidatorScope.ADMIN);

    ExpectedValidator expected = guard.resolveExpectedValidator(incident);

    assertEquals(IncidentValidatorRole.ADMIN, expected.getRole());
    assertNull(expected.getTargetName());
  }

  @ParameterizedTest
  @EnumSource(IncidentValidatorScope.class)
  void creatorServiceManagerRequiresAdminByDefault(IncidentValidatorScope scope) {
    IncidentTypeConfig type = configuredType();
    type.setValidatorScope(scope);
    type.setDefaultTargetServiceId(serviceId);
    incident.setStatus(IncidentStatus.PENDING_VALIDATION);
    when(userClientService.resolveServiceHead(serviceId)).thenReturn(actorId);

    assertFalse(guard.canValidate(incident, user("INCIDENT_VALIDATE")));
    for (IncidentAction action : List.of(IncidentAction.VALIDATE, IncidentAction.REJECT)) {
      assertThrows(AccessDeniedException.class, () ->
        guard.assertCanAct(incident, user("INCIDENT_VALIDATE", "INCIDENT_REJECT"), action));
    }
    assertEquals(IncidentValidatorRole.ADMIN, guard.resolveExpectedValidator(incident).getRole());
    assertDoesNotThrow(() -> guard.assertCanAct(incident,
      user("INCIDENT_VIEW_ALL", "INCIDENT_VALIDATE"), IncidentAction.VALIDATE));
  }

  @ParameterizedTest
  @EnumSource(IncidentValidatorScope.class)
  void creatorServiceManagerCanValidateWhenExplicitlyEnabled(IncidentValidatorScope scope) {
    IncidentTypeConfig type = configuredType();
    type.setValidatorScope(scope);
    type.setDefaultTargetServiceId(serviceId);
    incident.setStatus(IncidentStatus.PENDING_VALIDATION);
    when(userClientService.resolveServiceHead(serviceId)).thenReturn(actorId);
    when(systemConfig.getThresholdLong("serviceManagerSelfValidationEnabled", 0)).thenReturn(1L);

    assertTrue(guard.canValidate(incident, user("INCIDENT_VALIDATE")));
    assertDoesNotThrow(() -> guard.assertCanAct(incident,
      user("INCIDENT_VALIDATE"), IncidentAction.VALIDATE));
    assertEquals(IncidentValidatorRole.SERVICE_MANAGER, guard.resolveExpectedValidator(incident).getRole());
  }

  @ParameterizedTest
  @EnumSource(IncidentValidatorScope.class)
  void unrelatedCreatorKeepsTheTypesValidationScope(IncidentValidatorScope scope) {
    UUID anotherCreator = UUID.randomUUID();
    incident.setCreatedBy(anotherCreator);
    incident.setStatus(IncidentStatus.PENDING_VALIDATION);
    IncidentTypeConfig type = configuredType();
    type.setValidatorScope(scope);
    type.setDefaultTargetServiceId(serviceId);
    when(userClientService.resolveServiceHead(serviceId)).thenReturn(actorId);
    UserDetailsImpl manager = user("INCIDENT_VALIDATE", "INCIDENT_VIEW_SERVICE");
    if (scope == IncidentValidatorScope.ADMIN || scope == IncidentValidatorScope.AGENCY_MANAGER) {
      assertFalse(guard.canValidate(incident, manager));
      assertThrows(AccessDeniedException.class, () -> guard.assertCanAct(incident, manager, IncidentAction.VALIDATE));
    } else {
      assertTrue(guard.canValidate(incident, manager));
      assertDoesNotThrow(() -> guard.assertCanAct(incident, manager, IncidentAction.VALIDATE));
    }
    IncidentValidatorRole expected = switch (scope) {
      case ADMIN -> IncidentValidatorRole.ADMIN;
      case AGENCY_MANAGER -> IncidentValidatorRole.AGENCY_MANAGER;
      default -> IncidentValidatorRole.SERVICE_MANAGER;
    };
    assertEquals(expected, guard.resolveExpectedValidator(incident).getRole());
    org.mockito.Mockito.verifyNoInteractions(systemConfig);
  }

  @Test
  void ownServiceCreatorCannotSkipValidationThroughAssignmentOrStart() {
    incident.setStatus(IncidentStatus.PENDING_VALIDATION);
    for (IncidentAction action : List.of(IncidentAction.ASSIGN, IncidentAction.START, IncidentAction.TRANSFER)) {
      assertThrows(InvalidStatusTransitionException.class, () ->
        guard.assertCanAct(incident, user("INCIDENT_ASSIGN", "INCIDENT_TREAT", "INCIDENT_TRANSFER"), action));
    }
  }

  @Test
  void creatorServiceHeadKeepsUsualValidationForAnotherServicesType() {
    IncidentTypeConfig type = configuredType();
    type.setValidatorScope(IncidentValidatorScope.SOURCE_SERVICE_MANAGER);
    UUID otherService = UUID.randomUUID();
    type.setDefaultTargetServiceId(otherService);
    incident.setStatus(IncidentStatus.PENDING_VALIDATION);
    when(userClientService.resolveServiceHead(otherService)).thenReturn(UUID.randomUUID());
    when(userClientService.resolveServiceHead(serviceId)).thenReturn(actorId);
    assertTrue(guard.canValidate(incident, user("INCIDENT_VALIDATE")));
    assertDoesNotThrow(() -> guard.assertCanAct(incident, user("INCIDENT_VALIDATE"), IncidentAction.VALIDATE));
    assertEquals(IncidentValidatorRole.SERVICE_MANAGER, guard.resolveExpectedValidator(incident).getRole());
  }

  private IncidentTypeConfig configuredType() {
    UUID typeId = UUID.randomUUID();
    IncidentTypeConfig type = new IncidentTypeConfig();
    incident.setTypeId(typeId);
    when(incidentTypeConfigRepository.findById(typeId)).thenReturn(
      Optional.of(type)
    );
    return type;
  }

  private void configureCloserRoles(IncidentActorRole... roles) {
    configuredType().setCloserRoles(Set.of(roles));
  }

  private void configureReopenerRoles(IncidentActorRole... roles) {
    configuredType().setReopenerRoles(Set.of(roles));
  }
}
