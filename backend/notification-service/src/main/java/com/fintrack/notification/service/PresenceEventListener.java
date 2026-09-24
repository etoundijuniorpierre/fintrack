// Contrat metier : expose les operations du domaine presence utilisateur.

package com.fintrack.notification.service;

import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

// Suit les connexions WebSocket et diffuse la liste des utilisateurs en ligne.
@Slf4j
@Component
@RequiredArgsConstructor
public class PresenceEventListener {

  private static final String PRESENCE_TOPIC = "/topic/presence";

  private final PresenceRegistry presenceRegistry;
  private final SimpMessagingTemplate messagingTemplate;

  // Enregistre la session authentifiee et diffuse la presence si elle change.
  @EventListener
  public void onConnected(SessionConnectedEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    Principal user = event.getUser();
    if (user == null) return;
    if (presenceRegistry.connect(accessor.getSessionId(), user.getName())) {
      broadcast();
      log.debug("Presence : {} en ligne", user.getName());
    }
  }

  // Libere la session fermee et diffuse la presence si elle change.
  @EventListener
  public void onDisconnected(SessionDisconnectEvent event) {
    if (presenceRegistry.disconnect(event.getSessionId())) {
      broadcast();
      log.debug("Presence : session {} hors ligne", event.getSessionId());
    }
  }

  // Diffuse la liste courante aux abonnes du topic de presence.
  private void broadcast() {
    messagingTemplate.convertAndSend(
      PRESENCE_TOPIC,
      presenceRegistry.snapshot()
    );
  }
}
