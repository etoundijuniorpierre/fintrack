package com.fintrack.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.user.constant.ApiConstants;
import com.fintrack.user.model.dto.request.RoleRequest;
import com.fintrack.user.model.dto.response.RoleResponse;
import com.fintrack.user.model.entity.Role;
import com.fintrack.user.model.mapper.RoleMapper;
import com.fintrack.user.security.JwtUtils;
import com.fintrack.user.service.RoleService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.jackson.JacksonEndpointAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RoleController.class)
@Import(JacksonEndpointAutoConfiguration.class)
class RoleControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private RoleService roleService;

  @MockitoBean
  private RoleMapper roleMapper;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @MockitoBean
  private JwtUtils jwtUtils;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
  }

  @Test
  @WithMockUser(authorities = "USER_VIEW_ALL")
  void shouldGetRoleById() throws Exception {
    UUID roleId = UUID.randomUUID();
    Role role = new Role();
    role.setId(roleId);
    role.setName("ROLE_ADMIN");

    RoleResponse roleResponse = new RoleResponse();
    roleResponse.setId(roleId);
    roleResponse.setName("ROLE_ADMIN");

    when(roleService.findById(roleId)).thenReturn(role);
    when(roleMapper.roleToRoleResponse(role)).thenReturn(roleResponse);

    mockMvc
      .perform(get(ApiConstants.Endpoints.ROLES + "/" + roleId))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value("ROLE_ADMIN"));
  }

  @Test
  @WithMockUser(authorities = "ROLE_CREATE")
  void shouldCreateRole() throws Exception {
    RoleRequest request = new RoleRequest();
    request.setName("ROLE_SUPERVISOR");

    Role role = new Role();
    RoleResponse response = new RoleResponse();
    response.setName("ROLE_SUPERVISOR");

    when(roleMapper.roleRequestToRole(any(RoleRequest.class))).thenReturn(role);
    when(roleService.create(any(Role.class))).thenReturn(role);
    when(roleMapper.roleToRoleResponse(any(Role.class))).thenReturn(response);

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.ROLES)
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.name").value("ROLE_SUPERVISOR"));
  }
}
