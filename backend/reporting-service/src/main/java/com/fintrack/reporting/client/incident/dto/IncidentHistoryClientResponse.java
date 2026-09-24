package com.fintrack.reporting.client.incident.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fintrack.reporting.client.superadmin.dto.UserSummaryClientResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IncidentHistoryClientResponse {

  private UserSummaryClientResponse user;
  private String action;
  private String newValue;
  private Object createdAt;
}
