// Securite : applique l'authentification et les autorisations liees a jwt authentication filter.

package com.fintrack.incident.security;

import com.fintrack.common.security.FullJwtAuthenticationFilter;
import java.util.Collection;
import java.util.UUID;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

// Filtre de securite interceptant les requetes pour valider le token JWT.

public class JwtAuthenticationFilter extends FullJwtAuthenticationFilter {

  // Initialise le filtre avec ses dependances de securite.

  public JwtAuthenticationFilter(JwtUtils jwtUtils) {
    super(jwtUtils);
  }

  @Override
  // Prepare l'ajout de incident apres validation metier.
  protected UserDetails createUserDetails(
    UUID id,
    String username,
    boolean enabled,
    Collection<SimpleGrantedAuthority> authorities,
    UUID serviceId,
    UUID agencyId
  ) {
    return new UserDetailsImpl(
      id,
      username,
      null,
      enabled,
      authorities,
      serviceId,
      agencyId
    );
  }
}
