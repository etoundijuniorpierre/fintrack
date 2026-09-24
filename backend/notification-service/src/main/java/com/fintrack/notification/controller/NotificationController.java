// Controleur REST : expose les operations HTTP liees a notification.

package com.fintrack.notification.controller;

import com.fintrack.notification.constant.ApiConstants;
import com.fintrack.notification.model.constant.NotificationStatus;
import com.fintrack.notification.model.dto.request.BulkPurgeRequest;
import com.fintrack.notification.model.dto.request.NotificationRequest;
import com.fintrack.notification.model.dto.response.BulkPurgeResponse;
import com.fintrack.notification.model.dto.response.NotificationResponse;
import com.fintrack.notification.model.dto.response.NotificationStatsResponse;
import com.fintrack.notification.model.mapper.NotificationMapper;
import com.fintrack.notification.service.NotificationPurgeService;
import com.fintrack.notification.service.NotificationService;
import com.fintrack.notification.service.NotificationStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Controleur REST exposant les operations de gestion des notifications.
// Adaptateur HTTP : il recoit/retourne des DTO, extrait l'identite et le
// perimetre (droit global ou non), delegue la logique metier au service et
// confie toute conversion entite <-> DTO au mapper.
@RestController
@RequestMapping(ApiConstants.Endpoints.NOTIFICATIONS)
@Tag(
  name = "Notification Management",
  description = "Endpoints for managing notifications"
)
@RequiredArgsConstructor
public class NotificationController {

  private static final String NOTIFICATION_VIEW_ALL = "NOTIFICATION_VIEW_ALL";
  private static final String NOTIFICATION_MANAGE = "NOTIFICATION_MANAGE";

  private final NotificationService notificationService;
  private final NotificationMapper notificationMapper;
  private final NotificationStatsService notificationStatsService;
  private final NotificationPurgeService notificationPurgeService;

  @GetMapping
  @PreAuthorize(
    "hasAnyAuthority('NOTIFICATION_VIEW_OWN', 'NOTIFICATION_VIEW_ALL')"
  )
  @Operation(summary = "Get notifications with pagination")
  // Fournit global notifications au cas d usage appelant.
  public ResponseEntity<Page<NotificationResponse>> getAllNotifications(
    Pageable pageable,
    Authentication authentication
  ) {
    return ResponseEntity.ok(
      notificationService
        .getVisible(
          authentication.getName(),
          canViewAll(authentication),
          pageable
        )
        .map(notificationMapper::toResponse)
    );
  }

  // Fournit all notifications list a la couche appelante.

  @GetMapping("/all")
  @PreAuthorize(
    "hasAnyAuthority('NOTIFICATION_VIEW_OWN', 'NOTIFICATION_VIEW_ALL')"
  )
  @Operation(summary = "Get notifications as a list")
  public ResponseEntity<List<NotificationResponse>> getAllNotificationsList(
    Authentication authentication
  ) {
    return ResponseEntity.ok(
      notificationMapper.toResponses(
        notificationService.getVisible(
          authentication.getName(),
          canViewAll(authentication)
        )
      )
    );
  }

  @GetMapping("/{id}")
  @PreAuthorize(
    "hasAnyAuthority('NOTIFICATION_VIEW_OWN', 'NOTIFICATION_VIEW_ALL')"
  )
  @Operation(summary = "Get notification by ID")
  // Fournit notification by identifiant au cas d usage appelant.
  public ResponseEntity<NotificationResponse> getNotificationById(
    @PathVariable String id,
    Authentication authentication
  ) {
    return ResponseEntity.ok(
      notificationMapper.toResponse(
        notificationService.getVisibleById(
          id,
          authentication.getName(),
          canViewAll(authentication)
        )
      )
    );
  }

