// Contrat metier : expose les operations du domaine metadonnees de piece jointe.

package com.fintrack.document.service;

import com.fintrack.document.model.entity.AttachmentMetadata;
import com.fintrack.document.model.entity.AttachmentUpload;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// Contrat du service de gestion des metadonnees et fichiers des pieces jointes.
public interface AttachmentMetadataService {
  // Liste les pieces jointes selon le perimetre demande.
  Page<AttachmentMetadata> findAll(Pageable pageable);
  // Liste les pieces jointes selon le perimetre demande.
  List<AttachmentMetadata> findAll();
  // Recherche les pieces jointes par identifiant.
  AttachmentMetadata findById(UUID id);
  // Recherche les pieces jointes par incident identifiant.
  List<AttachmentMetadata> findByIncidentId(UUID incidentId);
  // Recherche les pieces jointes par uploaded by.
  List<AttachmentMetadata> findByUploadedBy(UUID uploadedBy);
  // Prepare l'enregistrement de la ressource selon les regles metier.
  AttachmentMetadata create(AttachmentMetadata attachment);
  // Applique le changement demande apres validation metier.
  AttachmentMetadata update(UUID id, AttachmentMetadata attachment);
  // Supprime ou invalide les donnees ciblees apres controle metier.
  void delete(UUID id);
  // Supprime ou invalide les donnees ciblees apres controle metier.
  long deleteByIncidentId(UUID incidentId);

  /** Sauvegarde le fichier sur disque et enregistre les métadonnées en base. */
  // Enregistre une piece jointe d'incident apres stockage du fichier.
  AttachmentMetadata upload(AttachmentUpload upload);

  // Enregistre l'avatar d'un utilisateur apres stockage du fichier.
  AttachmentMetadata uploadUserAvatar(AttachmentUpload upload);

  /** Retourne le fichier sous forme de Resource pour téléchargement. */
  // Prepare le telechargement de piece jointe autorise.
  Resource download(UUID id);
}
