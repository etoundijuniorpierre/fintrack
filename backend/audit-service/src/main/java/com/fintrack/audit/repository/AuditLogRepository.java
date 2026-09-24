// Acces aux donnees : expose les requetes persistantes liees a audit log.

package com.fintrack.audit.repository;

import com.fintrack.audit.model.constant.AuditAction;
import com.fintrack.audit.model.constant.AuditStatus;
import com.fintrack.audit.model.entity.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

// Repository MongoDB d'acces aux entrees de journal d'audit.
@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
  // Recherche les journaux audit par utilisateur identifiant.

  List<AuditLog> findByUserId(UUID userId);
  // Recherche les journaux audit par action.

  List<AuditLog> findByAction(AuditAction action);
  // Recherche les journaux audit par resource type.

  List<AuditLog> findByResourceType(String resourceType);
  // Recherche les journaux audit par resource type and resource identifiant.

  List<AuditLog> findByResourceTypeAndResourceId(
    String resourceType,
    String resourceId
  );
  // Recherche les journaux audit par statut.

  List<AuditLog> findByStatus(AuditStatus status);
  // Recherche les journaux audit par timestamp periode.

  List<AuditLog> findByTimestampBetween(LocalDateTime from, LocalDateTime to);
  // Recherche les journaux audit par utilisateur identifiant and timestamp periode.

  List<AuditLog> findByUserIdAndTimestampBetween(
    UUID userId,
    LocalDateTime from,
    LocalDateTime to
  );
}
