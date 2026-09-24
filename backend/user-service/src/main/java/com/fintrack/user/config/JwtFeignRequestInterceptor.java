// Configuration Spring : declare les regles techniques liees a jwt feign request interceptor.

package com.fintrack.user.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// Intercepteur Feign : propage le JWT de l'utilisateur, l'identifiant de correlation et le token de service interne sur les appels sortants.
@Slf4j
@Component
public class JwtFeignRequestInterceptor implements RequestInterceptor {

  @Value("${fintrack.internal-service.token:}")
  private String internalServiceToken;

  // Ajoute les en-tetes de correlation, de service interne et d'authentification a chaque requete Feign.
  @Override
  public void apply(RequestTemplate template) {
    // Recupere ou genere un identifiant de correlation pour tracer la requete entre services.
    String correlationId = MDC.get("correlationId");
    if (correlationId == null) {
      correlationId = UUID.randomUUID().toString();
      MDC.put("correlationId", correlationId);
    }
    template.header("X-Correlation-ID", correlationId);
    addInternalServiceToken(template);

    // Sans contexte de requete HTTP entrante (ex: appel asynchrone), aucun JWT a propager.
    ServletRequestAttributes attributes =
      (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes == null) return;

    HttpServletRequest request = attributes.getRequest();
    String authHeader = request.getHeader("Authorization");
    if (!StringUtils.hasText(authHeader)) return;

    // Recopie le JWT entrant en garantissant le prefixe "Bearer ".
    template.header(
      "Authorization",
      authHeader.startsWith("Bearer ") ? authHeader : "Bearer " + authHeader
    );
  }

  // Ajoute le token de service interne pour les appels techniques entre services.
  private void addInternalServiceToken(RequestTemplate template) {
    if (
      !StringUtils.hasText(internalServiceToken) ||
      template.feignTarget() == null
    ) {
      return;
    }

    String targetName = template.feignTarget().name();
    if (
      "audit-service".equals(targetName) ||
      "notification-service".equals(targetName) ||
      "reporting-service".equals(targetName)
    ) {
      template.header("X-Internal-Service-Token", internalServiceToken);
    }
  }
}
