// Controleur REST : expose les operations HTTP liees a report.

package com.fintrack.reporting.controller;

import com.fintrack.reporting.model.constant.ReportFormat;
import com.fintrack.reporting.model.dto.request.ReportGenerateRequest;
import com.fintrack.reporting.model.dto.response.ReportResponse;
import com.fintrack.reporting.model.entity.GeneratedReport;
import com.fintrack.reporting.model.entity.ReportDownload;
import com.fintrack.reporting.model.mapper.ReportMapper;
import com.fintrack.reporting.security.UserDetailsImpl;
import com.fintrack.reporting.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// Controleur REST pour generer et telecharger les rapports d'incidents.
@RestController
@RequestMapping("/api/v1/reportingService/reports")
@Tag(name = "Reports", description = "Endpoints for managing generated reports")
@RequiredArgsConstructor
public class ReportController {

  private final ReportService reportService;
  private final ReportMapper reportMapper;

  // Point d'acces GET pour recuperer des donnees.
  @GetMapping
  @PreAuthorize(
    "hasAnyAuthority('REPORT_VIEW_OWN', 'REPORT_VIEW_AGENCY', 'REPORT_VIEW_SERVICE', 'REPORT_VIEW_ALL')"
  )
  @Operation(summary = "Get list of generated reports")
  public ResponseEntity<Page<ReportResponse>> getReports(
    @AuthenticationPrincipal UserDetailsImpl currentUser,
    @RequestParam(required = false) String scope,
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) String type,
    @RequestParam(required = false) String generationType,
    @RequestParam(required = false) String format,
    @RequestParam(required = false) String status,
    @RequestParam(required = false) Boolean missingFile,
    Pageable pageable
  ) {
    return ResponseEntity.ok(
      reportService
        .findAll(
          currentUser,
          scope,
          keyword,
          type,
          generationType,
          format,
          status,
          missingFile,
          pageable
        )
        .map(reportMapper::toResponse)
    );
  }

  // Point d'acces POST pour creer une ressource.
  @PostMapping("/generate")
  @PreAuthorize("hasAuthority('REPORT_GENERATE')")
  @Operation(summary = "Generate a new report")
  public ResponseEntity<ReportResponse> generateReport(
    @RequestBody ReportGenerateRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    GeneratedReport report = reportMapper.toEntity(request, currentUser);
    return ResponseEntity.ok(
      reportMapper.toResponse(reportService.generate(report, currentUser))
    );
  }

  @PostMapping("/{id}/retry")
  @PreAuthorize("hasAuthority('REPORT_GENERATE')")
  @Operation(summary = "Retry generating a failed report")
  // Realise l'intention metier retry report.
  public ResponseEntity<ReportResponse> retryReport(
    @PathVariable UUID id,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    return ResponseEntity.ok(
      reportMapper.toResponse(reportService.rerun(id, currentUser))
    );
  }

  // Point d'acces GET pour recuperer des donnees.
  @GetMapping("/{id}/download")
  @PreAuthorize("hasAuthority('REPORT_EXPORT')")
  @Operation(summary = "Download a report file")
  public ResponseEntity<Resource> downloadReport(
    @PathVariable UUID id,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    ReportDownload download = reportService.download(id, currentUser);
    return ResponseEntity.ok()
      .contentType(mediaTypeFor(download.getFormat()))
      .header(
        HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=\"" + download.getFilename() + "\""
      )
      .contentLength(download.getContent().length)
      .body(new ByteArrayResource(download.getContent()));
  }

  // Point d'acces POST pour creer une ressource.
  @PostMapping("/{id}/send-email")
  @PreAuthorize("hasAuthority('REPORT_SEND_EMAIL')")
  @Operation(summary = "Send report by email")
  public ResponseEntity<Void> sendEmail(
    @PathVariable UUID id,
    @RequestBody Map<String, List<String>> request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    reportService.sendEmail(id, request.get("recipients"), currentUser);
    return ResponseEntity.ok().build();
  }

  // Point d'acces DELETE pour supprimer une ressource.
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('REPORT_DELETE')")
  @Operation(summary = "Delete a generated report")
  public ResponseEntity<Void> deleteReport(
    @PathVariable UUID id,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    reportService.delete(id, currentUser);
    return ResponseEntity.noContent().build();
  }

  // Realise l'intention metier media type for.

  private MediaType mediaTypeFor(ReportFormat format) {
    return switch (format) {
      case PDF -> MediaType.APPLICATION_PDF;
      case JSON -> MediaType.APPLICATION_JSON;
      case EXCEL -> MediaType.parseMediaType(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
      );
    };
  }
}
