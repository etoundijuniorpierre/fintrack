// Service metier : coordonne les operations du domaine audit purge.

package com.fintrack.audit.service.impl;

import com.fintrack.audit.model.constant.AuditAction;
import com.fintrack.audit.model.constant.AuditStatus;
import com.fintrack.audit.model.entity.AuditLog;
import com.fintrack.audit.model.readmodel.AuditPurgeResult;
import com.fintrack.audit.service.AuditPurgeService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

// Purge : supprime les logs > olderThanDays, sauf:
// - les preserveLastN plus recents (par timestamp)
// - tous les logs dont l'action figure dans NEVER_PURGE_ACTIONS (preuve securite/conformite)
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditPurgeServiceImpl implements AuditPurgeService {

  // Conservees indefiniment (jamais purgees) : journaux a valeur de preuve
  // securite / conformite. Limite aux actions reellement emises par le systeme.
  static final List<String> NEVER_PURGE_ACTIONS = List.of(
    "USER_CREATE",
    "USER_UPDATE",
    "USER_DELETE",
    "ROLE_CREATE",
    "ROLE_UPDATE",
    "ROLE_DELETE",
    "DEPARTMENT_CREATE",
    "DEPARTMENT_UPDATE",
    "DEPARTMENT_DELETE",
    "DEPARTMENT_ASSIGN_HEAD",
    "AGENCY_CREATE",
    "AGENCY_UPDATE",
    "AGENCY_DELETE",
    "AGENCY_ASSIGN_HEAD",
    "SETTINGS_CHANGE",
    "REPORT_DELETE",
    "AUDIT_EXPORT",
    "AUDIT_PURGE",
    "ESCALATION_RULE_TOGGLE",
    "ESCALATION_RULE_TRIGGER",
    "JOB_TRIGGER",
    "CACHE_INVALIDATION",
    "LOGIN_FAILURE"
  );

  private final MongoTemplate mongoTemplate;

  // Supprime ou invalide les donnees ciblees apres controle metier.

  @Override
  public AuditPurgeResult purge(
    int olderThanDays,
    int preserveLastN,
    boolean dryRun,
    String actor
  ) {
    if (olderThanDays < 1) {
      throw new IllegalArgumentException("olderThanDays doit etre >= 1");
    }
    if (preserveLastN < 0) {
      throw new IllegalArgumentException("preserveLastN doit etre >= 0");
    }

    LocalDateTime ageCutoff = LocalDateTime.now().minusDays(olderThanDays);
    LocalDateTime preserveCutoff = resolvePreserveCutoff(preserveLastN);
    LocalDateTime effectiveCutoff =
      preserveCutoff == null || ageCutoff.isBefore(preserveCutoff)
        ? ageCutoff
        : preserveCutoff;

    Criteria candidateCriteria = Criteria.where("timestamp")
      .lt(effectiveCutoff)
      .and("action")
      .nin(NEVER_PURGE_ACTIONS);
    Query candidateQuery = new Query(candidateCriteria);

    long deletedCount = mongoTemplate.count(candidateQuery, AuditLog.class);
    long preservedSensitive = mongoTemplate.count(
      new Query(
        Criteria.where("timestamp")
          .lt(effectiveCutoff)
          .and("action")
          .in(NEVER_PURGE_ACTIONS)
      ),
      AuditLog.class
    );
    long totalPreserved =
      mongoTemplate.count(new Query(), AuditLog.class) - deletedCount;

    if (!dryRun) {
      long actual = mongoTemplate
        .remove(candidateQuery, AuditLog.class)
        .getDeletedCount();
      log.info(
        "Purge d'audit exécutée: {} logs supprimés (plus anciens que {} jours, conserverDerniersN={})",
        actual,
        olderThanDays,
        preserveLastN
      );
      deletedCount = actual;
      totalPreserved = mongoTemplate.count(new Query(), AuditLog.class);
      recordPurgeAudit(actor, olderThanDays, preserveLastN, deletedCount);
    }

    return AuditPurgeResult.builder()
      .scope("audit-logs")
      .deletedCount(deletedCount)
      .preservedCount(totalPreserved)
      .preservedSensitive(preservedSensitive)
      .dryRun(dryRun)
      .executedAt(Instant.now())
      .preservedSensitiveActions(NEVER_PURGE_ACTIONS)
      .build();
  }

  // Auto-audit : une purge reelle est elle-meme un acte sensible (suppression de
  // preuve) et doit etre tracee. Le log AUDIT_PURGE est lui-meme non purgeable
  // (present dans NEVER_PURGE_ACTIONS). timestamp pose par @CreatedDate a la sauvegarde.
  private void recordPurgeAudit(
    String actor,
    int olderThanDays,
    int preserveLastN,
    long deletedCount
  ) {
    AuditLog entry = new AuditLog();
    entry.setUsername(actor);
    entry.setAction(AuditAction.AUDIT_PURGE);
    entry.setResourceType("AUDIT_LOG");
    entry.setResourceId("bulk");
    entry.setStatus(AuditStatus.SUCCESS);
    entry.setDetails(
      Map.of(
        "olderThanDays",
        olderThanDays,
        "preserveLastN",
        preserveLastN,
        "deletedCount",
        deletedCount
      )
    );
    mongoTemplate.save(entry);
  }

  // Retourne le timestamp du Nieme log le plus recent. Tout ce qui est >= reste preserve.
  private LocalDateTime resolvePreserveCutoff(int preserveLastN) {
    if (preserveLastN <= 0) {
      return null;
    }
    Query query = new Query()
      .with(Sort.by(Sort.Direction.DESC, "timestamp"))
      .skip(preserveLastN - 1L)
      .limit(1);
    AuditLog nth = mongoTemplate.findOne(query, AuditLog.class);
    return nth == null ? null : nth.getTimestamp();
  }
}
