// DTO : transporte les donnees liees a audit sample entre les couches.

package com.fintrack.audit.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de audit sample.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditSample {

  private String id;
  private String timestamp;
  private String username;
  private String action;
  private String status;
  private String resourceType;
  private String resourceId;
  private String ipAddress;
}
