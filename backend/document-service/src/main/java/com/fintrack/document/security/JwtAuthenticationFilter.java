// Securite : applique l'authentification et les autorisations liees a jwt authentication filter.

package com.fintrack.document.security;

import com.fintrack.common.security.SimpleJwtAuthenticationFilter;

// Filtre d'authentification validant le jeton JWT present dans les requetes entrantes.
public class JwtAuthenticationFilter extends SimpleJwtAuthenticationFilter {

  // Initialise le filtre avec ses dependances de securite.

  public JwtAuthenticationFilter(JwtUtils jwtUtils) {
    super(jwtUtils);
  }
}
