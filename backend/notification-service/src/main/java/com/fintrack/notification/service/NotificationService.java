// Contrat metier : expose les operations du domaine notification.

package com.fintrack.notification.service;

import com.fintrack.notification.model.constant.NotificationStatus;
import com.fintrack.notification.model.entity.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// Interface du service de traitement et d'envoi des notifications.
// Le service porte la logique metier (perimetre de visibilite, controles
// d'acces, transitions d'etat) et travaille uniquement avec des entites ;
// la conversion DTO est assuree par le mapper, cote controller.

public interface NotificationService {
  // Fournit visible a la couche appelante.

  Page<Notification> getVisible(
    String requester,
    boolean canViewAll,
    Pageable pageable
  );
  // Fournit visible a la couche appelante.

  List<Notification> getVisible(String requester, boolean canViewAll);
  // Fournit visible by id a la couche appelante.

  Notification getVisibleById(String id, String requester, boolean canViewAll);
  // Fournit visible by statut a la couche appelante.

  List<Notification> getVisibleByStatus(
    NotificationStatus status,
    String requester,
    boolean canViewAll
  );
  // Fournit visible by incident id a la couche appelante.

  List<Notification> getVisibleByIncidentId(
    UUID incidentId,
    String requester,
    boolean canViewAll
  );
  // Fournit by recipient a la couche appelante.

  List<Notification> getByRecipient(
    String recipient,
    String requester,
    boolean canViewAll
  );
  // Fournit by recipient pagine a la couche appelante.

  Page<Notification> getByRecipient(
    String recipient,
    String requester,
    boolean canViewAll,
    Pageable pageable
  );
  // Prepare l'enregistrement de la ressource selon les regles metier.

  Notification create(Notification notification);
  // Applique le changement demande apres validation metier.

  Notification markAsRead(String id, String requester, boolean canViewAll);
  // Applique le changement demande apres validation metier.

  List<Notification> markAllAsRead(String requester, boolean canViewAll);
  // Supprime ou invalide les donnees ciblees apres controle metier.

  void delete(String id, String requester, boolean canManageAll);
  // Traite en masse delete.

  void bulkDelete(List<String> ids);
}
