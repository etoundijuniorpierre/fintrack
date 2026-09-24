// DTO : transporte les donnees liees a audit log client entre les couches.

package com.fintrack.user.client.audit.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

/**
 * DTO envoyé à l'audit-service pour enregistrer un log d'audit.
 * Les valeurs de {@code action} et {@code status} correspondent aux enums
 * AuditAction et AuditStatus de l'audit-service.
 */
@Data
@Builder
// Modelise la responsabilite applicative liee a journal d'audit.
public class AuditLogClientRequest {

  private UUID userId;
  private String username;
  private List<String> roles;
  /** Valeur de AuditAction (ex: "USER_CREATE", "LOGIN_SUCCESS"). */
  private String action;
  /** Type de ressource (ex: "USER", "ROLE"). */
  private String resourceType;
  private String resourceId;
  private String ipAddress;
  private String userAgent;
  /** Valeur de AuditStatus ("SUCCESS" ou "FAILURE"). */
  private String status;
  private Map<String, Object> details;
}
