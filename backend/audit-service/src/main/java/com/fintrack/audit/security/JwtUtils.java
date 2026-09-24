// Securite : applique l'authentification et les autorisations liees a jwt utils.

package com.fintrack.audit.security;

import com.fintrack.common.security.CommonJwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Utilitaires JWT du service, initialises avec le secret de configuration.
@Component
public class JwtUtils extends CommonJwtUtils {

  // Initialise l'utilitaire JWT avec le secret de signature du service.
  public JwtUtils(@Value("${fintrack.jwt.secret}") String secret) {
    super(secret);
  }
}
