// Client inter-services : communique avec les services externes lies a notification client.

package com.fintrack.incident.client.notification;

import com.fintrack.incident.client.notification.dto.NotificationClientRequest;
import com.fintrack.incident.client.user.UserClientService;
import com.fintrack.common.notification.EmailNotificationEvent;
import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.constant.IncidentValidatorScope;
import com.fintrack.incident.model.constant.IncidentValidatorRole;
import com.fintrack.incident.model.dto.response.AgencySummaryResponse;
import com.fintrack.incident.model.dto.response.ServiceSummaryResponse;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentTypeConfig;
import com.fintrack.incident.service.IncidentTypeConfigService;
import com.fintrack.incident.security.IncidentValidationPolicy;
import com.fintrack.incident.model.readmodel.ExternalUser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Diffuse les notifications liees aux incidents (dashboard + email) via le notification-service.
 * Les textes (subject et content) sont resolus cote backend via MessageSource.
 */
@Slf4j
@Service
@RequiredArgsConstructor
// Porte les regles metier du domaine notification.
public class NotificationClientService {

  private final NotificationClient notificationClient;
  private final UserClientService userClientService;
  private final IncidentTypeConfigService incidentTypeConfigService;
  private final MessageSource messageSource;
  private final IncidentValidationPolicy validationPolicy;
  private final com.fintrack.incident.client.reporting.ReportingEmailNotificationConfigService emailNotificationConfig;

  // Format de date lisible pour le CSV du rapport quotidien (Excel FR).
  private static final DateTimeFormatter CSV_DATE_FORMAT =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  @Value("${app.frontend.url:http://localhost:5173}")
  private String frontendUrl;

  // Agrege les destinataires internes et e-mail d'un evenement incident.
  private static class EventRecipients {

    Map<UUID, ExternalUser> internalUsers = new HashMap<>();
    Map<UUID, ExternalUser> emailUsers = new HashMap<>();
    // Evenement e-mail associe : pilote l'interrupteur global et la liste
    // d'exclusion Super Admin appliques a cet envoi.
    EmailNotificationEvent event;

    // Ajoute un destinataire interne eligible.
    void addInternal(ExternalUser user) {
      if (user != null && user.getId() != null) internalUsers.put(
        user.getId(),
        user
      );
    }

    // Ajoute un destinataire e-mail lorsque son adresse est exploitable.
    void addEmail(ExternalUser user) {
      if (
        user != null &&
        user.getId() != null &&
        StringUtils.hasText(user.getEmail())
      ) {
        emailUsers.put(user.getId(), user);
      }
    }

    // Retire un utilisateur des canaux internes et e-mail de l'evenement.
    void remove(UUID userId) {
      if (userId == null) return;
      internalUsers.remove(userId);
      emailUsers.remove(userId);
    }

    // Ajoute les administrateurs aux destinataires de l'evenement.
    void addAdmins(UserClientService userClientService, boolean sendEmail) {
      List<ExternalUser> admins = userClientService.resolveAdmins();
      if (admins != null) {
        admins.forEach(admin -> {
          addInternal(admin);
          if (sendEmail) addEmail(admin);
        });
      }
    }
  }

  // Identifie le service actuellement responsable de l'incident.
  private UUID getActiveServiceId(Incident incident) {
    return incident.getTransferredToService() != null
      ? incident.getTransferredToService()
      : incident.getCreatorServiceId();
  }

  // Diffuse une seule notification de soumission, creation ou resoumission incluse.
  public void notifyIncidentSubmitted(
    Incident incident,
    IncidentValidatorScope validatorScope,
    UUID actionneurId,
    String comment
  ) {
    EventRecipients recipients = new EventRecipients();
    recipients.event = EmailNotificationEvent.INCIDENT_SUBMITTED;
    Map<UUID, String> recipientRoles = new HashMap<>();

    addRecipientWithRole(
      recipients,
      recipientRoles,
      safeGetUser(incident.getCreatedBy()),
      "creator"
    );

    IncidentTypeConfig config = getIncidentTypeConfig(incident);

    if (incident.getStatus() == IncidentStatus.PENDING_VALIDATION) {
      addValidatorRecipients(
        recipients,
        recipientRoles,
        incident,
        validatorScope,
        config
      );
    }

    if (incident.getAssignedTo() != null) {
      addRecipientWithRole(
        recipients,
        recipientRoles,
        safeGetUser(incident.getAssignedTo()),
        "assignee"
      );
    }

    boolean sendEmail = emailAllowed(
      EmailNotificationEvent.INCIDENT_SUBMITTED,
      config
    );

    List<ExternalUser> admins = userClientService.resolveAdmins();
    if (admins != null) {
      admins.forEach(admin ->
        addRecipientWithRole(recipients, recipientRoles, admin, "other")
      );
    }

    if (sendEmail) {
      recipients.internalUsers.values().forEach(recipients::addEmail);
    }

    dispatchSubmittedEvent(
      incident,
      actionneurId,
      comment,
      recipients,
      recipientRoles
    );
  }

  // Ajoute le destinataire "validator" attendu selon la portee de validation.
  // Le scope ADMIN n'a pas de valideur unique : tous les administrateurs sont deja
  // notifies systematiquement pour chaque soumission (resolveAdmins plus bas), on
  // n'ajoute donc rien ici pour eviter un doublon et ne pas etiqueter a tort le chef
  // d'agence comme valideur. Les autres scopes ont un valideur unique.
  private void addValidatorRecipients(
    EventRecipients recipients,
    Map<UUID, String> recipientRoles,
    Incident incident,
    IncidentValidatorScope validatorScope,
    IncidentTypeConfig config
  ) {
    IncidentValidatorRole ownServiceValidator = validationPolicy.resolveOwnServiceValidator(incident, config);
    if (ownServiceValidator == IncidentValidatorRole.SERVICE_MANAGER) {
      addRecipientWithRole(recipients, recipientRoles, safeGetUser(incident.getCreatedBy()), "validator");
      return;
    }
    if (ownServiceValidator == IncidentValidatorRole.ADMIN || validatorScope == IncidentValidatorScope.ADMIN) {
      return;
    }
    addRecipientWithRole(
      recipients,
      recipientRoles,
      resolveValidator(incident, validatorScope, config),
      "validator"
    );
  }

  // Resout le valideur attendu pour la soumission d'un incident.
  private ExternalUser resolveValidator(
    Incident incident,
    IncidentValidatorScope validatorScope,
    IncidentTypeConfig config
  ) {
    IncidentValidatorScope effectiveScope =
      validatorScope != null
        ? validatorScope
        : IncidentValidatorScope.SOURCE_SERVICE_MANAGER;

    // TARGET : le service cible vient du TYPE, pas du service actif de l'incident --
    // c'est le critere applique par IncidentWorkflowGuard pour autoriser la validation.
    // Type sans service cible : le garde retombe sur le chef d'agence, nous aussi.
    if (effectiveScope == IncidentValidatorScope.TARGET_SERVICE_MANAGER) {
      UUID targetServiceId = config != null
        ? config.getDefaultTargetServiceId()
        : null;
      if (targetServiceId != null) {
        UUID serviceHeadId = userClientService.resolveServiceHead(
          targetServiceId
        );
        if (serviceHeadId == null) {
          log.warn(
            "Soumission incident : aucun responsable configure pour le service cible {} de l'incident {}",
            targetServiceId,
            incident.getId()
          );
        }
        return safeGetUser(serviceHeadId);
      }
    }

    // SOURCE (defaut) : chef du service du createur ; repli agence si pas de service
    // ou service sans chef. Aligne sur IncidentWorkflowGuard.assertCanValidateSourceService.
    if (effectiveScope == IncidentValidatorScope.SOURCE_SERVICE_MANAGER) {
      UUID creatorServiceId = incident.getCreatorServiceId();
      if (creatorServiceId != null) {
        UUID serviceHeadId = userClientService.resolveServiceHead(
          creatorServiceId
        );
        if (serviceHeadId != null) {
          return safeGetUser(serviceHeadId);
        }
      }
      // Pas de service, ou service sans chef : repli sur le chef d'agence ci-dessous.
    }

    // AGENCY_MANAGER, repli SOURCE ou repli TARGET : chef d'agence. (Le scope ADMIN est traite en
    // amont par addValidatorRecipients ; il ne parvient pas jusqu'ici.)
    UUID agencyHeadId = userClientService.resolveAgencyHead(
      incident.getAgencyId()
    );
    if (agencyHeadId == null) {
      log.warn(
        "Soumission incident : aucun responsable configure pour l'agence {} de l'incident {}",
        incident.getAgencyId(),
        incident.getId()
      );
    }
    return safeGetUser(agencyHeadId);
  }