  @GetMapping("/status/{status}")
  @PreAuthorize(
    "hasAnyAuthority('NOTIFICATION_VIEW_OWN', 'NOTIFICATION_VIEW_ALL')"
  )
  @Operation(summary = "Get notifications by status")
  // Fournit notifications by statut au cas d usage appelant.
  public ResponseEntity<List<NotificationResponse>> getNotificationsByStatus(
    @PathVariable NotificationStatus status,
    Authentication authentication
  ) {
    return ResponseEntity.ok(
      notificationMapper.toResponses(
        notificationService.getVisibleByStatus(
          status,
          authentication.getName(),
          canViewAll(authentication)
        )
      )
    );
  }

  @GetMapping("/incident/{incidentId}")
  @PreAuthorize(
    "hasAnyAuthority('NOTIFICATION_VIEW_OWN', 'NOTIFICATION_VIEW_ALL')"
  )
  @Operation(summary = "Get notifications for a given incident")
  // Fournit notifications by incident identifiant au cas d usage appelant.
  public ResponseEntity<
    List<NotificationResponse>
  > getNotificationsByIncidentId(
    @PathVariable UUID incidentId,
    Authentication authentication
  ) {
    return ResponseEntity.ok(
      notificationMapper.toResponses(
        notificationService.getVisibleByIncidentId(
          incidentId,
          authentication.getName(),
          canViewAll(authentication)
        )
      )
    );
  }

  @GetMapping("/recipient/{recipient}")
  @PreAuthorize(
    "hasAnyAuthority('NOTIFICATION_VIEW_OWN', 'NOTIFICATION_VIEW_ALL')"
  )
  @Operation(summary = "Get notifications for a given recipient")
  // Fournit notifications by destinataire au cas d usage appelant.
  public ResponseEntity<List<NotificationResponse>> getNotificationsByRecipient(
    @PathVariable String recipient,
    Authentication authentication
  ) {
    return ResponseEntity.ok(
      notificationMapper.toResponses(
        notificationService.getByRecipient(
          recipient,
          authentication.getName(),
          canViewAll(authentication)
        )
      )
    );
  }

  @GetMapping("/recipient/{recipient}/page")
  @PreAuthorize(
    "hasAnyAuthority('NOTIFICATION_VIEW_OWN', 'NOTIFICATION_VIEW_ALL')"
  )
  @Operation(summary = "Get paginated notifications for a given recipient")
  // Fournit notifications paginees by destinataire au cas d usage appelant.
  public ResponseEntity<
    Page<NotificationResponse>
  > getNotificationsByRecipientPage(
    @PathVariable String recipient,
    Pageable pageable,
    Authentication authentication
  ) {
    return ResponseEntity.ok(
      notificationService
        .getByRecipient(
          recipient,
          authentication.getName(),
          canViewAll(authentication),
          pageable
        )
        .map(notificationMapper::toResponse)
    );
  }

  @PostMapping
  @PreAuthorize(
    "hasAnyAuthority('NOTIFICATION_MANAGE', 'NOTIFICATION_INTERNAL')"
  )
  @Operation(summary = "Create a new notification")
  // Prepare l'ajout de notification apres validation metier.
  public ResponseEntity<NotificationResponse> createNotification(
    @Valid @RequestBody NotificationRequest request
  ) {
    return new ResponseEntity<>(
      notificationMapper.toResponse(
        notificationService.create(notificationMapper.toDocument(request))
      ),
      HttpStatus.CREATED
    );
  }

  // Applique le changement demande apres validation metier.

  @PatchMapping("/{id}/read")
  @PreAuthorize(
    "hasAnyAuthority('NOTIFICATION_VIEW_OWN', 'NOTIFICATION_VIEW_ALL')"
  )
  @Operation(summary = "Mark a notification as read")
  public ResponseEntity<NotificationResponse> markAsRead(
    @PathVariable String id,
    Authentication authentication
  ) {
    return ResponseEntity.ok(
      notificationMapper.toResponse(
        notificationService.markAsRead(
          id,
          authentication.getName(),
          canViewAll(authentication)
        )
      )
    );
  }

