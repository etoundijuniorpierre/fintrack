package com.fintrack.user.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fintrack.user.constant.ApiConstants;
import com.fintrack.user.model.dto.response.PermissionResponse;
import com.fintrack.user.model.entity.Permission;
import com.fintrack.user.model.mapper.PermissionMapper;
import com.fintrack.user.security.JwtUtils;
import com.fintrack.user.service.PermissionService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.jackson.JacksonEndpointAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PermissionController.class)
@Import(JacksonEndpointAutoConfiguration.class)
class PermissionControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private PermissionService permissionService;

  @MockitoBean
  private PermissionMapper permissionMapper;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @MockitoBean
  private JwtUtils jwtUtils;

  @Test
  @WithMockUser(authorities = "USER_VIEW_ALL")
  void shouldGetPermissionById() throws Exception {
    UUID permissionId = UUID.randomUUID();
    Permission permission = new Permission();
    permission.setId(permissionId);
    permission.setName("USER_READ");

    PermissionResponse response = new PermissionResponse();
    response.setId(permissionId);
    response.setName("USER_READ");

    when(permissionService.findById(permissionId)).thenReturn(permission);
    when(
      permissionMapper.permissionToPermissionResponse(permission)
    ).thenReturn(response);

    mockMvc
      .perform(get(ApiConstants.Endpoints.PERMISSIONS + "/" + permissionId))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value("USER_READ"));
  }
}
