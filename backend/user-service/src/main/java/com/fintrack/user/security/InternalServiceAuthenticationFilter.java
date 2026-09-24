// Securite : applique l'authentification et les autorisations liees a internal service authentication filter.

package com.fintrack.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

// Porte la responsabilite applicative liee a interne service authentication filter.

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
      UUID systemId = UUID.fromString("00000000-0000-0000-0000-000000000000");
      UserDetailsImpl internalUser = new UserDetailsImpl(
        systemId,
        "internal-service",
        "",
        true,
        false,
        null,
        null,
        List.of(
          new SimpleGrantedAuthority("ROLE_SYSTEM"),
          new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"),
          new SimpleGrantedAuthority("USER_VIEW_ALL")
        )
      );

      UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
          internalUser,
          null,
          internalUser.getAuthorities()
        );
      authentication.setDetails(
        new WebAuthenticationDetailsSource().buildDetails(request)
      );
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
  }

  // Realise l'intention metier matches internal token.

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
