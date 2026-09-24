// Acces aux donnees : expose les requetes persistantes liees a notification.

package com.fintrack.notification.repository;

import com.fintrack.notification.model.constant.NotificationStatus;
import com.fintrack.notification.model.entity.Notification;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

// Repertoire Spring Data MongoDB pour la gestion des notifications.

@Repository
public interface NotificationRepository
  extends MongoRepository<Notification, String>
{
  // Recherche les notifications par statut.
  List<Notification> findByStatus(NotificationStatus status);
  // Recherche les notifications par incident identifiant.
  List<Notification> findByIncidentId(UUID incidentId);
  // Recherche une notification par cle d'idempotence.
  Optional<Notification> findByIdempotencyKey(String idempotencyKey);
  // Recherche les notifications par destinataire.
  List<Notification> findByRecipient(String recipient);
  // Recherche les notifications par destinataire avec pagination.
  Page<Notification> findByRecipient(String recipient, Pageable pageable);
  // Recherche les notifications par destinataire and statut.
  List<Notification> findByRecipientAndStatus(
    String recipient,
    NotificationStatus status
  );
  // Recherche les notifications par statut not.
  List<Notification> findByStatusNot(NotificationStatus status);
  // Recherche les notifications par destinataire and statut not.
  List<Notification> findByRecipientAndStatusNot(
    String recipient,
    NotificationStatus status
  );
  // Recherche les notifications par destinataire and incident identifiant.
  List<Notification> findByRecipientAndIncidentId(
    String recipient,
    UUID incidentId
  );
  // Recherche les notifications par statut and next retry date maximale.
  List<Notification> findByStatusAndNextRetryBefore(
    NotificationStatus status,
    LocalDateTime dateTime
  );
}
