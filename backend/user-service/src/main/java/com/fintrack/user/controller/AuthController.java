// Controleur REST : expose les operations HTTP liees a auth.

package com.fintrack.user.controller;

import com.fintrack.user.client.audit.AuditServiceClientService;
import com.fintrack.user.client.audit.constant.AuditAction;
import com.fintrack.user.client.audit.constant.AuditStatus;
import com.fintrack.user.constant.ApiConstants;
import com.fintrack.user.model.dto.request.ContactAdminRequest;
import com.fintrack.user.model.dto.request.LoginRequest;
import com.fintrack.user.model.dto.response.AuthResponse;
import com.fintrack.user.model.entity.User;
import com.fintrack.user.model.mapper.AuthMapper;
import com.fintrack.user.security.JwtUtils;
import com.fintrack.user.security.UserDetailsImpl;
import com.fintrack.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

// Controleur REST d'authentification : login, rafraichissement de token et deconnexion.
@RestController
@RequestMapping(ApiConstants.Endpoints.AUTH)
@Tag(
  name = "Authentication",
  description = "Endpoints for user login and token management"
)
@RequiredArgsConstructor
public class AuthController {

  private final UserService userService;
  private final JwtUtils jwtUtils;
  private final AuthMapper authMapper;
  private final AuditServiceClientService auditServiceClientService;

  // Securise par defaut : le cookie de refresh n'est emis que sur HTTPS.
  // Le profil dev (HTTP local) surcharge cette valeur a false.
  @Value("${fintrack.security.cookie-secure:true}")
  private boolean cookieSecure;

  // Authentifie l'utilisateur, emet le JWT et le cookie de refresh, et trace le resultat dans l'audit.
  @PostMapping("/login")
  @Operation(summary = "Authenticate user and get JWT")
  public ResponseEntity<AuthResponse> authenticateUser(
    @Valid @RequestBody LoginRequest loginRequest,
    HttpServletRequest httpRequest,
    HttpServletResponse response
  ) {
    try {
      User user = userService.authenticate(
        loginRequest.getUsername(),
        loginRequest.getPassword()
      );

      UserDetailsImpl userDetails = UserDetailsImpl.build(user);
      SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
          userDetails,
          null,
          userDetails.getAuthorities()
        )
      );

      long sessionStartedAt = System.currentTimeMillis();
      String jwt;
      if (user.isFirstLogin()) {
        jwt = jwtUtils.generateFirstLoginToken(user, sessionStartedAt);
        clearRefreshTokenCookie(response);
      } else {
        jwt = jwtUtils.generateJwtToken(user, sessionStartedAt);
        String refreshToken = jwtUtils.generateRefreshToken(
          user,
          sessionStartedAt
        );
        addRefreshTokenCookie(response, refreshToken);
      }

      auditServiceClientService.audit(
        user.getId(),
        user.getUsername(),
        user
          .getRoles()
          .stream()
          .map(r -> r.getName())
          .toList(),
        AuditAction.LOGIN_SUCCESS.getName(),
        "USER",
        user.getId().toString(),
        AuditStatus.SUCCESS.getName(),
        httpRequest.getRemoteAddr(),
        httpRequest.getHeader("User-Agent"),
        null
      );

