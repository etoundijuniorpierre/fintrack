package com.fintrack.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fintrack.document.TestcontainersConfiguration;
import com.fintrack.document.model.StoredFile;
import com.fintrack.document.model.entity.AttachmentUpload;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@Import(TestcontainersConfiguration.class)
class MinioIntegrationTest {

  @Container
  @SuppressWarnings("resource")
  static GenericContainer<?> minio = new GenericContainer<>(
    "minio/minio:RELEASE.2023-11-20T22-40-07Z"
  )
    .withEnv("MINIO_ROOT_USER", "minioadmin")
    .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
    .withCommand("server /data")
    .withExposedPorts(9000);

  @DynamicPropertySource
  static void configureMinio(DynamicPropertyRegistry registry) {
    String endpoint =
      "http://" + minio.getHost() + ":" + minio.getMappedPort(9000);
    registry.add("document.storage.type", () -> "minio");
    registry.add("document.storage.minio.endpoint", () -> endpoint);
    registry.add("document.storage.minio.access-key", () -> "minioadmin");
    registry.add("document.storage.minio.secret-key", () -> "minioadmin");
    registry.add("document.storage.minio.bucket", () -> "fintrack-test-bucket");
  }

  @Autowired
  private FileStorageService fileStorageService;

  @Test
  @DisplayName("Upload, load and delete roundtrip in MinIO Testcontainer")
  void uploadLoadDeleteRoundtrip() throws Exception {
    String objectKey = "incidents/test-integration.txt";
    String contentText = "Hello MinIO Integration Test!";
    byte[] bytes = contentText.getBytes(StandardCharsets.UTF_8);

    AttachmentUpload upload = new AttachmentUpload();
    upload.setOriginalFilename("test-integration.txt");
    upload.setContentType("text/plain");
    upload.setSize((long) bytes.length);
    upload.setContent(new ByteArrayInputStream(bytes));

    StoredFile storedFile = fileStorageService.store(upload, objectKey);

    assertThat(storedFile).isNotNull();
    assertThat(storedFile.getObjectKey()).isEqualTo(objectKey);
    assertThat(storedFile.getSize()).isEqualTo(bytes.length);
    assertThat(storedFile.getChecksum()).isNotEmpty();

    Resource resource = fileStorageService.load(objectKey);

    assertThat(resource).isNotNull();
    assertThat(resource.getFilename()).isEqualTo("test-integration.txt");
    try (InputStream is = resource.getInputStream()) {
      String loadedContent = new String(
        is.readAllBytes(),
        StandardCharsets.UTF_8
      );
      assertThat(loadedContent).isEqualTo(contentText);
    }

    fileStorageService.delete(objectKey);

    assertThatThrownBy(() ->
      fileStorageService.load(objectKey).getInputStream()
    ).isNotNull();
  }
}
