// Contrat metier : expose les operations du domaine presence utilisateur.

package com.fintrack.notification.service;

import com.fintrack.notification.model.readmodel.PresenceSnapshot;
import com.fintrack.notification.model.readmodel.PresenceState;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.stereotype.Service;

// Registre en memoire des utilisateurs disposant d'une session WebSocket active.
@Service
public class PresenceRegistry {

  private final Map<String, String> usernameBySession = new HashMap<>();
  private final Map<String, Set<String>> sessionsByUsername = new HashMap<>();
  private final Map<String, PresenceState> stateByUsername = new HashMap<>();
  private final Clock clock;

  // Utilise le fuseau horaire configure pour le processus Java.
  public PresenceRegistry() {
    this(Clock.systemDefaultZone());
  }

  // Permet aux tests de rendre les horodatages deterministes.
  PresenceRegistry(Clock clock) {
    this.clock = clock;
  }

  // Enregistre une session et indique si l'utilisateur vient de passer en ligne.
  public synchronized boolean connect(String sessionId, String username) {
    if (sessionId == null || username == null) return false;

    String existingUsername = usernameBySession.get(sessionId);
    if (existingUsername != null) return false;

    usernameBySession.put(sessionId, username);
    Set<String> sessions = sessionsByUsername.computeIfAbsent(
      username,
      ignored -> new HashSet<>()
    );
    boolean wasOnline = !sessions.isEmpty();
    sessions.add(sessionId);

    if (!wasOnline) {
      PresenceState previous = stateByUsername.get(username);
      stateByUsername.put(
        username,
        PresenceState
          .builder()
          .online(true)
          .connectedAt(LocalDateTime.now(clock))
          .disconnectedAt(
            previous != null ? previous.getDisconnectedAt() : null
          )
          .build()
      );
    }
    return !wasOnline;
  }

  // Retire une session et indique si l'utilisateur vient de passer hors ligne.
  public synchronized boolean disconnect(String sessionId) {
    if (sessionId == null) return false;
    String username = usernameBySession.remove(sessionId);
    if (username == null) return false;

    Set<String> sessions = sessionsByUsername.get(username);
    if (sessions == null) return false;
    sessions.remove(sessionId);
    if (!sessions.isEmpty()) return false;

    sessionsByUsername.remove(username);
    PresenceState previous = stateByUsername.get(username);
    stateByUsername.put(
      username,
      PresenceState
        .builder()
        .online(false)
        .connectedAt(previous != null ? previous.getConnectedAt() : null)
        .disconnectedAt(LocalDateTime.now(clock))
        .build()
    );
    return true;
  }

  // Liste triee des utilisateurs actuellement en ligne.
  public synchronized Set<String> online() {
    return new TreeSet<>(sessionsByUsername.keySet());
  }

  // Retourne atomiquement la presence et les derniers changements connus.
  public synchronized PresenceSnapshot snapshot() {
    Map<String, PresenceState> states = new TreeMap<>();
    stateByUsername.forEach((username, state) ->
      states.put(
        username,
        PresenceState
          .builder()
          .online(state.isOnline())
          .connectedAt(state.getConnectedAt())
          .disconnectedAt(state.getDisconnectedAt())
          .build()
      )
    );
    return PresenceSnapshot
      .builder()
      .online(new TreeSet<>(sessionsByUsername.keySet()))
      .states(states)
      .build();
  }
}
