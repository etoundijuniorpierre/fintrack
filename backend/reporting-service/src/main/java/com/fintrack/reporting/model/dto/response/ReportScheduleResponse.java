// DTO : transporte les donnees liees a report schedule entre les couches.

package com.fintrack.reporting.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fintrack.reporting.model.constant.ReportFormat;
import com.fintrack.reporting.model.constant.ReportContentType;
import com.fintrack.reporting.model.constant.ReportType;
import com.fintrack.reporting.model.dto.BaseDto;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

// DTO de reponse exposant le planning d'un rapport.

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReportScheduleResponse extends BaseDto {

  private String name;
  private ReportType type;
  private ReportContentType contentType;
  private ReportFormat format;
  private List<String> recipientEmails;
  private LocalTime sendTime;
  private Integer weekDay;
  private String scope;

  @JsonProperty("isActive")
  private boolean isActive;

  private LocalDateTime lastGeneratedAt;
  /** Utilisateur ayant créé la programmation (résolu depuis user-service). */
  private UserSummaryResponse createdBy;
}