  // Applique le changement demande apres validation metier.

  @PatchMapping("/read-all")
  @PreAuthorize(
    "hasAnyAuthority('NOTIFICATION_VIEW_OWN', 'NOTIFICATION_VIEW_ALL')"
  )
  @Operation(summary = "Mark all visible notifications as read")
  public ResponseEntity<List<NotificationResponse>> markAllAsRead(
    Authentication authentication
  ) {
    return ResponseEntity.ok(
      notificationMapper.toResponses(
        notificationService.markAllAsRead(
          authentication.getName(),
          canViewAll(authentication)
        )
      )
    );
  }

  @GetMapping("/stats")
  @PreAuthorize(
    "hasAnyAuthority('NOTIFICATION_VIEW_ALL', 'NOTIFICATION_MANAGE')"
  )
  @Operation(
    summary = "Aggregated notification stats computed by MongoDB ($group)"
  )
  // Fournit statistiques au cas d usage appelant.
  public ResponseEntity<NotificationStatsResponse> getStats(
    @RequestParam(value = "from", required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) LocalDateTime from,
    @RequestParam(value = "to", required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) LocalDateTime to,
    @RequestParam(value = "sampleSize", defaultValue = "20") int sampleSize
  ) {
    return ResponseEntity.ok(
      notificationMapper.toStatsResponse(
        notificationStatsService.computeStats(from, to, sampleSize)
      )
    );
  }

  // Supprime ou invalide les donnees ciblees apres controle metier.

  @DeleteMapping("/{id}")
  @PreAuthorize(
    "hasAnyAuthority('NOTIFICATION_VIEW_OWN', 'NOTIFICATION_VIEW_ALL', 'NOTIFICATION_MANAGE')"
  )
  @Operation(
    summary = "Delete a notification (own notification, or any with NOTIFICATION_MANAGE)"
  )
  public ResponseEntity<Void> deleteNotification(
    @PathVariable String id,
    Authentication authentication
  ) {
    notificationService.delete(
      id,
      authentication.getName(),
      canManageAll(authentication)
    );
    return ResponseEntity.noContent().build();
  }

  // Traite en masse delete notifications.

  @DeleteMapping("/bulk")
  @PreAuthorize("hasAuthority('NOTIFICATION_MANAGE')")
  @Operation(summary = "Bulk delete notifications")
  public ResponseEntity<Void> bulkDeleteNotifications(
    @RequestBody List<String> ids
  ) {
    notificationService.bulkDelete(ids);
    return ResponseEntity.noContent().build();
  }

  // Traite en masse purge.

  @DeleteMapping("/purge")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @Operation(
    summary = "Bulk purge old notifications (preserves N most recent regardless of age)"
  )
  public ResponseEntity<BulkPurgeResponse> bulkPurge(
    @Valid @RequestBody BulkPurgeRequest request
  ) {
    return ResponseEntity.ok(
      notificationMapper.toBulkPurgeResponse(
        notificationPurgeService.purge(
          request.getOlderThanDays(),
          request.getPreserveLastN(),
          request.isDryRun()
        )
      )
    );
  }

  // Perimetre de lecture : droit global (toutes les notifications) ou non (les siennes).
  private boolean canViewAll(Authentication authentication) {
    return (
      authentication != null &&
      authentication
        .getAuthorities()
        .stream()
        .anyMatch(granted ->
          NOTIFICATION_VIEW_ALL.equalsIgnoreCase(granted.getAuthority())
        )
    );
  }

  // Droit de supprimer n'importe quelle notification (sinon : uniquement les siennes).
  private boolean canManageAll(Authentication authentication) {
    return (
      authentication != null &&
      authentication
        .getAuthorities()
        .stream()
        .anyMatch(granted ->
          NOTIFICATION_MANAGE.equalsIgnoreCase(granted.getAuthority())
        )
    );
  }
}
