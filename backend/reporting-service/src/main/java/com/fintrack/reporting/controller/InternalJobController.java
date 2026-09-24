// Controleur REST : expose les operations HTTP liees a internal job.

package com.fintrack.reporting.controller;

import com.fintrack.reporting.client.audit.AuditServiceClientService;
import com.fintrack.reporting.client.audit.constant.AuditAction;
import com.fintrack.reporting.constant.ApiConstants;
import com.fintrack.reporting.model.dto.response.internal.InternalJobResponse;
import com.fintrack.reporting.model.dto.response.internal.JobTriggerResultResponse;
import com.fintrack.reporting.scheduler.ReportSchedulerJob;
import com.fintrack.reporting.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
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
@RequestMapping(ApiConstants.API_BASE_PATH + "/internal/jobs")
@Tag(
  name = "Internal Jobs API",
  description = "Endpoints internes pour les jobs"
)
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class InternalJobController {

  private final ObjectProvider<ReportSchedulerJob> reportSchedulerJob;
  private final AuditServiceClientService auditService;

  // Liste jobs.

  @GetMapping
  @Operation(summary = "Lister les jobs internes")
  public ResponseEntity<List<InternalJobResponse>> listJobs() {
    return ResponseEntity.ok(
      List.of(
        InternalJobResponse.builder()
          .name("generateScheduledReports")
          .description("Genere les rapports planifies")
          .schedule("cron = 0 * * * * *")
          .failureCount(0L)
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
    if ("generateScheduledReports".equals(jobName)) {
      ReportSchedulerJob schedulerJob = reportSchedulerJob.getIfAvailable();
      if (schedulerJob == null) {
        return ResponseEntity.status(503).build();
      }
      int affected = schedulerJob.triggerNow();
      audit(
        actor,
        AuditAction.JOB_TRIGGER.getName(),
        "internal_job",
        jobName,
        Map.of("service", "reporting", "affected", affected)
      );
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

  private void audit(
    UserDetailsImpl actor,
    String action,
    String resourceType,
    String resourceId,
    Map<String, Object> details
  ) {
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
      action,
      resourceType,
      resourceId,
      "SUCCESS",
      details
    );
  }
}
