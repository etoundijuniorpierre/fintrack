// Securite : applique l'authentification et les autorisations liees a jwt authentication filter.

package com.fintrack.audit.security;

import com.fintrack.common.security.SimpleJwtAuthenticationFilter;

// Filtre d'authentification JWT du service, base sur l'implementation commune.
public class JwtAuthenticationFilter extends SimpleJwtAuthenticationFilter {

  // Initialise le filtre avec ses dependances de securite.

  public JwtAuthenticationFilter(JwtUtils jwtUtils) {
    super(jwtUtils);
  }
}
