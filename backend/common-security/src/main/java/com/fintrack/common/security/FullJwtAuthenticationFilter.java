// Securite : applique l'authentification et les autorisations liees a full jwt authentication filter.

package com.fintrack.common.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtre JWT pour les services nécessitant le contexte utilisateur complet
 * (userId, rôles, agencyId, serviceId), exposé comme principal typé via @AuthenticationPrincipal.
 * Utilisé par : incident-service, reporting-service.
 * Les sous-classes implémentent {@link #createUserDetails} pour fournir leur UserDetails local.
 */
// Filtre les requetes authentifiees par un JWT utilisateur complet.
public abstract class FullJwtAuthenticationFilter extends OncePerRequestFilter {

  private final CommonJwtUtils jwtUtils;

  // Initialise le filtre avec ses dependances de securite.

  protected FullJwtAuthenticationFilter(CommonJwtUtils jwtUtils) {
    this.jwtUtils = jwtUtils;
  }

  // Fabrique le UserDetails local du service a partir des claims du JWT.
  protected abstract UserDetails createUserDetails(
    UUID id,
    String username,
    boolean enabled,
    Collection<SimpleGrantedAuthority> authorities,
    UUID serviceId,
    UUID agencyId
  );

  // Authentifie une requete a partir d'un JWT contenant le contexte utilisateur complet.
  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
  ) throws ServletException, IOException {
    try {
      String jwt = parseJwt(request);
      if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
        String username = jwtUtils.getUserNameFromJwtToken(jwt);
        String userId = jwtUtils.getUserIdFromJwtToken(jwt);
        Claims claims = jwtUtils.getClaimsFromJwtToken(jwt);

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        if (roles != null) {
          roles.forEach(role ->
            authorities.add(
              new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())
            )
          );
        }

        @SuppressWarnings("unchecked")
        List<String> permissions = claims.get("permissions", List.class);
        if (permissions != null) {
          permissions.forEach(permission ->
            authorities.add(
              new SimpleGrantedAuthority(permission.toUpperCase())
            )
          );
        }

        String serviceIdClaim = claims.get("serviceId", String.class);
        String agencyIdClaim = claims.get("agencyId", String.class);

        UUID serviceId =
          serviceIdClaim != null ? UUID.fromString(serviceIdClaim) : null;
        UUID agencyId =
          agencyIdClaim != null ? UUID.fromString(agencyIdClaim) : null;

        UserDetails userDetails = createUserDetails(
          UUID.fromString(userId),
          username,
          true,
          authorities,
          serviceId,
          agencyId
        );

        UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            authorities
          );
        authentication.setDetails(
          new WebAuthenticationDetailsSource().buildDetails(request)
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    } catch (Exception e) {
      // L'authentification echoue silencieusement (la requete poursuit en anonyme) ;
      // la cause est tracee pour le diagnostic.
      logger.debug("Authentification JWT ignorée: " + e.getMessage());
    }
    filterChain.doFilter(request, response);
  }

  // Lit les donnees brutes du domaine full jwt authentication filter dans un format exploitable.

  private String parseJwt(HttpServletRequest request) {
    String headerAuth = request.getHeader("Authorization");
    if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
      return headerAuth.substring(7);
    }
    return null;
  }
}
