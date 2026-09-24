package com.fintrack.reporting.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fintrack.reporting.config.TestSecurityConfig;
import com.fintrack.reporting.model.mapper.ReportMapper;
import com.fintrack.reporting.security.UserDetailsImpl;
import com.fintrack.reporting.service.ReportService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class ReportControllerSecurityTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ReportService reportService;

  @MockitoBean
  private ReportMapper reportMapper;

  private UserDetailsImpl userWith(String... authorities) {
    return new UserDetailsImpl(
      UUID.randomUUID(),
      "report-user",
      null,
      true,
      java.util.Arrays
        .stream(authorities)
        .map(SimpleGrantedAuthority::new)
        .collect(java.util.stream.Collectors.toSet())
    );
  }

  @Test
  void sendEmail_ViewPermissionOnly_IsForbidden() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/reportingService/reports/{id}/send-email", UUID.randomUUID())
          .with(csrf())
          .with(user(userWith("REPORT_VIEW_ALL")))
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"recipients\":[\"audit@example.com\"]}")
      )
      .andExpect(status().isForbidden());
  }

  @Test
  void sendEmail_DedicatedPermission_IsAllowed() throws Exception {
    UUID reportId = UUID.randomUUID();
    UserDetailsImpl currentUser = userWith("REPORT_SEND_EMAIL");

    mockMvc
      .perform(
        post("/api/v1/reportingService/reports/{id}/send-email", reportId)
          .with(csrf())
          .with(user(currentUser))
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"recipients\":[\"audit@example.com\"]}")
      )
      .andExpect(status().isOk());

    verify(reportService).sendEmail(
      eq(reportId),
      eq(List.of("audit@example.com")),
      eq(currentUser)
    );
  }

  @Test
  void deleteReport_ViewPermissionOnly_IsForbidden() throws Exception {
    mockMvc
      .perform(
        delete("/api/v1/reportingService/reports/{id}", UUID.randomUUID())
          .with(csrf())
          .with(user(userWith("REPORT_VIEW_ALL")))
      )
      .andExpect(status().isForbidden());
  }

  @Test
  void deleteReport_DedicatedPermission_IsAllowed() throws Exception {
    UUID reportId = UUID.randomUUID();
    UserDetailsImpl currentUser = userWith("REPORT_DELETE");

    mockMvc
      .perform(
        delete("/api/v1/reportingService/reports/{id}", reportId)
          .with(csrf())
          .with(user(currentUser))
      )
      .andExpect(status().isNoContent());

    verify(reportService).delete(reportId, currentUser);
  }
}
