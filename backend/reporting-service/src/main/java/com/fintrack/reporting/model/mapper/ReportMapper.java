// Mapper : convertit les donnees liees a report entre modeles.

package com.fintrack.reporting.model.mapper;

import com.fintrack.reporting.model.constant.ReportGenerationType;
import com.fintrack.reporting.model.constant.ReportContentType;
import com.fintrack.reporting.model.dto.request.ReportGenerateRequest;
import com.fintrack.reporting.model.dto.response.ReportResponse;
import com.fintrack.reporting.model.entity.GeneratedReport;
import com.fintrack.reporting.security.UserDetailsImpl;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

// Mapper dedie aux rapports generes.
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportMapper {

  private static final DateTimeFormatter PERIOD_DATE_FORMAT =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private final ObjectMapper objectMapper;

  // Convertit les donnees du domaine rapport entre les modeles utilises.

  public GeneratedReport toEntity(
    ReportGenerateRequest request,
    UserDetailsImpl user
  ) {
    GeneratedReport report = new GeneratedReport();
    // Le titre explicite est calcule par ReportServiceImpl (nature + perimetre +
    // periode), une fois le perimetre resolu.
    report.setType(request.getType());
    report.setContentType(ReportContentType.orDefault(request.getContentType()));
    report.setFormat(request.getFormat());
    report.setPeriodStart(
      request.getStartDate() != null
        ? request.getStartDate().atStartOfDay()
        : null
    );
    report.setPeriodEnd(
      request.getEndDate() != null
        ? request.getEndDate().atTime(23, 59, 59)
        : null
    );
    report.setFilters(serializeFilterPayload(request, user));
    List<String> recipients = normalizeRecipients(request.getRecipients());
    report.setAutoSendEmail(request.isSendEmail() && !recipients.isEmpty());
    try {
      report.setEmailRecipients(
        !recipients.isEmpty()
          ? objectMapper.writeValueAsString(recipients)
          : null
      );
    } catch (Exception e) {
      log.warn(
        "Impossible de serialiser les destinataires : {}",
        e.getMessage()
      );
    }
    return report;
  }

  // Convertit les donnees du domaine rapport entre les modeles utilises.

  public ReportResponse toResponse(GeneratedReport report) {
    Map<String, Object> payload = readJson(report.getFilters());
    Map<String, Object> metrics = readJson(report.getMetrics());
    return ReportResponse.builder()
      .id(report.getId())
      .name(report.getName())
      .type(report.getType())
      .contentType(ReportContentType.orDefault(report.getContentType()))
      .generationType(
        report.getGenerationType() != null
          ? report.getGenerationType()
          : ReportGenerationType.MANUAL
      )
      .period(formatPeriod(report))
      .generatedAt(report.getCreatedAt())
      .format(report.getFormat())
      .status(report.getStatus())
      .fileSize(report.getFileSize())
      .downloadUrl(report.getDownloadUrl())
      .createdBy(report.getCreatedBy())
      .agencyId(report.getAgencyId())
      .serviceId(report.getServiceId())
      .scope(extractScope(payload))
      .createdByLabel(asString(payload.get("createdByLabel")))
      .filters(extractMap(payload.get("filters")))
      .requestedMetrics(extractStringList(payload.get("requestedMetrics")))
      .metrics(metrics)
      .recipients(extractRecipients(report, payload))
      .build();
  }

  // Formate une periode de rapport.

  private String formatPeriod(GeneratedReport report) {
    String start =
      report.getPeriodStart() != null
        ? report.getPeriodStart().format(PERIOD_DATE_FORMAT)
        : null;
    String end =
      report.getPeriodEnd() != null
        ? report.getPeriodEnd().format(PERIOD_DATE_FORMAT)
        : null;
    if (start == null && end == null) {
      return "-";
    }
    return (
      (start != null ? start : "...") + " -> " + (end != null ? end : "...")
    );
  }

  // Extrait les informations utiles du domaine rapport.

  private String extractScope(Map<String, Object> payload) {
    Map<String, Object> filters = extractMap(payload.get("filters"));
    Object view = filters.get("view");
    return view instanceof String value && !value.isBlank() ? value : "own";
  }

  // Lit les donnees brutes du domaine rapport dans un format exploitable.

  private Map<String, Object> readJson(String json) {
    if (json == null || json.isBlank()) {
      return new LinkedHashMap<>();
    }
    try {
      return objectMapper.readValue(
        json,
        new TypeReference<>() {}
      );
    } catch (Exception e) {
      log.warn(
        "Impossible de deserialiser le contenu du rapport: {}",
        e.getMessage()
      );
      return new LinkedHashMap<>();
    }
  }

  // Extrait les informations utiles du domaine rapport.

  @SuppressWarnings("unchecked")
  private Map<String, Object> extractMap(Object value) {
    if (value instanceof Map<?, ?> map) {
      return (Map<String, Object>) map;
    }
    return new LinkedHashMap<>();
  }

  // Extrait les informations utiles du domaine rapport.

  private List<String> extractStringList(Object value) {
    if (value instanceof List<?> list) {
      return list
        .stream()
        .map(Object::toString)
        .filter(item -> !item.isBlank())
        .toList();
    }
    return List.of();
  }

  // Convertit chaine.

  private String asString(Object value) {
    return value instanceof String text && !text.isBlank() ? text : null;
  }

  // Extrait les informations utiles du domaine rapport.

  private List<String> extractRecipients(
    GeneratedReport report,
    Map<String, Object> payload
  ) {
    List<String> persistedRecipients = readStringListJson(
      report.getEmailRecipients()
    );
    return !persistedRecipients.isEmpty()
      ? persistedRecipients
      : extractStringList(payload.get("recipients"));
  }

  // Lit les donnees brutes du domaine rapport dans un format exploitable.

  private List<String> readStringListJson(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return normalizeRecipients(
        objectMapper.readValue(
          json,
          new TypeReference<List<String>>() {}
        )
      );
    } catch (Exception e) {
      log.warn(
        "Impossible de deserialiser les destinataires du rapport : {}",
        e.getMessage()
      );
      return List.of();
    }
  }

  // Serialise les donnees du domaine rapport pour stockage ou transmission.

  private String serializeFilterPayload(
    ReportGenerateRequest request,
    UserDetailsImpl user
  ) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("filters", request.getFilters());
    payload.put(
      "requestedMetrics",
      request.getMetrics() != null ? request.getMetrics() : List.of()
    );
    List<String> recipients = normalizeRecipients(request.getRecipients());
    payload.put("sendEmail", request.isSendEmail() && !recipients.isEmpty());
    payload.put("recipients", recipients);
    payload.put("createdByLabel", user != null ? user.getUsername() : null);
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (Exception e) {
      log.warn(
        "Impossible de serialiser le contenu du rapport: {}",
        e.getMessage()
      );
      return null;
    }
  }

  // Normalise les valeurs du domaine rapport avant traitement.

  private List<String> normalizeRecipients(List<String> recipients) {
    if (recipients == null || recipients.isEmpty()) {
      return List.of();
    }
    return recipients
      .stream()
      .filter(item -> item != null && !item.isBlank())
      .flatMap(item -> Arrays.stream(item.split("[,;\\s]+")))
      .map(String::trim)
      .filter(item -> !item.isBlank())
      .distinct()
      .toList();
  }
}
