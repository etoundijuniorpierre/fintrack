// Controleur REST : expose les operations HTTP liees a internal system config.

package com.fintrack.reporting.controller;

import com.fintrack.reporting.constant.ApiConstants;
import com.fintrack.reporting.model.dto.response.superadmin.SystemThresholdsResponse;
import com.fintrack.reporting.model.mapper.superadmin.SuperAdminMapper;
import com.fintrack.reporting.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Pilote les echanges HTTP du domaine internal system config.

@RestController
@RequestMapping(ApiConstants.API_BASE_PATH + "/internal/system-config")
@RequiredArgsConstructor
public class InternalSystemConfigController {

  private final SystemConfigService systemConfigService;
  private final SuperAdminMapper superAdminMapper;
  private final MessageSource messageSource;

  private String t(String key, Object... args) {
    return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
  }

  @Value("${fintrack.internal-service.token:}")
  private String internalServiceToken;

  @GetMapping("/thresholds")
  // Fournit thresholds au cas d usage appelant.
  public SystemThresholdsResponse getThresholds(
    @RequestHeader(
      value = "X-Internal-Service-Token",
      required = false
    ) String token
  ) {
    if (
      !StringUtils.hasText(internalServiceToken) ||
      !internalServiceToken.equals(token)
    ) {
      throw new AccessDeniedException(t("reporting.error.internal_service_token_required"));
    }
    return superAdminMapper.toThresholdsResponse(
      systemConfigService.getThresholds()
    );
  }

  @GetMapping("/email-notifications")
  // Reglages e-mail par evenement, lus en runtime par les services emetteurs.
  public com.fintrack.reporting.model.readmodel.superadmin.EmailNotificationSettings getEmailNotifications(
    @RequestHeader(
      value = "X-Internal-Service-Token",
      required = false
    ) String token
  ) {
    if (
      !StringUtils.hasText(internalServiceToken) ||
      !internalServiceToken.equals(token)
    ) {
      throw new AccessDeniedException(
        t("reporting.error.internal_service_token_required")
      );
    }
    return systemConfigService.getEmailNotificationSettings();
  }
}
