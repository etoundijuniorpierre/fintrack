// Controleur REST : expose les operations HTTP liees a super admin.

package com.fintrack.reporting.controller;

import com.fintrack.reporting.constant.ApiConstants;
import com.fintrack.reporting.model.dto.response.superadmin.*;
import com.fintrack.reporting.model.mapper.superadmin.SuperAdminMapper;
import com.fintrack.reporting.security.UserDetailsImpl;
import com.fintrack.reporting.service.SuperAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Pilote les echanges HTTP du domaine super admin.

@RestController
@RequestMapping(ApiConstants.Endpoints.SUPER_ADMIN)
@Tag(
  name = "Super Admin",
  description = "Cross-service governance and operations endpoints"
)
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class SuperAdminController {

  private final SuperAdminService superAdminService;
  private final SuperAdminMapper superAdminMapper;

  // -----------------------------------------------------------------
  // 7 endpoints lazy par onglet (chacun avec son cache et TTL serveur).
  // -----------------------------------------------------------------

  @GetMapping("/governance")
  @Operation(
    summary = "Gouvernance globale : KPIs, ratios, top agences/services (cache 30s)"
  )
  public ResponseEntity<GovernanceSectionResponse> getGovernance() {
    return ResponseEntity.ok(
      superAdminMapper.toGovernanceResponse(superAdminService.getGovernance())
    );
  }

  // Fournit sante a la couche appelante.

  @GetMapping("/health")
  @Operation(
    summary = "Sante des services : probes /actuator/health authentifiees (cache 10s)"
  )
  public ResponseEntity<SystemHealthSectionResponse> getHealth() {
    return ResponseEntity.ok(
      superAdminMapper.toSystemHealthResponse(
        superAdminService.getSystemHealth()
      )
    );
  }

  // Fournit controles qualite a la couche appelante.

  @GetMapping("/controls-quality")
  @Operation(
    summary = "Permissions critiques + qualite des donnees (cache 60s)"
  )
  public ResponseEntity<ControlsQualitySectionResponse> getControlsQuality() {
    return ResponseEntity.ok(
      superAdminMapper.toControlsQualityResponse(
        superAdminService.getControlsQuality()
      )
    );
  }

  // Fournit operations a la couche appelante.

  @GetMapping("/operations")
  @Operation(
    summary = "Supervision notifications avec diagnostics exploitables (cache 30s)"
  )
  public ResponseEntity<OperationsSectionResponse> getOperations() {
    return ResponseEntity.ok(
      superAdminMapper.toOperationsResponse(superAdminService.getOperations())
    );
  }

  // Fournit audit vue d ensemble a la couche appelante.

  @GetMapping("/audit-overview")
  @Operation(summary = "Export et filtres du journal d'audit (cache 30s)")
  public ResponseEntity<AuditSectionResponse> getAuditOverview() {
    return ResponseEntity.ok(
      superAdminMapper.toAuditOverviewResponse(
        superAdminService.getAuditOverview()
      )
    );
  }

  // Fournit configuration systeme a la couche appelante.

  @GetMapping("/system-config")
  @Operation(summary = "Seuils metier utiles et comprehensibles (cache 5min)")
  public ResponseEntity<SystemConfigSectionResponse> getSystemConfig() {
    return ResponseEntity.ok(
      superAdminMapper.toSystemConfigResponse(
        superAdminService.getSystemConfig()
      )
    );
  }

  // Fournit reporting vue d ensemble a la couche appelante.

  @GetMapping("/reporting-overview")
  @Operation(
    summary = "Rapports en attente ou en echec avec details de diagnostic (cache 60s)"
  )
  public ResponseEntity<ReportingSectionResponse> getReportingOverview() {
    return ResponseEntity.ok(
      superAdminMapper.toReportingOverviewResponse(
        superAdminService.getReportingOverview()
      )
    );
  }

  // -----------------------------------------------------------------
  // /overview deprecated : compose les 7 sections (retro-compat).
  // -----------------------------------------------------------------

  @GetMapping("/overview")
  @Deprecated
  @Operation(
    summary = "[DEPRECATED] Aggregat complet des 7 sections",
    description = "Utiliser les endpoints lazy /governance, /health, /controls-quality, /operations, " +
      "/audit-overview, /system-config, /reporting-overview"
  )
  // Fournit vue d ensemble a la couche appelante.
  public ResponseEntity<SuperAdminOverviewResponse> getOverview() {
    return ResponseEntity.ok(
      superAdminMapper.toOverviewResponse(superAdminService.getOverview())
    );
  }

  // -----------------------------------------------------------------
  // Mutations (audit trail garanti).
  // -----------------------------------------------------------------

  @PatchMapping("/system-config/thresholds")
  @Operation(summary = "Met a jour les seuils metier (audite)")
  public ResponseEntity<ThresholdsUpdateResponse> updateThresholds(
    @RequestBody @NotNull SystemThresholdsResponse thresholds,
    @AuthenticationPrincipal UserDetailsImpl actor
  ) {
    return ResponseEntity.ok(
      superAdminMapper.toThresholdsUpdateResponse(
        superAdminService.updateThresholds(
          superAdminMapper.toThresholdsReadModel(thresholds),
          actor
        )
      )
    );
  }

  @GetMapping("/system-config/email-notifications")
  @Operation(
    summary = "Reglages d'envoi e-mail par evenement (interrupteurs + exclusions)"
  )
  public ResponseEntity<
    com.fintrack.reporting.model.readmodel.superadmin.EmailNotificationSettings
  > getEmailNotifications() {
    return ResponseEntity.ok(
      superAdminService.getEmailNotificationSettings()
    );
  }

  @PatchMapping("/system-config/email-notifications")
  @Operation(
    summary = "Met a jour les reglages d'envoi e-mail par evenement (audite)"
  )
  public ResponseEntity<
    com.fintrack.reporting.model.readmodel.superadmin.EmailNotificationSettings
  > updateEmailNotifications(
    @RequestBody @NotNull com.fintrack.reporting.model.readmodel.superadmin.EmailNotificationSettings settings,
    @AuthenticationPrincipal UserDetailsImpl actor
  ) {
    return ResponseEntity.ok(
      superAdminService.updateEmailNotificationSettings(settings, actor)
    );
  }

  @GetMapping(value = "/audit/export", produces = "text/csv")
  @Operation(summary = "Exporte les logs d'audit au format CSV (audite)")
  // Realise l'intention metier export audit.
  public ResponseEntity<byte[]> exportAudit(
    @RequestParam(value = "action", required = false) String action,
    @RequestParam(value = "status", required = false) String status,
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
    @RequestParam(value = "limit", required = false) Integer limit,
    @AuthenticationPrincipal UserDetailsImpl actor
  ) {
    byte[] csv = superAdminService.exportAuditCsv(
      action,
      status,
      from,
      to,
      limit,
      actor
    );
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("text/csv"));
    headers.setContentDispositionFormData("attachment", "audit-export.csv");
    headers.setContentLength(csv.length);
    return new ResponseEntity<>(csv, headers, 200);
  }

  // Invalide cache.

  @PostMapping("/cache/invalidate")
  @Operation(summary = "Invalide tous les caches Super Admin")
  public ResponseEntity<Void> invalidateCache(
    @AuthenticationPrincipal UserDetailsImpl actor
  ) {
    superAdminService.invalidateCache(actor);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/backup/trigger")
  @Operation(summary = "Declenche la sauvegarde manuelle des bases et la synchronisation Backblaze B2")
  public ResponseEntity<BackupTriggerResponse> triggerBackup(
    @AuthenticationPrincipal UserDetailsImpl actor
  ) {
    return ResponseEntity.ok(superAdminService.triggerBackup(actor));
  }
}
