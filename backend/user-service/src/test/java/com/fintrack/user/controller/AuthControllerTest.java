package com.fintrack.user.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fintrack.user.client.audit.AuditServiceClientService;
import com.fintrack.user.model.dto.request.LoginRequest;
import com.fintrack.user.model.dto.response.AuthResponse;
import com.fintrack.user.model.entity.User;
import com.fintrack.user.model.mapper.AuthMapper;
import com.fintrack.user.security.JwtUtils;
import com.fintrack.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock
  private UserService userService;

  @Mock
  private JwtUtils jwtUtils;

  @Mock
  private AuthMapper authMapper;

  @Mock
  private AuditServiceClientService auditServiceClientService;

  @Mock
  private Authentication authentication;

  @Mock
  private HttpServletResponse response;

  @Mock
  private HttpServletRequest request;

  @InjectMocks
  private AuthController authController;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Authenticate user - Normal login success")
  void authenticateUser_normalLogin_success() {
    LoginRequest loginRequest = new LoginRequest();
    loginRequest.setUsername("testuser");
    loginRequest.setPassword("password");

    UUID userId = UUID.randomUUID();
    User user = new User();
    user.setId(userId);
    user.setUsername("testuser");
    user.setRoles(Collections.emptySet());
    user.setPermissions(Collections.emptySet());
    user.setFirstLogin(false);

    AuthResponse authResponse = new AuthResponse();
    authResponse.setToken("fake-jwt");
    authResponse.setUsername("testuser");
    authResponse.setFirstLogin(false);

    when(userService.authenticate(anyString(), anyString())).thenReturn(user);
    when(jwtUtils.generateJwtToken(eq(user), anyLong())).thenReturn("fake-jwt");
    when(jwtUtils.generateRefreshToken(eq(user), anyLong())).thenReturn(
      "fake-refresh-token"
    );
    when(authMapper.userToAuthResponse(user)).thenReturn(authResponse);
    doNothing()
      .when(auditServiceClientService)
      .audit(
        any(),
        anyString(),
        any(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        any(),
        any(),
        any()
      );

    ResponseEntity<AuthResponse> result = authController.authenticateUser(
      loginRequest,
      request,
      response
    );

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertNotNull(result.getBody());
    assertEquals("fake-jwt", result.getBody().getToken());
    assertEquals("testuser", result.getBody().getUsername());
    assertFalse(result.getBody().isFirstLogin());

    verify(userService, times(1)).authenticate("testuser", "password");
    verify(jwtUtils, times(1)).generateJwtToken(eq(user), anyLong());
    verify(jwtUtils, times(1)).generateRefreshToken(eq(user), anyLong());
  }

  @Test
  @DisplayName("Authenticate user - First login success")
  void authenticateUser_firstLogin_success() {
    LoginRequest loginRequest = new LoginRequest();
    loginRequest.setUsername("newuser");
    loginRequest.setPassword("tempPassword123");

    UUID userId = UUID.randomUUID();
    User user = new User();
    user.setId(userId);
    user.setUsername("newuser");
    user.setRoles(Collections.emptySet());
    user.setPermissions(Collections.emptySet());
    user.setFirstLogin(true);

    AuthResponse authResponse = new AuthResponse();
    authResponse.setToken("fake-jwt");
    authResponse.setUsername("newuser");
    authResponse.setFirstLogin(true);

    when(userService.authenticate(anyString(), anyString())).thenReturn(user);
    when(jwtUtils.generateFirstLoginToken(eq(user), anyLong())).thenReturn(
      "fake-jwt"
    );
    when(authMapper.userToAuthResponse(user)).thenReturn(authResponse);
    doNothing()
      .when(auditServiceClientService)
      .audit(
        any(),
        anyString(),
        any(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        any(),
        any(),
        any()
      );

    ResponseEntity<AuthResponse> result = authController.authenticateUser(
      loginRequest,
      request,
      response
    );

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertNotNull(result.getBody());
    assertEquals("fake-jwt", result.getBody().getToken());
    assertEquals("newuser", result.getBody().getUsername());
    assertTrue(result.getBody().isFirstLogin());

    verify(userService, times(1)).authenticate("newuser", "tempPassword123");
    verify(jwtUtils, times(1)).generateFirstLoginToken(eq(user), anyLong());
    verify(jwtUtils, never()).generateRefreshToken(eq(user), anyLong());
  }

  @Test
  @DisplayName("Refresh token - Preserves the original absolute session start")
  void refreshToken_activeSession_preservesSessionStart() {
    UUID userId = UUID.randomUUID();
    long sessionStartedAt = System.currentTimeMillis() - 3_600_000;
    User user = new User();
    user.setId(userId);
    user.setUsername("testuser");
    user.setActive(true);
    user.setFirstLogin(false);
    user.setRoles(Collections.emptySet());
    user.setPermissions(Collections.emptySet());

    AuthResponse authResponse = new AuthResponse();
    when(request.getCookies()).thenReturn(new Cookie[] {
      new Cookie("refreshToken", "old-refresh"),
    });
    when(jwtUtils.validateJwtToken("old-refresh", "REFRESH")).thenReturn(true);
    when(jwtUtils.isRefreshSessionActive("old-refresh")).thenReturn(true);
    when(jwtUtils.getSessionStartedAt("old-refresh")).thenReturn(
      sessionStartedAt
    );
    when(jwtUtils.getUserIdFromJwtToken("old-refresh")).thenReturn(
      userId.toString()
    );
    when(userService.findById(userId)).thenReturn(user);
    when(jwtUtils.generateJwtToken(user, sessionStartedAt)).thenReturn(
      "new-access"
    );
    when(jwtUtils.generateRefreshToken(user, sessionStartedAt)).thenReturn(
      "new-refresh"
    );
    when(jwtUtils.getRefreshTokenRemainingSeconds("new-refresh")).thenReturn(
      39_600
    );
    when(authMapper.userToAuthResponse(user)).thenReturn(authResponse);

    ResponseEntity<AuthResponse> result = authController.refreshToken(
      request,
      response
    );

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals("new-access", result.getBody().getToken());
    verify(jwtUtils).generateRefreshToken(user, sessionStartedAt);
    verify(response).addCookie(argThat(cookie -> cookie.getMaxAge() == 39_600));
  }

  @Test
  @DisplayName("Refresh token - Rejects a session past its absolute duration")
  void refreshToken_expiredAbsoluteSession_returnsUnauthorized() {
    when(request.getCookies()).thenReturn(new Cookie[] {
      new Cookie("refreshToken", "expired-refresh"),
    });
    when(jwtUtils.validateJwtToken("expired-refresh", "REFRESH")).thenReturn(
      true
    );
    when(jwtUtils.isRefreshSessionActive("expired-refresh")).thenReturn(false);

    ResponseEntity<AuthResponse> result = authController.refreshToken(
      request,
      response
    );

    assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    verify(userService, never()).findById(any());
    verify(response, never()).addCookie(any());
  }

  @Test
  @DisplayName("Refresh token - Rejects an unfinished first-login session")
  void refreshToken_firstLogin_returnsUnauthorized() {
    UUID userId = UUID.randomUUID();
    User user = new User();
    user.setId(userId);
    user.setActive(true);
    user.setFirstLogin(true);

    when(request.getCookies()).thenReturn(new Cookie[] {
      new Cookie("refreshToken", "legacy-refresh"),
    });
    when(jwtUtils.validateJwtToken("legacy-refresh", "REFRESH")).thenReturn(
      true
    );
    when(jwtUtils.isRefreshSessionActive("legacy-refresh")).thenReturn(true);
    when(jwtUtils.getSessionStartedAt("legacy-refresh")).thenReturn(
      System.currentTimeMillis()
    );
    when(jwtUtils.getUserIdFromJwtToken("legacy-refresh")).thenReturn(
      userId.toString()
    );
    when(userService.findById(userId)).thenReturn(user);

    ResponseEntity<AuthResponse> result = authController.refreshToken(
      request,
      response
    );

    assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    verify(jwtUtils, never()).generateJwtToken(any(), anyLong());
    verify(response, never()).addCookie(any());
  }

  @Test
  @DisplayName(
    "Authenticate user - Failed login keeps known user identifiers in audit"
  )
  void authenticateUser_failedLogin_knownUser_auditsUserIdentifiers() {
    LoginRequest loginRequest = new LoginRequest();
    loginRequest.setUsername("agent3");
    loginRequest.setPassword("bad-password");
    UUID userId = UUID.randomUUID();
    User user = new User();
    user.setId(userId);
    user.setUsername("agent3");

    when(userService.authenticate("agent3", "bad-password")).thenThrow(
      new RuntimeException("Bad credentials")
    );
    when(userService.findByUsername("agent3")).thenReturn(user);
    when(request.getRemoteAddr()).thenReturn("172.18.0.11");
    when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

    assertThrows(RuntimeException.class, () ->
      authController.authenticateUser(loginRequest, request, response)
    );

    verify(auditServiceClientService).audit(
      eq(userId),
      eq("agent3"),
      isNull(),
      eq("LOGIN_FAILURE"),
      eq("USER"),
      eq(userId.toString()),
      eq("FAILURE"),
      eq("172.18.0.11"),
      eq("Mozilla/5.0"),
      isNull()
    );
  }
}
