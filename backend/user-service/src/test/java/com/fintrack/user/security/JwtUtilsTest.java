package com.fintrack.user.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fintrack.user.model.entity.Agency;
import com.fintrack.user.model.entity.Permission;
import com.fintrack.user.model.entity.Role;
import com.fintrack.user.model.entity.ServiceEntity;
import com.fintrack.user.model.entity.User;
import io.jsonwebtoken.Claims;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilsTest {

  private JwtUtils jwtUtils;

  @BeforeEach
  void setUp() {
    jwtUtils = new JwtUtils("01234567890123456789012345678901");
    ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 60_000);
    ReflectionTestUtils.setField(jwtUtils, "refreshExpirationMs", 604_800_000);
    ReflectionTestUtils.setField(jwtUtils, "sessionMaxDurationMs", 43_200_000L);
  }

  @Test
  @DisplayName(
    "Access token includes agencyId and serviceId when user is attached"
  )
  void generateJwtToken_includesUserScopeClaims() {
    UUID agencyId = UUID.randomUUID();
    UUID serviceId = UUID.randomUUID();
    User user = userWithScope(agencyId, serviceId);

    Claims claims = jwtUtils.getClaimsFromJwtToken(
      jwtUtils.generateJwtToken(user)
    );

    assertThat(claims.get("agencyId", String.class)).isEqualTo(
      agencyId.toString()
    );
    assertThat(claims.get("serviceId", String.class)).isEqualTo(
      serviceId.toString()
    );
  }

  @Test
  @DisplayName(
    "Access token omits scope claims when user has no agency or service"
  )
  void generateJwtToken_omitsScopeClaimsWhenMissing() {
    User user = userWithScope(null, null);

    Claims claims = jwtUtils.getClaimsFromJwtToken(
      jwtUtils.generateJwtToken(user)
    );

    assertThat(claims.get("agencyId", String.class)).isNull();
    assertThat(claims.get("serviceId", String.class)).isNull();
  }

  @Test
  @DisplayName("First-login token contains no role or permission")
  void generateFirstLoginToken_hasNoAuthorities() {
    User user = userWithScope(UUID.randomUUID(), UUID.randomUUID());

    String token = jwtUtils.generateFirstLoginToken(
      user,
      System.currentTimeMillis()
    );
    Claims claims = jwtUtils.getClaimsFromJwtToken(token);

    assertThat(claims.get("type", String.class)).isEqualTo("FIRST_LOGIN");
    assertThat((Collection<?>) claims.get("roles", Collection.class)).isEmpty();
    assertThat(
      (Collection<?>) claims.get("permissions", Collection.class)
    ).isEmpty();
    assertThat(claims.get("agencyId")).isNull();
    assertThat(claims.get("serviceId")).isNull();
    assertThat(jwtUtils.validateJwtToken(token)).isFalse();
    assertThat(
      jwtUtils.validateJwtToken(token, JwtUtils.FIRST_LOGIN_TOKEN_TYPE)
    ).isTrue();
  }

  @Test
  @DisplayName(
    "Refresh token preserves the absolute session start during rotation"
  )
  void generateRefreshToken_preservesAbsoluteSessionStart() {
    User user = userWithScope(null, null);
    long sessionStartedAt = System.currentTimeMillis() - 3_600_000;

    String refreshToken = jwtUtils.generateRefreshToken(user, sessionStartedAt);
    Claims claims = jwtUtils.getClaimsFromJwtToken(refreshToken);

    assertThat(claims.get("sessionStartedAt", Long.class)).isEqualTo(
      sessionStartedAt
    );
    assertThat(claims.getExpiration()).isCloseTo(
      new Date(sessionStartedAt + 43_200_000L),
      1_000
    );
    assertThat(jwtUtils.isRefreshSessionActive(refreshToken)).isTrue();
  }

  @Test
  @DisplayName("Rotated access token cannot exceed the absolute session end")
  void generateJwtToken_capsExpirationAtAbsoluteSessionEnd() {
    User user = userWithScope(null, null);
    long sessionStartedAt = System.currentTimeMillis() - 42_000_000L;
    ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 43_200_000);

    Claims claims = jwtUtils.getClaimsFromJwtToken(
      jwtUtils.generateJwtToken(user, sessionStartedAt)
    );

    assertThat(claims.get("sessionStartedAt", Long.class)).isEqualTo(
      sessionStartedAt
    );
    assertThat(claims.getExpiration()).isCloseTo(
      new Date(sessionStartedAt + 43_200_000L),
      1_000
    );
  }

  @Test
  @DisplayName("Refresh session is rejected after the absolute duration")
  void isRefreshSessionActive_rejectsExpiredAbsoluteSession() {
    User user = userWithScope(null, null);
    ReflectionTestUtils.setField(jwtUtils, "sessionMaxDurationMs", 60_000L);
    String refreshToken = jwtUtils.generateRefreshToken(
      user,
      System.currentTimeMillis() - 120_000
    );

    assertThat(jwtUtils.isRefreshSessionActive(refreshToken)).isFalse();
  }

  @Test
  @DisplayName(
    "Access token permissions = (direct union role) minus revoked"
  )
  void generateJwtToken_subtractsRevokedPermissions() {
    Permission treat = permission("INCIDENT_TREAT");
    Permission viewService = permission("USER_VIEW_SERVICE");

    Role role = new Role();
    role.setId(UUID.randomUUID());
    role.setName("AGENT");
    role.setPermissions(Set.of(treat));

    User user = userWithScope(null, null);
    user.setRoles(Set.of(role));
    user.setPermissions(Set.of(viewService));
    // La permission heritee du role est explicitement revoquee pour cet utilisateur.
    user.setRevokedPermissions(Set.of(treat));

    Claims claims = jwtUtils.getClaimsFromJwtToken(
      jwtUtils.generateJwtToken(user)
    );
    @SuppressWarnings("unchecked")
    List<String> permissions = claims.get("permissions", List.class);

    assertThat(permissions).contains("USER_VIEW_SERVICE");
    assertThat(permissions).doesNotContain("INCIDENT_TREAT");
  }

  private Permission permission(String name) {
    Permission permission = new Permission();
    permission.setId(UUID.randomUUID());
    permission.setName(name);
    return permission;
  }

  private User userWithScope(UUID agencyId, UUID serviceId) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername("agent");
    user.setRoles(Collections.emptySet());
    user.setPermissions(Collections.emptySet());

    if (agencyId != null) {
      Agency agency = new Agency();
      agency.setId(agencyId);
      user.setAgency(agency);
    }
    if (serviceId != null) {
      ServiceEntity service = new ServiceEntity();
      service.setId(serviceId);
      user.setService(service);
    }

    return user;
  }
}
