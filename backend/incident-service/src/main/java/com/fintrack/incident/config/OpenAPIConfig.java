// Configuration Spring : declare les regles techniques liees a open api.

package com.fintrack.incident.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configuration de la documentation OpenAPI/Swagger avec authentification JWT Bearer
@Configuration
public class OpenAPIConfig {

  private static final String SECURITY_SCHEME_NAME = "bearerAuth";

  // Realise l'intention metier open api.

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
      .components(
        new Components().addSecuritySchemes(
          SECURITY_SCHEME_NAME,
          new SecurityScheme()
            .name(SECURITY_SCHEME_NAME)
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .in(SecurityScheme.In.HEADER)
        )
      )
      .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
      .info(
        new Info()
          .title("FinTrack Incident Service API")
          .description(
            "API documentation for the Incident Management microservice"
          )
          .version("v1.0")
      );
  }
}
