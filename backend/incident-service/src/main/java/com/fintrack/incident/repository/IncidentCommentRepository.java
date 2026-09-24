// Acces aux donnees : expose les requetes persistantes liees a incident comment.

package com.fintrack.incident.repository;

import com.fintrack.incident.model.entity.IncidentComment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Repertoire Spring Data JPA pour l'acces aux commentaires d'incidents.

@Repository
public interface IncidentCommentRepository
  extends JpaRepository<IncidentComment, UUID>
{
  // Recherche les commentaires incident par incident identifiant order by creation at asc.

  List<IncidentComment> findByIncidentIdOrderByCreatedAtAsc(UUID incidentId);
  // Recherche les commentaires incident par incident identifiant and is internal order by creation at asc.

  List<IncidentComment> findByIncidentIdAndIsInternalOrderByCreatedAtAsc(
    UUID incidentId,
    boolean isInternal
  );

  // Indique si le commentaire a recu au moins une reponse.
  boolean existsByParentCommentId(UUID parentCommentId);

  // Indique si un autre utilisateur a commente l'incident apres cette date.
  boolean existsByIncidentIdAndCreatedAtAfterAndUserIdNot(
    UUID incidentId,
    LocalDateTime createdAt,
    UUID userId
  );
}
