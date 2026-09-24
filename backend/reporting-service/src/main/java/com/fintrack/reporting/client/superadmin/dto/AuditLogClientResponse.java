// DTO : transporte les donnees liees a audit log client entre les couches.

package com.fintrack.reporting.client.superadmin.dto;

// Transporte les donnees liees a audit journal client entre services.

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditLogClientResponse {

  private String id;
  private Object timestamp;
  private String username;
  private UUID userId;
  private String action;
  private String status;
  private String resourceType;
  private String resourceId;
  private String ipAddress;
  private String service;
}
