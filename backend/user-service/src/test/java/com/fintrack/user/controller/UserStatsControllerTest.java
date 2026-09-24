package com.fintrack.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fintrack.user.config.TestSecurityConfig;
import com.fintrack.user.constant.ApiConstants;
import com.fintrack.user.model.dto.response.UserStatsResponse;
import com.fintrack.user.model.readmodel.UserStats;
import com.fintrack.user.model.mapper.UserMapper;
import com.fintrack.user.security.UserDetailsImpl;
import com.fintrack.user.service.UserStatsService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserStatsController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class UserStatsControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private UserStatsService userStatsService;

  @MockitoBean
  private UserMapper userMapper;

  private UserDetailsImpl principal(String authority) {
    return new UserDetailsImpl(
      UUID.randomUUID(),
      "chef",
      "password",
      true,
      false,
      UUID.randomUUID(),
      UUID.randomUUID(),
      List.of(new SimpleGrantedAuthority(authority))
    );
  }

  @Test
  void getStatsWithoutView_defaultsToAgencyScopeForAgencyViewer()
    throws Exception {
    UserDetailsImpl currentUser = principal("USER_VIEW_AGENCY");
    UserStats stats = UserStats.builder().connectedCount(2).totalCount(5).build();
    UserStatsResponse response = UserStatsResponse.builder()
      .connectedCount(2)
      .totalCount(5)
      .build();

    when(
      userStatsService.getStats(
        isNull(),
        isNull(),
        isNull(),
        same("agency"),
        isNull(),
        isNull(),
        same(currentUser)
      )
    ).thenReturn(stats);
    when(userMapper.userStatsToUserStatsResponse(stats)).thenReturn(response);

    mockMvc
      .perform(get(ApiConstants.Endpoints.USERS + "/stats").with(user(currentUser)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.connectedCount").value(2))
      .andExpect(jsonPath("$.totalCount").value(5));

    verify(userStatsService).getStats(
      isNull(),
      isNull(),
      isNull(),
      same("agency"),
      isNull(),
      isNull(),
      same(currentUser)
    );
  }

  @Test
  void getStatsWithoutView_defaultsToServiceScopeForServiceViewer()
    throws Exception {
    UserDetailsImpl currentUser = principal("USER_VIEW_SERVICE");
    UserStats stats = UserStats.builder().connectedCount(1).totalCount(3).build();
    UserStatsResponse response = UserStatsResponse.builder()
      .connectedCount(1)
      .totalCount(3)
      .build();

    when(
      userStatsService.getStats(
        isNull(),
        isNull(),
        isNull(),
        same("service"),
        isNull(),
        isNull(),
        same(currentUser)
      )
    ).thenReturn(stats);
    when(userMapper.userStatsToUserStatsResponse(stats)).thenReturn(response);

    mockMvc
      .perform(get(ApiConstants.Endpoints.USERS + "/stats").with(user(currentUser)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.connectedCount").value(1))
      .andExpect(jsonPath("$.totalCount").value(3));

    verify(userStatsService).getStats(
      isNull(),
      isNull(),
      isNull(),
      same("service"),
      isNull(),
      isNull(),
      same(currentUser)
    );
  }

  @Test
  void getStatsAllView_isForbiddenForScopedViewer() throws Exception {
    UserDetailsImpl currentUser = principal("USER_VIEW_AGENCY");

    mockMvc
      .perform(
        get(ApiConstants.Endpoints.USERS + "/stats")
          .param("view", "all")
          .with(user(currentUser))
      )
      .andExpect(status().isForbidden());

    verify(userStatsService, never()).getStats(
      any(),
      any(),
      any(),
      any(),
      any(),
      any(),
      any()
    );
  }
}
