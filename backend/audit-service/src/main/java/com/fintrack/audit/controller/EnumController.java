// Controleur REST : expose les operations HTTP liees a enum.

package com.fintrack.audit.controller;

import com.fintrack.audit.constant.ApiConstants;
import com.fintrack.audit.model.constant.AuditAction;
import com.fintrack.audit.model.constant.AuditStatus;
import com.fintrack.audit.model.dto.response.EnumResponse;
import com.fintrack.audit.model.mapper.EnumMapper;
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

// Controleur exposant les valeurs d'enumerations d'audit traduites.
@RestController
@RequestMapping(ApiConstants.API_BASE_PATH + "/enums")
@Tag(
  name = "Enums",
  description = "Endpoints for retrieving translated audit enum values"
)
@RequiredArgsConstructor
public class EnumController {

  private final EnumMapper enumMapper;

  // Renvoie toutes les actions d'audit avec leur libelle et description traduits.
  @GetMapping("/audit-actions")
  @PreAuthorize("isAuthenticated()")
  @Operation(
    summary = "Get all audit actions with translated name and description"
  )
  public ResponseEntity<List<EnumResponse>> getAuditActions() {
    return ResponseEntity.ok(
      enumMapper.toResponses(
        AuditAction.values(),
        LocaleContextHolder.getLocale()
      )
    );
  }

  // Renvoie tous les statuts d'audit avec leur libelle et description traduits.
  @GetMapping("/audit-statuses")
  @PreAuthorize("isAuthenticated()")
  @Operation(
    summary = "Get all audit statuses with translated name and description"
  )
  public ResponseEntity<List<EnumResponse>> getAuditStatuses() {
    return ResponseEntity.ok(
      enumMapper.toResponses(
        AuditStatus.values(),
        LocaleContextHolder.getLocale()
      )
    );
  }
}