  // Ajoute un destinataire en conservant son role metier prioritaire.
  private void addRecipientWithRole(
    EventRecipients recipients,
    Map<UUID, String> recipientRoles,
    ExternalUser user,
    String role
  ) {
    if (user == null || user.getId() == null) {
      return;
    }
    recipients.addInternal(user);
    recipientRoles.putIfAbsent(user.getId(), role);
  }

  // Diffuse l'information du domaine notification client aux destinataires concernes.

  public void notifyIncidentAssigned(Incident incident, UUID assignee) {
    EventRecipients recipients = new EventRecipients();
    recipients.event = EmailNotificationEvent.INCIDENT_ASSIGNED;

    recipients.addInternal(safeGetUser(incident.getCreatedBy()));
    recipients.addInternal(safeGetUser(assignee));

    if (getActiveServiceId(incident) != null) {
      UUID serviceHeadId = userClientService.resolveServiceHead(
        getActiveServiceId(incident)
      );
      recipients.addInternal(safeGetUser(serviceHeadId));
    }

    dispatchAssignedEvent(incident, assignee, recipients);
  }

  // Diffuse l'information du domaine notification client aux destinataires concernes.

  public void notifyStatusChanged(
    Incident incident,
    String newStatusLabel,
    UUID actionneurId,
    String comment
  ) {
    notifyStatusChanged(incident, newStatusLabel, actionneurId, comment, Set.of());
  }

  public void notifyStatusChanged(
    Incident incident,
    String newStatusLabel,
    UUID actionneurId,
    String comment,
    Set<UUID> extraRecipientIds
  ) {
    // Commentaire libre (saisi par l'utilisateur) : identique dans les deux langues.
    BilingualText bilingualComment = StringUtils.hasText(comment)
      ? new BilingualText(comment, comment)
      : null;
    dispatchStatusChangedForRecipients(
      incident,
      actionneurId,
      bilingualComment,
      extraRecipientIds,
      EmailNotificationEvent.STATUS_CHANGED
    );
  }

  // Variante systeme (transitions automatiques) : le motif est resolu dans les deux
  // langues depuis une cle i18n (le job planifie n'a pas acces au type BilingualText interne).
  public void notifyAutoStatusChanged(
    Incident incident,
    UUID actionneurId,
    String commentKey,
    Object... commentArgs
  ) {
    BilingualText comment = commentKey != null
      ? lmsg(commentKey, commentArgs)
      : null;
    dispatchStatusChangedForRecipients(
      incident,
      actionneurId,
      comment,
      Set.of(),
      EmailNotificationEvent.AUTO_STATUS_CHANGED
    );
  }

  // Assemble les destinataires standards d'un changement de statut puis diffuse.
  // Toute la chaine de traitement doit etre couverte
  private void dispatchStatusChangedForRecipients(
    Incident incident,
    UUID actionneurId,
    BilingualText comment,
    Set<UUID> extraRecipientIds,
    EmailNotificationEvent event
  ) {
    EventRecipients recipients = new EventRecipients();
    recipients.event = event;

    recipients.addInternal(safeGetUser(incident.getCreatedBy()));
    recipients.addInternal(safeGetUser(incident.getAssignedTo()));
    recipients.addInternal(safeGetUser(incident.getValidatedBy()));
    recipients.addInternal(safeGetUser(actionneurId));
    if (getActiveServiceId(incident) != null) {
      UUID serviceHeadId = userClientService.resolveServiceHead(
        getActiveServiceId(incident)
      );
      recipients.addInternal(safeGetUser(serviceHeadId));
    }
    if (incident.getAgencyId() != null) {
      UUID agencyHeadId = userClientService.resolveAgencyHead(
        incident.getAgencyId()
      );
      recipients.addInternal(safeGetUser(agencyHeadId));
    }
    if (extraRecipientIds != null) {
      extraRecipientIds.forEach(id -> recipients.addInternal(safeGetUser(id)));
    }

    dispatchStatusChangedEvent(incident, actionneurId, comment, recipients, event);
  }

  // Soumission d'une proposition de solution pour validation prealable : chaque valideur
  // designe recoit une notification interne + un e-mail
  public void notifySolutionProposed(
    Incident incident,
    Set<UUID> validatorIds,
    UUID actorId
  ) {
    EventRecipients recipients = new EventRecipients();
    recipients.event = EmailNotificationEvent.DIRECTION_SUBMITTED;
    Set<UUID> resolvedValidatorIds = new HashSet<>();
    if (validatorIds != null) {
      for (UUID validatorId : validatorIds) {
        ExternalUser validator = safeGetUser(validatorId);
        if (validator != null && validator.getId() != null) {
          recipients.addInternal(validator);
          recipients.addEmail(validator);
          resolvedValidatorIds.add(validator.getId());
        }
      }
    }
    if (getActiveServiceId(incident) != null) {
      UUID serviceHeadId = userClientService.resolveServiceHead(
        getActiveServiceId(incident)
      );
      recipients.addInternal(safeGetUser(serviceHeadId));
    }
    recipients.addAdmins(userClientService, false);

    String actionerName = actionerDisplay(actorId);
    Map<String, Object> extraParams = new HashMap<>();
    extraParams.put("actioner_full_name", actionerName);
    extraParams.put("email_template", "direction_validation_template");
    extraParams.put(
      "proposed_solution",
      incident.getProposedSolution() != null
        ? incident.getProposedSolution()
        : ""
    );
    extraParams.put(
      "incident_description",
      incident.getDescription() != null ? incident.getDescription() : ""
    );

    BilingualText subject = lmsg(
      "notification.incident.direction.submitted.subject",
      actionerName
    );
    dispatchDirectionEvent(
      incident,
      recipients,
      subject,
      user -> {
        boolean isValidator = resolvedValidatorIds.contains(user.getId());
        String key =
          "notification.incident.direction.submitted.content." +
          (isValidator ? "validator" : "other");
        return isValidator
          ? lmsg(key, incident.getTitle())
          : lmsg(key, incident.getTitle(), actionerName);
      },
      extraParams
    );
  }

  // Validation prealable accordee : le traitant est notifie (interne + e-mail),
  // le chef de service et les administrateurs en interne uniquement.
  public void notifyDirectionValidated(Incident incident, UUID actorId) {
    EventRecipients recipients = new EventRecipients();
    recipients.event = EmailNotificationEvent.DIRECTION_VALIDATED;
    if (incident.getAssignedTo() != null) {
      ExternalUser assignee = safeGetUser(incident.getAssignedTo());
      recipients.addInternal(assignee);
      recipients.addEmail(assignee);
    }
    if (getActiveServiceId(incident) != null) {
      UUID serviceHeadId = userClientService.resolveServiceHead(
        getActiveServiceId(incident)
      );
      recipients.addInternal(safeGetUser(serviceHeadId));
    }
    recipients.addAdmins(userClientService, false);

    String actionerName = actionerDisplay(actorId);
    Map<String, Object> extraParams = new HashMap<>();
    extraParams.put("actioner_full_name", actionerName);

    BilingualText subject = lmsg(
      "notification.incident.direction.validated.subject",
      actionerName
    );
    UUID assigneeId = incident.getAssignedTo();
    dispatchDirectionEvent(
      incident,
      recipients,
      subject,
      user -> {
        boolean isAssignee =
          assigneeId != null && assigneeId.equals(user.getId());
        String key =
          "notification.incident.direction.validated.content." +
          (isAssignee ? "assignee" : "other");
        return isAssignee
          ? lmsg(key, incident.getTitle())
          : lmsg(key, incident.getTitle(), actionerName);
      },
      extraParams
    );
  }

