// DTO : transporte les donnees liees a audit log client entre les couches.

package com.fintrack.reporting.client.audit.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

// DTO de requete envoye au service d'audit pour enregistrer un log.

@Data
@Builder
public class AuditLogClientRequest {

  private UUID userId;
  private String username;
  private List<String> roles;
  private String action;
  private String resourceType;
  private String resourceId;
  private String ipAddress;
  private String userAgent;
  private String status;
  private Map<String, Object> details;
}
