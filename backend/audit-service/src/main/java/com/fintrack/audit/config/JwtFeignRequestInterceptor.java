// Configuration Spring : declare les regles techniques liees a jwt feign request interceptor.

package com.fintrack.audit.config;

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

// Propage sur les appels Feign sortants l'identifiant de correlation et
// le token JWT de la requete entrante.
@Slf4j
@Component
public class JwtFeignRequestInterceptor implements RequestInterceptor {

  // Declare le comportement technique attendu par l'infrastructure.

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
