// Controleur REST : expose les operations HTTP liees a enum.

package com.fintrack.notification.controller;

import com.fintrack.notification.constant.ApiConstants;
import com.fintrack.notification.model.constant.NotificationStatus;
import com.fintrack.notification.model.constant.NotificationType;
import com.fintrack.notification.model.dto.response.EnumResponse;
import com.fintrack.notification.model.mapper.EnumMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Controleur exposant les valeurs d'enumerations de notification traduites selon la langue.
@RestController
@RequestMapping(ApiConstants.API_BASE_PATH + "/enums")
@Tag(
  name = "Enums",
  description = "Endpoints for retrieving translated enum values"
)
@RequiredArgsConstructor
public class EnumController {

  private final EnumMapper enumMapper;

  // Renvoie tous les types de notification avec libelle et description traduits.
  @GetMapping("/notification-types")
  @PreAuthorize("isAuthenticated()")
  @Operation(
    summary = "Get all notification types with translated name and description"
  )
  public ResponseEntity<List<EnumResponse>> getNotificationTypes() {
    return ResponseEntity.ok(
      enumMapper.toResponses(
        NotificationType.values(),
        LocaleContextHolder.getLocale()
      )
    );
  }

  // Renvoie tous les statuts de notification avec libelle et description traduits.
  @GetMapping("/notification-statuses")
  @PreAuthorize("isAuthenticated()")
  @Operation(
    summary = "Get all notification statuses with translated name and description"
  )
  public ResponseEntity<List<EnumResponse>> getNotificationStatuses() {
    return ResponseEntity.ok(
      enumMapper.toResponses(
        NotificationStatus.values(),
        LocaleContextHolder.getLocale()
      )
    );
  }
}
