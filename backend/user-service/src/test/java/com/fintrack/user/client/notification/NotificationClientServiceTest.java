// Tests unitaires : verifie l'adaptation des donnees de presence interservices.

package com.fintrack.user.client.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fintrack.user.model.dto.response.PresenceResponse;
import com.fintrack.user.model.mapper.NotificationRequestMapper;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Verifie le contrat de lecture de presence de notification-service.
@ExtendWith(MockitoExtension.class)
class NotificationClientServiceTest {

  @Mock
  private NotificationClient notificationClient;

  @Mock
  private NotificationRequestMapper notificationRequestMapper;

  @InjectMocks
  private NotificationClientService notificationClientService;

  // Retourne une copie stable des utilisateurs actuellement connectes.
  @Test
  void getOnlineUsernames_presenceAvailable_returnsOnlineUsers() {
    when(notificationClient.getPresence()).thenReturn(
      PresenceResponse.builder().online(Set.of("alice", "bob")).build()
    );

    assertThat(notificationClientService.getOnlineUsernames())
      .containsExactlyInAnyOrder("alice", "bob");
  }
}
