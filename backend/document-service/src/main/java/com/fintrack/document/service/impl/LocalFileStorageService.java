// Service metier : implementation locale du stockage de fichiers sur disque.

package com.fintrack.document.service.impl;

import com.fintrack.document.exception.FileStorageException;
import com.fintrack.document.model.StoredFile;
import com.fintrack.document.model.entity.AttachmentUpload;
import com.fintrack.document.service.FileStorageService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

// Implementation locale de stockage des fichiers sur disque.
// Utilise comme fallback ou pour le developpement en local sans MinIO.
@Slf4j
@Service
@ConditionalOnProperty(name = "document.storage.type", havingValue = "local")
public class LocalFileStorageService implements FileStorageService {

  @Value("${document.storage.base-path:/data/fintrack/attachments}")
  private String basePath;

  // Stocke le fichier sur disque et retourne les metadonnees physiques.
  @Override
  public StoredFile store(AttachmentUpload upload, String objectKey) {
    Path filePath = resolveWithinBase(objectKey);
    try {
      Files.createDirectories(filePath.getParent());

      MessageDigest md = MessageDigest.getInstance("SHA-256");
      try (
        InputStream is = upload.getContent();
        DigestInputStream dis = new DigestInputStream(is, md)
      ) {
        Files.copy(dis, filePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Fichier stocké localement au chemin: {}", filePath);

        long size = Files.size(filePath);
        String checksum = bytesToHex(md.digest());

        return StoredFile.builder()
          .objectKey(objectKey)
          .size(size)
          .checksum(checksum)
          .build();
      }
    } catch (IOException | NoSuchAlgorithmException e) {
      throw new FileStorageException(
        "Échec de stockage du fichier localement : " + e.getMessage(),
        e
      );
    }
  }

  // Charge le fichier depuis le disque sous forme de Resource.
  @Override
  public Resource load(String objectKey) {
    Path filePath = resolveWithinBase(objectKey);
    try {
      Resource resource = new UrlResource(filePath.toUri());
      if (!resource.exists() || !resource.isReadable()) {
        throw new FileStorageException(
          "Fichier local introuvable ou illisible : " + objectKey
        );
      }
      return resource;
    } catch (IOException e) {
      throw new FileStorageException(
        "Impossible de lire le fichier local : " + e.getMessage(),
        e
      );
    }
  }

  // Supprime le fichier du disque local.
  @Override
  public void delete(String objectKey) {
    try {
      Path filePath = resolveWithinBase(objectKey);
      boolean deleted = Files.deleteIfExists(filePath);
      if (deleted) {
        log.info("Fichier local supprimé: {}", filePath);
      } else {
        log.warn(
          "Fichier local introuvable lors de la suppression: {}",
          filePath
        );
      }
    } catch (IOException e) {
      throw new FileStorageException("Echec de suppression du fichier local", e);
    }
  }

  // Resout un chemin de stockage en garantissant qu'il reste confine dans basePath.
  private Path resolveWithinBase(String storagePath) {
    Path base = Paths.get(basePath).toAbsolutePath().normalize();
    Path resolved = base.resolve(storagePath).normalize();
    if (!resolved.startsWith(base)) {
      throw new FileStorageException(
        "Chemin de stockage invalide : " + storagePath
      );
    }
    return resolved;
  }

  private String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
