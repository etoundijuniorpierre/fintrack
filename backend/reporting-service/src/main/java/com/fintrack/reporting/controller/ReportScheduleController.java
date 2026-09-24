// Controleur REST : expose les operations HTTP liees a report schedule.

package com.fintrack.reporting.controller;

import com.fintrack.reporting.constant.ApiConstants;
import com.fintrack.reporting.model.dto.request.ReportScheduleRequest;
import com.fintrack.reporting.model.dto.response.ReportScheduleResponse;
import com.fintrack.reporting.model.entity.ReportSchedule;
import com.fintrack.reporting.model.mapper.ReportScheduleMapper;
import com.fintrack.reporting.security.UserDetailsImpl;
import com.fintrack.reporting.service.ReportScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.*;

// Controleur REST pour planifier des taches de generation de rapports.
@RestController
@RequestMapping(ApiConstants.Endpoints.REPORT_SCHEDULES)
@Tag(
  name = "Report Schedules",
  description = "Endpoints for managing report schedules"
)
@RequiredArgsConstructor
public class ReportScheduleController {

  private static final String REPORT_VIEW_ALL = "REPORT_VIEW_ALL";

  private final ReportScheduleService reportScheduleService;
  private final ReportScheduleMapper reportScheduleMapper;
  private final MessageSource messageSource;

  private String t(String key, Object... args) {
    return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
  }

  @GetMapping
  @PreAuthorize(
    "hasAnyAuthority('REPORT_VIEW_OWN', 'REPORT_VIEW_AGENCY', 'REPORT_VIEW_SERVICE', 'REPORT_VIEW_ALL')"
  )
  @Operation(summary = "Get report schedules with pagination")
  // Fournit global planifications au cas d usage appelant.
  public ResponseEntity<Page<ReportScheduleResponse>> getAllSchedules(
    Pageable pageable,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    Page<ReportSchedule> schedules = hasAuthority(currentUser, REPORT_VIEW_ALL)
      ? reportScheduleService.findAll(pageable)
      : reportScheduleService.findByCreatedBy(currentUser.getId(), pageable);
    return ResponseEntity.ok(schedules.map(reportScheduleMapper::toResponse));
  }

  @GetMapping("/all")
  @PreAuthorize(
    "hasAnyAuthority('REPORT_VIEW_OWN', 'REPORT_VIEW_AGENCY', 'REPORT_VIEW_SERVICE', 'REPORT_VIEW_ALL')"
  )
  @Operation(summary = "Get report schedules as a list")
  // Fournit global planifications list au cas d usage appelant.
  public ResponseEntity<List<ReportScheduleResponse>> getAllSchedulesList(
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    List<ReportSchedule> schedules = hasAuthority(currentUser, REPORT_VIEW_ALL)
      ? reportScheduleService.findAll()
      : reportScheduleService.findByCreatedBy(currentUser.getId());
    return ResponseEntity.ok(toResponses(schedules));
  }

  @GetMapping("/active")
  @PreAuthorize(
    "hasAnyAuthority('REPORT_VIEW_OWN', 'REPORT_VIEW_AGENCY', 'REPORT_VIEW_SERVICE', 'REPORT_VIEW_ALL')"
  )
  @Operation(summary = "Get active report schedules")
  // Fournit actif planifications au cas d usage appelant.
  public ResponseEntity<List<ReportScheduleResponse>> getActiveSchedules(
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    List<ReportSchedule> schedules = hasAuthority(currentUser, REPORT_VIEW_ALL)
      ? reportScheduleService.findActive()
      : reportScheduleService.findActiveByCreatedBy(currentUser.getId());
    return ResponseEntity.ok(toResponses(schedules));
  }

  @GetMapping("/{id}")
  @PreAuthorize(
    "hasAnyAuthority('REPORT_VIEW_OWN', 'REPORT_VIEW_AGENCY', 'REPORT_VIEW_SERVICE', 'REPORT_VIEW_ALL')"
  )
  @Operation(summary = "Get report schedule by ID")
  // Fournit planification by identifiant au cas d usage appelant.
  public ResponseEntity<ReportScheduleResponse> getScheduleById(
    @PathVariable UUID id,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    ReportSchedule schedule = reportScheduleService.findById(id);
    assertCanAccessSchedule(schedule, currentUser);
    return ResponseEntity.ok(reportScheduleMapper.toResponse(schedule));
  }