  // Validation prealable refusee : le traitant est notifie (interne + e-mail) avec le
  // motif ; les administrateurs sont notifies en interne uniquement.
  public void notifyDirectionRejected(
    Incident incident,
    UUID actorId,
    String reason
  ) {
    EventRecipients recipients = new EventRecipients();
    recipients.event = EmailNotificationEvent.DIRECTION_REJECTED;
    if (incident.getAssignedTo() != null) {
      ExternalUser assignee = safeGetUser(incident.getAssignedTo());
      recipients.addInternal(assignee);
      recipients.addEmail(assignee);
    }
    recipients.addAdmins(userClientService, false);

    String actionerName = actionerDisplay(actorId);
    String safeReason = reason != null ? reason : "";
    Map<String, Object> extraParams = new HashMap<>();
    extraParams.put("actioner_full_name", actionerName);
    extraParams.put("comment", safeReason);

    BilingualText subject = lmsg(
      "notification.incident.direction.rejected.subject",
      actionerName
    );
    UUID assigneeId = incident.getAssignedTo();
    dispatchDirectionEvent(
      incident,
      recipients,
      subject,
      user -> {
        boolean isAssignee =
          assigneeId != null && assigneeId.equals(user.getId());
        String key =
          "notification.incident.direction.rejected.content." +
          (isAssignee ? "assignee" : "other");
        return isAssignee
          ? lmsg(key, incident.getTitle(), safeReason)
          : lmsg(key, incident.getTitle(), safeReason, actionerName);
      },
      extraParams
    );
  }

  // Resout le nom affichable de l'acteur, avec repli sur le libelle systeme.
  private String actionerDisplay(UUID actorId) {
    String name = displayName(safeGetUser(actorId));
    return StringUtils.hasText(name) ? name : msg("notification.system.name");
  }

  // Diffuse un evenement de validation prealable : sujet bilingue partage et contenu
  // bilingue resolu par destinataire (valideur/traitant vs autres).
  private void dispatchDirectionEvent(
    Incident incident,
    EventRecipients recipients,
    BilingualText subject,
    java.util.function.Function<ExternalUser, BilingualText> contentFor,
    Map<String, Object> extraParams
  ) {
    // Interrupteur Direction : la notification interne (valideurs / traitant /
    // admins) reste inchangee ; l'e-mail est filtre par le verrou emailRecipients().
    for (ExternalUser user : recipients.internalUsers.values()) {
      send(
        "INTERNAL",
        user,
        user.getUsername(),
        incident,
        subject,
        contentFor.apply(user),
        extraParams
      );
    }
    for (ExternalUser user : emailRecipients(recipients)) {
      send(
        "EMAIL",
        user,
        user.getEmail(),
        incident,
        subject,
        contentFor.apply(user),
        extraParams
      );
    }
  }

  // Diffuse l'information du domaine notification client aux destinataires concernes.

  public void notifyIncidentTransferred(
    Incident incident,
    UUID oldServiceId,
    UUID newServiceId,
    UUID actorId
  ) {
    EventRecipients recipients = new EventRecipients();
    recipients.event = EmailNotificationEvent.TRANSFERRED_SERVICE;

    recipients.addInternal(safeGetUser(incident.getCreatedBy()));

    if (oldServiceId != null) {
      UUID oldServiceHeadId = userClientService.resolveServiceHead(
        oldServiceId
      );
      recipients.addInternal(safeGetUser(oldServiceHeadId));
    }

    if (newServiceId != null) {
      UUID newServiceHeadId = userClientService.resolveServiceHead(
        newServiceId
      );
      recipients.addInternal(safeGetUser(newServiceHeadId));
    }

    String targetServiceName = userClientService.resolveServiceName(newServiceId);
    String actionerName = actionerDisplay(actorId);
    dispatchEvent(
      incident,
      "notification.incident.transferred.subject",
      new Object[] { targetServiceName, actionerName },
      "notification.incident.transferred.content",
      new Object[] {
        incident.getTitle(),
        targetServiceName,
        actionerName,
      },
      recipients
    );
  }

  // Notifie le retour d'un incident vers l'agence du declarant.
  public void notifyIncidentTransferredToAgency(
    Incident incident,
    UUID oldServiceId,
    UUID newAgencyId,
    UUID actorId
  ) {
    EventRecipients recipients = new EventRecipients();
    recipients.event = EmailNotificationEvent.TRANSFERRED_AGENCY;
    recipients.addInternal(safeGetUser(incident.getCreatedBy()));
    if (oldServiceId != null) {
      recipients.addInternal(
        safeGetUser(userClientService.resolveServiceHead(oldServiceId))
      );
    }
    recipients.addInternal(
      safeGetUser(userClientService.resolveAgencyHead(newAgencyId))
    );
    String targetAgencyName = userClientService.resolveAgencyName(newAgencyId);
    String actionerName = actionerDisplay(actorId);
    dispatchEvent(
      incident,
      "notification.incident.transferred_to_agency.subject",
      new Object[] { targetAgencyName, actionerName },
      "notification.incident.transferred_to_agency.content",
      new Object[] {
        incident.getTitle(),
        targetAgencyName,
        actionerName,
      },
      recipients
    );
  }

  // Diffuse l'information du domaine notification client aux destinataires concernes.

  public void notifyQualificationChanged(
    Incident incident,
    String changeSummary,
    UUID actorId
  ) {
    EventRecipients recipients = new EventRecipients();
    recipients.event = EmailNotificationEvent.QUALIFICATION_CHANGED;

    // Notify creator
    recipients.addInternal(safeGetUser(incident.getCreatedBy()));

    // Notify admins
    recipients.addAdmins(userClientService, false);

    String actionerName = actionerDisplay(actorId);
    dispatchEvent(
      incident,
      "notification.incident.qualification_changed.subject",
      new Object[] { actionerName },
      "notification.incident.qualification_changed.content",
      new Object[] {
        incident.getTitle(),
        changeSummary,
        actionerName,
      },
      recipients
    );
  }

  // Relance d'une action attendue apres traitement (resolution, cloture). Les deux
  // canaux ont leur propre interrupteur Super Admin : l'interne est pilote par l'appelant,
  // l'e-mail par l'evenement.
  public void notifyPendingActionReminder(
    Incident incident,
    Set<UUID> actorIds,
    long waitingHours,
    boolean internalEnabled
  ) {
    EventRecipients recipients = new EventRecipients();
    recipients.event = EmailNotificationEvent.PENDING_ACTION_REMINDER;

    if (actorIds != null) {
      actorIds.forEach(id -> {
        ExternalUser actor = safeGetUser(id);
        if (internalEnabled) {
          recipients.addInternal(actor);
        }
        recipients.addEmail(actor);
      });
    }
    if (internalEnabled) {
      recipients.addAdmins(userClientService, false);
    }

    dispatchEvent(
      incident,
      "notification.incident.pending_action_reminder.subject",
      new Object[] { incident.getTitle() },
      "notification.incident.pending_action_reminder.content",
      new Object[] {
        incident.getTitle(),
        waitingHours,
        msg(incident.getStatus().getNameKey()),
      },
      recipients
    );
  }

  // Relance periodique : le declarant et le traitant, chacun avec son chef.
  public void notifyCriticalIncidentReminder(
    Incident incident,
    long openHours
  ) {
    EventRecipients recipients = new EventRecipients();
    recipients.event = EmailNotificationEvent.CRITICAL_INCIDENT_REMINDER;

    addWithManager(recipients, safeGetUser(incident.getCreatedBy()));
    addWithManager(recipients, safeGetUser(incident.getAssignedTo()));
    // Sans traitant, le chef du service en charge reste le destinataire utile.
    if (incident.getAssignedTo() == null) {
      UUID handlingService = getActiveServiceId(incident);
      if (handlingService != null) {
        addBoth(
          recipients,
          safeGetUser(userClientService.resolveServiceHead(handlingService))
        );
      }
    }

    dispatchEvent(
      incident,
      "notification.incident.critical_reminder.subject",
      new Object[] { incident.getTitle(), openHours },
      "notification.incident.critical_reminder.content",
      new Object[] {
        incident.getTitle(),
        openHours,
        msg(incident.getStatus().getNameKey()),
      },
      recipients
    );
  }

  // Chef de service, ou d'agence a defaut : sinon la relance ne quitte pas l'interesse.
  private void addWithManager(EventRecipients recipients, ExternalUser user) {
    if (user == null) return;
    addBoth(recipients, user);

    UUID managerId = user.getServiceId() != null
      ? userClientService.resolveServiceHead(user.getServiceId())
      : null;
    if (managerId == null || managerId.equals(user.getId())) {
      managerId = user.getAgencyId() != null
        ? userClientService.resolveAgencyHead(user.getAgencyId())
        : null;
    }
    if (managerId != null && !managerId.equals(user.getId())) {
      addBoth(recipients, safeGetUser(managerId));
    }
  }

  // Inscrit un destinataire sur les deux canaux.
  private void addBoth(EventRecipients recipients, ExternalUser user) {
    recipients.addInternal(user);
    recipients.addEmail(user);
  }

