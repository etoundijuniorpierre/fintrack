// Configuration Spring : declare les regles techniques du decodeur d'erreurs Feign.

package com.fintrack.incident.config;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configuration du decodeur d'erreurs Feign : journalise les echecs d'appels inter-services
@Slf4j
@Configuration
public class FeignErrorDecoderConfig {

  // Expose le decodeur d'erreurs Feign.

  @Bean
  public ErrorDecoder feignErrorDecoder() {
    return new StructuredFeignErrorDecoder();
  }

  // Decodeur qui trace les erreurs HTTP selon leur gravite puis delegue au decodeur par defaut
  static class StructuredFeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    // Decode decode.

    @Override
    public Exception decode(String methodKey, Response response) {
      int status = response.status();
      String url = response.request().url();

      if (status >= 500) {
        log.error(
          "[FEIGN-ERREUR] Échec de communication inter-services | méthode={} url={} statut={}",
          methodKey,
          url,
          status
        );
      } else if (status == 404) {
        log.warn(
          "[FEIGN-WARN] Ressource introuvable dans le service distant | méthode={} url={} statut={}",
          methodKey,
          url,
          status
        );
      } else if (status >= 400) {
        log.warn(
          "[FEIGN-WARN] Erreur client dans l'appel inter-service | méthode={} url={} statut={}",
          methodKey,
          url,
          status
        );
      }

      return defaultDecoder.decode(methodKey, response);
    }
  }
}