  @GetMapping("/created-by/{userId}")
  @PreAuthorize(
    "hasAnyAuthority('REPORT_VIEW_OWN', 'REPORT_VIEW_AGENCY', 'REPORT_VIEW_SERVICE', 'REPORT_VIEW_ALL')"
  )
  @Operation(summary = "Get report schedules created by a given user")
  // Fournit planifications by creation by au cas d usage appelant.
  public ResponseEntity<List<ReportScheduleResponse>> getSchedulesByCreatedBy(
    @PathVariable UUID userId,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    assertCanAccessUserSchedules(userId, currentUser);
    return ResponseEntity.ok(
      toResponses(reportScheduleService.findByCreatedBy(userId))
    );
  }

  @PostMapping
  @PreAuthorize("hasAuthority('REPORT_GENERATE')")
  @Operation(summary = "Create a new report schedule")
  // Prepare l'ajout de planification de rapport apres validation metier.
  public ResponseEntity<ReportScheduleResponse> createSchedule(
    @Valid @RequestBody ReportScheduleRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    return new ResponseEntity<>(
      reportScheduleMapper.toResponse(
        reportScheduleService.create(
          reportScheduleMapper.toEntity(request),
          currentUser.getId()
        )
      ),
      HttpStatus.CREATED
    );
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('REPORT_GENERATE')")
  @Operation(summary = "Update a report schedule")
  // Applique une modification contrelee sur planification de rapport.
  public ResponseEntity<ReportScheduleResponse> updateSchedule(
    @PathVariable UUID id,
    @Valid @RequestBody ReportScheduleRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    assertCanAccessSchedule(reportScheduleService.findById(id), currentUser);
    return ResponseEntity.ok(
      reportScheduleMapper.toResponse(
        reportScheduleService.update(id, reportScheduleMapper.toEntity(request))
      )
    );
  }

  @PatchMapping("/{id}/toggle")
  @PreAuthorize("hasAuthority('REPORT_GENERATE')")
  @Operation(summary = "Toggle active status of a report schedule")
  // Applique une modification contrelee sur planification de rapport.
  public ResponseEntity<ReportScheduleResponse> toggleActive(
    @PathVariable UUID id,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    assertCanAccessSchedule(reportScheduleService.findById(id), currentUser);
    return ResponseEntity.ok(
      reportScheduleMapper.toResponse(reportScheduleService.toggleActive(id))
    );
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('REPORT_DELETE')")
  @Operation(summary = "Delete a report schedule")
  // Retire les planifications de rapport cibles apres contrele metier.
  public ResponseEntity<Void> deleteSchedule(
    @PathVariable UUID id,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    assertCanAccessSchedule(reportScheduleService.findById(id), currentUser);
    reportScheduleService.delete(id);
    return ResponseEntity.noContent().build();
  }

  // Convertit les donnees du domaine planification de rapport entre les modeles utilises.

  private List<ReportScheduleResponse> toResponses(
    List<ReportSchedule> schedules
  ) {
    return schedules.stream().map(reportScheduleMapper::toResponse).toList();
  }

  // Verifie que les regles metier autorisent l operation sur planification de rapport.

  private void assertCanAccessUserSchedules(
    UUID userId,
    UserDetailsImpl currentUser
  ) {
    if (
      currentUser.getId().equals(userId) ||
      hasAuthority(currentUser, REPORT_VIEW_ALL)
    ) {
      return;
    }
    throw new AccessDeniedException(
      t("reporting.error.schedule_access_denied")
    );
  }

  // Verifie que les regles metier autorisent l operation sur planification de rapport.

  private void assertCanAccessSchedule(
    ReportSchedule schedule,
    UserDetailsImpl currentUser
  ) {
    assertCanAccessUserSchedules(schedule.getCreatedBy(), currentUser);
  }

  // Verifie que les regles metier autorisent l operation sur planification de rapport.

  private boolean hasAuthority(UserDetailsImpl user, String authority) {
    return user
      .getAuthorities()
      .stream()
      .anyMatch(granted -> authority.equalsIgnoreCase(granted.getAuthority()));
  }
}
