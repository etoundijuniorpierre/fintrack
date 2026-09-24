// Securite : applique l'authentification et les autorisations liees a incident workflow guard.

package com.fintrack.incident.security;

import com.fintrack.incident.client.user.UserClientService;
import com.fintrack.incident.exception.BusinessRuleViolationException;
import com.fintrack.incident.exception.ErrorCode;
import com.fintrack.incident.exception.InvalidStatusTransitionException;
import com.fintrack.incident.model.constant.IncidentAction;
import com.fintrack.incident.model.constant.IncidentActorRole;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.constant.IncidentValidatorRole;
import com.fintrack.incident.model.constant.IncidentValidatorScope;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentTypeConfig;
import com.fintrack.incident.model.readmodel.ExpectedValidator;
import com.fintrack.incident.model.readmodel.ExternalService;
import com.fintrack.incident.model.readmodel.ExternalUser;
import com.fintrack.incident.repository.IncidentTypeConfigRepository;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Point central de controle du workflow incident.
 * Les annotations de permissions restent le premier filtre; ce garde verifie
 * ensuite le perimetre, le statut et les identifiants cibles de l'action.
 */
@Component
@RequiredArgsConstructor
// Modelise la responsabilite applicative liee a incident.
public class IncidentWorkflowGuard {

  private static final Set<IncidentStatus> EDITABLE_STATUSES = EnumSet.of(
    IncidentStatus.OPEN,
    IncidentStatus.PENDING_VALIDATION,
    IncidentStatus.REOPENED
  );
  private static final Set<IncidentStatus> DELETABLE_STATUSES = EnumSet.of(
    IncidentStatus.OPEN,
    IncidentStatus.PENDING_VALIDATION,
    IncidentStatus.REOPENED
  );
  private static final Set<IncidentStatus> VALIDATION_STATUSES = EnumSet.of(
    IncidentStatus.OPEN,
    IncidentStatus.PENDING_VALIDATION
  );
  private static final Set<IncidentStatus> ROUTING_STATUSES = EnumSet.of(
    IncidentStatus.OPEN,
    IncidentStatus.VALIDATED,
    IncidentStatus.TRANSFERRED,
    IncidentStatus.ASSIGNED,
    IncidentStatus.IN_PROGRESS
  );
  private static final Set<IncidentStatus> TRANSFER_STATUSES = EnumSet.of(
    IncidentStatus.VALIDATED,
    IncidentStatus.TRANSFERRED,
    IncidentStatus.ASSIGNED,
    IncidentStatus.IN_PROGRESS,
    IncidentStatus.BLOCKED
  );
  private static final Set<IncidentStatus> START_STATUSES = EnumSet.of(
    IncidentStatus.ASSIGNED
  );
  private static final Set<IncidentStatus> BLOCK_STATUSES = EnumSet.of(
    IncidentStatus.IN_PROGRESS
  );
  private static final Set<IncidentStatus> RESUME_STATUSES = EnumSet.of(
    IncidentStatus.BLOCKED
  );
  private static final Set<IncidentStatus> TREAT_STATUSES = EnumSet.of(
    IncidentStatus.IN_PROGRESS
  );
  // L'attente prolongee ne se quitte plus d'un seul geste : le traitant demande a
  // l'entite source si l'incident est encore d'actualite, elle confirme ou l'infirme.
  private static final Set<IncidentStatus> CONFIRMATION_STATUSES = EnumSet.of(
    IncidentStatus.UNRESOLVED_PROLONGED_WAIT
  );
  // Le valideur agit sur un incident déjà traité (Traité → Résolu, ou renvoi en traitement).
  private static final Set<IncidentStatus> RESOLVE_STATUSES = EnumSet.of(
    IncidentStatus.TREATED
  );
  private static final Set<IncidentStatus> CLOSE_STATUSES = EnumSet.of(
    IncidentStatus.RESOLVED
  );
  private static final Set<IncidentStatus> REOPEN_STATUSES = EnumSet.of(
    IncidentStatus.RESOLVED,
    IncidentStatus.REJECTED,
    IncidentStatus.UNRESOLVED_PROLONGED_WAIT
  );
  private static final Set<IncidentStatus> RESUBMIT_STATUSES = EnumSet.of(
    IncidentStatus.REOPENED
  );
  // Disjoint de VALIDATION_STATUSES : un incident pas encore valide se rejette,
  // il ne s'annule pas. Voir IncidentServiceImpl.CANCELLABLE_STATUSES.
  private static final Set<IncidentStatus> CANCEL_STATUSES = EnumSet.of(
    IncidentStatus.VALIDATED,
    IncidentStatus.TRANSFERRED,
    IncidentStatus.ASSIGNED,
    IncidentStatus.BLOCKED,
    IncidentStatus.UNRESOLVED_PROLONGED_WAIT
  );
  private final IncidentAccessGuard incidentAccessGuard;
  private final UserClientService userClientService;
  private final IncidentTypeConfigRepository incidentTypeConfigRepository;
  private final IncidentValidationPolicy validationPolicy;

  // Verifie que les regles metier autorisent l operation sur incident.

