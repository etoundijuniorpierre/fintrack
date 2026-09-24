// Contrat metier : expose les operations du domaine incident.

package com.fintrack.incident.service;

import com.fintrack.incident.model.constant.IncidentCause;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentSearchCriteria;
import com.fintrack.incident.security.UserDetailsImpl;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// Interface du service gerant le cycle de vie et le workflow des incidents.
public interface IncidentService {
  // Liste les elements du domaine incident.
  Page<Incident> findAll(Pageable pageable);
  // Recherche les incidents pour filtered.
  Page<Incident> findFiltered(
    IncidentSearchCriteria searchCriteria,
    UserDetailsImpl currentUser,
    Pageable pageable
  );
  // Recherche les incidents par agence identifiant.
  List<Incident> findByAgencyId(UUID agencyId);
  // Recherche les incidents par creation by.
  List<Incident> findByCreatedBy(UUID userId);
  // Recherche les incidents par assigne fin.
  List<Incident> findByAssignedTo(UUID userId);
  // Recherche les incidents par identifiant.
  Incident findById(UUID id);

  // Recherche un incident par son code metier.
  Incident findByReference(String reference);

  // Prepare l'ajout de incident apres validation metier.
  Incident create(
    Incident incident,
    UUID createdBy,
    UUID agencyId,
    boolean requiresCreatorValidation,
    UUID requestedTargetServiceId,
    boolean assignToSelf
  );
  // Applique le changement demande apres validation metier.

  Incident update(UUID id, Incident details, UUID requestingUserId);
  // Supprime ou invalide les donnees ciblees apres controle metier.
  void delete(UUID id, UUID deletedBy);
  // Verifie que les regles metier autorisent l'operation sur incident.
  Incident validate(
    UUID id,
    UUID validatedBy,
    String comment,
    UUID targetServiceId,
    UUID targetUserId,
    Set<String> validatorPermissions
  );
  // Refuse un incident avec le motif metier fourni.
  Incident reject(UUID id, UUID rejectedBy, String reason, String comment);
  // Transfere un incident vers le service ou l'agence cible apres validation des droits.
  Incident transfer(
    UUID id,
    UUID transferredBy,
    UUID targetServiceId,
    UUID targetAgencyId,
    UUID newTypeId,
    String reason,
    String comment,
    Set<String> requesterPermissions
  );
  // Applique le changement demande apres validation metier.
  Incident assign(UUID id, UUID assignedBy, UUID assignedTo, String comment);
  // Realise l'intention metier start progress. Le traitant peut estimer le temps
  // de resolution (en heures) a la prise en charge ; s'il est renseigne il pilote le SLA.
  Incident startProgress(
    UUID id,
    UUID startedBy,
    String comment,
    Integer estimatedResolutionHours
  );
  // Realise l'intention metier block.
  Incident block(UUID id, UUID blockedBy, String reason, String comment);
  // Realise l'intention metier resume.
  Incident resume(UUID id, UUID resumedBy, String comment);
  // Attente prolongee : le traitant reprend l'incident et interroge l'entite source
  // sur son actualite. Le statut ne bouge pas tant qu'elle n'a pas repondu.
  Incident requestConfirmation(UUID id, UUID requestedBy, String comment);
  // L'entite source confirme : l'incident repart en traitement, echeance remise a neuf,
  // toutes ses donnees conservees. L'infirmation passe par cancel.
  Incident confirmRelevance(UUID id, UUID confirmedBy, String comment);
  // Le traitant clot son traitement : IN_PROGRESS -> TREATED (cause optionnelle, imposee selon le type).
  Incident treat(
    UUID id,
    UUID treatedBy,
    String treatmentDescription,
    IncidentCause cause,
    String causeDetail
  );
  // Le valideur confirme la resolution : TREATED -> RESOLVED (note obligatoire).
  Incident resolve(UUID id, UUID validatedBy, String resolutionNote);
  // Le valideur juge la resolution insuffisante : TREATED -> IN_PROGRESS (motif obligatoire).
  Incident markUnresolved(UUID id, UUID validatedBy, String reason);
  // Realise l'intention metier close.
  Incident close(
    UUID id,
    UUID closedBy,
    String closureDescription,
    String comment
  );
  // Verifie que les regles metier autorisent l operation sur incident.
  Incident cancel(UUID id, UUID canceledBy, String reason);
  // Realise l'intention metier reopen.
  Incident reopen(UUID id, UUID reopenedBy, String reason, String comment);
  // Realise l'intention metier clone incident.
  Incident cloneIncident(UUID id, UUID clonedBy);
  // Realise l'intention metier resubmit.
  Incident resubmit(UUID id, UUID submittedBy, String comment, boolean requiresCreatorValidation);

  Incident submitSolution(
    UUID id,
    String proposedSolution,
    Integer estimatedResolutionHours,
    UserDetailsImpl actor
  );
  Incident validateDirection(UUID id, UserDetailsImpl actor);
  Incident rejectDirection(UUID id, String rejectionReason, UserDetailsImpl actor);
}
