// Contrat metier : expose les operations du domaine incident comment.

package com.fintrack.incident.service;

import com.fintrack.incident.model.entity.IncidentComment;
import java.util.List;
import java.util.UUID;

// Interface du service de gestion des commentaires d'incidents.

public interface IncidentCommentService {
  void deleteCommentIfEmpty(UUID incidentId, UUID commentId, UUID userId);
  void assertCanModifyAttachments(UUID incidentId, UUID commentId, UUID userId);
  // Recherche les commentaires incident par incident identifiant.
  List<IncidentComment> findByIncidentId(UUID incidentId);
  // Recherche les commentaires incident pour public by incident identifiant.
  List<IncidentComment> findPublicByIncidentId(UUID incidentId);
  // Recherche les commentaires incident par identifiant.
  IncidentComment findById(UUID id);
  // Cree un element du domaine incident comment apres validation metier.
  IncidentComment addComment(
    UUID incidentId,
    UUID userId,
    String content,
    boolean isInternal
  );
  // Cree un element du domaine incident comment apres validation metier.
  IncidentComment addComment(
    UUID incidentId,
    UUID userId,
    String content,
    boolean isInternal,
    UUID parentCommentId
  );
  // Modifie un commentaire d'incident apres validation metier.
  IncidentComment updateComment(
    UUID incidentId,
    UUID commentId,
    UUID userId,
    String content
  );
  // Supprime un commentaire d'incident apres validation metier.
  void deleteComment(
    UUID incidentId,
    UUID commentId,
    UUID userId
  );
}
