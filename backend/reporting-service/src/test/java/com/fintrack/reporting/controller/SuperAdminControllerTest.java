package com.fintrack.reporting.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fintrack.reporting.config.TestSecurityConfig;
import com.fintrack.reporting.model.dto.response.superadmin.*;
import com.fintrack.reporting.model.mapper.superadmin.SuperAdminMapper;
import com.fintrack.reporting.model.readmodel.superadmin.SuperAdminSnapshot;
import com.fintrack.reporting.security.UserDetailsImpl;
import com.fintrack.reporting.service.SuperAdminService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SuperAdminController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class SuperAdminControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private SuperAdminService superAdminService;

  @MockitoBean
  private SuperAdminMapper superAdminMapper;

  @Test
  @DisplayName(
    "GET /api/v1/reportingService/super-admin/overview returns overview for SUPER_ADMIN"
  )
  void getOverview_AsSuperAdmin_ReturnsOverview() throws Exception {
    GovernanceSectionResponse governance = GovernanceSectionResponse.builder()
      .totalIncidents(7)
      .build();

    HealthRowResponse healthRow = HealthRowResponse.builder()
      .key("reporting")
      .status("UP")
      .build();

    PermissionsOverviewResponse permissions =
      PermissionsOverviewResponse.builder()
        .criticalPermissions(List.of("INCIDENT_VIEW_ALL"))
        .build();

    AuditSectionResponse audit = AuditSectionResponse.builder().build();
    DataQualityOverviewResponse dataQuality =
      DataQualityOverviewResponse.builder().issues(List.of()).build();

    ReportingSectionResponse reporting = ReportingSectionResponse.builder()
      .failed(0)
      .build();

    NotificationsOverviewResponse notifications =
      NotificationsOverviewResponse.builder().failed(0).build();

    SuperAdminSnapshot snapshot = SuperAdminSnapshot.builder().build();
    SuperAdminOverviewResponse response = new SuperAdminOverviewResponse(
      governance,
      List.of(healthRow),
      audit,
      permissions,
      SystemThresholdsResponse.builder().build(),
      dataQuality,
      reporting,
      notifications,
      SuperAdminOverviewMetaResponse.builder().generatedAt("now").build()
    );
    when(superAdminService.getOverview()).thenReturn(snapshot);
    when(superAdminMapper.toOverviewResponse(snapshot)).thenReturn(response);

    mockMvc
      .perform(
        get("/api/v1/reportingService/super-admin/overview").with(
          user(principal("ROLE_SUPER_ADMIN"))
        )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.governance.totalIncidents").value(7))
      .andExpect(jsonPath("$.systemHealth[0].status").value("UP"))
      .andExpect(
        jsonPath("$.permissions.criticalPermissions[0]").value(
          "INCIDENT_VIEW_ALL"
        )
      );
  }

  @Test
  @DisplayName(
    "GET /api/v1/reportingService/super-admin/overview rejects non super admin"
  )
  void getOverview_AsAdmin_ReturnsForbidden() throws Exception {
    mockMvc
      .perform(
        get("/api/v1/reportingService/super-admin/overview").with(
          user(principal("ROLE_ADMIN"))
        )
      )
      .andExpect(status().isForbidden());

    verifyNoInteractions(superAdminService, superAdminMapper);
  }

  private UserDetailsImpl principal(String authority) {
    return new UserDetailsImpl(
      UUID.randomUUID(),
      "super.admin",
      null,
      true,
      List.of(new SimpleGrantedAuthority(authority))
    );
  }
}
