// Securite : applique l'authentification et les autorisations liees au JWT.

package com.fintrack.user.security;

import com.fintrack.user.constant.ApiConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

// Filtre de securite interceptant les requetes pour valider le token JWT.
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtils jwtUtils;
  private final UserDetailsService userDetailsService;

  // Authentifie une requete utilisateur a partir du JWT et de l'etat actuel du compte.
  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
  ) throws ServletException, IOException {
    try {
      String jwt = parseJwt(request);
      boolean accessToken = jwt != null && jwtUtils.validateJwtToken(jwt);
      boolean firstLoginToken =
        jwt != null &&
        jwtUtils.validateJwtToken(jwt, JwtUtils.FIRST_LOGIN_TOKEN_TYPE);

      if (accessToken || firstLoginToken) {
        String username = jwtUtils.getUserNameFromJwtToken(jwt);
        UserDetails userDetails = userDetailsService.loadUserByUsername(
          username
        );
        boolean firstLogin =
          userDetails instanceof UserDetailsImpl ud && ud.isFirstLogin();

        if (
          firstLoginToken &&
          (!firstLogin || !isOwnInitialPasswordChange(request, jwt))
        ) {
          log.debug(
            "JWT de premiere connexion rejete pour {} sur {}",
            username,
            request.getRequestURI()
          );
          filterChain.doFilter(request, response);
          return;
        }
        if (accessToken && !userDetails.isEnabled()) {
          log.debug("JWT rejete : compte desactive pour {}", username);
          filterChain.doFilter(request, response);
          return;
        }

        Collection<? extends GrantedAuthority> authorities = firstLoginToken
          ? List.of(new SimpleGrantedAuthority("FIRST_LOGIN_PASSWORD_CHANGE"))
          : userDetails.getAuthorities();
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
      log.debug(
        "Authentification JWT ignoree pour {}: {}",
        request.getRequestURI(),
        e.getMessage()
      );
    }

    filterChain.doFilter(request, response);
  }

  private boolean isOwnInitialPasswordChange(
    HttpServletRequest request,
    String jwt
  ) {
    String expectedPath =
      ApiConstants.Endpoints.USERS +
      "/" +
      jwtUtils.getUserIdFromJwtToken(jwt) +
      "/change-password";
    return (
      HttpMethod.POST.matches(request.getMethod()) &&
      expectedPath.equals(request.getRequestURI())
    );
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