  // Relance d'une decision attendue, adressee a qui doit la prendre.
  public void notifyValidationReminder(Incident incident, long waitingHours) {
    EventRecipients recipients = new EventRecipients();
    recipients.event = EmailNotificationEvent.VALIDATION_REMINDER;
    IncidentTypeConfig config = getIncidentTypeConfig(incident);

    // Apres un rejet Direction, c'est le traitant qu'on attend, pas la Direction.
    boolean awaitingHandler =
      incident.getStatus() == IncidentStatus.DRAFT &&
      incident.getDirectionRejectionReason() != null;

    if (awaitingHandler) {
      recipients.addInternal(safeGetUser(incident.getAssignedTo()));
      UUID handlingService = getActiveServiceId(incident);
      if (handlingService != null) {
        recipients.addInternal(
          safeGetUser(userClientService.resolveServiceHead(handlingService))
        );
      }
    } else if (incident.getStatus() == IncidentStatus.DRAFT) {
      Set<UUID> directionValidatorIds = config != null
        ? config.getDirectionValidatorIds()
        : null;
      if (directionValidatorIds != null) {
        directionValidatorIds.forEach(id ->
          recipients.addInternal(safeGetUser(id))
        );
      }
    } else {
      addValidatorRecipients(
        recipients,
        new HashMap<>(),
        incident,
        config != null ? config.getValidatorScope() : null,
        config
      );
    }
    recipients.addAdmins(userClientService, false);

    dispatchEvent(
      incident,
      awaitingHandler
        ? "notification.incident.solution_resubmission_reminder.subject"
        : "notification.incident.validation_reminder.subject",
      new Object[] { incident.getTitle() },
      awaitingHandler
        ? "notification.incident.solution_resubmission_reminder.content"
        : "notification.incident.validation_reminder.content",
      new Object[] { incident.getTitle(), waitingHours },
      recipients
    );
  }

  // Attente prolongee : le traitant demande a l'entite source si l'incident est encore
  // d'actualite. Le destinataire utile est celui qui peut repondre.
  public void notifyConfirmationRequested(
    Incident incident,
    UUID requestedBy,
    String comment
  ) {
    EventRecipients recipients = new EventRecipients();
    recipients.event = EmailNotificationEvent.RELEVANCE_CONFIRMATION;
    addSourceEntityHead(incident, recipients);
    recipients.addInternal(safeGetUser(incident.getCreatedBy()));
    recipients.addAdmins(userClientService, false);

    dispatchEvent(
      incident,
      "notification.incident.confirmation_requested.subject",
      new Object[] { actionerDisplay(requestedBy) },
      "notification.incident.confirmation_requested.content",
      new Object[] {
        incident.getTitle(),
        actionerDisplay(requestedBy),
        comment != null ? comment : "",
      },
      recipients
    );
  }

  // Relance sur une demande d'actualite restee sans reponse. Les administrateurs sont
  // destinataires des le premier envoi comme de chaque relance : un incident ne doit
  // jamais dependre du seul silence de l'entite source.
  public void notifyConfirmationReminder(Incident incident, long waitingDays) {
    EventRecipients recipients = new EventRecipients();
    recipients.event = EmailNotificationEvent.RELEVANCE_CONFIRMATION;
    addSourceEntityHead(incident, recipients);
    recipients.addInternal(safeGetUser(incident.getAssignedTo()));
    recipients.addAdmins(userClientService, false);

    dispatchEvent(
      incident,
      "notification.incident.confirmation_reminder.subject",
      new Object[] { incident.getTitle() },
      "notification.incident.confirmation_reminder.content",
      new Object[] { incident.getTitle(), waitingDays },
      recipients
    );
  }

  // Responsable de l'entite d'ou vient l'incident : chef du service createur s'il en
  // existe un, sinon chef de l'agence source (miroir de IncidentWorkflowGuard).
  private void addSourceEntityHead(
    Incident incident,
    EventRecipients recipients
  ) {
    UUID creatorServiceId = incident.getCreatorServiceId();
    if (creatorServiceId != null) {
      UUID serviceHeadId = userClientService.resolveServiceHead(
        creatorServiceId
      );
      if (serviceHeadId != null) {
        recipients.addInternal(safeGetUser(serviceHeadId));
        return;
      }
    }
    UUID agencyHeadId = userClientService.resolveAgencyHead(
      incident.getAgencyId()
    );
    if (agencyHeadId != null) {
      recipients.addInternal(safeGetUser(agencyHeadId));
    }
  }

  // Diffuse l'information du domaine notification client aux destinataires concernes.

  public void notifyIncidentUnassigned(Incident incident, UUID serviceId) {
    EventRecipients recipients = new EventRecipients();
    recipients.event = EmailNotificationEvent.UNASSIGNED_SERVICE;
    recipients.addAdmins(userClientService, false);

    dispatchEvent(
      incident,
      "notification.incident.unassigned.subject",
      "notification.incident.unassigned.content",
      new Object[] {
        incident.getTitle(),
        userClientService.resolveServiceName(serviceId),
      },
      recipients
    );
  }

  // Alerte les administrateurs lorsqu'une agence cible n'a pas de responsable.
  public void notifyIncidentUnassignedAtAgency(
    Incident incident,
    UUID agencyId
  ) {
    EventRecipients recipients = new EventRecipients();
    recipients.event = EmailNotificationEvent.UNASSIGNED_AGENCY;
    recipients.addAdmins(userClientService, false);
    dispatchEvent(
      incident,
      "notification.incident.unassigned.subject",
      "notification.incident.unassigned_at_agency.content",
      new Object[] {
        incident.getTitle(),
        userClientService.resolveAgencyName(agencyId),
      },
      recipients
    );
  }

  // Diffuse l'information du domaine notification client aux destinataires concernes.

  public void notifySlaBreached(Incident incident, String newStatusLabel) {
    EventRecipients recipients = new EventRecipients();
    recipients.event = EmailNotificationEvent.SLA_REMINDER;

    ExternalUser creator = safeGetUser(incident.getCreatedBy());
    recipients.addInternal(creator);

    if (incident.getAssignedTo() != null) {
      ExternalUser assignee = safeGetUser(incident.getAssignedTo());
      recipients.addInternal(assignee);
      recipients.addEmail(assignee);
    }

    UUID agencyHeadId = userClientService.resolveAgencyHead(
      incident.getAgencyId()
    );
    recipients.addInternal(safeGetUser(agencyHeadId));

    Set<UUID> relevantServiceIds = new LinkedHashSet<>();
    if (incident.getCreatorServiceId() != null) {
      relevantServiceIds.add(incident.getCreatorServiceId());
    }
    if (getActiveServiceId(incident) != null) {
      relevantServiceIds.add(getActiveServiceId(incident));
    }
    for (UUID serviceId : relevantServiceIds) {
      ExternalUser serviceHead = safeGetUser(
        userClientService.resolveServiceHead(serviceId)
      );
      recipients.addInternal(serviceHead);
      recipients.addEmail(serviceHead);
    }

    dispatchEvent(
      incident,
      "notification.incident.sla_breached.subject",
      "notification.incident.sla_breached.content",
      locale ->
        new Object[] { incident.getTitle(), statusLabel(incident, locale) },
      recipients,
      true
    );
  }

  // Diffuse l'information du domaine notification client aux destinataires concernes.

  public void notifyIncidentComment(Incident incident, UUID commentAuthorId) {
    EventRecipients recipients = new EventRecipients();

    recipients.addInternal(safeGetUser(commentAuthorId));
    recipients.addInternal(safeGetUser(incident.getCreatedBy()));
    if (incident.getAssignedTo() != null) {
      recipients.addInternal(safeGetUser(incident.getAssignedTo()));
    }
    if (getActiveServiceId(incident) != null) {
      UUID serviceHeadId = userClientService.resolveServiceHead(
        getActiveServiceId(incident)
      );
      recipients.addInternal(safeGetUser(serviceHeadId));
    }
    // En interne seulement (pas d'e-mail) : les administrateurs suivent la vie de
    // l'incident comme pour toutes les autres notifications du service.
    recipients.addAdmins(userClientService, false);

    ExternalUser commentAuthor = safeGetUser(commentAuthorId);
    String authorName = displayName(commentAuthor);
    BilingualText subject = lmsgPer(
      "notification.incident.comment.subject",
      locale ->
        new Object[] {
          StringUtils.hasText(authorName)
            ? authorName
            : msgIn(locale, "notification.system.name"),
        }
    );

    for (ExternalUser user : recipients.internalUsers.values()) {
      boolean isSelf = user.getId() != null && user.getId().equals(commentAuthorId);
      String contentKey = isSelf
        ? "notification.incident.comment.content.self"
        : "notification.incident.comment.content.other";
      Object[] args = isSelf
        ? new Object[] { incident.getTitle() }
        : new Object[] { authorName, incident.getTitle() };

      send(
        "INTERNAL",
        user,
        user.getUsername(),
        incident,
        subject,
        lmsg(contentKey, args),
        null
      );
    }
  }

