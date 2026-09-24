// Controleur REST : expose les operations HTTP liees a dashboard.

package com.fintrack.incident.controller;

import com.fintrack.incident.constant.ApiConstants;
import com.fintrack.incident.exception.BusinessRuleViolationException;
import com.fintrack.incident.exception.ErrorCode;
import com.fintrack.incident.model.constant.PeriodType;
import com.fintrack.incident.model.dto.response.ComparisonResponse;
import com.fintrack.incident.model.dto.response.DashboardMetricsResponse;
import com.fintrack.incident.model.dto.response.PeriodActivityResponse;
import com.fintrack.incident.model.mapper.IncidentMapper;
import com.fintrack.incident.model.readmodel.PeriodActivityIncidents;
import com.fintrack.incident.security.UserDetailsImpl;
import com.fintrack.incident.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Controleur REST exposant les metriques du tableau de bord selon le perimetre RBAC de l'utilisateur
@RestController
@RequestMapping(ApiConstants.Endpoints.DASHBOARD)
@Tag(name = "Dashboard", description = "Endpoints for dashboard metrics")
@RequiredArgsConstructor
public class DashboardController {

  private final DashboardService dashboardService;
  private final IncidentMapper incidentMapper;

  @GetMapping("/metrics")
  @PreAuthorize(
    "hasAnyAuthority('INCIDENT_VIEW_ALL', 'INCIDENT_VIEW_AGENCY', 'INCIDENT_VIEW_SERVICE', 'INCIDENT_VIEW_OWN')"
  )
  @Operation(
    summary = "Get dashboard metrics based on view, with optional agency/service filter"
  )
  // Fournit indicateurs au cas d usage appelant.
  public ResponseEntity<DashboardMetricsResponse> getMetrics(
    @RequestParam(required = true) String view,
    @RequestParam(required = false) UUID agencyId,
    @RequestParam(required = false) UUID serviceId,
    @RequestParam(required = false) Integer year,
    @RequestParam(required = false) PeriodType period,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) LocalDateTime dateFrom,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) LocalDateTime dateTo,
    @RequestParam(required = false) UUID targetUserId,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    validatePeriod(period, dateFrom, dateTo);

    String normalizedView = normalizeAndAuthorizeView(view, currentUser);
    if (agencyId != null && serviceId != null) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        "dashboard.error.scope_filters_mutually_exclusive"
      );
    }
    if (
      (agencyId != null || serviceId != null || targetUserId != null) &&
      !hasAuthority(currentUser, "INCIDENT_VIEW_ALL")
    ) {
      throw new AccessDeniedException(
        "Les filtres agence/service/utilisateur exigent la permission de vue globale"
      );
    }
    int effectiveYear = year != null ? year : Year.now().getValue();
    PeriodType effectivePeriod =
      period != null ? period : PeriodType.LAST_30_DAYS;
    return ResponseEntity.ok(
      incidentMapper.toDashboardResponse(
        dashboardService.getMetrics(
          normalizedView,
          currentUser,
          agencyId,
          serviceId,
          targetUserId,
          effectiveYear,
          effectivePeriod,
          dateFrom,
          dateTo
        )
      )
    );
  }

  @GetMapping("/period-activity")
  @PreAuthorize(
    "hasAnyAuthority('INCIDENT_VIEW_ALL', 'INCIDENT_VIEW_AGENCY', 'INCIDENT_VIEW_SERVICE', 'INCIDENT_VIEW_OWN')"
  )
  @Operation(
    summary = "List incidents backing the period flow counters (treated/resolved/closed)"
  )
  // Fournit les listes de flux (traites/resolus/clotures) adossant les compteurs de la periode.
  public ResponseEntity<PeriodActivityResponse> getPeriodActivity(
    @RequestParam(required = true) String view,
    @RequestParam(required = false) UUID agencyId,
    @RequestParam(required = false) UUID serviceId,
    @RequestParam(required = false) PeriodType period,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) LocalDateTime dateFrom,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) LocalDateTime dateTo,
    @RequestParam(required = false) UUID targetUserId,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    validatePeriod(period, dateFrom, dateTo);

    String normalizedView = normalizeAndAuthorizeView(view, currentUser);
    if (agencyId != null && serviceId != null) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        "dashboard.error.scope_filters_mutually_exclusive"
      );
    }
    if (
      (agencyId != null || serviceId != null || targetUserId != null) &&
      !hasAuthority(currentUser, "INCIDENT_VIEW_ALL")
    ) {
      throw new AccessDeniedException(
        "Les filtres agence/service/utilisateur exigent la permission de vue globale"
      );
    }
    PeriodActivityIncidents activity = dashboardService.getPeriodActivity(
      normalizedView,
      currentUser,
      agencyId,
      serviceId,
      targetUserId,
      period,
      dateFrom,
      dateTo
    );
    return ResponseEntity.ok(
      PeriodActivityResponse.builder()
        .treated(incidentMapper.toSummaryResponseList(activity.getTreated()))
        .resolved(incidentMapper.toSummaryResponseList(activity.getResolved()))
        .closed(incidentMapper.toSummaryResponseList(activity.getClosed()))
        .build()
    );
  }

  @GetMapping("/synthesis")
  @PreAuthorize("hasAuthority('INCIDENT_VIEW_ALL')")
  @Operation(
    summary = "Get dashboard synthesis metrics (direction view, forced ALL)"
  )
  // Fournit synthesis indicateurs au cas d usage appelant.
  public ResponseEntity<DashboardMetricsResponse> getSynthesisMetrics(
    @RequestParam(required = false) Integer year,
    @RequestParam(required = false) PeriodType period,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) LocalDateTime dateFrom,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) LocalDateTime dateTo,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    validatePeriod(period, dateFrom, dateTo);

    int effectiveYear = year != null ? year : Year.now().getValue();
    PeriodType effectivePeriod =
      period != null ? period : PeriodType.LAST_30_DAYS;

    // Force la vue "all" sans filtre agence, service ou utilisateur.
    return ResponseEntity.ok(
      incidentMapper.toDashboardResponse(
        dashboardService.getMetrics(
          "all",
          currentUser,
          null,
          null,
          null,
          effectiveYear,
          effectivePeriod,
          dateFrom,
          dateTo
        )
      )
    );
  }

  @GetMapping("/metrics/comparison")
  @PreAuthorize("hasAuthority('INCIDENT_VIEW_ALL')")
  @Operation(
    summary = "Compare metrics across several entities of the same type (USER/SERVICE/AGENCY)"
  )
  // Fournit comparison au cas d usage appelant.
  public ResponseEntity<ComparisonResponse> getComparison(
    @RequestParam String entityType,
    @RequestParam List<UUID> ids,
    @RequestParam(required = false) Integer year,
    @RequestParam(required = false) PeriodType period,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) LocalDateTime dateFrom,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) LocalDateTime dateTo,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    validatePeriod(period, dateFrom, dateTo);
    if (ids == null || ids.isEmpty() || ids.size() > 6) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        "dashboard.error.comparison_ids_limit"
      );
    }
    int effectiveYear = year != null ? year : Year.now().getValue();
    PeriodType effectivePeriod =
      period != null ? period : PeriodType.LAST_30_DAYS;
    return ResponseEntity.ok(
      incidentMapper.toComparisonResponse(
        entityType,
        dashboardService.getComparison(
          entityType,
          ids,
          currentUser,
          effectiveYear,
          effectivePeriod,
          dateFrom,
          dateTo
        )
      )
    );
  }

  // Valide la coherence des bornes d'une periode personnalisee.
  private void validatePeriod(
    PeriodType period,
    LocalDateTime dateFrom,
    LocalDateTime dateTo
  ) {
    if (period == PeriodType.CUSTOM && (dateFrom == null || dateTo == null)) {
      throw new BusinessRuleViolationException(
        ErrorCode.STATS_CUSTOM_PERIOD_DATES_REQUIRED,
        ErrorCode.STATS_CUSTOM_PERIOD_DATES_REQUIRED.getMessageKey()
      );
    }
    if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
      throw new BusinessRuleViolationException(
        ErrorCode.STATS_CUSTOM_PERIOD_DATE_ORDER,
        ErrorCode.STATS_CUSTOM_PERIOD_DATE_ORDER.getMessageKey()
      );
    }
  }

  // Verifie que les regles metier autorisent l operation sur incident.

  private boolean hasAuthority(UserDetailsImpl user, String authority) {
    return user
      .getAuthorities()
      .stream()
      .map(GrantedAuthority::getAuthority)
      .anyMatch(authority::equalsIgnoreCase);
  }

  // Normalise les valeurs du domaine tableau de bord avant traitement.

  private String normalizeAndAuthorizeView(
    String view,
    UserDetailsImpl currentUser
  ) {
    String normalizedView = view == null ? "" : view.toLowerCase(Locale.ROOT);
    String requiredAuthority = switch (normalizedView) {
      case "own" -> "INCIDENT_VIEW_OWN";
      case "agency" -> "INCIDENT_VIEW_AGENCY";
      case "service" -> "INCIDENT_VIEW_SERVICE";
      case "all" -> "INCIDENT_VIEW_ALL";
      default -> throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        "dashboard.error.unsupported_view"
      );
    };

    boolean allowed = currentUser
      .getAuthorities()
      .stream()
      .map(GrantedAuthority::getAuthority)
      .anyMatch(requiredAuthority::equalsIgnoreCase);
    if (!allowed) {
      throw new AccessDeniedException(
        "Vous n'avez pas la permission d'acceder a la vue tableau de bord : " +
          normalizedView
      );
    }
    return normalizedView;
  }
}
