// Securite : applique l'authentification et les autorisations liees a jwt utils.

package com.fintrack.incident.security;

import com.fintrack.common.security.CommonJwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Utilitaire de gestion des jetons JWT (generation, validation et extraction).

@Component
public class JwtUtils extends CommonJwtUtils {

  // Initialise l'utilitaire JWT avec le secret de signature du service.
  public JwtUtils(@Value("${fintrack.jwt.secret}") String secret) {
    super(secret);
  }
}