  // Diffuse l'information du domaine notification client aux destinataires concernes.

  public void notifyIncidentCommentReply(
    Incident incident,
    UUID replyAuthorId,
    UUID parentAuthorId
  ) {
    EventRecipients recipients = new EventRecipients();

    recipients.addInternal(safeGetUser(replyAuthorId));
    recipients.addInternal(safeGetUser(incident.getCreatedBy()));
    if (incident.getAssignedTo() != null) {
      recipients.addInternal(safeGetUser(incident.getAssignedTo()));
    }
    if (getActiveServiceId(incident) != null) {
      UUID serviceHeadId = userClientService.resolveServiceHead(
        getActiveServiceId(incident)
      );
      recipients.addInternal(safeGetUser(serviceHeadId));
    }
    if (parentAuthorId != null) {
      recipients.addInternal(safeGetUser(parentAuthorId));
    }
    recipients.addAdmins(userClientService, false);

    ExternalUser replyAuthor = safeGetUser(replyAuthorId);
    String authorName = displayName(replyAuthor);
    BilingualText subject = lmsgPer(
      "notification.incident.comment_reply.subject",
      locale ->
        new Object[] {
          StringUtils.hasText(authorName)
            ? authorName
            : msgIn(locale, "notification.system.name"),
        }
    );

    for (ExternalUser user : recipients.internalUsers.values()) {
      boolean isSelf = user.getId() != null && user.getId().equals(replyAuthorId);
      boolean isParentAuthor = !isSelf && parentAuthorId != null && user.getId() != null && user.getId().equals(parentAuthorId);

      String contentKey;
      Object[] args;

      if (isSelf) {
        contentKey = "notification.incident.comment_reply.content.self";
        args = new Object[] { incident.getTitle() };
      } else if (isParentAuthor) {
        contentKey = "notification.incident.comment_reply.content.parent_author";
        args = new Object[] { incident.getTitle(), authorName };
      } else {
        contentKey = "notification.incident.comment_reply.content.other";
        args = new Object[] { authorName, incident.getTitle() };
      }

      send(
        "INTERNAL",
        user,
        user.getUsername(),
        incident,
        subject,
        lmsg(contentKey, args),
        null
      );
    }
  }

  // Diffuse l'information du domaine notification client aux destinataires concernes.

  public void notifyLateIncidentsDailyReport(List<Incident> lateIncidents) {
    if (lateIncidents == null || lateIncidents.isEmpty()) return;

    // Interrupteur Super Admin : si l'e-mail du rapport quotidien est coupe, on
    // n'envoie rien (ce rapport n'a pas de pendant interne).
    if (
      !emailNotificationConfig.isEmailEnabled(
        EmailNotificationEvent.LATE_INCIDENTS_DAILY_REPORT
      )
    ) {
      return;
    }

    EventRecipients recipients = new EventRecipients();
    recipients.event = EmailNotificationEvent.LATE_INCIDENTS_DAILY_REPORT;
    recipients.addAdmins(userClientService, true);

    // Construit le CSV : BOM UTF-8 (accents corrects sous Excel) + entete lisible.
    // Separateur point-virgule (attendu par Excel en locale FR) et valeurs traduites.
    StringBuilder csv = new StringBuilder();
    csv.append('\uFEFF');
    csv.append(
      "Référence;Titre;Statut;Criticité;Agence;Assigné à;Date de création;Échéance\n"
    );
    for (Incident inc : lateIncidents) {
      String ref = StringUtils.hasText(inc.getReference())
        ? inc.getReference()
        : (inc.getId() != null
          ? inc.getId().toString().substring(0, 8).toUpperCase()
          : "");
      csv
        .append(csvCell(ref))
        .append(';')
        .append(csvCell(inc.getTitle()))
        .append(';')
        .append(csvCell(statusLabel(inc, Locale.FRENCH)))
        .append(';')
        .append(csvCell(criticalityLabel(inc, Locale.FRENCH)))
        .append(';')
        .append(csvCell(resolveAgencyText(inc).fr()))
        .append(';')
        .append(csvCell(assigneeName(inc)))
        .append(';')
        .append(csvCell(formatCsvDate(inc.getCreatedAt())))
        .append(';')
        .append(csvCell(formatCsvDate(inc.getDueDate())))
        .append('\n');
    }
    String encodedCsv = Base64.getEncoder().encodeToString(
      csv.toString().getBytes(StandardCharsets.UTF_8)
    );

    for (ExternalUser admin : emailRecipients(recipients)) {
      try {
        Map<String, Object> params = new HashMap<>();
        params.put("email_template", "late_incidents_report");
        params.put(
          "firstName",
          admin.getFirstName() != null ? admin.getFirstName() : ""
        );
        params.put(
          "name",
          admin.getLastName() != null
            ? admin.getLastName()
            : admin.getUsername()
        );
        params.put("report_name", "Rapport quotidien des incidents en retard");
        params.put("report_count", lateIncidents.size());
        params.put("report_date", LocalDate.now());
        params.put(
          "attachment_name",
          "incidents_en_retard_" + LocalDate.now() + ".csv"
        );
        params.put("attachment_content", encodedCsv);

        notificationClient.createNotification(
          NotificationClientRequest.builder()
            .type("EMAIL")
            .recipient(admin.getEmail())
            .subject("Rapport quotidien des incidents en retard")
            .content(
              "Veuillez trouver en pièce jointe la liste des incidents en retard."
            )
            .templateParams(params)
            .build()
        );
      } catch (Exception e) {
        log.error(
          "Échec d'envoi du rapport des incidents en retard vers {}: {}",
          admin.getEmail(),
          e.getMessage()
        );
      }
    }
  }

  // Diffuse l'information du domaine notification client aux destinataires concernes.

  private void dispatchEvent(
    Incident incident,
    String subjectKey,
    String contentKey,
    Object[] contentArgs,
    EventRecipients recipients
  ) {
    dispatchEvent(
      incident,
      subjectKey,
      contentKey,
      contentArgs,
      recipients,
      true
    );
  }

  // Variante avec contenu traduit et sujet sans argument specifique.
  private void dispatchEvent(
    Incident incident,
    String subjectKey,
    String contentKey,
    java.util.function.Function<Locale, Object[]> contentArgsFor,
    EventRecipients recipients,
    boolean includeAdmins
  ) {
    dispatchEvent(
      incident,
      subjectKey,
      new Object[0],
      contentKey,
      contentArgsFor,
      recipients,
      includeAdmins
    );
  }

  // Diffuse un evenement dont le sujet porte son propre contexte metier.
  private void dispatchEvent(
    Incident incident,
    String subjectKey,
    Object[] subjectArgs,
    String contentKey,
    Object[] contentArgs,
    EventRecipients recipients
  ) {
    dispatchEvent(
      incident,
      subjectKey,
      subjectArgs,
      contentKey,
      locale -> contentArgs,
      recipients,
      true
    );
  }

  // Decide de l'ampleur de l'audience e-mail a la construction : envoi autorise si
  // le type d'incident l'active ET l'interrupteur global de l'evenement l'autorise
  // (regle "superposee"). Ne s'applique qu'aux familles gouvernees par le flag type.
  private boolean emailAllowed(
    EmailNotificationEvent event,
    IncidentTypeConfig config
  ) {
    boolean typeFlag = config != null && config.isEmailNotificationsEnabled();
    return (
      typeFlag &&
      (event == null || emailNotificationConfig.isEmailEnabled(event))
    );
  }

  // VERROU UNIQUE de la politique e-mail Super Admin. Tout envoi e-mail DOIT
  // iterer sur cette methode plutot que sur recipients.emailUsers directement :
  //  - interrupteur global coupe  => aucun destinataire (master switch) ;
  //  - utilisateurs exclus retires => meme s'ils sont administrateurs.
  // Ainsi aucun chemin d'envoi ne peut contourner la configuration.
  private Collection<ExternalUser> emailRecipients(EventRecipients recipients) {
    if (
      recipients.event != null &&
      !emailNotificationConfig.isEmailEnabled(recipients.event)
    ) {
      return List.of();
    }
    Set<UUID> excluded = recipients.event != null
      ? emailNotificationConfig.excludedRecipients(recipients.event)
      : Set.of();
    if (excluded.isEmpty()) {
      return List.copyOf(recipients.emailUsers.values());
    }
    return recipients.emailUsers
      .values()
      .stream()
      .filter(user -> user.getId() == null || !excluded.contains(user.getId()))
      .toList();
  }

