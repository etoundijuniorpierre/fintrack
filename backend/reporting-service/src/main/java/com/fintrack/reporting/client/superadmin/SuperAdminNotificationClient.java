// Client inter-services : communique avec les services externes lies a super admin notification.

package com.fintrack.reporting.client.superadmin;

import com.fintrack.reporting.client.superadmin.dto.NotificationClientResponse;
import com.fintrack.reporting.client.superadmin.dto.NotificationStatsClientResponse;
import com.fintrack.reporting.client.superadmin.dto.NotificationsPageClientResponse;
import com.fintrack.reporting.client.superadmin.fallback.SuperAdminNotificationClientFallback;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
  name = "notification-service",
  contextId = "superAdminNotificationClient",
  url = "${notification.service.url}",
  fallback = SuperAdminNotificationClientFallback.class
)
// Definit le contrat super admin notification attendu par les autres couches.
public interface SuperAdminNotificationClient {
  @GetMapping("/api/v1/notificationService/notifications")
  // Fournit notifications au cas d usage appelant.
  NotificationsPageClientResponse getNotifications(
    @RequestParam("page") int page,
    @RequestParam("size") int size,
    @RequestParam("sort") String sort
  );

  @GetMapping("/api/v1/notificationService/notifications/all")
  // Fournit global notifications au cas d usage appelant.
  List<NotificationClientResponse> getAllNotifications();

  @GetMapping("/api/v1/notificationService/notifications/stats")
  // Fournit statistiques au cas d usage appelant.
  NotificationStatsClientResponse getStats(
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
    @RequestParam(value = "sampleSize", defaultValue = "20") int sampleSize
  );

  @GetMapping("/api/v1/notificationService/notifications/{id}")
  // Fournit notification by identifiant au cas d usage appelant.
  NotificationClientResponse getNotificationById(@PathVariable("id") String id);

  @RequestMapping(
    method = RequestMethod.DELETE,
    value = "/api/v1/notificationService/notifications/purge"
  )
  // Traite en masse purge.
  Map<String, Object> bulkPurge(@RequestBody Map<String, Object> request);
}
