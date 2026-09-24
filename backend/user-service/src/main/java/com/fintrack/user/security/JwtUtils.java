// Securite : applique l'authentification et les autorisations liees a jwt utils.

package com.fintrack.user.security;

import com.fintrack.common.security.CommonJwtUtils;
import com.fintrack.user.model.entity.Permission;
import com.fintrack.user.model.entity.Role;
import com.fintrack.user.model.entity.User;
import io.jsonwebtoken.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Utilitaire de gestion des jetons JWT (generation, validation et extraction).

@Slf4j
@Component
public class JwtUtils extends CommonJwtUtils {

  public static final String FIRST_LOGIN_TOKEN_TYPE = "FIRST_LOGIN";
  private static final String SESSION_STARTED_AT_CLAIM = "sessionStartedAt";

  @Value("${fintrack.jwt.expirationMs}")
  private int jwtExpirationMs;

  @Value("${fintrack.jwt.refreshExpirationMs}")
  private int refreshExpirationMs;

  @Value("${fintrack.jwt.sessionMaxDurationMs}")
  private long sessionMaxDurationMs;

  // Initialise l'utilitaire JWT avec le secret de signature du service.
  public JwtUtils(@Value("${fintrack.jwt.secret}") String secret) {
    super(secret);
  }

  // Genere le jeton JWT d'acces de l'utilisateur.
  public String generateJwtToken(User user) {
    return generateJwtToken(user, System.currentTimeMillis());
  }

  // Genere un access token qui ne depasse jamais la fin absolue de session.
  public String generateJwtToken(User user, long sessionStartedAt) {
    return generateTokenFromUsername(user, jwtExpirationMs, sessionStartedAt);
  }

  // Genere un jeton sans autorisations, reserve au changement du mot de passe initial.
  public String generateFirstLoginToken(User user, long sessionStartedAt) {
    long now = System.currentTimeMillis();
    if (sessionStartedAt <= 0 || sessionStartedAt > now) {
      throw new IllegalArgumentException("Invalid session start time");
    }
    return Jwts.builder()
      .subject(user.getId().toString())
      .claim("username", user.getUsername())
      .claim("roles", Set.of())
      .claim("permissions", Set.of())
      .claim("type", FIRST_LOGIN_TOKEN_TYPE)
      .claim(SESSION_STARTED_AT_CLAIM, sessionStartedAt)
      .issuedAt(new Date(now))
      .expiration(
        new Date(
          Math.min(
            now + jwtExpirationMs,
            sessionStartedAt + sessionMaxDurationMs
          )
        )
      )
      .signWith(getKey(), Jwts.SIG.HS256)
      .compact();
  }

  // Genere le jeton JWT de rafraichissement de l'utilisateur.
  public String generateRefreshToken(User user) {
    return generateRefreshToken(user, System.currentTimeMillis());
  }

  // Renouvelle le refresh token sans repousser la fin absolue de la session.
  public String generateRefreshToken(User user, long sessionStartedAt) {
    long now = System.currentTimeMillis();
    if (sessionStartedAt <= 0 || sessionStartedAt > now) {
      throw new IllegalArgumentException("Invalid session start time");
    }
    long rollingExpiration = now + refreshExpirationMs;
    long absoluteExpiration = sessionStartedAt + sessionMaxDurationMs;

    return Jwts.builder()
      .subject(user.getId().toString())
      .claim("username", user.getUsername())
      .claim("type", "REFRESH")
      .claim(SESSION_STARTED_AT_CLAIM, sessionStartedAt)
      .issuedAt(new Date(now))
      .expiration(new Date(Math.min(rollingExpiration, absoluteExpiration)))
      .signWith(getKey(), Jwts.SIG.HS256)
      .compact();
  }

  // Retourne l'origine immuable de la session portee par le refresh token.
  public long getSessionStartedAt(String refreshToken) {
    return getClaimsFromJwtToken(refreshToken).get(
      SESSION_STARTED_AT_CLAIM,
      Long.class
    );
  }