  // Le libelle du nouveau statut est resolu par langue depuis l'incident.
  private void dispatchStatusChangedEvent(
    Incident incident,
    UUID actionneurId,
    BilingualText comment,
    EventRecipients recipients,
    EmailNotificationEvent event
  ) {
    IncidentTypeConfig config = getIncidentTypeConfig(incident);
    boolean sendEmail = emailAllowed(event, config);

    recipients.addAdmins(userClientService, sendEmail);
    if (sendEmail) {
      recipients.internalUsers.values().forEach(recipients::addEmail);
    }

    ExternalUser actioner = safeGetUser(actionneurId);
    String resolvedActionerName = displayName(actioner);

    boolean hasComment = comment != null && StringUtils.hasText(comment.fr());
    Map<String, Object> extraParams = new HashMap<>();
    extraParams.put(
      "actioner_full_name",
      StringUtils.hasText(resolvedActionerName)
        ? resolvedActionerName
        : msg("notification.system.name")
    );
    if (hasComment) {
      extraParams.put("comment", comment.fr());
    }

    BilingualText subject = lmsgPer(
      "notification.incident.status_changed.subject",
      locale ->
        new Object[] {
          statusLabel(incident, locale),
          StringUtils.hasText(resolvedActionerName)
            ? resolvedActionerName
            : msgIn(locale, "notification.system.name"),
        }
    );

    for (ExternalUser user : recipients.internalUsers.values()) {
      send(
        "INTERNAL",
        user,
        user.getUsername(),
        incident,
        subject,
        statusChangedContent(
          incident,
          user,
          actionneurId,
          resolvedActionerName,
          hasComment,
          comment
        ),
        extraParams
      );
    }

    for (ExternalUser user : emailRecipients(recipients)) {
      send(
        "EMAIL",
        user,
        user.getEmail(),
        incident,
        subject,
        statusChangedContent(
          incident,
          user,
          actionneurId,
          resolvedActionerName,
          hasComment,
          comment
        ),
        extraParams
      );
    }
  }

  // Contenu du changement de statut, personnalise par role et par langue.
  private BilingualText statusChangedContent(
    Incident incident,
    ExternalUser user,
    UUID actionneurId,
    String resolvedActionerName,
    boolean hasComment,
    BilingualText comment
  ) {
    String roleKey = determineRoleKey(user, incident, actionneurId);
    String contentKey =
      "notification.incident.status_changed.content." +
      roleKey +
      (hasComment ? ".with_comment" : "");
    return lmsgPer(contentKey, locale -> {
      String actionerName = StringUtils.hasText(resolvedActionerName)
        ? resolvedActionerName
        : msgIn(locale, "notification.system.name");
      String localeComment = comment == null
        ? null
        : (Locale.FRENCH.getLanguage().equals(locale.getLanguage())
          ? comment.fr()
          : comment.en());
      return buildArgs(
        roleKey,
        hasComment,
        incident.getTitle(),
        statusLabel(incident, locale),
        actionerName,
        localeComment
      );
    });
  }

  // Envoie une affectation avec un contenu adapte au createur, au traitant ou a un administrateur.
  private void dispatchAssignedEvent(
    Incident incident,
    UUID assigneeId,
    EventRecipients recipients
  ) {
    IncidentTypeConfig config = getIncidentTypeConfig(incident);
    boolean sendEmail = emailAllowed(
      EmailNotificationEvent.INCIDENT_ASSIGNED,
      config
    );

    recipients.addAdmins(userClientService, sendEmail);
    if (sendEmail) {
      recipients.internalUsers.values().forEach(recipients::addEmail);
    }

    String resolvedAssigneeName = displayName(safeGetUser(assigneeId));
    Map<String, Object> extraParams = new HashMap<>();
    extraParams.put(
      "assignee_full_name",
      StringUtils.hasText(resolvedAssigneeName)
        ? resolvedAssigneeName
        : msg("notification.system.name")
    );
    BilingualText subject = lmsgPer(
      "notification.incident.assigned.subject",
      locale ->
        new Object[] {
          StringUtils.hasText(resolvedAssigneeName)
            ? resolvedAssigneeName
            : msgIn(locale, "notification.system.name"),
        }
    );

    for (ExternalUser user : recipients.internalUsers.values()) {
      String roleKey = determineRoleKey(user, incident, null);
      send(
        "INTERNAL",
        user,
        user.getUsername(),
        incident,
        subject,
        assignedContent(incident, roleKey, resolvedAssigneeName),
        extraParams
      );
    }

    for (ExternalUser user : emailRecipients(recipients)) {
      String roleKey = determineRoleKey(user, incident, null);
      send(
        "EMAIL",
        user,
        user.getEmail(),
        incident,
        subject,
        assignedContent(incident, roleKey, resolvedAssigneeName),
        extraParams
      );
    }
  }

  // Contenu de l'affectation, personnalise par role et par langue.
  private BilingualText assignedContent(
    Incident incident,
    String roleKey,
    String resolvedAssigneeName
  ) {
    String contentKey = "notification.incident.assigned.content." + roleKey;
    return lmsgPer(contentKey, locale -> {
      String assigneeName = StringUtils.hasText(resolvedAssigneeName)
        ? resolvedAssigneeName
        : msgIn(locale, "notification.system.name");
      return buildAssignedArgs(roleKey, incident.getTitle(), assigneeName);
    });
  }

  // Prepare les arguments de traduction de la notification d'affectation.
  private Object[] buildAssignedArgs(
    String roleKey,
    String title,
    String assigneeName
  ) {
    return "assignee".equals(roleKey)
      ? new Object[] { title }
      : new Object[] { title, assigneeName };
  }

  // Envoie la notification unique de soumission avec un texte adapte au destinataire.
  private void dispatchSubmittedEvent(
    Incident incident,
    UUID actionneurId,
    String comment,
    EventRecipients recipients,
    Map<UUID, String> recipientRoles
  ) {
    ExternalUser actioner = safeGetUser(actionneurId);
    String resolvedActionerName = displayName(actioner);

    boolean hasComment = StringUtils.hasText(comment);
    // Flux normal, mais l'urgence se lit des l'objet, sans ouvrir le message.
    boolean critical = incident.getCriticality() == Criticality.CRITICAL;
    BilingualText subject = lmsgPer(
      critical
        ? "notification.incident.submitted.subject_critical"
        : "notification.incident.submitted.subject",
      locale ->
        new Object[] {
          StringUtils.hasText(resolvedActionerName)
            ? resolvedActionerName
            : msgIn(locale, "notification.system.name"),
        }
    );
    Map<String, Object> extraParams = new HashMap<>();
    extraParams.put(
      "actioner_full_name",
      StringUtils.hasText(resolvedActionerName)
        ? resolvedActionerName
        : msg("notification.system.name")
    );
    if (hasComment) {
      extraParams.put("comment", comment);
    }

    for (ExternalUser user : recipients.internalUsers.values()) {
      String roleKey = recipientRoles.getOrDefault(user.getId(), "other");
      send(
        "INTERNAL",
        user,
        user.getUsername(),
        incident,
        subject,
        submittedContent(
          incident,
          roleKey,
          resolvedActionerName,
          hasComment,
          comment
        ),
        extraParams
      );
    }

    for (ExternalUser user : emailRecipients(recipients)) {
      String roleKey = recipientRoles.getOrDefault(user.getId(), "other");
      send(
        "EMAIL",
        user,
        user.getEmail(),
        incident,
        subject,
        submittedContent(
          incident,
          roleKey,
          resolvedActionerName,
          hasComment,
          comment
        ),
        extraParams
      );
    }
  }

