// Mapper : convertit les donnees liees a reporting builder entre modeles.

package com.fintrack.reporting.model.mapper.superadmin;

import static com.fintrack.reporting.model.mapper.superadmin.SuperAdminMaps.hasText;
import static com.fintrack.reporting.model.mapper.superadmin.SuperAdminMaps.string;

import com.fintrack.reporting.model.dto.response.superadmin.ReportRowResponse;
import com.fintrack.reporting.model.dto.response.superadmin.ReportingSectionResponse;
import com.fintrack.reporting.model.entity.GeneratedReport;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

// Assure les conversions du domaine reporting builder.

public final class ReportingBuilder {

  // Initialise le mapper avec ses dependances de construction.

  private ReportingBuilder() {}

  // Construit la representation attendue pour le domaine reporting.

  public static ReportingSectionResponse build(List<GeneratedReport> reports) {
    List<ReportRowResponse> reportRows = reports
      .stream()
      .map(ReportingBuilder::toReportRow)
      .toList();
    List<ReportRowResponse> failed = reportRows
      .stream()
      .filter(report -> "FAILED".equalsIgnoreCase(report.getStatus()))
      .toList();
    List<ReportRowResponse> pending = reportRows
      .stream()
      .filter(report ->
        Set.of("PENDING", "GENERATING").contains(
          string(report.getStatus(), "").toUpperCase(Locale.ROOT)
        )
      )
      .toList();
    List<ReportRowResponse> availableWithoutFile = reportRows
      .stream()
      .filter(report -> "AVAILABLE".equalsIgnoreCase(report.getStatus()))
      .filter(report -> !report.isHasFile())
      .toList();

    Double avgGenerationSeconds = reports
      .stream()
      .filter(
        report -> report.getCreatedAt() != null && report.getUpdatedAt() != null
      )
      .filter(report -> "AVAILABLE".equalsIgnoreCase(report.getStatus()))
      .map(
        report ->
          (double) Duration.between(
            report.getCreatedAt(),
            report.getUpdatedAt()
          ).getSeconds()
      )
      .filter(duration -> duration >= 0)
      .collect(
        Collectors.collectingAndThen(Collectors.toList(), list ->
          list.isEmpty()
            ? null
            : list
                .stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0)
        )
      );

    return new ReportingSectionResponse(
      reports.size(),
      failed.size(),
      pending.size(),
      reports
        .stream()
        .filter(report -> "AVAILABLE".equalsIgnoreCase(report.getStatus()))
        .count(),
      failed,
      pending,
      availableWithoutFile,
      avgGenerationSeconds,
      "PENDING_OR_GENERATING_REPORTS",
      null
    );
  }

  // Convertit les donnees du domaine reporting entre les modeles utilises.

  public static ReportRowResponse toReportRow(GeneratedReport report) {
    boolean hasFile =
      hasText(report.getFilePath()) &&
      report.getFileSize() != null &&
      report.getFileSize() > 0;
    return new ReportRowResponse(
      report.getId(),
      report.getName(),
      report.getType(),
      report.getFormat(),
      report.getStatus(),
      report.getFileSize(),
      report.getCreatedBy(),
      report.getAgencyId(),
      report.getServiceId(),
      report.getCreatedAt(),
      report.getUpdatedAt(),
      report.getPeriodStart(),
      report.getPeriodEnd(),
      report.getGenerationType(),
      report.getAutoSendEmail(),
      report.getEmailRecipients(),
      report.getFilePath(),
      report.getDownloadUrl(),
      hasFile,
      report.getErrorMessage()
    );
  }
}
