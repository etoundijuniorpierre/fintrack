// Controleur REST : expose les operations HTTP liees a audit log.

package com.fintrack.audit.controller;

import com.fintrack.audit.client.user.UserServiceClientService;
import com.fintrack.audit.constant.ApiConstants;
import com.fintrack.audit.model.constant.AuditAction;
import com.fintrack.audit.model.constant.AuditStatus;
import com.fintrack.audit.model.dto.request.AuditLogRequest;
import com.fintrack.audit.model.dto.request.BulkPurgeRequest;
import com.fintrack.audit.model.dto.response.AuditLogResponse;
import com.fintrack.audit.model.dto.response.AuditStatsResponse;
import com.fintrack.audit.model.dto.response.BulkPurgeResponse;
import com.fintrack.audit.model.dto.response.UserSummaryResponse;
import com.fintrack.audit.model.entity.AuditLog;
import com.fintrack.audit.model.mapper.AuditLogMapper;
import com.fintrack.audit.service.AuditLogService;
import com.fintrack.audit.service.AuditPurgeService;
import com.fintrack.audit.service.AuditStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Points d'entree REST des journaux d'audit : consultation filtree
// (reservee a AUDIT_VIEW) et enregistrement de nouvelles entrees.
@RestController
@RequestMapping(ApiConstants.Endpoints.AUDIT_LOGS)
@Tag(
  name = "Audit Logs",
  description = "Endpoints for managing and querying audit logs"
)
@RequiredArgsConstructor
public class AuditLogController {

  private final AuditLogService auditLogService;
  private final AuditLogMapper auditLogMapper;
  private final UserServiceClientService userServiceClientService;
  private final AuditStatsService auditStatsService;
  private final AuditPurgeService auditPurgeService;

