// Securite : applique l'authentification et les autorisations liees a simple jwt authentication filter.

package com.fintrack.common.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtre JWT pour les services dont le principal se limite au nom d'utilisateur
 * et aux permissions (String). Utilisé par : document-service, notification-service, audit-service.
 */
@RequiredArgsConstructor
// Filtre les requetes authentifiees par un JWT de service.
public class SimpleJwtAuthenticationFilter extends OncePerRequestFilter {

  private final CommonJwtUtils jwtUtils;

  // Authentifie une requete a partir d'un JWT limite aux permissions du service.
  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
  ) throws ServletException, IOException {
    try {
      String jwt = parseJwt(request);
      if (
        jwt != null &&
        SecurityContextHolder.getContext().getAuthentication() == null &&
        jwtUtils.validateJwtToken(jwt)
      ) {
        String username = jwtUtils.getUserNameFromJwtToken(jwt);
        Claims claims = jwtUtils.getClaimsFromJwtToken(jwt);

        @SuppressWarnings("unchecked")
        List<String> permissions = claims.get("permissions", List.class);
        Collection<SimpleGrantedAuthority> authorities =
          permissions == null
            ? List.of()
            : permissions
                .stream()
                .map(p -> new SimpleGrantedAuthority(p.toUpperCase()))
                .collect(Collectors.toList());

        UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(username, null, authorities);
        authentication.setDetails(
          new WebAuthenticationDetailsSource().buildDetails(request)
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    } catch (Exception e) {
      logger.error(
        "Impossible de définir l'authentification utilisateur: {}",
        e
      );
    }
    filterChain.doFilter(request, response);
  }

  // Extrait le jeton Bearer de l'en-tete d'autorisation.

  private String parseJwt(HttpServletRequest request) {
    String headerAuth = request.getHeader("Authorization");
    if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
      return headerAuth.substring(7);
    }
    return null;
  }
}
