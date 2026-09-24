// Service metier : coordonne les operations du domaine audit statistiques.

package com.fintrack.audit.service.impl;

import com.fintrack.audit.model.constant.AuditStatus;
import com.fintrack.audit.model.entity.AuditLog;
import com.fintrack.audit.model.readmodel.AuditStats;
import com.fintrack.audit.model.readmodel.RepeatedAuditAction;
import com.fintrack.audit.service.AuditStatsService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

// Implemente les regles metier du domaine audit statistiques.

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditStatsServiceImpl implements AuditStatsService {

  // Actions sensibles = mutations privilegiees reellement emises par les services
  // (securite comptes/roles, structure org., parametres, export, actes destructifs).
  private static final Set<String> SENSITIVE_ACTIONS = Set.of(
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
    "REPORT_EXPORT",
    "REPORT_DELETE",
    "REPORT_RERUN",
    "AUDIT_EXPORT",
    "AUDIT_PURGE",
    "JOB_TRIGGER",
    "ESCALATION_RULE_TOGGLE",
    "ESCALATION_RULE_TRIGGER",
    "INCIDENT_CLOSE",
    "INCIDENT_DELETE"
  );

  private final MongoTemplate mongoTemplate;

  // Calcule stats.
  @Override
  public AuditStats computeStats(
    LocalDateTime from,
    LocalDateTime to,
    int sampleSize,
    int repeatedThreshold
  ) {
    int safeSample = Math.max(1, Math.min(100, sampleSize));
    int safeRepeated = Math.max(2, repeatedThreshold);
    Criteria baseCriteria = buildBaseCriteria(from, to);

    Map<String, Long> byAction = aggregateCountBy("action", baseCriteria);
    Map<String, Long> byStatus = aggregateCountBy("status", baseCriteria);
    long total = byAction.values().stream().mapToLong(Long::longValue).sum();
    long sensitiveCount = byAction
      .entrySet()
      .stream()
      .filter(entry -> SENSITIVE_ACTIONS.contains(entry.getKey()))
      .mapToLong(Map.Entry::getValue)
      .sum();

    Query recentQuery = new Query(baseCriteria)
      .with(Sort.by(Sort.Direction.DESC, "timestamp"))
      .limit(safeSample);
    List<AuditLog> recentLogs = mongoTemplate.find(recentQuery, AuditLog.class);

    Criteria sensitiveCriteria = new Criteria().andOperator(
      baseCriteria,
      Criteria.where("action").in(SENSITIVE_ACTIONS)
    );
    Query sensitiveQuery = new Query(sensitiveCriteria)
      .with(Sort.by(Sort.Direction.DESC, "timestamp"))
      .limit(safeSample);
    List<AuditLog> recentSensitive = mongoTemplate.find(
      sensitiveQuery,
      AuditLog.class
    );

    // Inclut la matrice des roles et les droits ajoutes/revoques directement sur un utilisateur.
    Criteria directPermissionChange = new Criteria().andOperator(
      Criteria.where("action").is("USER_UPDATE"),
      new Criteria().orOperator(
        Criteria.where("details.roles").exists(true),
        Criteria.where("details.permissions").exists(true),
        Criteria.where("details.revokedPermissions").exists(true),
        Criteria.where("details.operation").in(
          "assign_permissions",
          "add_permission",
          "remove_permission"
        )
      )
    );
    Criteria permissionCriteria = new Criteria().andOperator(
      baseCriteria,
      new Criteria().orOperator(
        Criteria.where("action").regex("^ROLE_"),
        directPermissionChange
      )
    );
    Query permissionQuery = new Query(permissionCriteria)
      .with(Sort.by(Sort.Direction.DESC, "timestamp"))
      .limit(safeSample);
    List<AuditLog> permissionHistory = mongoTemplate.find(
      permissionQuery,
      AuditLog.class
    );

    List<RepeatedAuditAction> repeatedSensitiveActions =
      aggregateRepeatedSensitive(baseCriteria, safeRepeated, safeSample);

    Criteria failureCriteria = new Criteria().andOperator(
      baseCriteria,
      Criteria.where("status").is(AuditStatus.FAILURE)
    );
    Map<String, Long> failuresByResourceType = aggregateCountBy(
      "resource_type",
      failureCriteria
    );

    Map<Integer, Map<Integer, Long>> heatmapLast7Days =
      aggregateHeatmapLast7Days();

    return AuditStats.builder()
      .total(total)
      .byAction(byAction)
      .byStatus(byStatus)
      .sensitiveCount(sensitiveCount)
      .recentLogs(recentLogs)
      .recentSensitive(recentSensitive)
      .permissionChangeHistory(permissionHistory)
      .repeatedSensitiveActions(repeatedSensitiveActions)
      .failuresByResourceType(failuresByResourceType)
      .heatmapLast7Days(heatmapLast7Days)
      .computedAt(Instant.now())
      .build();
  }

  // $group sur (dayOfWeek, hour) du timestamp sur la fenetre des 7 derniers jours.
  // Independent de la fenetre demandee (le heatmap est toujours sur 7j glissants).
  private Map<Integer, Map<Integer, Long>> aggregateHeatmapLast7Days() {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
    Aggregation aggregation = Aggregation.newAggregation(
      Aggregation.match(Criteria.where("timestamp").gte(cutoff)),
      Aggregation.project()
        .and(DateOperators.DayOfWeek.dayOfWeek("timestamp"))
        .as("d")
        .and(DateOperators.Hour.hour("timestamp"))
        .as("h"),
      Aggregation.group("d", "h").count().as("count")
    );
    try {
      AggregationResults<Map> results = mongoTemplate.aggregate(
        aggregation,
        "audit_logs",
        Map.class
      );
      Map<Integer, Map<Integer, Long>> out = new LinkedHashMap<>();
      for (Map<?, ?> row : results.getMappedResults()) {
        Object idObject = row.get("_id");
        if (!(idObject instanceof Map<?, ?> idMap)) continue;
        int day = idMap.get("d") instanceof Number n1 ? n1.intValue() : 0;
        int hour = idMap.get("h") instanceof Number n2 ? n2.intValue() : 0;
        long value =
          row.get("count") instanceof Number n3 ? n3.longValue() : 0L;
        out.computeIfAbsent(day, k -> new LinkedHashMap<>()).put(hour, value);
      }
      return out;
    } catch (RuntimeException ex) {
      log.warn(
        "Échec de l'agrégation de carte thermique d'audit: {}",
        ex.toString()
      );
      return Map.of();
    }
  }

  // Construit la representation attendue pour le domaine audit statistiques.

  private Criteria buildBaseCriteria(LocalDateTime from, LocalDateTime to) {
    Criteria criteria = new Criteria();
    if (from != null && to != null) {
      criteria.and("timestamp").gte(from).lte(to);
    } else if (from != null) {
      criteria.and("timestamp").gte(from);
    } else if (to != null) {
      criteria.and("timestamp").lte(to);
    }
    return criteria;
  }

  // Agrege count par.

  private Map<String, Long> aggregateCountBy(
    String field,
    Criteria baseCriteria
  ) {
    Aggregation aggregation = Aggregation.newAggregation(
      Aggregation.match(baseCriteria),
      Aggregation.group(field).count().as("count"),
      Aggregation.sort(Sort.Direction.DESC, "count")
    );
    try {
      AggregationResults<Map> results = mongoTemplate.aggregate(
        aggregation,
        "audit_logs",
        Map.class
      );
      Map<String, Long> out = new LinkedHashMap<>();
      for (Map<?, ?> row : results.getMappedResults()) {
        Object id = row.get("_id");
        Object count = row.get("count");
        String key = id == null ? "UNKNOWN" : id.toString();
        long value = count instanceof Number number ? number.longValue() : 0L;
        out.put(key, value);
      }
      return out;
    } catch (RuntimeException ex) {
      log.warn(
        "Échec de l'agrégation des statistiques d'audit par {} a echoue: {}",
        field,
        ex.toString()
      );
      return Map.of();
    }
  }

  // Agrege repeated sensitive.

  private List<RepeatedAuditAction> aggregateRepeatedSensitive(
    Criteria baseCriteria,
    int threshold,
    int sampleSize
  ) {
    Criteria criteria = new Criteria().andOperator(
      baseCriteria,
      Criteria.where("action").in(SENSITIVE_ACTIONS)
    );
    Aggregation aggregation = Aggregation.newAggregation(
      Aggregation.match(criteria),
      Aggregation.group("username", "action").count().as("count"),
      Aggregation.match(Criteria.where("count").gte(threshold)),
      Aggregation.sort(Sort.Direction.DESC, "count"),
      Aggregation.limit(sampleSize)
    );
    try {
      AggregationResults<Map> results = mongoTemplate.aggregate(
        aggregation,
        "audit_logs",
        Map.class
      );
      return results
        .getMappedResults()
        .stream()
        .map(row -> {
          Object idObject = row.get("_id");
          String username = "unknown";
          String action = "UNKNOWN";
          if (idObject instanceof Map<?, ?> id) {
            Object u = id.get("username");
            Object a = id.get("action");
            if (u != null) {
              username = u.toString();
            }
            if (a != null) {
              action = a.toString();
            }
          }
          long count =
            row.get("count") instanceof Number number ? number.longValue() : 0L;
          return new RepeatedAuditAction(
            username + ":" + action,
            username,
            action,
            count
          );
        })
        .toList();
    } catch (RuntimeException ex) {
      log.warn("Échec de l'agrégation sensible répétée: {}", ex.toString());
      return List.of();
    }
  }
}
