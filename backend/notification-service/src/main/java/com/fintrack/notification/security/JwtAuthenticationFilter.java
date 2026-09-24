// Securite : applique l'authentification et les autorisations liees a jwt authentication filter.

package com.fintrack.notification.security;

import com.fintrack.common.security.SimpleJwtAuthenticationFilter;

// Filtre de securite interceptant les requetes pour valider le token JWT.

public class JwtAuthenticationFilter extends SimpleJwtAuthenticationFilter {

  // Initialise le filtre avec ses dependances de securite.

  public JwtAuthenticationFilter(JwtUtils jwtUtils) {
    super(jwtUtils);
  }
}
