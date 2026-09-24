// Configuration Spring : declare les regles techniques liees a open api.

package com.fintrack.audit.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configuration de la documentation OpenAPI/Swagger du service.
@Configuration
public class OpenAPIConfig {

  private static final String SECURITY_SCHEME_NAME = "bearerAuth";

  // Declare le schema de securite JWT et les metadonnees de l'API.
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
          .title("FinTrack Audit Service API")
          .description("API documentation for the Audit Log microservice")
          .version("v1.0")
      );
  }
}