  // Verifie que la session n'a pas depasse sa duree absolue autorisee.
  public boolean isRefreshSessionActive(String refreshToken) {
    try {
      long startedAt = getSessionStartedAt(refreshToken);
      long now = System.currentTimeMillis();
      return (
        startedAt > 0 &&
        startedAt <= now &&
        now - startedAt < sessionMaxDurationMs
      );
    } catch (
      JwtException
      | IllegalArgumentException
      | NullPointerException ex
    ) {
      log.warn(
        "Refresh token sans origine de session valide: {}",
        ex.getMessage()
      );
      return false;
    }
  }

  // Calcule la duree de vie restante du refresh token pour son cookie HTTP.
  public int getRefreshTokenRemainingSeconds(String refreshToken) {
    long remainingMs =
      getClaimsFromJwtToken(refreshToken).getExpiration().getTime() -
      System.currentTimeMillis();
    return (int) Math.max(0, (remainingMs + 999) / 1000);
  }

  // Construit un jeton d'acces enrichi avec les roles, permissions et rattachements de l'utilisateur.

  private String generateTokenFromUsername(
    User user,
    int expiration,
    long sessionStartedAt
  ) {
    long now = System.currentTimeMillis();
    if (sessionStartedAt <= 0 || sessionStartedAt > now) {
      throw new IllegalArgumentException("Invalid session start time");
    }
    Set<String> roles =
      user.getRoles() == null
        ? Set.of()
        : user
            .getRoles()
            .stream()
            .map(Role::getName)
            .collect(Collectors.toSet());
    Set<String> permissions = new HashSet<>();
    if (user.getPermissions() != null) {
      user
        .getPermissions()
        .stream()
        .map(Permission::getName)
        .map(String::toUpperCase)
        .forEach(permissions::add);
    }
    if (user.getRoles() != null) {
      user
        .getRoles()
        .stream()
        .filter(r -> r.getPermissions() != null)
        .flatMap(r -> r.getPermissions().stream())
        .map(Permission::getName)
        .map(String::toUpperCase)
        .forEach(permissions::add);
    }
    // Soustraction des permissions revoquees : (directes ∪ role) - revoquees.
    if (user.getRevokedPermissions() != null) {
      user
        .getRevokedPermissions()
        .stream()
        .map(Permission::getName)
        .map(String::toUpperCase)
        .forEach(permissions::remove);
    }

    JwtBuilder builder = Jwts.builder()
      .subject(user.getId().toString())
      .claim("username", user.getUsername())
      .claim("roles", roles)
      .claim("permissions", permissions)
      .claim("type", "ACCESS")
      .claim(SESSION_STARTED_AT_CLAIM, sessionStartedAt)
      .issuedAt(new Date(now))
      .expiration(
        new Date(
          Math.min(now + expiration, sessionStartedAt + sessionMaxDurationMs)
        )
      );

    if (user.getAgency() != null) {
      builder.claim("agencyId", user.getAgency().getId().toString());
    }
    if (user.getService() != null) {
      builder.claim("serviceId", user.getService().getId().toString());
    }

    return builder.signWith(getKey(), Jwts.SIG.HS256).compact();
  }

  // Valide la signature et la portee d'un jeton JWT.
  public boolean validateJwtToken(String authToken, String expectedType) {
    try {
      Claims claims = Jwts.parser()
        .verifyWith(getKey())
        .build()
        .parseSignedClaims(authToken)
        .getPayload();
      String tokenType = claims.get("type", String.class);
      return expectedType == null || expectedType.equals(tokenType);
    } catch (
      MalformedJwtException
      | ExpiredJwtException
      | UnsupportedJwtException
      | IllegalArgumentException e
    ) {
      log.error("Erreur de validation JWT: {}", e.getMessage());
    }
    return false;
  }
}
