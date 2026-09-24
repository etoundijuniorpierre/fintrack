package com.fintrack.document.model.readmodel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.*;
@Data @AllArgsConstructor
public class ValidatedAttachment implements AutoCloseable {
  private Path path;
  private long size;
  private String mimeType;
  private String checksum;
  @Override public void close() throws IOException { Files.deleteIfExists(path); }
}
