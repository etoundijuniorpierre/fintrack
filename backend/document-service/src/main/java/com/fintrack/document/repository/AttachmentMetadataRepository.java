// Acces aux donnees : expose les requetes persistantes liees a attachment metadata.

package com.fintrack.document.repository;

import com.fintrack.document.model.entity.AttachmentMetadata;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Repository d'acces aux metadonnees des pieces jointes en base de donnees.
@Repository
public interface AttachmentMetadataRepository
  extends JpaRepository<AttachmentMetadata, UUID>
{
  // Recherche les pieces jointes par incident identifiant.
  List<AttachmentMetadata> findByIncidentId(UUID incidentId);
  List<AttachmentMetadata> findByIncidentIdAndCommentId(UUID incidentId, UUID commentId);
  boolean existsByStoragePath(String storagePath);
  // Recherche les pieces jointes par uploaded by.
  List<AttachmentMetadata> findByUploadedBy(UUID uploadedBy);
  // Supprime ou invalide les donnees ciblees apres controle metier.
  long deleteByIncidentId(UUID incidentId);
}
