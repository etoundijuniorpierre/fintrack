// Service metier : implementation du stockage de fichiers via MinIO.

package com.fintrack.document.service.impl;

import com.fintrack.document.exception.FileStorageException;
import com.fintrack.document.model.StoredFile;
import com.fintrack.document.model.entity.AttachmentUpload;
import com.fintrack.document.service.FileStorageService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.net.URI;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

// Implementation du service de stockage utilisant MinIO.
// Gere le stockage, le chargement et la suppression dans un bucket prive MinIO.
@Slf4j
@Service
@ConditionalOnProperty(
  name = "document.storage.type",
  havingValue = "minio",
  matchIfMissing = true
)
public class MinioFileStorageService implements FileStorageService {

  private final MinioClient minioClient;
  private final String bucketName;

  public MinioFileStorageService(
    @Value(
      "${document.storage.minio.endpoint:http://minio:9000}"
    ) String endpoint,
    @Value("${document.storage.minio.access-key:minioadmin}") String accessKey,
    @Value("${document.storage.minio.secret-key:minioadmin}") String secretKey,
    @Value(
      "${document.storage.minio.bucket:fintrack-documents}"
    ) String bucketName,
    @Value("${document.storage.minio.secure:false}") boolean secure
  ) {
    this.bucketName = bucketName;
    this.minioClient = buildClient(endpoint, accessKey, secretKey, secure);
  }

  // Initialise le bucket sur MinIO s'il n'existe pas deja.
  @PostConstruct
  public void init() {
    try {
      boolean found = minioClient.bucketExists(
        BucketExistsArgs.builder().bucket(bucketName).build()
      );
      if (!found) {
        minioClient.makeBucket(
          MakeBucketArgs.builder().bucket(bucketName).build()
        );
        log.info("Bucket MinIO '{}' créé avec succès.", bucketName);
      } else {
        log.info("Bucket MinIO '{}' déjà existant.", bucketName);
      }
    } catch (Exception e) {
      log.warn(
        "Impossible de vérifier ou créer le bucket MinIO '{}' lors de l'initialisation : {}",
        bucketName,
        e.getMessage()
      );
    }
  }

  // Stocke le fichier dans MinIO et retourne les metadonnees physiques.
  @Override
  public StoredFile store(AttachmentUpload upload, String objectKey) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      try (
        InputStream is = upload.getContent();
        DigestInputStream dis = new DigestInputStream(is, md)
      ) {
        minioClient.putObject(
          PutObjectArgs.builder()
            .bucket(bucketName)
            .object(objectKey)
            .stream(dis, upload.getSize(), -1)
            .contentType(upload.getContentType())
            .build()
        );

        String checksum = bytesToHex(md.digest());
        log.info(
          "Fichier stocké avec succès dans MinIO bucket '{}' sous la clé '{}'",
          bucketName,
          objectKey
        );

        return StoredFile.builder()
          .objectKey(objectKey)
          .size(upload.getSize())
          .checksum(checksum)
          .build();
      }
    } catch (Exception e) {
      throw new FileStorageException(
        "Échec de l'écriture du fichier dans MinIO : " + e.getMessage(),
        e
      );
    }
  }

  // Charge le fichier depuis MinIO sous forme de Resource.
  @Override
  public Resource load(String objectKey) {
    try {
      InputStream stream = minioClient.getObject(
        GetObjectArgs.builder().bucket(bucketName).object(objectKey).build()
      );
      return new InputStreamResource(stream) {
        @Override
        public String getFilename() {
          if (objectKey.contains("/")) {
            return objectKey.substring(objectKey.lastIndexOf('/') + 1);
          }
          return objectKey;
        }

        @Override
        public long contentLength() {
          try {
            return minioClient
              .statObject(
                StatObjectArgs.builder()
                  .bucket(bucketName)
                  .object(objectKey)
                  .build()
              )
              .size();
          } catch (Exception e) {
            return -1;
          }
        }
      };
    } catch (Exception e) {
      throw new FileStorageException(
        "Échec de lecture du fichier depuis MinIO : " + e.getMessage(),
        e
      );
    }
  }

  // Supprime l'objet du bucket MinIO.
  @Override
  public void delete(String objectKey) {
    try {
      minioClient.removeObject(
        RemoveObjectArgs.builder().bucket(bucketName).object(objectKey).build()
      );
      log.info("Objet supprimé de MinIO: {}", objectKey);
    } catch (Exception e) {
      throw new FileStorageException("Echec de suppression de l'objet MinIO", e);
    }
  }

  private String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  // Construit le client MinIO depuis une URL complete ou depuis un hote configure avec le flag TLS.
  private MinioClient buildClient(
    String endpoint,
    String accessKey,
    String secretKey,
    boolean secure
  ) {
    MinioClient.Builder builder = MinioClient.builder().credentials(
      accessKey,
      secretKey
    );
    if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
      return builder.endpoint(endpoint).build();
    }

    URI uri = URI.create((secure ? "https://" : "http://") + endpoint);
    int port = uri.getPort() > 0 ? uri.getPort() : secure ? 443 : 9000;
    return builder.endpoint(uri.getHost(), port, secure).build();
  }
}