  // Contenu de la soumission, personnalise par role et par langue.
  private BilingualText submittedContent(
    Incident incident,
    String roleKey,
    String resolvedActionerName,
    boolean hasComment,
    String comment
  ) {
    String contentKey =
      "notification.incident.submitted.content." +
      roleKey +
      (hasComment ? ".with_comment" : "");
    BilingualText base = lmsgPer(contentKey, locale -> {
      String statusLabel = statusLabel(incident, locale);
      String actionerName = StringUtils.hasText(resolvedActionerName)
        ? resolvedActionerName
        : msgIn(locale, "notification.system.name");
      if ("creator".equals(roleKey)) {
        return hasComment
          ? new Object[] { incident.getTitle(), statusLabel, comment }
          : new Object[] { incident.getTitle(), statusLabel };
      }
      return hasComment
        ? new Object[] {
          incident.getTitle(),
          statusLabel,
          actionerName,
          comment,
        }
        : new Object[] { incident.getTitle(), statusLabel, actionerName };
    });
    if (incident.getCriticality() != Criticality.CRITICAL) {
      return base;
    }
    return new BilingualText(
      base.fr() + " " + msgIn(Locale.FRENCH, "notification.incident.critical_notice"),
      base.en() + " " + msgIn(Locale.ENGLISH, "notification.incident.critical_notice")
    );
  }

  private String determineRoleKey(
    ExternalUser user,
    Incident incident,
    UUID actionneurId
  ) {
    if (
      user.getId() != null && user.getId().equals(actionneurId)
    ) return "self";
    if (
      incident.getCreatedBy() != null &&
      incident.getCreatedBy().equals(user.getId())
    ) return "creator";
    if (
      incident.getAssignedTo() != null &&
      incident.getAssignedTo().equals(user.getId())
    ) return "assignee";
    return "other";
  }

  private Object[] buildArgs(
    String roleKey,
    boolean hasComment,
    String title,
    String status,
    String actionerName,
    String comment
  ) {
    if ("self".equals(roleKey)) {
      return hasComment
        ? new Object[] { title, status, comment }
        : new Object[] { title, status };
    }
    return hasComment
      ? new Object[] { title, status, actionerName, comment }
      : new Object[] { title, status, actionerName };
  }

  // Diffuse un evenement en controlant l'ajout automatique des administrateurs.
  private void dispatchEvent(
    Incident incident,
    String subjectKey,
    String contentKey,
    Object[] contentArgs,
    EventRecipients recipients,
    boolean includeAdmins
  ) {
    dispatchEvent(
      incident,
      subjectKey,
      new Object[0],
      contentKey,
      locale -> contentArgs,
      recipients,
      includeAdmins
    );
  }

  // Variante avec des arguments propres a chaque langue (labels traduits).
  private void dispatchEvent(
    Incident incident,
    String subjectKey,
    Object[] subjectArgs,
    String contentKey,
    java.util.function.Function<Locale, Object[]> contentArgsFor,
    EventRecipients recipients,
    boolean includeAdmins
  ) {
    IncidentTypeConfig config = getIncidentTypeConfig(incident);
    // Le flag par type gouverne l'ampleur de l'audience e-mail ajoutee ici ;
    // l'interrupteur global et les exclusions sont appliques par le verrou unique
    // emailRecipients() au moment de l'envoi (y c. les destinataires pre-ajoutes,
    // ex. l'assigne d'un rappel SLA).
    boolean sendEmail = emailAllowed(recipients.event, config);

    if (includeAdmins) {
      recipients.addAdmins(userClientService, sendEmail);
    }
    if (sendEmail) {
      recipients.internalUsers.values().forEach(recipients::addEmail);
    }

    BilingualText subject = lmsg(subjectKey, subjectArgs);
    BilingualText content = lmsgPer(contentKey, contentArgsFor);

    for (ExternalUser user : recipients.internalUsers.values()) {
      send(
        "INTERNAL",
        user,
        user.getUsername(),
        incident,
        subject,
        content,
        null
      );
    }
    for (ExternalUser user : emailRecipients(recipients)) {
      send(
        "EMAIL",
        user,
        user.getEmail(),
        incident,
        subject,
        content,
        null
      );
    }
  }

  // Libelle du gabarit e-mail : rendu dans la langue de reference de la
  // plateforme, comme le corps expedie, pour ne pas melanger les langues.
  private String msg(String key, Object... args) {
    return messageSource.getMessage(key, args, key, Locale.FRENCH);
  }

  // Texte rendu dans les deux langues de l'interface ; le lecteur choisit.
  record BilingualText(String fr, String en) {}

  // Nom d'agence rendu dans les deux langues lorsque la reference est indisponible.
  record AgencyText(String fr, String en) {}

  private String msgIn(Locale locale, String key, Object... args) {
    return messageSource.getMessage(key, args, key, locale);
  }

  // Rend une cle en francais et en anglais avec les memes arguments.
  private BilingualText lmsg(String key, Object... args) {
    return new BilingualText(
      msgIn(Locale.FRENCH, key, args),
      msgIn(Locale.ENGLISH, key, args)
    );
  }

  // Rend une cle avec des arguments propres a chaque langue (labels traduits).
  private BilingualText lmsgPer(
    String key,
    java.util.function.Function<Locale, Object[]> argsFor
  ) {
    return new BilingualText(
      msgIn(Locale.FRENCH, key, argsFor.apply(Locale.FRENCH)),
      msgIn(Locale.ENGLISH, key, argsFor.apply(Locale.ENGLISH))
    );
  }

  // Libelle du statut courant de l'incident dans la langue demandee.
  private String statusLabel(Incident incident, Locale locale) {
    if (incident.getStatus() == null) return "";
    return msgIn(
      locale,
      incident.getStatus().getNameKey(),
      (Object[]) null
    );
  }

  // Fournit une lecture tolerante pour les donnees du domaine notification client.

  private ExternalUser safeGetUser(UUID id) {
    if (id == null) return null;
    try {
      return userClientService.getUser(id);
    } catch (RuntimeException ex) {
      log.warn(
        "Notification : impossible de résoudre l'utilisateur {}",
        id,
        ex
      );
      return null;
    }
  }

  // Resout l'agence de l'incident via le cache inter-services.
  private AgencyText resolveAgencyText(Incident incident) {
    if (incident != null && incident.getAgencyId() != null) {
      try {
        AgencySummaryResponse agency = userClientService.resolveAgency(
          incident.getAgencyId()
        );
        if (
          agency != null &&
          StringUtils.hasText(agency.getName()) &&
          !"unknown".equalsIgnoreCase(agency.getName())
        ) {
          return new AgencyText(agency.getName(), agency.getName());
        }
      } catch (RuntimeException ex) {
        log.warn(
          "Notification : impossible de resoudre l'agence {}",
          incident.getAgencyId(),
          ex
        );
      }
    }
    return new AgencyText(
      msgIn(Locale.FRENCH, "notification.agency.unknown"),
      msgIn(Locale.ENGLISH, "notification.agency.unknown")
    );
  }

  // Resout le service responsable ou indique que le traitement reste en agence.
  private BilingualText resolveServiceText(Incident incident) {
    UUID serviceId = incident != null ? getActiveServiceId(incident) : null;
    if (serviceId != null) {
      try {
        ServiceSummaryResponse service = userClientService.resolveService(
          serviceId
        );
        if (
          service != null &&
          StringUtils.hasText(service.getName()) &&
          !"unknown".equalsIgnoreCase(service.getName())
        ) {
          return new BilingualText(service.getName(), service.getName());
        }
      } catch (RuntimeException ex) {
        log.warn(
          "Notification : impossible de resoudre le service {}",
          serviceId,
          ex
        );
      }
    }
    return lmsg("notification.service.agency_processing");
  }

  // Libelle de criticite de l'incident dans la langue demandee.
  private String criticalityLabel(Incident incident, Locale locale) {
    if (incident.getCriticality() == null) return "";
    return msgIn(
      locale,
      incident.getCriticality().getNameKey(),
      (Object[]) null
    );
  }

  // Encadre une valeur de cellule CSV et echappe les guillemets internes.
  private static String csvCell(String value) {
    String safe = value != null ? value.replace("\"", "\"\"") : "";
    return "\"" + safe + "\"";
  }

  // Nom lisible de l'assigne pour le rapport, ou mention explicite si absent.
  private String assigneeName(Incident incident) {
    if (incident.getAssignedTo() == null) return "Non assigné";
    ExternalUser user = safeGetUser(incident.getAssignedTo());
    if (user == null) return "Non assigné";
    String fullName = (
      (user.getFirstName() != null ? user.getFirstName() : "") +
      " " +
      (user.getLastName() != null ? user.getLastName() : "")
    ).trim();
    if (StringUtils.hasText(fullName)) return fullName;
    return StringUtils.hasText(user.getUsername())
      ? user.getUsername()
      : "Non assigné";
  }

  // Formate une date pour le CSV (dd/MM/yyyy HH:mm), vide si absente.
  private static String formatCsvDate(LocalDateTime dateTime) {
    return dateTime != null ? dateTime.format(CSV_DATE_FORMAT) : "";
  }