      AuthResponse authResponse = authMapper.userToAuthResponse(user);
      authResponse.setToken(jwt);
      // Garantit explicitement les flags de premiere connexion / activation,
      // utilises par le frontend pour afficher la modale de changement de mot de passe.
      authResponse.setFirstLogin(user.isFirstLogin());
      authResponse.setActive(user.isActive());
      return ResponseEntity.ok(authResponse);
    } catch (Exception ex) {
      auditFailedLogin(loginRequest.getUsername(), httpRequest);
      throw ex;
    }
  }

  // Trace un echec de connexion en rattachant le compte quand le username existe.
  private void auditFailedLogin(
    String username,
    HttpServletRequest httpRequest
  ) {
    User user = findUserQuietly(username);
    UUID userId = user != null ? user.getId() : null;
    String auditUsername = user != null ? user.getUsername() : username;
    String resourceId =
      user != null && user.getId() != null ? user.getId().toString() : username;
    auditServiceClientService.audit(
      userId,
      auditUsername,
      null,
      AuditAction.LOGIN_FAILURE.getName(),
      "USER",
      resourceId,
      AuditStatus.FAILURE.getName(),
      httpRequest.getRemoteAddr(),
      httpRequest.getHeader("User-Agent"),
      null
    );
  }

  // Tente de retrouver l'utilisateur sans masquer l'echec d'authentification initial.
  private User findUserQuietly(String username) {
    if (username == null || username.isBlank()) {
      return null;
    }
    try {
      return userService.findByUsername(username);
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  // Genere un nouveau JWT a partir du cookie de refresh valide, si le compte est toujours actif.
  @PostMapping("/refresh")
  @Operation(summary = "Refresh access token using refresh token cookie")
  public ResponseEntity<AuthResponse> refreshToken(
    HttpServletRequest request,
    HttpServletResponse response
  ) {
    String refreshToken = getRefreshTokenFromCookies(request);

    if (
      refreshToken != null &&
      jwtUtils.validateJwtToken(refreshToken, "REFRESH") &&
      jwtUtils.isRefreshSessionActive(refreshToken)
    ) {
      long sessionStartedAt = jwtUtils.getSessionStartedAt(refreshToken);
      User user = userService.findById(
        UUID.fromString(jwtUtils.getUserIdFromJwtToken(refreshToken))
      );

      if (user != null && user.isActive() && !user.isFirstLogin()) {
        String newJwt = jwtUtils.generateJwtToken(user, sessionStartedAt);
        String newRefreshToken = jwtUtils.generateRefreshToken(
          user,
          sessionStartedAt
        );
        addRefreshTokenCookie(response, newRefreshToken);

        AuthResponse authResponse = authMapper.userToAuthResponse(user);
        authResponse.setToken(newJwt);
        authResponse.setFirstLogin(user.isFirstLogin());
        authResponse.setActive(user.isActive());
        return ResponseEntity.ok(authResponse);
      }
    }

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }

  // Deconnecte l'utilisateur : efface le cookie de refresh et vide le contexte de securite.
  @PostMapping("/logout")
  @Operation(summary = "Logout user by clearing refresh token cookie")
  public ResponseEntity<Void> logout(
    HttpServletRequest request,
    HttpServletResponse response
  ) {
    Authentication authentication =
      SecurityContextHolder.getContext().getAuthentication();

    if (
      authentication != null &&
      authentication.getPrincipal() instanceof UserDetailsImpl userDetails
    ) {
      auditServiceClientService.audit(
        userDetails.getId(),
        userDetails.getUsername(),
        null,
        AuditAction.LOGOUT.getName(),
        "USER",
        userDetails.getId().toString(),
        AuditStatus.SUCCESS.getName(),
        request.getRemoteAddr(),
        request.getHeader("User-Agent"),
        null
      );
    }

    clearRefreshTokenCookie(response);
    SecurityContextHolder.clearContext();
    return ResponseEntity.noContent().build();
  }

  // Invalide le cookie de refresh cote client (maxAge a 0).
  private void clearRefreshTokenCookie(HttpServletResponse response) {
    Cookie cookie = new Cookie("refreshToken", null);
    cookie.setHttpOnly(true);
    cookie.setSecure(cookieSecure);
    cookie.setPath("/api/v1/userService/auth/refresh");
    cookie.setMaxAge(0);
    response.addCookie(cookie);
  }

  // Depose le token de refresh dans un cookie httpOnly limite au chemin /auth/refresh.
  private void addRefreshTokenCookie(
    HttpServletResponse response,
    String refreshToken
  ) {
    Cookie cookie = new Cookie("refreshToken", refreshToken);
    cookie.setHttpOnly(true);
    cookie.setSecure(cookieSecure);
    cookie.setPath("/api/v1/userService/auth/refresh");
    cookie.setMaxAge(jwtUtils.getRefreshTokenRemainingSeconds(refreshToken));
    response.addCookie(cookie);
  }

  // Extrait la valeur du cookie "refreshToken" de la requete, ou null s'il est absent.
  private String getRefreshTokenFromCookies(HttpServletRequest request) {
    return request.getCookies() == null
      ? null
      : Arrays.stream(request.getCookies())
          .filter(cookie -> "refreshToken".equals(cookie.getName()))
          .map(Cookie::getValue)
          .findFirst()
          .orElse(null);
  }

  // Realise l'intention metier contact admin.

  @PostMapping("/contact-admin")
  @Operation(summary = "Contact an admin (unauthenticated)")
  public ResponseEntity<Void> contactAdmin(
    @Valid @RequestBody ContactAdminRequest request
  ) {
    userService.contactAdmin(
      request.getUsername(),
      request.getSubject(),
      request.getMessage()
    );
    return ResponseEntity.ok().build();
  }
}