  @GetMapping
  @PreAuthorize("hasAuthority('AUDIT_VIEW')")
  @Operation(
    summary = "Get audit logs with pagination and optional server-side filters"
  )
  // Fournit global audit logs au cas d usage appelant.
  public ResponseEntity<Page<AuditLogResponse>> getAllAuditLogs(
    Pageable pageable,
    @RequestParam(required = false) AuditAction action,
    @RequestParam(required = false) AuditStatus status,
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) LocalDateTime from,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) LocalDateTime to
  ) {
    Page<AuditLog> logs = auditLogService.search(
      pageable,
      action,
      status,
      from,
      to,
      keyword
    );
    Set<UUID> userIds = logs
      .getContent()
      .stream()
      .map(AuditLog::getUserId)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
    Map<UUID, UserSummaryResponse> usersById =
      userServiceClientService.resolveUsers(userIds);
    List<AuditLogResponse> content = logs
      .getContent()
      .stream()
      .map(log -> auditLogMapper.toResponse(log, usersById))
      .toList();
    return ResponseEntity.ok(
      new PageImpl<>(content, pageable, logs.getTotalElements())
    );
  }

  // Fournit audit journal by id a la couche appelante.

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('AUDIT_VIEW')")
  @Operation(summary = "Get audit log by ID")
  public ResponseEntity<AuditLogResponse> getAuditLogById(
    @PathVariable String id
  ) {
    return ResponseEntity.ok(
      auditLogMapper.toResponse(auditLogService.findById(id))
    );
  }

  // Fournit audit logs by utilisateur id a la couche appelante.

  @GetMapping("/user/{userId}")
  @PreAuthorize("hasAuthority('AUDIT_VIEW')")
  @Operation(summary = "Get audit logs for a given user")
  public ResponseEntity<List<AuditLogResponse>> getAuditLogsByUserId(
    @PathVariable UUID userId
  ) {
    return ResponseEntity.ok(
      auditLogService
        .findByUserId(userId)
        .stream()
        .map(auditLogMapper::toResponse)
        .toList()
    );
  }

  // Fournit audit logs by action a la couche appelante.

  @GetMapping("/action/{action}")
  @PreAuthorize("hasAuthority('AUDIT_VIEW')")
  @Operation(summary = "Get audit logs by action type")
  public ResponseEntity<List<AuditLogResponse>> getAuditLogsByAction(
    @PathVariable AuditAction action
  ) {
    return ResponseEntity.ok(
      auditLogService
        .findByAction(action)
        .stream()
        .map(auditLogMapper::toResponse)
        .toList()
    );
  }

  // Fournit audit logs by resource type a la couche appelante.

  @GetMapping("/resource/{resourceType}")
  @PreAuthorize("hasAuthority('AUDIT_VIEW')")
  @Operation(summary = "Get audit logs by resource type")
  public ResponseEntity<List<AuditLogResponse>> getAuditLogsByResourceType(
    @PathVariable String resourceType
  ) {
    return ResponseEntity.ok(
      auditLogService
        .findByResourceType(resourceType)
        .stream()
        .map(auditLogMapper::toResponse)
        .toList()
    );
  }

  @GetMapping("/resource/{resourceType}/{resourceId}")
  @PreAuthorize("hasAuthority('AUDIT_VIEW')")
  @Operation(summary = "Get audit logs for a specific resource instance")
  // Fournit audit logs by resource au cas d usage appelant.
  public ResponseEntity<List<AuditLogResponse>> getAuditLogsByResource(
    @PathVariable String resourceType,
    @PathVariable String resourceId
  ) {
    return ResponseEntity.ok(
      auditLogService
        .findByResourceTypeAndResourceId(resourceType, resourceId)
        .stream()
        .map(auditLogMapper::toResponse)
        .toList()
    );
  }

  // Fournit audit logs by statut a la couche appelante.

  @GetMapping("/status/{status}")
  @PreAuthorize("hasAuthority('AUDIT_VIEW')")
  @Operation(summary = "Get audit logs by status")
  public ResponseEntity<List<AuditLogResponse>> getAuditLogsByStatus(
    @PathVariable AuditStatus status
  ) {
    return ResponseEntity.ok(
      auditLogService
        .findByStatus(status)
        .stream()
        .map(auditLogMapper::toResponse)
        .toList()
    );
  }

  @GetMapping("/range")
  @PreAuthorize("hasAuthority('AUDIT_VIEW')")
  @Operation(summary = "Get audit logs within a time range")
  // Fournit audit logs by time periode au cas d usage appelant.
  public ResponseEntity<List<AuditLogResponse>> getAuditLogsByTimeRange(
    @RequestParam @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) LocalDateTime from,
    @RequestParam @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) LocalDateTime to
  ) {
    return ResponseEntity.ok(
      auditLogService
        .findByTimestampBetween(from, to)
        .stream()
        .map(auditLogMapper::toResponse)
        .toList()
    );
  }

  @GetMapping("/user/{userId}/range")
  @PreAuthorize("hasAuthority('AUDIT_VIEW')")
  @Operation(summary = "Get audit logs for a user within a time range")
  // Fournit audit logs by utilisateur and time periode au cas d usage appelant.
  public ResponseEntity<List<AuditLogResponse>> getAuditLogsByUserAndTimeRange(
    @PathVariable UUID userId,
    @RequestParam @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) LocalDateTime from,
    @RequestParam @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) LocalDateTime to
  ) {
    return ResponseEntity.ok(
      auditLogService
        .findByUserIdAndTimestampBetween(userId, from, to)
        .stream()
        .map(auditLogMapper::toResponse)
        .toList()
    );
  }

  @GetMapping("/stats")
  @PreAuthorize("hasAuthority('AUDIT_VIEW')")
  @Operation(summary = "Aggregated audit stats computed by MongoDB ($group)")
  // Fournit statistiques au cas d usage appelant.
  public ResponseEntity<AuditStatsResponse> getStats(
    @RequestParam(value = "from", required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) LocalDateTime from,
    @RequestParam(value = "to", required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) LocalDateTime to,
    @RequestParam(value = "sampleSize", defaultValue = "20") int sampleSize,
    @RequestParam(
      value = "repeatedThreshold",
      defaultValue = "3"
    ) int repeatedThreshold
  ) {
    return ResponseEntity.ok(
      auditLogMapper.toStatsResponse(
        auditStatsService.computeStats(from, to, sampleSize, repeatedThreshold)
      )
    );
  }

  @DeleteMapping("/bulk")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @Operation(
    summary = "Bulk purge old audit logs (sensitive actions always preserved)"
  )
  // Traite en masse purge.
  public ResponseEntity<BulkPurgeResponse> bulkPurge(
    @Valid @RequestBody BulkPurgeRequest request,
    Authentication authentication
  ) {
    String actor = authentication != null ? authentication.getName() : null;
    return ResponseEntity.ok(
      auditLogMapper.toBulkPurgeResponse(
        auditPurgeService.purge(
          request.getOlderThanDays(),
          request.getPreserveLastN(),
          request.isDryRun(),
          actor
        )
      )
    );
  }

  // Prepare l'enregistrement de la ressource selon les regles metier.

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Record a new audit log entry")
  public ResponseEntity<AuditLogResponse> createAuditLog(
    @Valid @RequestBody AuditLogRequest request
  ) {
    return new ResponseEntity<>(
      auditLogMapper.toResponse(
        auditLogService.create(auditLogMapper.toDocument(request))
      ),
      HttpStatus.CREATED
    );
  }
}
