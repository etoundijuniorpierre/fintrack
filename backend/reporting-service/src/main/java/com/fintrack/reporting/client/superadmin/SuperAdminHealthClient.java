// Client inter-services : communique avec les services externes lies a super admin health.

package com.fintrack.reporting.client.superadmin;

import com.fintrack.reporting.model.readmodel.superadmin.HealthProbe;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// Sonde sante authentifiee : propage le JWT de la requete courante ou le token interne
// pour obtenir les composants detailles (show-details=when-authorized).
@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminHealthClient {

  // Timeout de la sonde sante (reglage infra, surcharge par environnement).
  @Value("${fintrack.superadmin.health-probe-timeout-ms:500}")
  private int healthProbeTimeoutMs;

  private RestTemplate restTemplate;

  @Value("${fintrack.internal-service.token:}")
  private String internalServiceToken;

  @PostConstruct
  // Initialise rest template.
  void initRestTemplate() {
    this.restTemplate = buildRestTemplate(healthProbeTimeoutMs);
  }

  // Realise l'intention metier probe.

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public HealthProbe probe(String key, String baseUrl) {
    HealthProbe result = new HealthProbe();
    result.setKey(key);
    result.setBaseUrl(baseUrl);
    result.setLastCheckedAt(Instant.now().toString());

    if (baseUrl == null || baseUrl.startsWith("self:")) {
      result.setStatus("UP");
      result.setResponseTimeMs(0L);
      result.setEndpoint("local");
      result.setComponents(
        Map.of("application", baseUrl == null ? "local" : baseUrl.substring(5))
      );
      return result;
    }

    String endpoint = baseUrl + "/actuator/health";
    result.setEndpoint(endpoint);

    long startedAt = System.nanoTime();
    try {
      ResponseEntity<Map> response = restTemplate.exchange(
        URI.create(endpoint),
        HttpMethod.GET,
        new HttpEntity<>(buildHeaders()),
        Map.class
      );
      Map<?, ?> body =
        response.getBody() == null ? Map.of() : response.getBody();
      Object statusValue = body.get("status");
      result.setStatus(
        statusValue == null ? "UNKNOWN" : String.valueOf(statusValue)
      );
      result.setComponents(
        body.containsKey("components")
          ? (Map<String, Object>) body.get("components")
          : Map.of()
      );
    } catch (RuntimeException ex) {
      log.warn("Sonde de santé échouée pour {} : {}", key, ex.toString());
      result.setStatus("DOWN");
      result.setError(ex.getClass().getSimpleName());
    }
    result.setResponseTimeMs(
      Math.max(1L, (System.nanoTime() - startedAt) / 1_000_000L)
    );
    return result;
  }

  // Construit la representation attendue pour le domaine super-administration sante.

  private HttpHeaders buildHeaders() {
    HttpHeaders headers = new HttpHeaders();
    String authHeader = currentAuthorizationHeader();
    if (authHeader != null) {
      headers.set(
        "Authorization",
        authHeader.startsWith("Bearer ") ? authHeader : "Bearer " + authHeader
      );
    } else if (
      internalServiceToken != null && !internalServiceToken.isBlank()
    ) {
      headers.set("X-Internal-Service-Token", internalServiceToken);
    }
    return headers;
  }

  // Realise l'intention metier current authorization header.

  private String currentAuthorizationHeader() {
    try {
      ServletRequestAttributes attrs =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attrs == null) {
        return null;
      }
      HttpServletRequest request = attrs.getRequest();
      String header = request.getHeader("Authorization");
      if (
        header != null &&
        !header.isBlank() &&
        SecurityContextHolder.getContext().getAuthentication() != null
      ) {
        return header;
      }
    } catch (RuntimeException ignored) {
      // Utilise le repli local ci-dessous.
    }
    return null;
  }

  // Construit la representation attendue pour le domaine super-administration sante.

  private static RestTemplate buildRestTemplate(int timeoutMs) {
    SimpleClientHttpRequestFactory factory =
      new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(timeoutMs);
    factory.setReadTimeout(timeoutMs);
    return new RestTemplate(factory);
  }
}
