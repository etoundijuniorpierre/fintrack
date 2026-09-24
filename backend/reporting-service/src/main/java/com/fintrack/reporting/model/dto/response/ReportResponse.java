// DTO : transporte les donnees liees a report entre les couches.

package com.fintrack.reporting.model.dto.response;

import com.fintrack.reporting.model.constant.ReportFormat;
import com.fintrack.reporting.model.constant.ReportContentType;
import com.fintrack.reporting.model.constant.ReportGenerationType;
import com.fintrack.reporting.model.constant.ReportType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les metadonnees d'un rapport genere.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {

  private UUID id;
  private String name;
  private ReportType type;
  private ReportContentType contentType;
  private ReportGenerationType generationType;
  private String period;
  private LocalDateTime generatedAt;
  private ReportFormat format;
  private String status;
  private Long fileSize;
  private String downloadUrl;
  private UUID createdBy;
  private UUID agencyId;
  private UUID serviceId;
  private String scope;
  private String createdByLabel;
  private Map<String, Object> filters;
  private List<String> requestedMetrics;
  private Map<String, Object> metrics;
  private List<String> recipients;
}
