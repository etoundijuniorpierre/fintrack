// Controleur REST : expose les operations HTTP liees a user stats.

package com.fintrack.user.controller;

import com.fintrack.user.constant.ApiConstants;
import com.fintrack.user.exception.BusinessRuleViolationException;
import com.fintrack.user.exception.ErrorCode;
import com.fintrack.user.model.constant.PeriodType;
import com.fintrack.user.model.dto.response.UserStatsResponse;
import com.fintrack.user.model.mapper.UserMapper;
import com.fintrack.user.security.UserDetailsImpl;
import com.fintrack.user.service.UserStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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

// Controleur REST exposant les statistiques agregees des utilisateurs.
@RestController
@RequestMapping(ApiConstants.Endpoints.USERS)
@Tag(
  name = "User Statistics",
  description = "Endpoints for retrieving aggregated user statistics"
)
@RequiredArgsConstructor
public class UserStatsController {

  private final UserStatsService userStatsService;
  private final UserMapper userMapper;
  private final MessageSource messageSource;

  private String t(String key, Object... args) {
    return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
  }

  // Renvoie les statistiques utilisateurs pour la periode demandee (predefinie ou plage de dates).
  @GetMapping("/stats")
  @PreAuthorize(
    "hasAnyAuthority('USER_VIEW_ALL', 'USER_VIEW_AGENCY', 'USER_VIEW_SERVICE')"
  )
  @Operation(
    summary = "Get aggregated user statistics for the given period",
    description = "Returns enriched user statistics including active, inactive, connected, locked, and new users counts. Requires USER_VIEW_ALL, USER_VIEW_AGENCY, or USER_VIEW_SERVICE permission."
  )
  // Fournit statistiques au cas d usage appelant.
  public ResponseEntity<UserStatsResponse> getStats(
    @RequestParam(required = false) PeriodType period,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) LocalDateTime dateFrom,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE_TIME
    ) LocalDateTime dateTo,
    @RequestParam(required = false) String view,
    @RequestParam(required = false) UUID agencyId,
    @RequestParam(required = false) UUID serviceId,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    // Coherence des bornes quelle que soit la periode demandee : sans ce
    // check, des dates inversees passaient silencieusement (elles ne sont
    // exploitees que pour la periode CUSTOM cote service).
    if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
      throw new BusinessRuleViolationException(
        ErrorCode.STATS_CUSTOM_PERIOD_DATE_ORDER,
        t("user.error.stats_custom_period_date_order")
      );
    }

    String normalizedView = normalizeAndAuthorizeView(view, currentUser);
    if (agencyId != null && serviceId != null) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        t("user.error.stats_agency_service_mutually_exclusive")
      );
    }
    if (
      (agencyId != null || serviceId != null) &&
      !hasAuthority(currentUser, "USER_VIEW_ALL")
    ) {
      throw new AccessDeniedException(
        t("user.error.stats_global_view_required")
      );
    }

    UserStatsResponse response = userMapper.userStatsToUserStatsResponse(
      userStatsService.getStats(
        period,
        dateFrom,
        dateTo,
        normalizedView,
        agencyId,
        serviceId,
        currentUser
      )
    );

    return ResponseEntity.ok(response);
  }

  // Verifie que les regles metier autorisent l operation sur service interne.

  private boolean hasAuthority(UserDetailsImpl user, String authority) {
    if (user == null) {
      return false;
    }
    return user
      .getAuthorities()
      .stream()
      .map(GrantedAuthority::getAuthority)
      .anyMatch(authority::equalsIgnoreCase);
  }

  // Normalise les valeurs du domaine utilisateur statistiques avant traitement.

  private String normalizeAndAuthorizeView(
    String view,
    UserDetailsImpl currentUser
  ) {
    String normalizedView = view == null ? "" : view.toLowerCase(Locale.ROOT);
    if (normalizedView.isBlank()) {
      return preferredAuthorizedView(currentUser);
    }
    String requiredAuthority = switch (normalizedView) {
      case "agency" -> "USER_VIEW_AGENCY";
      case "service" -> "USER_VIEW_SERVICE";
      case "all" -> "USER_VIEW_ALL";
      default -> throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        t("user.error.stats_unsupported_view") + view
      );
    };

    boolean allowed = currentUser
      .getAuthorities()
      .stream()
      .map(GrantedAuthority::getAuthority)
      .anyMatch(requiredAuthority::equalsIgnoreCase);
    if (!allowed) {
      throw new AccessDeniedException(
        t("user.error.stats_view_access_denied", normalizedView)
      );
    }
    return normalizedView;
  }

  // Choisit la portee la plus large autorisee sans jamais promouvoir en global.
  private String preferredAuthorizedView(UserDetailsImpl currentUser) {
    if (hasAuthority(currentUser, "USER_VIEW_ALL")) {
      return "all";
    }
    if (hasAuthority(currentUser, "USER_VIEW_AGENCY")) {
      return "agency";
    }
    if (hasAuthority(currentUser, "USER_VIEW_SERVICE")) {
      return "service";
    }
    throw new AccessDeniedException(t("user.error.stats_view_access_denied", ""));
  }
}
