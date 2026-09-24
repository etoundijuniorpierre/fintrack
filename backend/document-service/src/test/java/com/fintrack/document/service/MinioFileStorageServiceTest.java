package com.fintrack.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fintrack.document.model.StoredFile;
import com.fintrack.document.model.entity.AttachmentUpload;
import com.fintrack.document.service.impl.MinioFileStorageService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MinioFileStorageServiceTest {
  @Test
  void deletionFailureIsPropagatedForDurableRetry() throws Exception {
    org.mockito.Mockito.doThrow(new java.io.IOException("offline")).when(minioClient).removeObject(any(RemoveObjectArgs.class));
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> minioFileStorageService.delete("key"))
      .isInstanceOf(com.fintrack.document.exception.FileStorageException.class);
  }

  @Mock
  private MinioClient minioClient;

  private MinioFileStorageService minioFileStorageService;
  private final String bucketName = "fintrack-documents";

  @BeforeEach
  void setUp() {
    minioFileStorageService = new MinioFileStorageService(
      "http://localhost:9000",
      "minioadmin",
      "minioadmin",
      bucketName,
      false
    );
    ReflectionTestUtils.setField(
      minioFileStorageService,
      "minioClient",
      minioClient
    );
  }

  @Test
  @DisplayName("init - Creates bucket if it does not exist")
  void init_BucketDoesNotExist_CreatesBucket() throws Exception {
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(
      false
    );

    minioFileStorageService.init();

    verify(minioClient).makeBucket(any(MakeBucketArgs.class));
  }

  @Test
  @DisplayName("init - Does not create bucket if it already exists")
  void init_BucketExists_DoesNotCreateBucket() throws Exception {
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(
      true
    );

    minioFileStorageService.init();

    verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
  }

  @Test
  @DisplayName("store - Stores file and returns metadata")
  void store_SavesFileSuccessfully() throws Exception {
    AttachmentUpload upload = new AttachmentUpload();
    upload.setOriginalFilename("test.txt");
    upload.setContentType("text/plain");
    upload.setSize(11L);
    upload.setContent(new ByteArrayInputStream("hello world".getBytes()));

    StoredFile storedFile = minioFileStorageService.store(
      upload,
      "incidents/test.txt"
    );

    assertThat(storedFile).isNotNull();
    assertThat(storedFile.getObjectKey()).isEqualTo("incidents/test.txt");
    assertThat(storedFile.getSize()).isEqualTo(11L);
    assertThat(storedFile.getChecksum()).isNotEmpty();

    verify(minioClient).putObject(any(PutObjectArgs.class));
  }

  @Test
  @DisplayName("load - Loads file from MinIO")
  void load_ReturnsResource() throws Exception {
    GetObjectResponse getObjectResponse = mock(GetObjectResponse.class);
    when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(
      getObjectResponse
    );

    Resource resource = minioFileStorageService.load("incidents/test.txt");

    assertThat(resource).isNotNull();
    assertThat(resource.getFilename()).isEqualTo("test.txt");
  }

  @Test
  @DisplayName("delete - Removes object from MinIO")
  void delete_RemovesObject() throws Exception {
    minioFileStorageService.delete("incidents/test.txt");

    verify(minioClient).removeObject(any(RemoveObjectArgs.class));
  }
}