  // Complete un sujet avec l'incident et son agence.
  private BilingualText contextualizeSubject(
    BilingualText subject,
    Incident incident,
    AgencyText agency
  ) {
    if (incident == null) {
      return subject;
    }
    return new BilingualText(
      msgIn(
        Locale.FRENCH,
        "notification.incident.subject.context",
        subject.fr(),
        incident.getTitle(),
        agency.fr()
      ),
      msgIn(
        Locale.ENGLISH,
        "notification.incident.subject.context",
        subject.en(),
        incident.getTitle(),
        agency.en()
      )
    );
  }

  // Diffuse l'information du domaine notification client aux destinataires concernes.

  private void send(
    String type,
    ExternalUser user,
    String recipient,
    Incident incident,
    BilingualText subject,
    BilingualText content,
    Map<String, Object> extraParams
  ) {
    if (!StringUtils.hasText(recipient)) return;
    try {
      Map<String, Object> params = new HashMap<>();
      AgencyText agency = resolveAgencyText(incident);
      BilingualText contextualSubject = contextualizeSubject(
        subject,
        incident,
        agency
      );
      if ("EMAIL".equalsIgnoreCase(type)) {
        params.put("email_template", "incident_notification");
      }
      if (user != null) {
        params.put(
          "firstName",
          user.getFirstName() != null ? user.getFirstName() : ""
        );
        params.put(
          "name",
          user.getLastName() != null ? user.getLastName() : user.getUsername()
        );
        addUserParams(params, "recipient", user);
      }
      if (incident != null) {
        BilingualText service = resolveServiceText(incident);
        params.put(
          "agency_id",
          incident.getAgencyId() != null ? incident.getAgencyId().toString() : ""
        );
        params.put("agency_name", agency.fr());
        params.put("agency_name_en", agency.en());
        params.put("incident_agency", agency.fr());
        params.put("incident_agency_en", agency.en());
        params.put("incident_service", service.fr());
        params.put("incident_service_en", service.en());
        // Le code metier (FT-I-2026-0001) identifie l'incident partout : e-mails,
        // notifications internes, rapports. Repli sur l'UUID abrege tant qu'un
        // ancien incident n'a pas encore recu sa reference.
        params.put(
          "incident_reference",
          StringUtils.hasText(incident.getReference())
            ? incident.getReference()
            : (incident.getId() != null
              ? incident.getId().toString().substring(0, 8).toUpperCase()
              : "")
        );
        params.put("incident_title", incident.getTitle());
        params.put(
          "incident_status_code",
          incident.getStatus() != null ? incident.getStatus().name() : ""
        );
        params.put(
          "incident_status",
          statusLabel(incident, Locale.FRENCH)
        );
        params.put(
          "incident_status_en",
          statusLabel(incident, Locale.ENGLISH)
        );
        params.put(
          "incident_criticality",
          criticalityLabel(incident, Locale.FRENCH)
        );
        params.put(
          "incident_criticality_en",
          criticalityLabel(incident, Locale.ENGLISH)
        );
        IncidentTypeConfig typeConfig = getIncidentTypeConfig(incident);
        params.put(
          "incident_type",
          typeConfig != null && typeConfig.getDisplayName() != null
            ? typeConfig.getDisplayName()
            : (incident.getTypeId() != null ? incident.getTypeId().toString() : "")
        );
        params.put(
          "incident_url",
          frontendUrl + "/dashboard/incidents/" + incident.getId()
        );
        params.put("display_incident_details", "block");
        params.put("cta_label", msg("notification.cta.view_incident"));
        addIncidentUsers(params, incident, user);
      }
      if (extraParams != null) {
        params.putAll(extraParams);
      }

      notificationClient.createNotification(
        NotificationClientRequest.builder()
          .type(type)
          .recipient(recipient)
          .subject(contextualSubject.fr())
          .content(content.fr())
          .subjectEn(contextualSubject.en())
          .contentEn(content.en())
          .incidentId(incident != null ? incident.getId() : null)
          .idempotencyKey(
            buildNotificationIdempotencyKey(
              type,
              recipient,
              incident,
              content.fr()
            )
          )
          .templateParams(params)
          .build()
      );
    } catch (Exception e) {
      log.error(
        "Échec d'envoi de {} notification à {}: {}",
        type,
        recipient,
        e.getMessage()
      );
    }
  }

  // Ajoute les personnes concernees par l'incident dans les parametres de notification.
  private void addIncidentUsers(
    Map<String, Object> params,
    Incident incident,
    ExternalUser recipient
  ) {
    ExternalUser creator = safeGetUser(incident.getCreatedBy());
    ExternalUser assignee = safeGetUser(incident.getAssignedTo());

    addUserParams(params, "creator", creator);
    addUserParams(params, "assignee", assignee);

    List<Map<String, Object>> concernedUsers = new ArrayList<>();
    addConcernedUser(concernedUsers, "recipient", recipient);
    addConcernedUser(concernedUsers, "creator", creator);
    addConcernedUser(concernedUsers, "assignee", assignee);
    params.put("concerned_users", concernedUsers);
  }

  // Ajoute les attributs affichables d'un utilisateur sous un prefixe stable.
  private void addUserParams(
    Map<String, Object> params,
    String prefix,
    ExternalUser user
  ) {
    if (user == null) {
      return;
    }
    params.put(
      prefix + "_id",
      user.getId() != null ? user.getId().toString() : ""
    );
    params.put(
      prefix + "_username",
      user.getUsername() != null ? user.getUsername() : ""
    );
    params.put(prefix + "_full_name", displayName(user));
    params.put(
      prefix + "_email",
      user.getEmail() != null ? user.getEmail() : ""
    );
    params.put(
      prefix + "_roles",
      user.getRoles() != null ? user.getRoles() : Set.of()
    );
  }

  // Ajoute une personne concernee sans dupliquer le meme utilisateur.
  private void addConcernedUser(
    List<Map<String, Object>> users,
    String role,
    ExternalUser user
  ) {
    if (user == null || user.getId() == null) {
      return;
    }
    boolean alreadyPresent = users
      .stream()
      .anyMatch(existing -> user.getId().toString().equals(existing.get("id")));
    if (alreadyPresent) {
      return;
    }

    Map<String, Object> item = new LinkedHashMap<>();
    item.put("role", role);
    item.put("id", user.getId().toString());
    item.put("username", user.getUsername() != null ? user.getUsername() : "");
    item.put("fullName", displayName(user));
    item.put("email", user.getEmail() != null ? user.getEmail() : "");
    item.put("roles", user.getRoles() != null ? user.getRoles() : Set.of());
    users.add(item);
  }

  // Construit un libelle utilisateur lisible pour les notifications.
  private String displayName(ExternalUser user) {
    if (user == null) {
      return "";
    }
    String firstName =
      user.getFirstName() != null ? user.getFirstName().trim() : "";
    String lastName =
      user.getLastName() != null ? user.getLastName().trim() : "";
    String fullName = (firstName + " " + lastName).trim();
    return StringUtils.hasText(fullName) ? fullName : user.getUsername();
  }

  // Construit une cle stable pour eviter les doublons techniques d'un meme evenement incident.
  private String buildNotificationIdempotencyKey(
    String type,
    String recipient,
    Incident incident,
    String content
  ) {
    if (incident == null || incident.getId() == null) {
      return null;
    }
    String eventVersion = incident.getUpdatedAt() != null
      ? incident.getUpdatedAt().toString()
      : incident.getCreatedAt() != null
        ? incident.getCreatedAt().toString()
        : "";
    String fingerprint = sha256(
      String.join(
        "|",
        Objects.toString(incident.getStatus(), ""),
        Objects.toString(incident.getAssignedTo(), ""),
        Objects.toString(incident.getTransferredToService(), ""),
        Objects.toString(eventVersion, ""),
        Objects.toString(content, "")
      )
    );
    return String.join(
      ":",
      "incident-service",
      Objects.toString(type, ""),
      incident.getId().toString(),
      recipient,
      fingerprint
    );
  }

  // Calcule une empreinte compacte et deterministe pour la cle d'idempotence.
  private String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 indisponible", e);
    }
  }

  private IncidentTypeConfig getIncidentTypeConfig(Incident incident) {
    if (incident == null || incident.getTypeId() == null) {
      return null;
    }
    try {
      return incidentTypeConfigService.findById(incident.getTypeId());
    } catch (Exception e) {
      log.warn("Impossible de recuperer la configuration du type d'incident {}", incident.getTypeId(), e);
      return null;
    }
  }
}
