// Configuration Spring : declare les regles techniques liees a jwt feign request interceptor.

package com.fintrack.incident.config;

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

// Intercepteur Feign qui propage le JWT, l'identifiant de correlation et le jeton inter-service sur les appels sortants
@Slf4j
@Component
public class JwtFeignRequestInterceptor implements RequestInterceptor {

  @Value("${fintrack.internal-service.token:}")
  private String internalServiceToken;

  // Declare le comportement technique attendu par l'infrastructure.

  @Override
  public void apply(RequestTemplate template) {
    // Reutilise le correlationId du contexte si present, sinon en genere un
    // local a cet appel. On n'ecrit PAS dans le MDC : sur un thread de pool,
    // une valeur laissee la fuirait vers la requete suivante de ce thread.
    String correlationId = MDC.get("correlationId");
    if (correlationId == null) {
      correlationId = UUID.randomUUID().toString();
    }

    template.header("X-Correlation-ID", correlationId);
    boolean notificationInternalCall = addInternalServiceToken(template);
    if (notificationInternalCall) {
      return;
    }

    ServletRequestAttributes attributes =
      (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

    if (attributes == null) {
      log.debug(
        "Aucun attribut de requête disponible pour appel Feign vers {}",
        template.url()
      );
      return;
    }

    HttpServletRequest request = attributes.getRequest();
    String authHeader = request.getHeader("Authorization");

    if (!StringUtils.hasText(authHeader)) {
      log.debug(
        "Aucun en-tête Authorization trouvé pour appel Feign vers {}",
        template.url()
      );
      return;
    }

    String finalHeader;
    if (authHeader.startsWith("Bearer ")) {
      finalHeader = authHeader;
    } else {
      finalHeader = "Bearer " + authHeader;
    }

    template.header("Authorization", finalHeader);

    log.debug(
      "Appel Feign préparé: url={}, method={}, correlationId={}",
      template.url(),
      template.method(),
      correlationId
    );
  }

  // Ajoute le jeton inter-service pour les endpoints techniques.
  private boolean addInternalServiceToken(RequestTemplate template) {
    if (
      !StringUtils.hasText(internalServiceToken) ||
      template.feignTarget() == null
    ) {
      return false;
    }

    String targetName = template.feignTarget().name();
    if ("audit-service".equals(targetName)) {
      template.header("X-Internal-Service-Token", internalServiceToken);
      return false;
    }

    if ("notification-service".equals(targetName)) {
      template.header("X-Internal-Service-Token", internalServiceToken);
      return true;
    }

    if ("reporting-service".equals(targetName)) {
      template.header("X-Internal-Service-Token", internalServiceToken);
      return true;
    }

    if ("user-service".equals(targetName)) {
      template.header("X-Internal-Service-Token", internalServiceToken);
      return true;
    }

    return false;
  }
}
