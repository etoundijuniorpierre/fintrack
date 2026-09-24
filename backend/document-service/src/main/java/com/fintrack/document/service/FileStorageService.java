// Service metier : definit l'abstraction de stockage physique des fichiers.

package com.fintrack.document.service;

import com.fintrack.document.model.StoredFile;
import com.fintrack.document.model.entity.AttachmentUpload;
import org.springframework.core.io.Resource;

// Interface d'abstraction pour le stockage des fichiers physiques.
// Permet de basculer de maniere transparente entre un stockage local et un stockage objet (MinIO).
public interface FileStorageService {
  StoredFile store(AttachmentUpload upload, String objectKey);
  Resource load(String objectKey);
  void delete(String objectKey);
}
