package com.fintrack.incident;

import com.fintrack.incident.client.user.UserClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class IncidentApplicationTests {

  @MockitoBean
  UserClient userClient;

  @Test
  @DisplayName("Spring context loads successfully")
  void contextLoads() {}
}
