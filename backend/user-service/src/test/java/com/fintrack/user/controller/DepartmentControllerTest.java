package com.fintrack.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.user.config.TestSecurityConfig;
import com.fintrack.user.constant.ApiConstants;
import com.fintrack.user.model.dto.request.ServiceRequest;
import com.fintrack.user.model.dto.response.ServiceResponse;
import com.fintrack.user.model.entity.ServiceEntity;
import com.fintrack.user.model.mapper.ServiceMapper;
import com.fintrack.user.service.DepartmentService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DepartmentController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class DepartmentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private DepartmentService departmentService;

  @MockitoBean
  private ServiceMapper serviceMapper;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @WithMockUser(authorities = "USER_VIEW_ALL")
  @DisplayName("Get department by ID - Success")
  void getDepartmentById_departmentExists_returnsServiceResponse()
    throws Exception {
    UUID serviceId = UUID.randomUUID();
    ServiceEntity service = new ServiceEntity();
    service.setId(serviceId);
    service.setName("IT");

    ServiceResponse response = new ServiceResponse();
    response.setId(serviceId);
    response.setName("IT");

    when(departmentService.findById(serviceId)).thenReturn(service);
    when(serviceMapper.serviceToServiceResponse(service)).thenReturn(response);

    mockMvc
      .perform(get(ApiConstants.Endpoints.DEPARTMENTS + "/" + serviceId))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value("IT"));
  }

  @Test
  @WithMockUser(authorities = "SETTINGS_SYSTEM")
  @DisplayName("Create department - Success")
  void createDepartment_validRequest_returnsCreatedServiceResponse()
    throws Exception {
    ServiceRequest request = new ServiceRequest();
    request.setName("Legal");

    ServiceEntity service = new ServiceEntity();
    ServiceResponse response = new ServiceResponse();
    response.setName("Legal");

    when(
      serviceMapper.serviceRequestToService(any(ServiceRequest.class))
    ).thenReturn(service);
    when(departmentService.create(any(ServiceEntity.class))).thenReturn(
      service
    );
    when(
      serviceMapper.serviceToServiceResponse(any(ServiceEntity.class))
    ).thenReturn(response);

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.DEPARTMENTS)
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.name").value("Legal"));
  }
}
