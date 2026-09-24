package com.fintrack.document.service;

import com.fintrack.document.exception.ErrorCode;
import com.fintrack.document.exception.FileStorageException;
import com.fintrack.document.model.entity.AttachmentUpload;
import com.fintrack.document.model.readmodel.ValidatedAttachment;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.ZipInputStream;
import org.springframework.stereotype.Component;

@Component
public class AttachmentContentValidator {
  private static final long MAX_SIZE = 150L * 1024 * 1024;
  private static final Map<String, String> TYPES = Map.ofEntries(
    Map.entry("pdf", "application/pdf"), Map.entry("png", "image/png"),
    Map.entry("jpg", "image/jpeg"), Map.entry("jpeg", "image/jpeg"),
    Map.entry("gif", "image/gif"), Map.entry("bmp", "image/bmp"),
    Map.entry("webp", "image/webp"), Map.entry("heic", "image/heic"),
    Map.entry("doc", "application/msword"), Map.entry("xls", "application/vnd.ms-excel"),
    Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    Map.entry("odt", "application/vnd.oasis.opendocument.text"),
    Map.entry("ods", "application/vnd.oasis.opendocument.spreadsheet"),
    Map.entry("zip", "application/zip"), Map.entry("txt", "text/plain"),
    Map.entry("csv", "text/csv"), Map.entry("rtf", "application/rtf")
  );

