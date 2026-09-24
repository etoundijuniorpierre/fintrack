// DTO : transporte les donnees liees a report row entre les couches.

package com.fintrack.reporting.model.dto.response.superadmin;

import com.fintrack.reporting.model.constant.ReportFormat;
import com.fintrack.reporting.model.constant.ReportGenerationType;
import com.fintrack.reporting.model.constant.ReportType;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de rapport ligne.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRowResponse {

  private UUID id;
  private String name;
  private ReportType type;
  private ReportFormat format;
  private String status;
  private Long fileSize;
  private UUID createdBy;
  private UUID agencyId;
  private UUID serviceId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime periodStart;
  private LocalDateTime periodEnd;
  private ReportGenerationType generationType;
  private Boolean autoSendEmail;
  private String emailRecipients;
  private String filePath;
  private String downloadUrl;
  private boolean hasFile;
  private String errorMessage;
}
