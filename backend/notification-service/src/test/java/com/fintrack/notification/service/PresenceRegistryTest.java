// Tests unitaires : verifie la presence WebSocket avec plusieurs sessions par utilisateur.

package com.fintrack.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

// Valide les transitions en ligne et hors ligne du registre de sessions.
class PresenceRegistryTest {

  // Maintient l'utilisateur en ligne tant qu'au moins un onglet reste connecte.
  @Test
  void disconnect_multipleSessions_marksOfflineAfterLastSession() {
    PresenceRegistry registry = registryAt("2026-07-29T08:27:00Z");

    assertThat(registry.connect("session-1", "alice")).isTrue();
    assertThat(registry.connect("session-2", "alice")).isFalse();
    assertThat(registry.disconnect("session-1")).isFalse();
    assertThat(registry.online()).containsExactly("alice");

    assertThat(registry.disconnect("session-2")).isTrue();
    assertThat(registry.online()).isEmpty();
    assertThat(
      registry.snapshot().getStates().get("alice").getConnectedAt()
    ).isEqualTo(LocalDateTime.of(2026, 7, 29, 9, 27));
    assertThat(
      registry.snapshot().getStates().get("alice").getDisconnectedAt()
    ).isEqualTo(LocalDateTime.of(2026, 7, 29, 9, 27));
  }

  // Ignore une seconde notification CONNECT pour la meme session.
  @Test
  void connect_duplicateSession_preservesFirstConnectedAt() {
    PresenceRegistry registry = registryAt("2026-07-29T08:30:00Z");

    assertThat(registry.connect("session-1", "alice")).isTrue();
    assertThat(registry.connect("session-1", "alice")).isFalse();

    assertThat(
      registry.snapshot().getStates().get("alice").getConnectedAt()
    ).isEqualTo(LocalDateTime.of(2026, 7, 29, 9, 30));
  }

  // Cree un registre fige dans le fuseau horaire applicatif de production.
  private PresenceRegistry registryAt(String instant) {
    return new PresenceRegistry(
      Clock.fixed(
        Instant.parse(instant),
        ZoneId.of("Africa/Lagos")
      )
    );
  }
}
