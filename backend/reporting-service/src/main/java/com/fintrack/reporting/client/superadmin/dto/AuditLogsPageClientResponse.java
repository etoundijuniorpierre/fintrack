// DTO : transporte les donnees liees a audit logs page client entre les couches.

package com.fintrack.reporting.client.superadmin.dto;

// Transporte les donnees liees a audit logs page client entre services.

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditLogsPageClientResponse {

  private List<AuditLogClientResponse> content;
  private long totalElements;
  private int totalPages;
  private int size;
  private int number;
}
