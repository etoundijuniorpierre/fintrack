// Tests web : verifie l'acces utilisateur et interservice a la presence temps reel.

package com.fintrack.notification.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fintrack.notification.config.TestSecurityConfig;
import com.fintrack.notification.constant.ApiConstants;
import com.fintrack.notification.model.readmodel.PresenceSnapshot;
import com.fintrack.notification.model.readmodel.PresenceState;
import com.fintrack.notification.service.PresenceRegistry;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Valide le contrat HTTP utilise par le frontend et user-service.
@WebMvcTest(PresenceController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class PresenceControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private PresenceRegistry presenceRegistry;

  // Autorise le service interne a lire le registre WebSocket courant.
  @Test
  @WithMockUser(authorities = "ROLE_SYSTEM")
  void getPresence_internalService_returnsOnlineUsers() throws Exception {
    when(presenceRegistry.snapshot()).thenReturn(
      PresenceSnapshot
        .builder()
        .online(Set.of("alice"))
        .states(
          Map.of(
            "alice",
            PresenceState
              .builder()
              .online(true)
              .connectedAt(LocalDateTime.of(2026, 7, 29, 8, 27))
              .build()
          )
        )
        .build()
    );

    mockMvc
      .perform(get(ApiConstants.API_BASE_PATH + "/presence"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.online[0]").value("alice"))
      .andExpect(jsonPath("$.states.alice.online").value(true))
      .andExpect(
        jsonPath("$.states.alice.connectedAt").value("2026-07-29T08:27:00")
      );
  }

  // Refuse la presence a un compte sans permission de consultation utilisateur.
  @Test
  @WithMockUser(authorities = "NOTIFICATION_VIEW_OWN")
  void getPresence_withoutUserViewPermission_returnsForbidden()
    throws Exception {
    mockMvc
      .perform(get(ApiConstants.API_BASE_PATH + "/presence"))
      .andExpect(status().isForbidden());
  }
}
