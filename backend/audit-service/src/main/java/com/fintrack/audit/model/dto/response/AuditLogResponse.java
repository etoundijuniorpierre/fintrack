// DTO : transporte les donnees liees a audit log entre les couches.

package com.fintrack.audit.model.dto.response;

import com.fintrack.audit.model.constant.AuditAction;
import com.fintrack.audit.model.constant.AuditStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO renvoyant les donnees d'une entree de journal d'audit au client.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

  private String id;
  private LocalDateTime timestamp;
  /** Utilisateur ayant déclenché l'action (résolu depuis user-service). */
  private UserSummaryResponse user;
  private String username;
  private List<String> roles;
  private AuditAction action;
  private String resourceType;
  private String resourceId;
  private String ipAddress;
  private String userAgent;
  private AuditStatus status;
  private Map<String, Object> details;
}
