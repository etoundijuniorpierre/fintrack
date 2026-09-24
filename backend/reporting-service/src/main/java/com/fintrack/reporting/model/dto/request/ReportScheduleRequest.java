// DTO : transporte les donnees liees a report schedule entre les couches.

package com.fintrack.reporting.model.dto.request;

import com.fintrack.reporting.model.constant.ReportFormat;
import com.fintrack.reporting.model.constant.ReportContentType;
import com.fintrack.reporting.model.constant.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

// DTO de requete pour planifier la generation recurrente de rapports.

@Getter
@Setter
public class ReportScheduleRequest {

  @NotBlank(message = "{validation.not_blank}")
  @Size(max = 100)
  private String name;

  @NotNull(message = "{validation.not_null}")
  private ReportType type;

  private ReportContentType contentType;

  @NotNull(message = "{validation.not_null}")
  private ReportFormat format;

  private List<String> recipientEmails;

  @NotNull(message = "{validation.not_null}")
  private LocalTime sendTime;

  private Integer weekDay;

  @NotBlank(message = "{validation.not_blank}")
  private String scope;
}
