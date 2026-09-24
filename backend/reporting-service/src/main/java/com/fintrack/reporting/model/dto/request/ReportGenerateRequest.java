// DTO : transporte les donnees liees a report generate entre les couches.

package com.fintrack.reporting.model.dto.request;

import com.fintrack.reporting.model.constant.ReportFormat;
import com.fintrack.reporting.model.constant.ReportContentType;
import com.fintrack.reporting.model.constant.ReportType;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de requete specifiant les parametres de generation d'un rapport.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportGenerateRequest {

  private ReportType type;
  @Builder.Default
  private ReportContentType contentType = ReportContentType.OPERATIONAL;
  private LocalDate startDate;
  private LocalDate endDate;
  private ReportFormat format;
  private ReportFilters filters;
  private List<String> metrics;
  private boolean sendEmail;
  private List<String> recipients;
}