  public void assertCanAct(
    Incident incident,
    UserDetailsImpl user,
    IncidentAction action
  ) {
    incidentAccessGuard.assertCanView(incident, user);
    requireStatusIn(incident, allowedStatuses(action));

    switch (action) {
      case VALIDATE, REJECT -> assertCanValidate(incident, user, action);
      case RESOLVE, MARK_UNRESOLVED -> assertCanResolve(incident, user, action);
      case TREAT -> assertCanTreat(incident, user, action);
      case START -> assertCanStart(incident, user);
      case BLOCK, RESUME, SUBMIT_SOLUTION -> assertCurrentAssignee(
        incident,
        user,
        action
      );
      case CANCEL -> assertCanCancel(incident, user);
      case REQUEST_CONFIRMATION -> assertCanRequestConfirmation(incident, user);
      case CONFIRM_RELEVANCE -> assertCanConfirmRelevance(incident, user);
      case DIRECTION_VALIDATE, DIRECTION_REJECT -> assertCanValidateDirection(
        incident,
        user
      );
      case CLOSE -> assertCanClose(incident, user, action);
      case RESUBMIT -> assertCreator(incident, user, action);
      case UPDATE -> assertUpdateAllowed(incident, user);
      default -> {
        // Aucune portee specifique supplementaire au-dela de la vue et de la permission.
      }
    }
  }

  // Verifie que les regles metier autorisent l operation sur incident.

  private void assertUpdateAllowed(Incident incident, UserDetailsImpl user) {
    if (!EDITABLE_STATUSES.contains(incident.getStatus())) {
      throw new InvalidStatusTransitionException(
        "incident.error.update_not_allowed"
      );
    }

    boolean isCreator = incident.getCreatedBy().equals(user.getId());
    if (!isCreator) {
      throw new AccessDeniedException(
        "Seul le createur peut modifier cet incident"
      );
    }
  }

  // Verifie que les regles metier autorisent l'operation sur incident.
  public void assertCanTransfer(
    Incident incident,
    UserDetailsImpl user,
    UUID targetServiceId,
    UUID targetAgencyId,
    String reason
  ) {
    assertCanAct(incident, user, IncidentAction.TRANSFER);
    assertSingleTransferTarget(targetServiceId, targetAgencyId);
    if (targetServiceId != null) {
      requireExistingService(targetServiceId);
    } else {
      assertCreatorAgencyTarget(incident, targetAgencyId);
    }

    if (
      (incident.getStatus() == IncidentStatus.IN_PROGRESS ||
        incident.getStatus() == IncidentStatus.BLOCKED) &&
      !StringUtils.hasText(reason)
    ) {
      throw new BusinessRuleViolationException(
        ErrorCode.INCIDENT_TRANSFER_REASON_REQUIRED,
        ErrorCode.INCIDENT_TRANSFER_REASON_REQUIRED.getMessageKey()
      );
    }
  }

