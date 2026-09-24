// DTO : transporte les donnees liees a audit log entre les couches.

package com.fintrack.audit.model.dto.request;

import com.fintrack.audit.model.constant.AuditAction;
import com.fintrack.audit.model.constant.AuditStatus;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO de création d'un log d'audit.
 * Envoyé par les autres services via l'API interne.
 */
@Getter
@Setter
// Modelise la responsabilite applicative liee a journal d'audit.
public class AuditLogRequest {

  private UUID userId;

  private String username;

  private List<String> roles;

  @NotNull(message = "{validation.not_blank}")
  private AuditAction action;

  @NotNull(message = "{validation.not_blank}")
  private String resourceType;

  private String resourceId;

  private String ipAddress;

  private String userAgent;

  @NotNull(message = "{validation.not_blank}")
  private AuditStatus status;

  private Map<String, Object> details;
}
