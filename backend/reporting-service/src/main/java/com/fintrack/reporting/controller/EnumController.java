// Controleur REST : expose les operations HTTP liees a enum.

package com.fintrack.reporting.controller;

import com.fintrack.reporting.constant.ApiConstants;
import com.fintrack.reporting.model.constant.ReportFormat;
import com.fintrack.reporting.model.constant.ReportType;
import com.fintrack.reporting.model.dto.response.EnumResponse;
import com.fintrack.reporting.model.mapper.EnumMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Controleur REST exposant les valeurs d'enumerations de rapport traduites selon la locale.
@RestController
@RequestMapping(ApiConstants.API_BASE_PATH + "/enums")
@Tag(
  name = "Enums",
  description = "Endpoints for retrieving translated enum values"
)
@RequiredArgsConstructor
public class EnumController {

  private final EnumMapper enumMapper;

  // Renvoie les types de rapport avec libelle et description traduits.
  @GetMapping("/report-types")
  @PreAuthorize("isAuthenticated()")
  @Operation(
    summary = "Get all report types with translated name and description"
  )
  public ResponseEntity<List<EnumResponse>> getReportTypes() {
    return ResponseEntity.ok(
      enumMapper.toResponses(
        ReportType.values(),
        LocaleContextHolder.getLocale()
      )
    );
  }

  // Renvoie les formats de rapport avec libelle et description traduits.
  @GetMapping("/report-formats")
  @PreAuthorize("isAuthenticated()")
  @Operation(
    summary = "Get all report formats with translated name and description"
  )
  public ResponseEntity<List<EnumResponse>> getReportFormats() {
    return ResponseEntity.ok(
      enumMapper.toResponses(
        ReportFormat.values(),
        LocaleContextHolder.getLocale()
      )
    );
  }
}