  // Exige exactement une destination de transfert.
  private void assertSingleTransferTarget(
    UUID targetServiceId,
    UUID targetAgencyId
  ) {
    if ((targetServiceId == null) == (targetAgencyId == null)) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        "validation.incident.transfer_target_required"
      );
    }
  }

  // Autorise uniquement le retour vers l'agence d'un createur sans service.
  private void assertCreatorAgencyTarget(
    Incident incident,
    UUID targetAgencyId
  ) {
    if (
      incident.getAgencyId() == null ||
      incident.getCreatorServiceId() != null ||
      !incident.getAgencyId().equals(targetAgencyId)
    ) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        "incident.error.transfer_agency_not_allowed"
      );
    }
    try {
      if (userClientService.getAgency(targetAgencyId) == null) {
        throw new BusinessRuleViolationException(
          ErrorCode.INVALID_INPUT,
          "incident.error.target_agency_not_found"
        );
      }
    } catch (BusinessRuleViolationException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        "incident.error.target_agency_not_found"
      );
    }
  }

  // Verifie que les regles metier autorisent l operation sur incident.

  public void assertCanAssign(
    Incident incident,
    UserDetailsImpl user,
    UUID assignedTo
  ) {
    assertCanAct(incident, user, IncidentAction.ASSIGN);
    requireId(assignedTo, "incident.error.assignee_required");

    // L'auto-attribution (se prendre soi-meme un incident) est ouverte aux profils
    // capables de traiter, comme a la creation. Assigner un AUTRE utilisateur reste
    // reserve a INCIDENT_ASSIGN. La portee (actif / peut traiter / meme perimetre)
    // est verifiee ci-dessous dans les deux cas.
    boolean selfAssign = assignedTo.equals(user.getId());
    if (
      !selfAssign &&
      !user
        .getAuthorities()
        .stream()
        .anyMatch(a -> a.getAuthority().equals("INCIDENT_ASSIGN"))
    ) {
      throw new AccessDeniedException(
        "Vous n'avez pas la permission d'assigner un incident (permission INCIDENT_ASSIGN manquante)"
      );
    }

    resolveValidAssigneeForIncident(incident, assignedTo);
  }

  // Resout valid assignee for incident a partir du contexte disponible.

  public ExternalUser resolveValidAssigneeForIncident(
    Incident incident,
    UUID assignedTo
  ) {
    requireId(assignedTo, "incident.error.assignee_required");

    ExternalUser assignee = requireExistingUser(assignedTo);
    if (!assignee.isActive()) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        "incident.error.assignee_inactive_or_not_found"
      );
    }

    assertAssigneeCanTreat(assignee, assignedTo);
    assertAssigneeMatchesIncidentScope(incident, assignee, assignedTo);
    return assignee;
  }

  // Verifie que les regles metier autorisent l operation sur incident.

  public void assertCanReopen(
    Incident incident,
    UserDetailsImpl user,
    String reason
  ) {
    assertCanAct(incident, user, IncidentAction.REOPEN);
    // Reouverture apres traitement (RESOLU) : reservee aux roles configures pour la
    // reouverture (defaut : chef d'agence source), avec bypass admin. Le cas REJETE
    // (createur ou rejeteur) reste porte par IncidentServiceImpl.reopen.
    if (incident.getStatus() == IncidentStatus.RESOLVED) {
      assertActorInRoles(
        incident,
        user,
        configuredRolesOrDefault(
          incident,
          IncidentTypeConfig::getReopenerRoles,
          DEFAULT_REOPENER_ROLES
        ),
        IncidentAction.REOPEN
      );
    }
    if (!StringUtils.hasText(reason)) {
      throw new BusinessRuleViolationException(
        ErrorCode.INCIDENT_REOPEN_REASON_REQUIRED,
        ErrorCode.INCIDENT_REOPEN_REASON_REQUIRED.getMessageKey()
      );
    }
  }

  // ===== Portee acteur par role configurable =====

  private static final Set<IncidentActorRole> DEFAULT_TREATER_ROLES = EnumSet.of(
    IncidentActorRole.ASSIGNEE
  );
  private static final Set<IncidentActorRole> DEFAULT_RESOLVER_ROLES =
    EnumSet.of(IncidentActorRole.SOURCE_AGENCY_MANAGER);
  private static final Set<IncidentActorRole> DEFAULT_CLOSER_ROLES = EnumSet.of(
    IncidentActorRole.ASSIGNEE
  );
  private static final Set<IncidentActorRole> DEFAULT_REOPENER_ROLES =
    EnumSet.of(IncidentActorRole.SOURCE_AGENCY_MANAGER);

  // Roles configures pour une etape, ou le defaut de l'etape si non configure/vide.
  private Set<IncidentActorRole> configuredRolesOrDefault(
    Incident incident,
    java.util.function.Function<IncidentTypeConfig, Set<IncidentActorRole>> extractor,
    Set<IncidentActorRole> defaults
  ) {
    if (incident.getTypeId() == null) {
      return defaults;
    }
    return incidentTypeConfigRepository
      .findById(incident.getTypeId())
      .map(extractor)
      .filter(roles -> roles != null && !roles.isEmpty())
      .orElse(defaults);
  }

  // Vrai si l'utilisateur endosse ce role sur l'incident.
  private boolean userHasActorRole(
    Incident incident,
    UserDetailsImpl user,
    IncidentActorRole role
  ) {
    return switch (role) {
      case CREATOR -> incident.getCreatedBy() != null &&
      incident.getCreatedBy().equals(user.getId());
      case ASSIGNEE -> incident.getAssignedTo() != null &&
      incident.getAssignedTo().equals(user.getId());
      case CHEF_SERVICE -> {
        UUID handlingService = closureService(incident);
        yield handlingService != null &&
        userManagesService(user, handlingService);
      }
      case SOURCE_AGENCY_MANAGER -> incidentAccessGuard.hasAuthority(
        user,
        "INCIDENT_VIEW_AGENCY"
      ) &&
      user.getAgencyId() != null &&
      user.getAgencyId().equals(incident.getAgencyId());
    };
  }

  // Exige que l'utilisateur endosse AU MOINS un des roles autorises (bypass admin
  // INCIDENT_VIEW_ALL, comme pour les autres etapes).
  private void assertActorInRoles(
    Incident incident,
    UserDetailsImpl user,
    Set<IncidentActorRole> roles,
    IncidentAction action
  ) {
    if (incidentAccessGuard.hasAuthority(user, "INCIDENT_VIEW_ALL")) {
      return;
    }
    boolean allowed = roles
      .stream()
      .anyMatch(role -> userHasActorRole(incident, user, role));
    if (!allowed) {
      throw new AccessDeniedException(
        "Votre profil n'est pas autorise a " +
        action.name().toLowerCase() +
        " cet incident (regle du type d'incident)"
      );
    }
  }

  // Realise l'intention metier allowed statuses.

  private Set<IncidentStatus> allowedStatuses(IncidentAction action) {
    return switch (action) {
      case UPDATE -> EDITABLE_STATUSES;
      case DELETE -> DELETABLE_STATUSES;
      case VALIDATE, REJECT -> VALIDATION_STATUSES;
      case TRANSFER -> TRANSFER_STATUSES;
      case ASSIGN -> ROUTING_STATUSES;
      case START -> START_STATUSES;
      case BLOCK -> BLOCK_STATUSES;
      case RESUME -> RESUME_STATUSES;
      case REQUEST_CONFIRMATION, CONFIRM_RELEVANCE -> CONFIRMATION_STATUSES;
      case TREAT -> TREAT_STATUSES;
      case RESOLVE, MARK_UNRESOLVED -> RESOLVE_STATUSES;
      case CLOSE -> CLOSE_STATUSES;
      case REOPEN -> REOPEN_STATUSES;
      case RESUBMIT -> RESUBMIT_STATUSES;
      case CLONE -> EnumSet.allOf(IncidentStatus.class);
      case CANCEL -> CANCEL_STATUSES;
      case SUBMIT_SOLUTION -> EnumSet.of(
        IncidentStatus.ASSIGNED,
        IncidentStatus.TRANSFERRED,
        IncidentStatus.VALIDATED,
        IncidentStatus.DRAFT
      );
      case DIRECTION_VALIDATE, DIRECTION_REJECT -> EnumSet.of(
        IncidentStatus.DRAFT
      );
    };
  }

  // Valider/rejeter au nom de la Direction est reserve aux SEULS titulaires de la
  // permission VALIDATION_DIRECTION (permission directe exclusive) : ni INCIDENT_VIEW_ALL
  // ni le simple statut de valideur designe ne suffisent. Pas la permission => pas d'action.
  public void assertCanValidateDirection(
    Incident incident,
    UserDetailsImpl user
  ) {
    if (incidentAccessGuard.hasAuthority(user, "VALIDATION_DIRECTION")) {
      return;
    }
    throw new AccessDeniedException(
      "Seul un titulaire de la permission VALIDATION_DIRECTION peut valider ou rejeter au nom de la Direction."
    );
  }

  // Exige status in.
  private void requireStatusIn(
    Incident incident,
    Set<IncidentStatus> allowed
  ) {
    if (!allowed.contains(incident.getStatus())) {
      throw new InvalidStatusTransitionException(
        "incident.error.action_not_allowed_for_status"
      );
    }
  }

  // Verdict de portee de validation pour l'UI
  public boolean canValidate(Incident incident, UserDetailsImpl user) {
    if (incident == null || user == null) {
      return false;
    }
    try {
      assertCanValidate(incident, user, IncidentAction.VALIDATE);
      return true;
    } catch (AccessDeniedException ex) {
      return false;
    }
  }

  // Resout le valideur REELLEMENT attendu sur cet incident : meme arbre de decision et
  // memes replis que assertCanValidate, mais restitue au lieu d'etre applique. Sert a
  // annoncer a l'utilisateur qui est attendu plutot qu'une portee abstraite.
  public ExpectedValidator resolveExpectedValidator(Incident incident) {
    IncidentTypeConfig type = incident.getTypeId() == null
      ? null
      : incidentTypeConfigRepository.findById(incident.getTypeId()).orElse(null);
    IncidentValidatorRole ownServiceValidator = validationPolicy.resolveOwnServiceValidator(incident, type);
    if (ownServiceValidator != null) {
      return ExpectedValidator.builder()
        .role(ownServiceValidator)
        .targetName(ownServiceValidator == IncidentValidatorRole.SERVICE_MANAGER
          ? userClientService.resolveServiceName(type.getDefaultTargetServiceId()) : null)
        .build();
    }
    IncidentValidatorScope scope =
      type != null && type.getValidatorScope() != null
        ? type.getValidatorScope()
        : IncidentValidatorScope.SOURCE_SERVICE_MANAGER;

    UUID serviceId = switch (scope) {
      case ADMIN -> null;
      case AGENCY_MANAGER -> null;
      // TARGET : le service cible vient du type ; sans service cible, repli agence.
      case TARGET_SERVICE_MANAGER -> type == null
        ? null
        : type.getDefaultTargetServiceId();
      // SOURCE : le service du createur, a condition qu'il ait un chef ; sinon repli agence.
      case SOURCE_SERVICE_MANAGER -> {
        UUID creatorServiceId = incident.getCreatorServiceId();
        yield creatorServiceId != null &&
          userClientService.resolveServiceHead(creatorServiceId) != null
          ? creatorServiceId
          : null;
      }
    };

    if (scope == IncidentValidatorScope.ADMIN) {
      return ExpectedValidator.builder()
        .role(IncidentValidatorRole.ADMIN)
        .build();
    }
    if (serviceId != null) {
      return ExpectedValidator.builder()
        .role(IncidentValidatorRole.SERVICE_MANAGER)
        .targetName(userClientService.resolveServiceName(serviceId))
        .build();
    }
    return ExpectedValidator.builder()
      .role(IncidentValidatorRole.AGENCY_MANAGER)
      .targetName(userClientService.resolveAgencyName(incident.getAgencyId()))
      .build();
  }

  // Applique la portee de validation configuree sur le type d'incident.
  private void assertCanValidate(
    Incident incident,
    UserDetailsImpl user,
    IncidentAction action
  ) {
    // Bypass admin : couvre tous les scopes, et rend le scope ADMIN = "admin seul".
    if (incidentAccessGuard.hasAuthority(user, "INCIDENT_VIEW_ALL")) {
      return;
    }
    IncidentTypeConfig type = incident.getTypeId() == null
      ? null
      : incidentTypeConfigRepository.findById(incident.getTypeId()).orElse(null);
    IncidentValidatorScope scope =
      type != null && type.getValidatorScope() != null
        ? type.getValidatorScope()
        : IncidentValidatorScope.SOURCE_SERVICE_MANAGER;

    IncidentValidatorRole ownServiceValidator = validationPolicy.resolveOwnServiceValidator(incident, type);
    if (ownServiceValidator == IncidentValidatorRole.ADMIN) {
      throw new AccessDeniedException(
        "Un administrateur doit valider cet incident : son createur dirige le service de traitement configure sur le type."
      );
    }
    if (ownServiceValidator == IncidentValidatorRole.SERVICE_MANAGER) {
      assertCreator(incident, user, action);
      return;
    }

    switch (scope) {
      case ADMIN -> throw new AccessDeniedException(
        "Seul un administrateur peut " +
          action.name().toLowerCase() +
          " cet incident"
      );
      case TARGET_SERVICE_MANAGER -> assertCanValidateTargetService(
        incident,
        user,
        type,
        action
      );
      case SOURCE_SERVICE_MANAGER -> assertCanValidateSourceService(
        incident,
        user,
        action
      );
      case AGENCY_MANAGER -> assertSameAgencyOrGlobal(incident, user, action);
    }
  }

  // TARGET : chef du service cible. Sans service cible configure, repli sur le perimetre
  // agence (decision produit : ne pas bloquer les types mal configures).
  private void assertCanValidateTargetService(
    Incident incident,
    UserDetailsImpl user,
    IncidentTypeConfig type,
    IncidentAction action
  ) {
    if (type != null && type.getDefaultTargetServiceId() != null) {
      if (userManagesService(user, type.getDefaultTargetServiceId())) {
        return;
      }
      throw new AccessDeniedException(
        "Seul le responsable du service cible peut " +
          action.name().toLowerCase() +
          " cet incident"
      );
    }
    assertSameAgencyOrGlobal(incident, user, action);
  }

  // SOURCE (defaut) : chef du service du createur. Sans service, ou service sans chef,
  // repli sur le chef d'agence. L'auto-validation depend du reglage Super Admin.
  private void assertCanValidateSourceService(
    Incident incident,
    UserDetailsImpl user,
    IncidentAction action
  ) {
    UUID creatorServiceId = incident.getCreatorServiceId();
    if (creatorServiceId == null) {
      assertSameAgencyOrGlobal(incident, user, action);
      return;
    }
    UUID headId = userClientService.resolveServiceHead(creatorServiceId);
    if (headId == null) {
      assertSameAgencyOrGlobal(incident, user, action);
      return;
    }
    if (user.getId() != null && user.getId().equals(headId)) {
      return;
    }
    throw new AccessDeniedException(
      "Seul le chef du service du createur peut " +
        action.name().toLowerCase() +
        " cet incident"
    );
  }

  // Verifie si l'utilisateur est responsable du service cible (service primaire ou services geres).

  private boolean userManagesService(UserDetailsImpl user, UUID serviceId) {
    if (serviceId.equals(user.getServiceId())) {
      return true;
    }
    try {
      ExternalUser ext = userClientService.getUser(user.getId());
      return (
        ext != null &&
        ext.getManagedServiceIds() != null &&
        ext.getManagedServiceIds().contains(serviceId)
      );
    } catch (RuntimeException ex) {
      return false;
    }
  }

  private boolean userIsServiceHead(UserDetailsImpl user, UUID serviceId) {
    if (serviceId == null || user.getId() == null) {
      return false;
    }
    try {
      UUID headId = userClientService.resolveServiceHead(serviceId);
      if (headId != null) {
        return user.getId().equals(headId);
      }
      ExternalUser ext = userClientService.getUser(user.getId());
      return (
        ext != null &&
        ext.getManagedServiceIds() != null &&
        ext.getManagedServiceIds().contains(serviceId)
      );
    } catch (RuntimeException ex) {
      return false;
    }
  }

  private boolean userIsAgencyHead(UserDetailsImpl user, UUID agencyId) {
    if (agencyId == null || user.getId() == null) {
      return false;
    }
    return user.getId().equals(userClientService.resolveAgencyHead(agencyId));
  }

  // Verifie que les regles metier autorisent l'operation sur incident.
  private void assertSameAgencyOrGlobal(
    Incident incident,
    UserDetailsImpl user,
    IncidentAction action
  ) {
    if (incidentAccessGuard.hasAuthority(user, "INCIDENT_VIEW_ALL")) {
      return;
    }

    // Exiger explicitement les droits de niveau Agence (Chef d'Agence)
    if (!incidentAccessGuard.hasAuthority(user, "INCIDENT_VIEW_AGENCY")) {
      throw new AccessDeniedException(
        "Seul le Chef d'Agence peut " +
          action.name().toLowerCase() +
          " cet incident."
      );
    }

    if (
      user.getAgencyId() != null &&
      user.getAgencyId().equals(incident.getAgencyId())
    ) {
      return;
    }
    throw new AccessDeniedException(
      "Action interdite sur un incident hors de votre agence : " + action.name()
    );
  }

  // Verifie que les regles metier autorisent l operation sur incident.

  private void assertCurrentAssignee(
    Incident incident,
    UserDetailsImpl user,
    IncidentAction action
  ) {
    if (incidentAccessGuard.hasAuthority(user, "INCIDENT_VIEW_ALL")) {
      return;
    }
    UUID userId = user.getId();
    if (userId != null && userId.equals(incident.getAssignedTo())) {
      return;
    }
    throw new AccessDeniedException(
      "Seul l'assigne courant ou l'administrateur peut " +
        action.name().toLowerCase() +
        " cet incident"
    );
  }

  // Demander l'actualite appartient au cote traitant : celui qui reprend l'incident, ou
  // son chef quand il n'est plus la (chef du service traitant, ou chef de l'agence
  // traitante lorsque l'incident est pris en charge par une agence).
  private void assertCanRequestConfirmation(
    Incident incident,
    UserDetailsImpl user
  ) {
    if (incident.getConfirmationRequestedAt() != null) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_STATUS_TRANSITION,
        "incident.error.confirmation_already_requested"
      );
    }
    if (incidentAccessGuard.hasAuthority(user, "INCIDENT_VIEW_ALL")) {
      return;
    }
    UUID userId = user.getId();
    if (userId != null && userId.equals(incident.getAssignedTo())) {
      return;
    }
    if (isHandlingSideHead(incident, user)) {
      return;
    }
    throw new AccessDeniedException(
      "Seul le traitant ou son chef peut demander la confirmation d'actualite"
    );
  }

  // Chef du cote traitant. Un incident est pris en charge soit par un service, soit par
  // une agence : dans le second cas l'agence traitante est agencyId, mais SEULEMENT
  // quand aucun service ne le porte (meme predicat que IncidentMapper pour
  // transferredToAgency). Sans cette garde, le chef de l'agence SOURCE passerait sur un
  // incident parti dans un service — un faux positif.
  private boolean isHandlingSideHead(Incident incident, UserDetailsImpl user) {
    UUID handlingService = incident.getTransferredToService();
    if (handlingService != null) {
      return userIsServiceHead(user, handlingService);
    }
    if (incident.getTransferredAt() == null) {
      // Jamais route : personne ne porte encore cet incident cote traitement.
      return false;
    }
    return userIsAgencyHead(user, incident.getAgencyId());
  }

  // Repondre appartient a l'entite d'ou vient l'incident : elle seule sait si le probleme
  // existe encore. Chef du service createur, a defaut chef de l'agence source.
  private void assertCanConfirmRelevance(
    Incident incident,
    UserDetailsImpl user
  ) {
    if (incident.getConfirmationRequestedAt() == null) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_STATUS_TRANSITION,
        "incident.error.confirmation_not_requested"
      );
    }
    if (!isSourceEntityHead(incident, user)) {
      throw new AccessDeniedException(
        "Seul le responsable de l'entite source peut confirmer l'actualite de cet incident"
      );
    }
  }

  // Responsable de l'entite d'origine : chef du service createur s'il en existe un,
  // sinon chef de l'agence source. Un administrateur passe toujours.
  private boolean isSourceEntityHead(Incident incident, UserDetailsImpl user) {
    if (incidentAccessGuard.hasAuthority(user, "INCIDENT_VIEW_ALL")) {
      return true;
    }
    UUID sourceServiceId = sourceServiceWithHead(incident);
    if (sourceServiceId != null) {
      return userIsServiceHead(user, sourceServiceId);
    }
    return userIsAgencyHead(user, incident.getAgencyId());
  }

  // Service createur, a condition qu'il ait un chef ; sinon null (repli agence).
  private UUID sourceServiceWithHead(Incident incident) {
    UUID creatorServiceId = incident.getCreatorServiceId();
    return creatorServiceId != null &&
      userClientService.resolveServiceHead(creatorServiceId) != null
      ? creatorServiceId
      : null;
  }

  // Personnes attendues pour l'action en cours, resolues par les memes roles que le
  // garde applique pour l'autoriser.
  public Set<UUID> resolveExpectedActors(
    Incident incident,
    IncidentAction action
  ) {
    Set<IncidentActorRole> roles = switch (action) {
      case RESOLVE -> configuredRolesOrDefault(
        incident,
        IncidentTypeConfig::getResolverRoles,
        DEFAULT_RESOLVER_ROLES
      );
      case CLOSE -> configuredRolesOrDefault(
        incident,
        IncidentTypeConfig::getCloserRoles,
        DEFAULT_CLOSER_ROLES
      );
      default -> Set.of();
    };

    Set<UUID> actors = new LinkedHashSet<>();
    for (IncidentActorRole role : roles) {
      UUID actor = switch (role) {
        case CREATOR -> incident.getCreatedBy();
        case ASSIGNEE -> incident.getAssignedTo();
        case CHEF_SERVICE -> {
          UUID service = closureService(incident);
          yield service == null
            ? null
            : userClientService.resolveServiceHead(service);
        }
        case SOURCE_AGENCY_MANAGER -> incident.getAgencyId() == null
          ? null
          : userClientService.resolveAgencyHead(incident.getAgencyId());
      };
      if (actor != null) {
        actors.add(actor);
      }
    }
    return actors;
  }

  // Nomme celui de qui la confirmation d'actualite est attendue, plutot que de parler
  // d'« entite source » : meme arbre de decision que isSourceEntityHead, restitue au
  // lieu d'etre applique (miroir de resolveExpectedValidator).
  public ExpectedValidator resolveRelevanceResponder(Incident incident) {
    UUID sourceServiceId = sourceServiceWithHead(incident);
    if (sourceServiceId != null) {
      return ExpectedValidator.builder()
        .role(IncidentValidatorRole.SERVICE_MANAGER)
        .targetName(userClientService.resolveServiceName(sourceServiceId))
        .build();
    }
    return ExpectedValidator.builder()
      .role(IncidentValidatorRole.AGENCY_MANAGER)
      .targetName(userClientService.resolveAgencyName(incident.getAgencyId()))
      .build();
  }

  // Verdict de portee pour l'UI : le bouton ne doit s'afficher que pour ceux que le
  // garde laisserait effectivement passer.
  public boolean canRequestConfirmation(
    Incident incident,
    UserDetailsImpl user
  ) {
    return canAct(incident, user, IncidentAction.REQUEST_CONFIRMATION);
  }

  public boolean canConfirmRelevance(Incident incident, UserDetailsImpl user) {
    return canAct(incident, user, IncidentAction.CONFIRM_RELEVANCE);
  }

  private boolean canAct(
    Incident incident,
    UserDetailsImpl user,
    IncidentAction action
  ) {
    if (
      incident == null ||
      user == null ||
      !allowedStatuses(action).contains(incident.getStatus())
    ) {
      return false;
    }
    try {
      switch (action) {
        case REQUEST_CONFIRMATION -> assertCanRequestConfirmation(
          incident,
          user
        );
        case CONFIRM_RELEVANCE -> assertCanConfirmRelevance(incident, user);
        default -> {
          // Aucun autre verdict n'est expose par ce raccourci.
        }
      }
      return true;
    } catch (AccessDeniedException | BusinessRuleViolationException ex) {
      return false;
    }
  }

  // Verdict de portee d'annulation pour l'UI : le bouton ne doit s'afficher que pour
  // ceux que le garde laisserait effectivement passer.
  public boolean canCancel(Incident incident, UserDetailsImpl user) {
    if (incident == null || user == null) {
      return false;
    }
    if (!CANCEL_STATUSES.contains(incident.getStatus())) {
      return false;
    }
    try {
      assertCanCancel(incident, user);
      return true;
    } catch (AccessDeniedException ex) {
      return false;
    }
  }

  private void assertCanCancel(Incident incident, UserDetailsImpl user) {
    if (incidentAccessGuard.hasAuthority(user, "INCIDENT_VIEW_ALL")) {
      return;
    }
    // Infirmer une demande d'actualite, c'est annuler : la porte s'ouvre a l'entite
    // source le temps qu'elle reponde, sans quoi seul le service traitant deciderait
    // du sort d'un incident dont il ne sait pas s'il est encore d'actualite.
    if (
      incident.getConfirmationRequestedAt() != null &&
      isSourceEntityHead(incident, user)
    ) {
      return;
    }
    UUID handlingService = incident.getTransferredToService();
    if (handlingService != null && isServiceOwnedForCancellation(incident)) {
      if (userIsServiceHead(user, handlingService)) {
        return;
      }
      throw new AccessDeniedException(
        "Seul le chef du service traitant ou un administrateur peut annuler cet incident."
      );
    }
    if (userIsAgencyHead(user, incident.getAgencyId())) {
      return;
    }
    throw new AccessDeniedException(
      "Seul le chef de l'agence source ou un administrateur peut annuler cet incident avant son routage vers un service."
    );
  }

  private boolean isServiceOwnedForCancellation(Incident incident) {
    return switch (incident.getStatus()) {
      case TRANSFERRED,
        ASSIGNED,
        IN_PROGRESS,
        BLOCKED,
        TREATED,
        RESOLVED,
        UNRESOLVED_PROLONGED_WAIT -> true;
      default -> false;
    };
  }

  // La proposition de solution
  public boolean canViewProposedSolution(
    Incident incident,
    UserDetailsImpl user
  ) {
    if (incident == null || user == null) {
      return false;
    }
    if (incidentAccessGuard.hasAuthority(user, "INCIDENT_VIEW_ALL")) {
      return true;
    }
    UUID userId = user.getId();
    if (userId != null && userId.equals(incident.getAssignedTo())) {
      return true;
    }
    if (incidentAccessGuard.hasAuthority(user, "VALIDATION_DIRECTION")) {
      return true;
    }
    if (isConfiguredDirectionValidator(incident, userId)) {
      return true;
    }
    ExternalUser submitter = resolveSubmitter(incident);
    if (submitter == null) {
      return false;
    }
    if (
      submitter.getServiceId() != null &&
      userIsServiceHead(user, submitter.getServiceId())
    ) {
      return true;
    }
    // Chef d'agence du soumetteur (cas agent d'agence sans service, ou en complement).
    return (
      incidentAccessGuard.hasAuthority(user, "INCIDENT_VIEW_AGENCY") &&
      user.getAgencyId() != null &&
      submitter.getAgencyId() != null &&
      user.getAgencyId().equals(submitter.getAgencyId())
    );
  }

  private ExternalUser resolveSubmitter(Incident incident) {
    UUID submitterId = incident.getAssignedTo();
    if (submitterId == null) {
      return null;
    }
    try {
      return userClientService.getUser(submitterId);
    } catch (RuntimeException ex) {
      return null;
    }
  }

  // Vrai si l'utilisateur figure dans la liste des validateurs Direction configuree
  // sur le type d'incident.
  private boolean isConfiguredDirectionValidator(
    Incident incident,
    UUID userId
  ) {
    if (userId == null || incident.getTypeId() == null) {
      return false;
    }
    return incidentTypeConfigRepository
      .findById(incident.getTypeId())
      .map(IncidentTypeConfig::getDirectionValidatorIds)
      .filter(ids -> ids != null && ids.contains(userId))
      .isPresent();
  }

  // Determine le service traitant pour les regles chef de service (transfert effectif,
  // sinon service cible par defaut du type d'incident).
  private UUID closureService(Incident incident) {
    if (incident.getTransferredToService() != null) {
      return incident.getTransferredToService();
    }
    if (incident.getTypeId() == null) {
      return null;
    }
    return incidentTypeConfigRepository
      .findById(incident.getTypeId())
      .map(IncidentTypeConfig::getDefaultTargetServiceId)
      .orElse(null);
  }

  // Verifie que les regles metier autorisent la cloture selon le type d'incident.

  private void assertCanClose(
    Incident incident,
    UserDetailsImpl user,
    IncidentAction action
  ) {
    if (
      !user
        .getAuthorities()
        .stream()
        .anyMatch(a -> a.getAuthority().equals("INCIDENT_CLOSE"))
    ) {
      throw new AccessDeniedException(
        "Vous n'avez pas la permission de clôturer un incident (permission INCIDENT_CLOSE manquante)"
      );
    }
    assertActorInRoles(
      incident,
      user,
      configuredRolesOrDefault(
        incident,
        IncidentTypeConfig::getCloserRoles,
        DEFAULT_CLOSER_ROLES
      ),
      action
    );
  }

  private void assertCanResolve(
    Incident incident,
    UserDetailsImpl user,
    IncidentAction action
  ) {
    assertActorInRoles(
      incident,
      user,
      configuredRolesOrDefault(
        incident,
        IncidentTypeConfig::getResolverRoles,
        DEFAULT_RESOLVER_ROLES
      ),
      action
    );
  }

  private void assertCanTreat(
    Incident incident,
    UserDetailsImpl user,
    IncidentAction action
  ) {
    assertActorInRoles(
      incident,
      user,
      configuredRolesOrDefault(
        incident,
        IncidentTypeConfig::getTreaterRoles,
        DEFAULT_TREATER_ROLES
      ),
      action
    );
  }

  // Verifie que les regles metier autorisent l operation sur incident.

  private void assertCanStart(Incident incident, UserDetailsImpl user) {
    if (incident.getAssignedTo() == null) {
      throw new BusinessRuleViolationException(
        ErrorCode.INCIDENT_START_REQUIRES_ASSIGNEE,
        ErrorCode.INCIDENT_START_REQUIRES_ASSIGNEE.getMessageKey()
      );
    }

    UUID userId = user.getId();
    if (userId != null && userId.equals(incident.getAssignedTo())) {
      return;
    }

    throw new AccessDeniedException(
      "Seul l assigne courant peut demarrer cet incident"
    );
  }

  // Verifie que les regles metier autorisent l operation sur incident.

  private void assertCreator(
    Incident incident,
    UserDetailsImpl user,
    IncidentAction action
  ) {
    UUID userId = user.getId();
    if (userId != null && userId.equals(incident.getCreatedBy())) {
      return;
    }
    throw new AccessDeniedException(
      "Seul le createur peut " + action.name().toLowerCase() + " cet incident"
    );
  }

  // Exige id.

  private void requireId(UUID id, String message) {
    if (id == null) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        message
      );
    }
  }

  // Exige existing service.

  private void requireExistingService(UUID serviceId) {
    ExternalService service;
    try {
      service = userClientService.getService(serviceId);
    } catch (RuntimeException ex) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        "incident.error.target_service_not_found"
      );
    }
    if (
      service == null ||
      service.getId() == null ||
      !serviceId.equals(service.getId())
    ) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        "incident.error.target_service_not_found"
      );
    }
  }

  // Exige existing user.

  private ExternalUser requireExistingUser(UUID userId) {
    ExternalUser user;
    try {
      user = userClientService.getUser(userId);
    } catch (RuntimeException ex) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        "incident.error.assignee_inactive_or_not_found"
      );
    }
    if (user == null || user.getId() == null || !userId.equals(user.getId())) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        "incident.error.assignee_inactive_or_not_found"
      );
    }
    return user;
  }

  // Verifie que les regles metier autorisent l operation sur incident.

  private void assertAssigneeCanTreat(ExternalUser assignee, UUID assignedTo) {
    Set<String> permissions = assignee.getPermissions();
    if (
      permissions == null ||
      (!permissions.contains("INCIDENT_TREAT") &&
        !permissions.contains("INCIDENT_RESOLVE"))
    ) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        "incident.error.assignee_lacks_permission"
      );
    }
  }

  // Verifie que les regles metier autorisent l'operation sur incident.
  private void assertAssigneeMatchesIncidentScope(
    Incident incident,
    ExternalUser assignee,
    UUID assignedTo
  ) {
    UUID handlingService = currentHandlingService(incident);
    if (handlingService != null) {
      boolean inService =
        handlingService.equals(assignee.getServiceId()) ||
        (assignee.getManagedServiceIds() != null &&
          assignee.getManagedServiceIds().contains(handlingService));
      if (!inService) {
        throw new BusinessRuleViolationException(
          ErrorCode.INVALID_INPUT,
          "incident.error.assignee_outside_service"
        );
      }
    } else {
      if (
        incident.getAgencyId() != null &&
        assignee.getAgencyId() != null &&
        !incident.getAgencyId().equals(assignee.getAgencyId())
      ) {
        throw new BusinessRuleViolationException(
          ErrorCode.INVALID_INPUT,
          "incident.error.assignee_outside_agency"
        );
      }
    }
  }

  // Realise l'intention metier current handling service.

  private UUID currentHandlingService(Incident incident) {
    return incident.getTransferredToService();
  }

  // Verifie que les regles metier autorisent l operation sur incident.

  public void assertCanClone(Incident incident, UserDetailsImpl userDetails) {
    boolean hasCreate = userDetails
      .getAuthorities()
      .stream()
      .anyMatch(
        a ->
          a.getAuthority().equals("INCIDENT_CREATE") ||
          a.getAuthority().equals("INCIDENT_CREATE_OPEN")
      );
    if (!hasCreate) {
      throw new AccessDeniedException(
        "Permission requise : INCIDENT_CREATE ou INCIDENT_CREATE_OPEN"
      );
    }
  }
}
