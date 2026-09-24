// Controleur REST : expose les operations HTTP liees a internal job.

package com.fintrack.user.controller;

import com.fintrack.user.client.audit.AuditServiceClientService;
import com.fintrack.user.client.audit.constant.AuditStatus;
import com.fintrack.user.model.dto.response.internal.InternalJobResponse;
import com.fintrack.user.model.dto.response.internal.JobTriggerResultResponse;
import com.fintrack.user.security.UserDetailsImpl;
import com.fintrack.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Pilote les echanges HTTP du domaine internal job.

@RestController
@RequestMapping("/api/v1/userService/internal/jobs")
@Tag(
  name = "Internal Jobs API",
  description = "Endpoints internes pour les jobs"
)
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class InternalJobController {

  private final UserService userService;
  private final AuditServiceClientService auditService;

  // Liste jobs.

  @GetMapping
  @Operation(summary = "Lister les jobs internes")
  public ResponseEntity<List<InternalJobResponse>> listJobs() {
    return ResponseEntity.ok(
      List.of(
        InternalJobResponse.builder()
          .name("cleanupExpiredTemporaryPasswords")
          .description("Purge les mots de passe temporaires expires")
          .schedule("fixedRate = 15m")
          .failureCount(0L)
          .triggerable(false)
          .build()
      )
    );
  }

  @PostMapping("/{jobName}/trigger")
  @Operation(summary = "Declencher un job manuellement")
  // Declenche job.
  public ResponseEntity<JobTriggerResultResponse> triggerJob(
    @PathVariable String jobName,
    @AuthenticationPrincipal UserDetailsImpl actor
  ) {
    if ("cleanupExpiredTemporaryPasswords".equals(jobName)) {
      int affected = userService.cleanupExpiredTemporaryPasswords();
      audit(actor, jobName, affected);
      return ResponseEntity.ok(
        JobTriggerResultResponse.builder()
          .jobName(jobName)
          .affected(affected)
          .build()
      );
    }
    return ResponseEntity.notFound().build();
  }

  // Trace l'action metier realisee sur le domaine interne job.

  private void audit(UserDetailsImpl actor, String jobName, long affected) {
    UUID userId = actor != null ? actor.getId() : null;
    String username = actor != null ? actor.getUsername() : "system";
    List<String> roles =
      actor != null
        ? actor
            .getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .toList()
        : List.of();
    auditService.audit(
      userId,
      username,
      roles,
      "JOB_TRIGGER",
      "internal_job",
      jobName,
      AuditStatus.SUCCESS.getName(),
      Map.of("service", "user", "affected", affected)
    );
  }
}
