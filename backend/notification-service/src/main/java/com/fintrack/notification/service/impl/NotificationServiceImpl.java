// Service metier : coordonne les operations du domaine notification.

package com.fintrack.notification.service.impl;

import com.fintrack.notification.client.reporting.ReportingSystemConfigClientService;
import com.fintrack.notification.exception.EntityNotFoundException;
import com.fintrack.notification.exception.ErrorCode;
import com.fintrack.notification.model.constant.NotificationStatus;
import com.fintrack.notification.model.constant.NotificationType;
import com.fintrack.notification.model.entity.Notification;
import com.fintrack.notification.repository.NotificationRepository;
import com.fintrack.notification.service.MailService;
import com.fintrack.notification.service.NotificationService;
import com.fintrack.notification.service.NotificationWebSocketService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

// Implementation du service d'envoi et de gestion des notifications.

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository notificationRepository;
  private final MailService mailService;
  private final NotificationWebSocketService webSocketService;
  private final MongoTemplate mongoTemplate;
  private final ReportingSystemConfigClientService reportingSystemConfigClientService;

  /** Repli si le seuil configuré (reporting) est indisponible. Aligné sur la valeur par défaut côté reporting. */
  private static final int MAX_RETRY_COUNT = 5;

  /** Nombre de notifications en échec scannées par passage du job de retry (réglage infra). */
  @Value("${fintrack.notification.failed-scan-page-size:50}")
  private int failedScanPageSize;

  // Fournit visible a la couche appelante.

  @Override
  public Page<Notification> getVisible(
    String requester,
    boolean canViewAll,
    Pageable pageable
  ) {
    return canViewAll
      ? notificationRepository.findAll(pageable)
      : notificationRepository.findByRecipient(requester, pageable);
  }

  // Fournit visible a la couche appelante.

  @Override
  public List<Notification> getVisible(String requester, boolean canViewAll) {
    return canViewAll
      ? notificationRepository.findAll()
      : notificationRepository.findByRecipient(requester);
  }

  // Fournit visible by id a la couche appelante.

  @Override
  public Notification getVisibleById(
    String id,
    String requester,
    boolean canViewAll
  ) {
    Notification notification = findEntity(id);
    assertCanRead(notification, requester, canViewAll);
    return notification;
  }

  // Fournit visible by statut a la couche appelante.

  @Override
  public List<Notification> getVisibleByStatus(
    NotificationStatus status,
    String requester,
    boolean canViewAll
  ) {
    return canViewAll
      ? notificationRepository.findByStatus(status)
      : notificationRepository.findByRecipientAndStatus(requester, status);
  }

  // Fournit visible by incident id a la couche appelante.

  @Override
  public List<Notification> getVisibleByIncidentId(
    UUID incidentId,
    String requester,
    boolean canViewAll
  ) {
    return canViewAll
      ? notificationRepository.findByIncidentId(incidentId)
      : notificationRepository.findByRecipientAndIncidentId(
          requester,
          incidentId
        );
  }

  // Fournit by recipient a la couche appelante.

  @Override
  public List<Notification> getByRecipient(
    String recipient,
    String requester,
    boolean canViewAll
  ) {
    assertCanReadRecipient(recipient, requester, canViewAll);
    return notificationRepository.findByRecipient(recipient);
  }

  // Fournit by recipient pagine a la couche appelante.

  @Override
  public Page<Notification> getByRecipient(
    String recipient,
    String requester,
    boolean canViewAll,
    Pageable pageable
  ) {
    assertCanReadRecipient(recipient, requester, canViewAll);
    return notificationRepository.findByRecipient(recipient, pageable);
  }

  // Recupere l'entite ou leve une 404 usage interne (lecture detaillee, transitions d'etat).
  private Notification findEntity(String id) {
    return notificationRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException(
          ErrorCode.NOTIFICATION_NOT_FOUND,
          "Notification introuvable avec l'identifiant : " + id
        )
      );
  }

  // Un utilisateur ne voit une notification que si elle lui est destinee, sauf droit global.
  private void assertCanRead(
    Notification notification,
    String requester,
    boolean canViewAll
  ) {
    if (canViewAll || notification.getRecipient().equalsIgnoreCase(requester)) {
      return;
    }
    throw new AccessDeniedException(
      "Vous n'avez pas la permission d'acceder a cette notification"
    );
  }

  // Verifie que les regles metier autorisent l operation sur notification.

  private void assertCanReadRecipient(
    String recipient,
    String requester,
    boolean canViewAll
  ) {
    if (canViewAll || recipient.equalsIgnoreCase(requester)) {
      return;
    }
    throw new AccessDeniedException(
      "Vous n'avez pas la permission d'acceder aux notifications de ce destinataire"
    );
  }

  @Override
  // Cree un element du domaine notification apres validation metier.
  public Notification create(Notification notification) {
    String idempotencyKey = notification.getIdempotencyKey();
    if (StringUtils.hasText(idempotencyKey)) {
      Notification existing = notificationRepository
        .findByIdempotencyKey(idempotencyKey)
        .orElse(null);
      if (existing != null) {
        log.info(
          "Notification idempotente deja existante pour destinataire {} et cle {}",
          existing.getRecipient(),
          idempotencyKey
        );
        return existing;
      }
    } else {
      notification.setIdempotencyKey(null);
    }

    notification.setStatus(NotificationStatus.PENDING);
    notification.setRetryCount(0);
    log.info(
      "Création de notification pour destinataire: {}",
      notification.getRecipient()
    );
    Notification saved;
    try {
      saved = notificationRepository.save(notification);
    } catch (DuplicateKeyException ex) {
      if (StringUtils.hasText(idempotencyKey)) {
        return notificationRepository
          .findByIdempotencyKey(idempotencyKey)
          .orElseThrow(() -> ex);
      }
      throw ex;
    }

    if (saved.getType() == NotificationType.EMAIL) {
      // Envoi e-mail asynchrone
      CompletableFuture.runAsync(() -> sendEmailSync(saved.getId()));
    } else {
      // Notification INTERNAL : livree au tableau de bord de l'utilisateur en attente de lecture (PENDING).
    }

    // Pousser la notification en temps reel au destinataire connecte via WebSocket.
    webSocketService.pushToRecipient(saved);

    return saved;
  }

  // Diffuse l'information du domaine notification aux destinataires concernes.

  private void sendEmailSync(String id) {
    try {
      Notification notification = notificationRepository
        .findById(id)
        .orElse(null);
      if (
        notification == null ||
        (notification.getStatus() != NotificationStatus.PENDING &&
          notification.getStatus() != NotificationStatus.FAILED)
      ) {
        return;
      }
      mailService.send(notification);
      notification.setStatus(NotificationStatus.SENT);
      notification.setSentAt(LocalDateTime.now());
      notification.setNextRetry(null);
      notificationRepository.save(notification);
    } catch (Exception e) {
      log.warn(
        "Échec d'envoi de l'e-mail à la notification {} : {}",
        id,
        e.getMessage()
      );
      Notification notification = notificationRepository
        .findById(id)
        .orElse(null);
      if (notification != null) {
        notification.setStatus(NotificationStatus.FAILED);
        notification.setRetryCount(
          (notification.getRetryCount() == null
            ? 0
            : notification.getRetryCount()) + 1
        );
        notification.setRetryAt(LocalDateTime.now());
        notification.setNextRetry(LocalDateTime.now().plusMinutes(30));
        notificationRepository.save(notification);
      }
    }
  }

  // Applique le changement demande apres validation metier.

  @Override
  public Notification markAsRead(
    String id,
    String requester,
    boolean canViewAll
  ) {
    Notification notification = getVisibleById(id, requester, canViewAll);

    if (notification.getStatus() == NotificationStatus.READ) {
      return notification;
    }

    notification.setStatus(NotificationStatus.READ);
    log.info("Marquage de la notification {} comme LUES", id);
    return notificationRepository.save(notification);
  }

  // Applique le changement demande apres validation metier.

  @Override
  public List<Notification> markAllAsRead(
    String requester,
    boolean canViewAll
  ) {
    Query query = new Query();
    if (!canViewAll) {
      query.addCriteria(Criteria.where("recipient").is(requester));
    }
    query.addCriteria(Criteria.where("status").ne(NotificationStatus.READ));

    Update update = new Update().set("status", NotificationStatus.READ);

    log.info(
      "Marquage de la notifications comme LUES pour {}",
      canViewAll ? "global scope" : requester
    );
    mongoTemplate.updateMulti(query, update, Notification.class);

    // Retourne la liste mise a jour
    return getVisibleByStatus(NotificationStatus.READ, requester, canViewAll);
  }

  // Traite en masse delete.

  @Override
  public void bulkDelete(List<String> ids) {
    if (ids == null || ids.isEmpty()) return;
    notificationRepository.deleteAllById(ids);
    log.info("Suppression groupee de {} notifications", ids.size());
  }

  // Relance les notifications en echec.

  @Scheduled(fixedRate = 5 * 60 * 1000)
  public void retryFailedNotifications() {
    LocalDateTime now = LocalDateTime.now();
    Query query = new Query(
      Criteria.where("status")
        .is(NotificationStatus.FAILED)
        .and("next_retry")
        .lte(now)
    )
      .with(Sort.by(Sort.Direction.ASC, "next_retry"))
      .limit(failedScanPageSize);

    List<Notification> due = mongoTemplate.find(query, Notification.class);
    if (due.isEmpty()) {
      return;
    }
    log.info(
      "Nouvelle tentative pour {} a echoue e-mail notification(s)",
      due.size()
    );

    // Seuil pilote par le Super Admin (reporting-service), repli sur la constante locale.
    int maxRetry = reportingSystemConfigClientService.getThresholdInt(
      "notificationMaxRetryCount",
      MAX_RETRY_COUNT
    );

    for (Notification notification : due) {
      int retryCount =
        notification.getRetryCount() == null ? 0 : notification.getRetryCount();
      if (retryCount >= maxRetry) {
        log.error(
          "Notification {} abandonnee apres {} tentatives",
          notification.getId(),
          retryCount
        );
        notification.setNextRetry(null);
        notificationRepository.save(notification);
        continue;
      }
      // Use the sync method which handles the catch block safely
      sendEmailSync(notification.getId());
    }
  }

  @Override
  // Supprime sa propre notification (ou n'importe laquelle avec NOTIFICATION_MANAGE).
  public void delete(String id, String requester, boolean canManageAll) {
    Notification notification = findEntity(id);
    assertCanRead(notification, requester, canManageAll);
    notificationRepository.deleteById(id);
    log.info("Supprimé notification avec id: {}", id);
  }
}
