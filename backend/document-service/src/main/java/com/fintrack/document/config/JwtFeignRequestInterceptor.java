// Configuration Spring : declare les regles techniques liees a jwt feign request interceptor.

package com.fintrack.document.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// Intercepteur Feign propageant l'identifiant de correlation et le jeton JWT aux appels inter-services.
@Slf4j
@Component
public class JwtFeignRequestInterceptor implements RequestInterceptor {

  // Ajoute l'en-tete de correlation et le jeton d'autorisation a chaque requete sortante.
  @Override
  public void apply(RequestTemplate template) {
    String correlationId = MDC.get("correlationId");
    if (correlationId == null) {
      correlationId = UUID.randomUUID().toString();
      MDC.put("correlationId", correlationId);
    }
    template.header("X-Correlation-ID", correlationId);

    ServletRequestAttributes attributes =
      (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes == null) return;

    HttpServletRequest request = attributes.getRequest();
    String authHeader = request.getHeader("Authorization");
    if (!StringUtils.hasText(authHeader)) return;

    template.header(
      "Authorization",
      authHeader.startsWith("Bearer ") ? authHeader : "Bearer " + authHeader
    );
  }
}
