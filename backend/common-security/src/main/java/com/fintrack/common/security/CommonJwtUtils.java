// Securite : applique l'authentification et les autorisations liees a common jwt utils.

package com.fintrack.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;

/**
 * Utilitaires de lecture et de validation des JWT, partagés par tous les microservices.
 * Étendre cette classe pour une logique spécifique (ex. génération de token dans user-service).
 */
@Slf4j
// Extrait les informations utiles depuis les jetons JWT.
public class CommonJwtUtils {

  private final SecretKey key;

  // Initialise l'utilitaire avec la configuration JWT.

  public CommonJwtUtils(String secret) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  // Extrait le nom d'utilisateur depuis le jeton JWT.

  public String getUserNameFromJwtToken(String token) {
    return Jwts.parser()
      .verifyWith(key)
      .build()
      .parseSignedClaims(token)
      .getPayload()
      .get("username", String.class);
  }

  // Extrait l'identifiant utilisateur depuis le jeton JWT.

  public String getUserIdFromJwtToken(String token) {
    return Jwts.parser()
      .verifyWith(key)
      .build()
      .parseSignedClaims(token)
      .getPayload()
      .getSubject();
  }

  // Extrait les claims depuis le jeton JWT.

  public Claims getClaimsFromJwtToken(String token) {
    return Jwts.parser()
      .verifyWith(key)
      .build()
      .parseSignedClaims(token)
      .getPayload();
  }

  // Extrait les roles depuis le jeton JWT.

  @SuppressWarnings("unchecked")
  public List<String> getRolesFromJwtToken(String token) {
    return getClaimsFromJwtToken(token).get("roles", List.class);
  }

  // Extrait les permissions depuis le jeton JWT.

  @SuppressWarnings("unchecked")
  public List<String> getPermissionsFromJwtToken(String token) {
    return getClaimsFromJwtToken(token).get("permissions", List.class);
  }

  // Verifie la signature et l'expiration, et que le token est bien de type ACCESS.
  public boolean validateJwtToken(String authToken) {
    try {
      Claims claims = Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(authToken)
        .getPayload();
      return "ACCESS".equals(claims.get("type", String.class));
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

  // Fournit la cle de signature JWT.

  protected SecretKey getKey() {
    return key;
  }
}
