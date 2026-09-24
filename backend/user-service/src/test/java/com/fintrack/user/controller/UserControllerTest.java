package com.fintrack.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.user.config.TestSecurityConfig;
import com.fintrack.user.constant.ApiConstants;
import com.fintrack.user.model.dto.request.ProfileUpdateRequest;
import com.fintrack.user.model.dto.request.UserRequest;
import com.fintrack.user.model.dto.response.UserResponse;
import com.fintrack.user.model.entity.User;
import com.fintrack.user.model.mapper.UserMapper;
import com.fintrack.user.security.UserAccessGuard;
import com.fintrack.user.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private UserMapper userMapper;

  @MockitoBean(name = "userAccess")
  private UserAccessGuard userAccess;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private UserRequest userRequest;
  private UserResponse userResponse;
  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();

    userRequest = new UserRequest();
    userRequest.setUsername("testuser");
    userRequest.setEmail("test@fintrack.com");
    userRequest.setFirstName("Test");
    userRequest.setLastName("User");

    userResponse = new UserResponse();
    userResponse.setId(userId);
    userResponse.setUsername("testuser");
    userResponse.setEmail("test@fintrack.com");
  }

  @Test
  @WithMockUser(authorities = "USER_VIEW_ALL")
  @DisplayName("Get user by ID - Success")
  void getUserById_userExists_returnsUserResponse() throws Exception {
    User user = new User();
    user.setId(userId);
    when(userService.findById(userId)).thenReturn(user);
    when(userAccess.hasAuthority("USER_VIEW_ALL")).thenReturn(true);
    when(userMapper.userToAdminUserResponse(user)).thenReturn(userResponse);

    mockMvc
      .perform(get(ApiConstants.Endpoints.USERS + "/" + userId))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.username").value("testuser"))
      .andExpect(jsonPath("$.id").value(userId.toString()));

    verify(userService, times(1)).findById(userId);
    verify(userMapper, times(1)).userToAdminUserResponse(user);
  }

  @Test
  @WithMockUser(authorities = "USER_CREATE_ALL_AGENT")
  @DisplayName("Create user - Success")
  void createUser_validRequest_returnsCreatedUserResponse() throws Exception {
    User user = new User();
    when(userMapper.userRequestToUser(any(UserRequest.class))).thenReturn(user);
    when(userService.create(any(User.class))).thenReturn(user);
    when(userMapper.userToAdminUserResponse(any(User.class))).thenReturn(
      userResponse
    );

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.USERS)
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(userRequest))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.username").value("testuser"));

    verify(userMapper, times(1)).userRequestToUser(any(UserRequest.class));
    verify(userService, times(1)).create(any(User.class));
    verify(userMapper, times(1)).userToAdminUserResponse(any(User.class));
  }

  @Test
  @WithMockUser(authorities = "USER_CREATE_ALL_AGENT")
  @DisplayName("Create user - Validation fails")
  void createUser_invalidRequest_returnsBadRequest() throws Exception {
    userRequest.setUsername("");

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.USERS)
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(userRequest))
      )
      .andExpect(status().isBadRequest());

    verify(userMapper, never()).userRequestToUser(any(UserRequest.class));
    verify(userService, never()).create(any(User.class));
  }

  @Test
  @DisplayName("Property 11 - Owner can update own profile (200)")
  void property11_ownerCanUpdateOwnProfile() throws Exception {
    User updatedUser = new User();
    updatedUser.setId(userId);
    ProfileUpdateRequest request = ProfileUpdateRequest.builder()
      .username("newusername")
      .build();
    User profile = new User();

    when(
      userMapper.profileUpdateRequestToUser(any(ProfileUpdateRequest.class))
    ).thenReturn(profile);
    when(userService.updateProfile(eq(userId), any(User.class))).thenReturn(
      updatedUser
    );
    when(userAccess.isSelf(userId)).thenReturn(true);
    when(userMapper.userToUserResponse(updatedUser)).thenReturn(userResponse);

    mockMvc
      .perform(
        patch(ApiConstants.Endpoints.USERS + "/" + userId + "/profile")
          .with(
            user(userId.toString()).authorities(
              new SimpleGrantedAuthority("USER_MANAGE_PROFILE")
            )
          )
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isOk());

    verify(userMapper, times(1)).profileUpdateRequestToUser(
      any(ProfileUpdateRequest.class)
    );
    verify(userService, times(1)).updateProfile(eq(userId), any(User.class));
  }

  @Test
  @DisplayName(
    "Property 11 - Cross-user access without USER_UPDATE is forbidden (403)"
  )
  void property11_crossUserAccessForbidden() throws Exception {
    ProfileUpdateRequest request = ProfileUpdateRequest.builder()
      .username("hacker")
      .build();

    mockMvc
      .perform(
        patch(ApiConstants.Endpoints.USERS + "/" + userId + "/profile")
          .with(user("different-user-id"))
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isForbidden());

    verify(userService, never()).updateProfile(
      any(UUID.class),
      any(User.class)
    );
  }

  @Test
  @WithMockUser(authorities = { "USER_VIEW_ALL" })
  void getDirectionValidators_success() throws Exception {
    User validator = new User();
    validator.setId(UUID.randomUUID());
    validator.setUsername("directeur");

    com.fintrack.user.model.dto.response.UserSummaryResponse summary =
      new com.fintrack.user.model.dto.response.UserSummaryResponse();
    summary.setId(validator.getId());
    summary.setUsername("directeur");
    summary.setFirstName("Jean");
    summary.setLastName("Dupont");

    when(userService.findDirectionValidators()).thenReturn(List.of(validator));
    when(userMapper.userToUserSummaryResponse(validator)).thenReturn(summary);

    mockMvc
      .perform(get(ApiConstants.Endpoints.USERS + "/validators"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].username").value("directeur"));

    verify(userService).findDirectionValidators();
  }

  @Test
  @WithMockUser
  @DisplayName("Change password - Rejects a password shorter than eight characters")
  void changePassword_shortPassword_returnsBadRequest() throws Exception {
    when(userAccess.isSelf(userId)).thenReturn(true);

    mockMvc
      .perform(
        post(
          ApiConstants.Endpoints.USERS +
          "/" +
          userId +
          "/change-password"
        )
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"newPassword\":\"short\"}")
      )
      .andExpect(status().isBadRequest());

    verify(userService, never()).changePassword(any(), any(), any());
  }

  @Test
  @WithMockUser(authorities = { "USER_VIEW_AGENCY", "REPORT_GENERATE" })
  void getAllUsers_reportPermissionDoesNotBypassAgencyScope()
    throws Exception {
    UUID agencyId = UUID.randomUUID();
    User user = new User();
    user.setId(userId);

    when(userAccess.hasAuthority("USER_VIEW_ALL")).thenReturn(false);
    when(userAccess.hasAuthority("USER_VIEW_AGENCY")).thenReturn(true);
    when(userAccess.currentAgencyId()).thenReturn(Optional.of(agencyId));
    when(
      userService.findFilteredByAgencyId(
        eq(agencyId),
        nullable(String.class),
        nullable(String.class),
        nullable(UUID.class),
        nullable(UUID.class),
        nullable(Boolean.class),
        nullable(Boolean.class),
        any(Pageable.class)
      )
    ).thenReturn(new PageImpl<>(List.of(user)));
    when(userMapper.userToScopedUserResponse(user)).thenReturn(userResponse);

    mockMvc
      .perform(get(ApiConstants.Endpoints.USERS))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content[0].username").value("testuser"));

    verify(userService).findFilteredByAgencyId(
      eq(agencyId),
      nullable(String.class),
      nullable(String.class),
      nullable(UUID.class),
      nullable(UUID.class),
      nullable(Boolean.class),
      nullable(Boolean.class),
      any(Pageable.class)
    );
    verify(userService, never()).findFiltered(
      nullable(String.class),
      nullable(String.class),
      nullable(UUID.class),
      nullable(UUID.class),
      nullable(Boolean.class),
      nullable(Boolean.class),
      any(Pageable.class)
    );
  }

  @Test
  @WithMockUser(authorities = { "USER_VIEW_SERVICE", "REPORT_SEND_EMAIL" })
  void getAllUsersList_reportPermissionDoesNotBypassServiceScope()
    throws Exception {
    UUID serviceId = UUID.randomUUID();
    User user = new User();
    user.setId(userId);

    when(userAccess.hasAuthority("USER_VIEW_ALL")).thenReturn(false);
    when(userAccess.hasAuthority("USER_VIEW_AGENCY")).thenReturn(false);
    when(userAccess.hasAuthority("USER_VIEW_SERVICE")).thenReturn(true);
    when(userAccess.currentServiceId()).thenReturn(Optional.of(serviceId));
    when(userService.findByServiceId(serviceId)).thenReturn(List.of(user));
    when(userMapper.userToScopedUserResponse(user)).thenReturn(userResponse);

    mockMvc
      .perform(get(ApiConstants.Endpoints.USERS + "/all"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].username").value("testuser"));

    verify(userService).findByServiceId(serviceId);
    verify(userService, never()).findAll();
  }
}
