// Securite : applique l'authentification et les autorisations liees a internal service authentication filter.

package com.fintrack.audit.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

// Authentifie les appels inter-services porteurs du jeton interne partage :
// permet aux autres microservices d'enregistrer des entrees d'audit sans JWT
// utilisateur. Leur accorde l'autorite technique AUDIT_INTERNAL.
@Component
public class InternalServiceAuthenticationFilter extends OncePerRequestFilter {

  public static final String INTERNAL_SERVICE_TOKEN_HEADER =
    "X-Internal-Service-Token";

  private final String internalServiceToken;

  // Initialise le composant avec ses dependances obligatoires.
  public InternalServiceAuthenticationFilter(
    @Value("${fintrack.internal-service.token:}") String internalServiceToken
  ) {
    this.internalServiceToken = internalServiceToken;
  }

  @Override
  // Authentifie les requetes internes avant la suite du filtre.
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
  ) throws ServletException, IOException {
    if (
      SecurityContextHolder.getContext().getAuthentication() == null &&
      matchesInternalToken(request.getHeader(INTERNAL_SERVICE_TOKEN_HEADER))
    ) {
      UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
          "internal-service",
          null,
          List.of(new SimpleGrantedAuthority("AUDIT_INTERNAL"))
        );
      authentication.setDetails(
        new WebAuthenticationDetailsSource().buildDetails(request)
      );
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
  }

  // Comparaison a temps constant pour ne pas exposer le jeton via une attaque temporelle.
  private boolean matchesInternalToken(String requestToken) {
    if (
      !StringUtils.hasText(internalServiceToken) ||
      !StringUtils.hasText(requestToken)
    ) {
      return false;
    }

    byte[] expected = internalServiceToken.getBytes(StandardCharsets.UTF_8);
    byte[] actual = requestToken.getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(expected, actual);
  }
}
