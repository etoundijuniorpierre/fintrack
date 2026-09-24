// Configuration Spring : declare les regles techniques liees a jwt feign request interceptor.

package com.fintrack.reporting.config;

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

// Intercepteur Feign propageant le token JWT dans les requetes inter-services.

@Slf4j
@Component
public class JwtFeignRequestInterceptor implements RequestInterceptor {

  @Value("${fintrack.internal-service.token:}")
  private String internalServiceToken;

  @Override
  // Propage les en-tetes techniques necessaires aux appels Feign.
  public void apply(RequestTemplate template) {
    String correlationId = MDC.get("correlationId");
    if (correlationId == null) {
      correlationId = UUID.randomUUID().toString();
      MDC.put("correlationId", correlationId);
    }
    template.header("X-Correlation-ID", correlationId);

    ServletRequestAttributes attributes =
      (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    String authHeader = null;
    if (attributes != null) {
      HttpServletRequest request = attributes.getRequest();
      authHeader = request.getHeader("Authorization");
    }

    if (StringUtils.hasText(authHeader)) {
      template.header(
        "Authorization",
        authHeader.startsWith("Bearer ") ? authHeader : "Bearer " + authHeader
      );
      return;
    }
    addInternalServiceToken(template);
  }

  // Cree un element du domaine jwt feign requete interceptor apres validation metier.

  private void addInternalServiceToken(RequestTemplate template) {
    if (
      !StringUtils.hasText(internalServiceToken) ||
      template.feignTarget() == null
    ) {
      return;
    }
    template.header("X-Internal-Service-Token", internalServiceToken);
  }
}
