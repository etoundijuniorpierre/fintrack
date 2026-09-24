// Controleur REST : expose les operations HTTP liees a la presence.

package com.fintrack.notification.controller;

import com.fintrack.notification.constant.ApiConstants;
import com.fintrack.notification.model.readmodel.PresenceSnapshot;
import com.fintrack.notification.service.PresenceRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Expose la liste des utilisateurs actuellement connectes au WebSocket.
@RestController
@RequestMapping(ApiConstants.API_BASE_PATH + "/presence")
@Tag(name = "Presence", description = "Realtime user presence")
@RequiredArgsConstructor
public class PresenceController {

  private final PresenceRegistry presenceRegistry;

  // Etat initial de presence ; les mises a jour arrivent via /topic/presence.
  @GetMapping
  @PreAuthorize(
    "hasAnyAuthority('USER_VIEW_ALL', 'USER_VIEW_AGENCY', 'USER_VIEW_SERVICE', 'ROLE_SYSTEM')"
  )
  @Operation(summary = "Get currently connected usernames")
  public ResponseEntity<PresenceSnapshot> getPresence() {
    return ResponseEntity.ok(presenceRegistry.snapshot());
  }
}
