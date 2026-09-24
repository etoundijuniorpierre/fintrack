// Client inter-services : communique avec les services externes lies a notification.

package com.fintrack.reporting.client.notification;

import com.fintrack.reporting.model.dto.request.notification.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", url = "${notification.service.url}")
// Definit le contrat notification attendu par les autres couches.
public interface NotificationClient {
  // Prepare l'enregistrement de la ressource selon les regles metier.

  @PostMapping("/api/v1/notificationService/notifications")
  void createNotification(@RequestBody NotificationRequest request);
}
