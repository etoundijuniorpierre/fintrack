package com.fintrack.document.service;

import static org.assertj.core.api.Assertions.*;
import com.fintrack.document.exception.FileStorageException;
import com.fintrack.document.model.entity.AttachmentUpload;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AttachmentContentValidatorTest {
  private final AttachmentContentValidator validator = new AttachmentContentValidator();
  private AttachmentUpload file(String name, String type, byte[] content) {
    return AttachmentUpload.builder().originalFilename(name).contentType(type).size(content.length)
      .content(new ByteArrayInputStream(content)).build();
  }
  @ParameterizedTest
  @CsvSource({"fake.pdf,text/html", "fake.pdf,application/pdf", "fake.png,image/png",
    "fake.docx,application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "fake.svg,image/svg+xml", "fake.exe,application/octet-stream"})
  void rejectsSpoofedAndActiveContent(String name, String type) {
    assertThatThrownBy(() -> validator.validate(file(name, type, "<html>bad</html>".getBytes()), false))
      .isInstanceOf(FileStorageException.class);
  }
  @Test void rejectsEmptyAndIncorrectActualSize() {
    assertThatThrownBy(() -> validator.validate(file("a.pdf", "application/pdf", new byte[0]), false))
      .isInstanceOf(FileStorageException.class);
    var upload = file("a.pdf", "application/pdf", "%PDF-1.7".getBytes());
    upload.setSize(100);
    assertThatThrownBy(() -> validator.validate(upload, false)).isInstanceOf(FileStorageException.class);
  }
  @Test void rejectsOversizedFilesAndAvatars() {
    var upload = file("a.png", "image/png", new byte[] {1});
    upload.setSize(150L * 1024 * 1024 + 1);
    assertThatThrownBy(() -> validator.validate(upload, false)).isInstanceOf(FileStorageException.class);
    upload.setSize(2L * 1024 * 1024 + 1);
    assertThatThrownBy(() -> validator.validate(upload, true)).isInstanceOf(FileStorageException.class);
    assertThatThrownBy(() -> validator.validate(file("a.pdf", "application/pdf", "%PDF-".getBytes()), true))
      .isInstanceOf(FileStorageException.class);
  }
  @Test void validatesAndRemovesTemporaryFile() throws Exception {
    var result = validator.validate(file("a.PDF", "application/octet-stream", "%PDF-1.7\n".getBytes()), false);
    assertThat(result.getMimeType()).isEqualTo("application/pdf");
    assertThat(result.getChecksum()).hasSize(64);
    assertThat(Files.exists(result.getPath())).isTrue();
    result.close();
    assertThat(Files.exists(result.getPath())).isFalse();
  }
  @Test void acceptsUtf8TextAndRefusesBinaryText() throws Exception {
    try (var result = validator.validate(file("a.txt", "text/plain", "écriture".getBytes(StandardCharsets.UTF_8)), false)) {
      assertThat(result.getSize()).isPositive();
    }
    assertThatThrownBy(() -> validator.validate(file("a.txt", "text/plain", new byte[] {0, 1}), false))
      .isInstanceOf(FileStorageException.class);
  }
  @Test void normalizesCommonWindowsMimeAliasesAfterContentValidation() throws Exception {
    try (var result = validator.validate(file("data.csv", "application/vnd.ms-excel", "a,b\n1,2".getBytes()), false)) {
      assertThat(result.getMimeType()).isEqualTo("text/csv");
    }
    byte[] archive = zip("data.txt", "safe".getBytes());
    try (var result = validator.validate(file("a.zip", "application/x-zip-compressed", archive), false)) {
      assertThat(result.getMimeType()).isEqualTo("application/zip");
    }
  }
  private byte[] zip(String entry, byte[] content) throws Exception {
    var output = new ByteArrayOutputStream();
    try (var zip = new ZipOutputStream(output)) {
      zip.putNextEntry(new ZipEntry(entry));
      zip.write(content);
      zip.closeEntry();
    }
    return output.toByteArray();
  }
  @Test void rejectsZipTraversalBombAndFakeOffice() throws Exception {
    byte[] traversal = zip("../secret", new byte[]{1});
    assertThatThrownBy(() -> validator.validate(file("a.zip", "application/zip", traversal), false))
      .isInstanceOf(FileStorageException.class);
    byte[] bomb = zip("large", new byte[2 * 1024 * 1024]);
    assertThatThrownBy(() -> validator.validate(file("a.zip", "application/zip", bomb), false))
      .isInstanceOf(FileStorageException.class);
    byte[] ordinary = zip("readme.txt", "normal".getBytes());
    assertThatThrownBy(() -> validator.validate(file("a.docx", "", ordinary), false))
      .isInstanceOf(FileStorageException.class);
    try (var result = validator.validate(file("a.zip", "application/zip", ordinary), false)) {
      assertThat(result.getMimeType()).isEqualTo("application/zip");
    }
  }
}
