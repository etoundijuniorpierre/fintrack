// Client inter-services : communique avec les services externes lies a notification.

package com.fintrack.incident.client.notification;

import com.fintrack.incident.client.notification.dto.NotificationClientRequest;
import com.fintrack.incident.client.notification.fallback.NotificationClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Client Feign vers le service de notification pour creer des notifications
@FeignClient(
  name = "notification-service",
  url = "${notification.service.url}",
  fallback = NotificationClientFallback.class
)
// Definit le contrat notification attendu par les autres couches.
public interface NotificationClient {
  // Cet endpoint demande la creation d'une notification au service de notification
  @PostMapping("/api/v1/notificationService/notifications")
  void createNotification(@RequestBody NotificationClientRequest request);
}
