// Contrat metier : expose les operations du domaine audit journal.

package com.fintrack.audit.service;

import com.fintrack.audit.model.constant.AuditAction;
import com.fintrack.audit.model.constant.AuditStatus;
import com.fintrack.audit.model.entity.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// Contrat du service de gestion des logs d'audit.
public interface AuditLogService {
  // Liste les elements du domaine audit journal.
  Page<AuditLog> findAll(Pageable pageable);

  /**
   * Recherche paginée et filtrée côté serveur. Chaque critère est optionnel
   * (null = ignoré) : action, statut et bornes temporelles se combinent. Le
   * filtrage doit rester serveur car les logs sont paginés — un filtre client
   * ne verrait que la page courante.
   */
  // Recherche search.
  Page<AuditLog> search(
    Pageable pageable,
    AuditAction action,
    AuditStatus status,
    LocalDateTime from,
    LocalDateTime to,
    String keyword
  );
  // Recherche les journaux audit par identifiant.

  AuditLog findById(String id);
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
  // Prepare l'enregistrement de la ressource selon les regles metier.
  AuditLog create(AuditLog auditLog);
  // Supprime ou invalide les donnees ciblees apres controle metier.
  void delete(String id);
}
