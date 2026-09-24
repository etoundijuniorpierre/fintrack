package com.fintrack.document.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fintrack.document.model.StoredFile;
import com.fintrack.document.model.entity.AttachmentUpload;
import com.fintrack.document.repository.*;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@DataJpaTest(properties = {
  "spring.test.database.replace=NONE", "spring.flyway.enabled=true",
  "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
  "spring.datasource.driver-class-name=org.postgresql.Driver", "spring.jpa.show-sql=false"
})
@Import({AttachmentUploadService.class, AttachmentUploadCommitService.class, AttachmentContentValidator.class,
  AttachmentScopeLock.class, AttachmentStorageCleanupService.class, AttachmentDeletionService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers(disabledWithoutDocker = true)
class AttachmentTransactionsTest {
  @Container static final PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:17"));
  @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }
  @Autowired AttachmentUploadService uploads;
  @Autowired AttachmentDeletionService deletion;
  @Autowired AttachmentStorageCleanupService cleanup;
  @Autowired AttachmentMetadataRepository metadata;
  @Autowired AttachmentStorageCleanupRepository jobs;
  @MockitoBean FileStorageService storage;
  final Map<String, byte[]> objects = new ConcurrentHashMap<>();
  final UUID incident = UUID.randomUUID(), user = UUID.randomUUID(), comment = UUID.randomUUID();
  @BeforeEach void setupStorage() {
    when(storage.store(any(), anyString())).thenAnswer(invocation -> {
      AttachmentUpload upload = invocation.getArgument(0);
      String key = invocation.getArgument(1);
      byte[] bytes = upload.getContent().readAllBytes();
      objects.put(key, bytes);
      return StoredFile.builder().size(bytes.length).objectKey(key)
        .checksum(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))).build();
    });
    doAnswer(i -> { objects.remove(i.getArgument(0)); return null; }).when(storage).delete(anyString());
  }
  AttachmentUpload upload(UUID uploadId) {
    byte[] bytes = "%PDF-1.7\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    return AttachmentUpload.builder().originalFilename("proof.pdf").contentType("application/pdf")
      .size(bytes.length).content(new ByteArrayInputStream(bytes)).incidentId(incident).commentId(comment)
      .category("COMMENT").uploadedBy(user).uploadId(uploadId).build();
  }
  @Test void concurrentRetriesCommitOneObjectAndOneMetadataRow() throws Exception {
    UUID requestId = UUID.randomUUID();
    try (var pool = Executors.newFixedThreadPool(2)) {
      var start = new CountDownLatch(1);
      var one = pool.submit(() -> { start.await(); return uploads.upload(upload(requestId), false); });
      var two = pool.submit(() -> { start.await(); return uploads.upload(upload(requestId), false); });
      start.countDown();
      assertThat(one.get(30, TimeUnit.SECONDS).getId()).isEqualTo(two.get(30, TimeUnit.SECONDS).getId());
    }
    assertThat(metadata.findByIncidentId(incident)).hasSize(1);
    verify(storage, times(1)).store(any(), anyString());
  }
  @Test void rollbackKeepsCleanupJournalUntilPhysicalObjectIsRemoved() {
    var invalid = upload(UUID.randomUUID());
    invalid.setCategory("x".repeat(51));
    assertThatThrownBy(() -> uploads.upload(invalid, false)).isInstanceOf(RuntimeException.class);
    assertThat(metadata.findByIncidentId(incident)).isEmpty();
    var pending = jobs.findAll().stream().filter(t -> t.getObjectKey().contains(incident.toString())).toList();
    assertThat(pending).hasSize(1);
    assertThat(objects).hasSize(1);
    var task = pending.getFirst();
    task.setRetryAfter(LocalDateTime.now().minusMinutes(1)); jobs.saveAndFlush(task);
    cleanup.process(task.getId());
    assertThat(objects).isEmpty();
    assertThat(jobs.existsById(task.getId())).isFalse();
  }
  @Test void sealingDoesNotDiscardCommittedFilesAndBlocksDelayedUploads() {
    var first = uploads.upload(upload(UUID.randomUUID()), false);
    assertThat(deletion.deleteScope(incident, comment, true)).isFalse();
    assertThat(metadata.existsById(first.getId())).isTrue();
    assertThat(deletion.deleteScope(incident, comment, false)).isTrue();
    assertThatThrownBy(() -> uploads.upload(upload(UUID.randomUUID()), false)).isInstanceOf(RuntimeException.class);
    assertThat(metadata.findByIncidentId(incident)).isEmpty();
    for (var task : jobs.findAll()) {
      if (task.getObjectKey().equals(first.getStoragePath())) cleanup.process(task.getId());
    }
    assertThat(objects.containsKey(first.getStoragePath())).isFalse();
  }
}
