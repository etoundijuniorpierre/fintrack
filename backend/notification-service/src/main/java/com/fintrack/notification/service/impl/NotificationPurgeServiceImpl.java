// Service metier : coordonne les operations du domaine notification purge.

package com.fintrack.notification.service.impl;

import com.fintrack.notification.model.entity.Notification;
import com.fintrack.notification.model.readmodel.NotificationPurgeResult;
import com.fintrack.notification.service.NotificationPurgeService;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

// Implemente les regles metier du domaine notification purge.

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPurgeServiceImpl implements NotificationPurgeService {

  private final MongoTemplate mongoTemplate;

  // Supprime ou invalide les donnees ciblees apres controle metier.

  @Override
  public NotificationPurgeResult purge(
    int olderThanDays,
    int preserveLastN,
    boolean dryRun
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

    Query candidateQuery = new Query(
      Criteria.where("created_at").lt(effectiveCutoff)
    );
    long deletedCount = mongoTemplate.count(candidateQuery, Notification.class);
    long totalPreserved =
      mongoTemplate.count(new Query(), Notification.class) - deletedCount;

    if (!dryRun) {
      long actual = mongoTemplate
        .remove(candidateQuery, Notification.class)
        .getDeletedCount();
      log.info(
        "Purge des notifications executee: {} notifications supprimees (plus anciens que {} jours, conserverDerniersN={})",
        actual,
        olderThanDays,
        preserveLastN
      );
      deletedCount = actual;
      totalPreserved = mongoTemplate.count(new Query(), Notification.class);
    }

    return NotificationPurgeResult.builder()
      .scope("notifications")
      .deletedCount(deletedCount)
      .preservedCount(totalPreserved)
      .dryRun(dryRun)
      .executedAt(Instant.now())
      .build();
  }

  // Resout preserve cutoff a partir du contexte disponible.

  private LocalDateTime resolvePreserveCutoff(int preserveLastN) {
    if (preserveLastN <= 0) {
      return null;
    }
    Query query = new Query()
      .with(Sort.by(Sort.Direction.DESC, "created_at"))
      .skip(preserveLastN - 1L)
      .limit(1);
    Notification nth = mongoTemplate.findOne(query, Notification.class);
    return nth == null ? null : nth.getCreatedAt();
  }
}
