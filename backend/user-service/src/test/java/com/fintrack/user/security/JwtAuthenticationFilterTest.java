package com.fintrack.user.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fintrack.user.constant.ApiConstants;
import com.fintrack.user.model.entity.User;
import jakarta.servlet.FilterChain;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

class JwtAuthenticationFilterTest {

  private JwtUtils jwtUtils;
  private UserDetailsService userDetailsService;
  private JwtAuthenticationFilter filter;
  private User user;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    jwtUtils = new JwtUtils("01234567890123456789012345678901");
    ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 60_000);
    ReflectionTestUtils.setField(jwtUtils, "refreshExpirationMs", 604_800_000);
    ReflectionTestUtils.setField(jwtUtils, "sessionMaxDurationMs", 43_200_000L);
    userDetailsService = mock(UserDetailsService.class);
    filter = new JwtAuthenticationFilter(jwtUtils, userDetailsService);

    user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername("newuser");
    user.setActive(false);
    user.setFirstLogin(true);
    user.setRoles(Set.of());
    user.setPermissions(Set.of());
    when(userDetailsService.loadUserByUsername("newuser")).thenAnswer(
      ignored -> UserDetailsImpl.build(user)
    );
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void firstLoginToken_authenticatesOnlyOwnPasswordChange() throws Exception {
    String token = jwtUtils.generateFirstLoginToken(
      user,
      System.currentTimeMillis()
    );
    MockHttpServletRequest request = request(
      "POST",
      ApiConstants.Endpoints.USERS +
      "/" +
      user.getId() +
      "/change-password",
      token
    );

    filter.doFilter(
      request,
      new MockHttpServletResponse(),
      mock(FilterChain.class)
    );

    assertThat(SecurityContextHolder.getContext().getAuthentication())
      .isNotNull();
    assertThat(
      SecurityContextHolder
        .getContext()
        .getAuthentication()
        .getAuthorities()
    )
      .extracting("authority")
      .containsExactly("FIRST_LOGIN_PASSWORD_CHANGE");
  }

  @Test
  void firstLoginToken_isRejectedOnOtherApi() throws Exception {
    String token = jwtUtils.generateFirstLoginToken(
      user,
      System.currentTimeMillis()
    );

    filter.doFilter(
      request("GET", ApiConstants.Endpoints.USERS, token),
      new MockHttpServletResponse(),
      mock(FilterChain.class)
    );

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void accessToken_isRejectedWhenFirstLoginAccountIsInactive()
    throws Exception {
    String token = jwtUtils.generateJwtToken(user);

    filter.doFilter(
      request("GET", ApiConstants.Endpoints.USERS, token),
      new MockHttpServletResponse(),
      mock(FilterChain.class)
    );

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  private MockHttpServletRequest request(
    String method,
    String path,
    String token
  ) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, path);
    request.setRequestURI(path);
    request.addHeader("Authorization", "Bearer " + token);
    return request;
  }
}
