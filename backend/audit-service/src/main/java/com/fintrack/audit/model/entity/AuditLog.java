// Entite metier : represente les donnees persistees liees a audit log.

package com.fintrack.audit.model.entity;

import com.fintrack.audit.model.constant.AuditAction;
import com.fintrack.audit.model.constant.AuditStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

// Document d'une entree de journal d'audit (collection MongoDB audit_logs).
// Les index sont crees au demarrage par AuditLogIndexInitializer, pas via @Indexed :
// garder l'initialiseur synchronise en cas d'ajout/retrait de champ indexe.
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Document(collection = "audit_logs")
public class AuditLog {

  @Id
  private String id;

  @NotNull
  @CreatedDate
  private LocalDateTime timestamp;

  @Field("user_id")
  private UUID userId;

  @Field("username")
  private String username;

  @Field("roles")
  private List<String> roles;

  @NotNull
  @Field("action")
  private AuditAction action;

  @NotNull
  @Field("resource_type")
  private String resourceType;

  @Field("resource_id")
  private String resourceId;

  @Field("ip_address")
  private String ipAddress;

  @Field("user_agent")
  private String userAgent;

  @NotNull
  @Field("status")
  private AuditStatus status;

  @Field("details")
  private Map<String, Object> details;

  // Verifie que l'entree satisfait tous les filtres fournis (action, statut, type, userId).
  public boolean matchesFilters(Map<String, Object> filters) {
    if (filters == null || filters.isEmpty()) return true;
    for (Map.Entry<String, Object> entry : filters.entrySet()) {
      switch (entry.getKey()) {
        case "action" -> {
          if (!action.getName().equals(entry.getValue())) return false;
        }
        case "status" -> {
          if (!status.getName().equals(entry.getValue())) return false;
        }
        case "resourceType" -> {
          if (!resourceType.equals(entry.getValue())) return false;
        }
        case "userId" -> {
          if (
            userId == null ||
            !userId.toString().equals(entry.getValue().toString())
          ) return false;
        }
        default -> {
        }
      }
    }
    return true;
  }
}
