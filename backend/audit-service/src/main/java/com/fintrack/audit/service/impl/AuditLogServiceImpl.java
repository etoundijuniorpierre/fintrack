// Service metier : coordonne les operations du domaine audit journal.

package com.fintrack.audit.service.impl;

import com.fintrack.audit.client.user.UserServiceClientService;
import com.fintrack.audit.exception.EntityNotFoundException;
import com.fintrack.audit.exception.ErrorCode;
import com.fintrack.audit.model.constant.AuditAction;
import com.fintrack.audit.model.constant.AuditStatus;
import com.fintrack.audit.model.entity.AuditLog;
import com.fintrack.audit.repository.AuditLogRepository;
import com.fintrack.audit.service.AuditLogService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

// Logique metier des journaux d'audit : enregistrement et consultation
// (par utilisateur, action, ressource, statut, plage de dates).
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

  private final AuditLogRepository auditLogRepository;
  private final UserServiceClientService userServiceClientService;
  private final MongoTemplate mongoTemplate;

  // Liste les elements du domaine audit journal.

  @Override
  public Page<AuditLog> findAll(Pageable pageable) {
    return auditLogRepository.findAll(pageable);
  }

  @Override
  // Recherche search.
  public Page<AuditLog> search(
    Pageable pageable,
    AuditAction action,
    AuditStatus status,
    LocalDateTime from,
    LocalDateTime to,
    String keyword
  ) {
    List<Criteria> criteria = new ArrayList<>();
    if (action != null) {
      criteria.add(Criteria.where("action").is(action));
    }
    if (status != null) {
      criteria.add(Criteria.where("status").is(status));
    }
    if (from != null && to != null) {
      criteria.add(Criteria.where("timestamp").gte(from).lte(to));
    } else if (from != null) {
      criteria.add(Criteria.where("timestamp").gte(from));
    } else if (to != null) {
      criteria.add(Criteria.where("timestamp").lte(to));
    }
    if (keyword != null && !keyword.isBlank()) {
      String escapedKeyword = Pattern.quote(keyword.trim());
      criteria.add(
        new Criteria().orOperator(
          Criteria.where("username").regex(escapedKeyword, "i"),
          Criteria.where("resource_type").regex(escapedKeyword, "i"),
          Criteria.where("resource_id").regex(escapedKeyword, "i"),
          Criteria.where("ip_address").regex(escapedKeyword, "i"),
          Criteria.where("user_agent").regex(escapedKeyword, "i")
        )
      );
    }

    Query query = new Query();
    if (!criteria.isEmpty()) {
      query.addCriteria(
        new Criteria().andOperator(criteria.toArray(new Criteria[0]))
      );
    }
    query.with(pageable);

    List<AuditLog> content = mongoTemplate.find(query, AuditLog.class);
    return PageableExecutionUtils.getPage(content, pageable, () ->
      mongoTemplate.count(Query.of(query).limit(-1).skip(-1), AuditLog.class)
    );
  }

  // Recherche les journaux audit par identifiant.

  @Override
  public AuditLog findById(String id) {
    return auditLogRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException(
          ErrorCode.AUDIT_LOG_NOT_FOUND,
          "Journal d'audit introuvable avec l'identifiant : " + id
        )
      );
  }

  // Recherche les journaux audit par utilisateur identifiant.

  @Override
  public List<AuditLog> findByUserId(UUID userId) {
    return auditLogRepository.findByUserId(userId);
  }

  // Recherche les journaux audit par action.

  @Override
  public List<AuditLog> findByAction(AuditAction action) {
    return auditLogRepository.findByAction(action);
  }

  // Recherche les journaux audit par resource type.

  @Override
  public List<AuditLog> findByResourceType(String resourceType) {
    return auditLogRepository.findByResourceType(resourceType);
  }

  // Recherche les journaux audit par resource type and resource identifiant.

  @Override
  public List<AuditLog> findByResourceTypeAndResourceId(
    String resourceType,
    String resourceId
  ) {
    return auditLogRepository.findByResourceTypeAndResourceId(
      resourceType,
      resourceId
    );
  }

  // Recherche les journaux audit par statut.

  @Override
  public List<AuditLog> findByStatus(AuditStatus status) {
    return auditLogRepository.findByStatus(status);
  }

  // Recherche les journaux audit par timestamp periode.

  @Override
  public List<AuditLog> findByTimestampBetween(
    LocalDateTime from,
    LocalDateTime to
  ) {
    return auditLogRepository.findByTimestampBetween(from, to);
  }

  // Recherche les journaux audit par utilisateur identifiant and timestamp periode.

  @Override
  public List<AuditLog> findByUserIdAndTimestampBetween(
    UUID userId,
    LocalDateTime from,
    LocalDateTime to
  ) {
    return auditLogRepository.findByUserIdAndTimestampBetween(userId, from, to);
  }

  // Enregistre une entree d'audit ; horodate a l'instant courant si absent.
  // Si l'appelant n'a pas fourni de username, on snapshote l'identite de
  // l'utilisateur depuis user-service au moment de l'ecriture afin que le
  // log reste lisible meme apres suppression ulterieure du compte.
  @Override
  public AuditLog create(AuditLog auditLog) {
    if (auditLog.getTimestamp() == null) {
      auditLog.setTimestamp(LocalDateTime.now());
    }
    snapshotUserIdentity(auditLog);
    log.info(
      "Enregistrement du log d'audit : action={} ressource={}/{} utilisateur={} statut={}",
      auditLog.getAction().getName(),
      auditLog.getResourceType(),
      auditLog.getResourceId(),
      auditLog.getUsername(),
      auditLog.getStatus().getName()
    );
    return auditLogRepository.save(auditLog);
  }

  // Complete l'audit log avec un instantane (username, roles) resolu depuis
  // user-service quand l'appelant ne les a pas fournis. Sert de plan B
  // robuste pour preserver la tracabilite meme apres suppression d'un user.
  private void snapshotUserIdentity(AuditLog auditLog) {
    if (auditLog.getUserId() == null) return;
    if (
      auditLog.getUsername() != null && !auditLog.getUsername().isBlank()
    ) return;
    try {
      String username = userServiceClientService.resolveUsername(
        auditLog.getUserId()
      );
      if (username != null && !username.isBlank()) {
        auditLog.setUsername(username);
      }
    } catch (RuntimeException ex) {
      log.warn(
        "Impossible de capturer l'identité utilisateur pour journal d audit (userId={}): {}",
        auditLog.getUserId(),
        ex.getMessage()
      );
    }
  }

  // Supprime ou invalide les donnees ciblees apres controle metier.

  @Override
  public void delete(String id) {
    if (!auditLogRepository.existsById(id)) {
      throw new EntityNotFoundException(
        ErrorCode.AUDIT_LOG_NOT_FOUND,
        "Journal d'audit introuvable avec l'identifiant : " + id
      );
    }
    auditLogRepository.deleteById(id);
    log.info("Log d'audit supprimé avec id: {}", id);
  }
}
