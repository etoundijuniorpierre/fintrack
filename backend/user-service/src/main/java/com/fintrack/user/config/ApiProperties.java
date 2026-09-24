// Configuration Spring : declare les regles techniques liees a api properties.

package com.fintrack.user.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Proprietes de configuration des chemins d'API (prefixe "api" dans application.yml).
@Getter
@Setter
@ConfigurationProperties(prefix = "api")
public class ApiProperties {

  private String basePath;
  private Endpoints endpoints = new Endpoints();

  // Regroupe les chemins de chaque endpoint expose par le service.
  @Getter
  @Setter
  public static class Endpoints {

    private String auth;
    private String users;
    private String agencies;
    private String departments;
    private String roles;
    private String permissions;
  }
}