  public ValidatedAttachment validate(AttachmentUpload upload, boolean avatar) {
    if (upload == null || upload.getContent() == null || upload.getSize() <= 0) {
      throw invalid("Le fichier ne doit pas etre vide");
    }
    long limit = avatar ? 2L * 1024 * 1024 : MAX_SIZE;
    if (upload.getSize() > limit) throw new FileStorageException(ErrorCode.FILE_SIZE_EXCEEDED, "Fichier trop volumineux");
    String name = Objects.toString(upload.getOriginalFilename(), "");
    String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    String type = TYPES.get(ext);
    if (type == null || (avatar && !type.startsWith("image/"))) {
      throw invalid("Format de fichier non autorise");
    }
    String declared = Objects.toString(upload.getContentType(), "").toLowerCase(Locale.ROOT).split(";")[0].trim();
    if (!declared.isEmpty() && !declared.equals("application/octet-stream")
        && !declared.equals(type) && !isKnownAlias(ext, declared)) {
      throw invalid("Le type annonce ne correspond pas au format du fichier");
    }
    Path temp = null;
    try {
      temp = Files.createTempFile("fintrack-attachment-", ".tmp");
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      long size = 0;
      try (InputStream input = upload.getContent(); OutputStream output = Files.newOutputStream(temp)) {
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
          size += count;
          if (size > limit) throw new FileStorageException(ErrorCode.FILE_SIZE_EXCEEDED, "Fichier trop volumineux");
          digest.update(buffer, 0, count);
          output.write(buffer, 0, count);
        }
      }
      if (size == 0 || size != upload.getSize()) throw invalid("Taille reelle du fichier incoherente");
      byte[] header;
      try (InputStream input = Files.newInputStream(temp)) { header = input.readNBytes(512); }
      boolean valid = switch (ext) {
        case "pdf" -> starts(header, "%PDF-");
        case "png" -> hex(header, "89504e470d0a1a0a");
        case "jpg", "jpeg" -> hex(header, "ffd8ff");
        case "gif" -> starts(header, "GIF87a") || starts(header, "GIF89a");
        case "bmp" -> starts(header, "BM");
        case "webp" -> starts(header, "RIFF") && at(header, 8, "WEBP");
        case "heic" -> at(header, 4, "ftyp") && (at(header, 8, "heic") || at(header, 8, "heix") || at(header, 8, "mif1"));
        case "doc", "xls" -> hex(header, "d0cf11e0a1b11ae1");
        case "rtf" -> starts(header, "{\\rtf");
        case "zip", "docx", "xlsx", "odt", "ods" -> validateZip(temp, ext);
        case "txt", "csv" -> validateText(temp);
        default -> false;
      };
      if (!valid) throw invalid("Le contenu ne correspond pas au format du fichier");
      return new ValidatedAttachment(temp, size, type, HexFormat.of().formatHex(digest.digest()));
    } catch (IOException | NoSuchAlgorithmException | RuntimeException ex) {
      if (temp != null) {
        try { Files.deleteIfExists(temp); } catch (IOException cleanup) { ex.addSuppressed(cleanup); }
      }
      if (ex instanceof FileStorageException storage) throw storage;
      throw new FileStorageException(ErrorCode.UNSUPPORTED_FILE_TYPE, "Fichier illisible ou invalide", ex);
    }
  }

  private boolean validateText(Path file) throws IOException {
    try (Reader reader = new InputStreamReader(Files.newInputStream(file),
        StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT))) {
      char[] buffer = new char[8192];
      int n;
      while ((n = reader.read(buffer)) != -1) {
        for (int i = 0; i < n; i++) if (buffer[i] == 0) return false;
      }
      return true;
    }
  }

  private boolean isKnownAlias(String ext, String type) {
    return switch (ext) {
      case "rtf" -> type.equals("text/rtf");
      case "zip" -> type.equals("application/x-zip-compressed");
      case "bmp" -> type.equals("image/x-ms-bmp");
      case "csv" -> type.equals("text/plain") || type.equals("application/vnd.ms-excel");
      case "xlsx" -> type.equals("application/vnd.ms-excel");
      default -> false;
    };
  }

  private boolean validateZip(Path file, String ext) throws IOException {
    Set<String> names = new HashSet<>();
    long total = 0;
    String mimetype = null;
    try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(file))) {
      java.util.zip.ZipEntry entry;
      byte[] buffer = new byte[8192];
      while ((entry = zip.getNextEntry()) != null) {
        if (!names.add(entry.getName()) || names.size() > 2000) return false;
        String name = entry.getName().replace('\\', '/');
        if (name.startsWith("/") || Arrays.asList(name.split("/")).contains("..")) return false;
        ByteArrayOutputStream mime = name.equals("mimetype") ? new ByteArrayOutputStream() : null;
        int n;
        while ((n = zip.read(buffer)) != -1) {
          total += n;
          if (total > 200L * 1024 * 1024 || total > Math.max(1024 * 1024, Files.size(file) * 100)) return false;
          if (mime != null) {
            if (mime.size() + n > 256) return false;
            mime.write(buffer, 0, n);
          }
        }
        if (mime != null) mimetype = mime.toString(StandardCharsets.UTF_8);
      }
    }
    return switch (ext) {
      case "docx" -> names.contains("[Content_Types].xml") && names.contains("word/document.xml");
      case "xlsx" -> names.contains("[Content_Types].xml") && names.contains("xl/workbook.xml");
      case "odt", "ods" -> TYPES.get(ext).equals(mimetype) && names.contains("content.xml");
      default -> !names.isEmpty();
    };
  }

  private boolean starts(byte[] data, String prefix) { return at(data, 0, prefix); }
  private boolean at(byte[] data, int offset, String value) {
    byte[] prefix = value.getBytes(StandardCharsets.US_ASCII);
    if (data.length < offset + prefix.length) return false;
    for (int i = 0; i < prefix.length; i++) if (data[offset + i] != prefix[i]) return false;
    return true;
  }
  private boolean hex(byte[] data, String signature) {
    byte[] prefix = HexFormat.of().parseHex(signature);
    return data.length >= prefix.length && Arrays.equals(Arrays.copyOf(data, prefix.length), prefix);
  }
  private FileStorageException invalid(String message) {
    return new FileStorageException(ErrorCode.UNSUPPORTED_FILE_TYPE, message);
  }
}
