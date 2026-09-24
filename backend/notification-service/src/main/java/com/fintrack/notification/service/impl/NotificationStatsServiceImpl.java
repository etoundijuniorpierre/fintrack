// Service metier : coordonne les operations du domaine notification statistiques.

package com.fintrack.notification.service.impl;

import com.fintrack.notification.model.constant.NotificationStatus;
import com.fintrack.notification.model.constant.NotificationType;
import com.fintrack.notification.model.entity.Notification;
import com.fintrack.notification.model.readmodel.NotificationStats;
import com.fintrack.notification.service.NotificationStatsService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

// Implemente les regles metier du domaine notification statistiques.

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationStatsServiceImpl implements NotificationStatsService {

  // Regex cote Mongo : @ suivi d'un caractere, puis . avant la fin.
  private static final String EMAIL_REGEX = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

  private final MongoTemplate mongoTemplate;

  // Calcule stats.
  @Override
  public NotificationStats computeStats(
    LocalDateTime from,
    LocalDateTime to,
    int sampleSize
  ) {
    int safeSample = Math.max(1, Math.min(100, sampleSize));
    Criteria baseCriteria = buildBaseCriteria(from, to);

    Map<String, Long> byStatus = aggregateCountBy("status", baseCriteria);
    Map<String, Long> byType = aggregateCountBy("type", baseCriteria);
    Map<String, Map<String, Long>> byTypeAndStatus =
      aggregateCountByTypeAndStatus(baseCriteria);

    long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
    long sent = byStatus.getOrDefault(NotificationStatus.SENT.name(), 0L);
    long failed = byStatus.getOrDefault(NotificationStatus.FAILED.name(), 0L);
    long pending = byStatus.getOrDefault(NotificationStatus.PENDING.name(), 0L);

    Query failedQuery = new Query(buildFailedCriteria(baseCriteria))
      .with(Sort.by(Sort.Direction.DESC, "created_at"))
      .limit(safeSample);
    List<Notification> failedNotifications = mongoTemplate.find(
      failedQuery,
      Notification.class
    );

    Criteria invalidCriteria = new Criteria().andOperator(
      baseCriteria,
      new Criteria().orOperator(
        Criteria.where("recipient").exists(false),
        Criteria.where("recipient").is(null),
        Criteria.where("recipient").regex("^\\s*$"),
        new Criteria().andOperator(
          Criteria.where("type").is(NotificationType.EMAIL),
          Criteria.where("recipient").not().regex(EMAIL_REGEX)
        )
      )
    );
    long invalidRecipientCount = mongoTemplate.count(
      new Query(invalidCriteria),
      Notification.class
    );
    Query invalidQuery = new Query(invalidCriteria)
      .with(Sort.by(Sort.Direction.DESC, "created_at"))
      .limit(safeSample);
    List<Notification> invalidNotifications = mongoTemplate.find(
      invalidQuery,
      Notification.class
    );

    Criteria sentWithoutTraceCriteria = new Criteria().andOperator(
      baseCriteria,
      Criteria.where("status").is(NotificationStatus.SENT),
      new Criteria().orOperator(
        Criteria.where("sent_at").exists(false),
        Criteria.where("sent_at").is(null)
      )
    );
    long sentWithoutTraceCount = mongoTemplate.count(
      new Query(sentWithoutTraceCriteria),
      Notification.class
    );
    Query sentWithoutTraceQuery = new Query(sentWithoutTraceCriteria)
      .with(Sort.by(Sort.Direction.DESC, "created_at"))
      .limit(safeSample);
    List<Notification> sentWithoutTraceNotifications = mongoTemplate.find(
      sentWithoutTraceQuery,
      Notification.class
    );

    return NotificationStats.builder()
      .total(total)
      .sent(sent)
      .failed(failed)
      .pending(pending)
      .byStatus(byStatus)
      .byType(byType)
      .byTypeAndStatus(byTypeAndStatus)
      .invalidRecipientCount(invalidRecipientCount)
      .sentWithoutTraceCount(sentWithoutTraceCount)
      .failedSample(failedNotifications)
      .invalidRecipientsSample(invalidNotifications)
      .sentWithoutTraceSample(sentWithoutTraceNotifications)
      .computedAt(Instant.now())
      .build();
  }

  // $group sur {type, status} pour obtenir le decoupage EMAIL/INTERNAL x SENT/FAILED/PENDING.
  private Map<String, Map<String, Long>> aggregateCountByTypeAndStatus(
    Criteria baseCriteria
  ) {
    Aggregation aggregation = Aggregation.newAggregation(
      Aggregation.match(baseCriteria),
      Aggregation.group("type", "status").count().as("count")
    );
    try {
      AggregationResults<Map> results = mongoTemplate.aggregate(
        aggregation,
        "notifications",
        Map.class
      );
      Map<String, Map<String, Long>> out = new LinkedHashMap<>();
      for (Map<?, ?> row : results.getMappedResults()) {
        Object id = row.get("_id");
        if (!(id instanceof Map<?, ?> idMap)) continue;
        String type =
          idMap.get("type") == null ? "UNKNOWN" : idMap.get("type").toString();
        String status =
          idMap.get("status") == null
            ? "UNKNOWN"
            : idMap.get("status").toString();
        long value =
          row.get("count") instanceof Number number ? number.longValue() : 0L;
        out
          .computeIfAbsent(type, k -> new LinkedHashMap<>())
          .put(status, value);
      }
      return out;
    } catch (RuntimeException ex) {
      log.warn(
        "Agregation des statistiques de notification par type et statut a echoue: {}",
        ex.toString()
      );
      return Map.of();
    }
  }

  // Construit la representation attendue pour le domaine notification statistiques.

  private Criteria buildBaseCriteria(LocalDateTime from, LocalDateTime to) {
    Criteria criteria = new Criteria();
    if (from != null && to != null) {
      criteria.and("created_at").gte(from).lte(to);
    } else if (from != null) {
      criteria.and("created_at").gte(from);
    } else if (to != null) {
      criteria.and("created_at").lte(to);
    }
    return criteria;
  }

  // Construit la representation attendue pour le domaine notification statistiques.

  private Criteria buildFailedCriteria(Criteria base) {
    return new Criteria().andOperator(
      base,
      Criteria.where("status").is(NotificationStatus.FAILED)
    );
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
        "notifications",
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
        "Agregation des statistiques de notification par {} a echoue: {}",
        field,
        ex.toString()
      );
      return Map.of();
    }
  }
}
