// Client inter-services : communique avec les services externes lies a notification.

package com.fintrack.user.client.notification;

import com.fintrack.user.model.dto.request.notification.NotificationRequest;
import com.fintrack.user.model.dto.response.PresenceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", url = "${notification.service.url}")
// Definit le contrat notification attendu par les autres couches.
public interface NotificationClient {
  // Recupere la presence courante maintenue par les sessions WebSocket.
  @GetMapping("/api/v1/notificationService/presence")
  PresenceResponse getPresence();

  // Prepare l'enregistrement de la ressource selon les regles metier.

  @PostMapping("/api/v1/notificationService/notifications")
  void createNotification(@RequestBody NotificationRequest request);
}
