// Configuration Spring : declare les regles techniques liees a web socket auth interceptor.

package com.fintrack.notification.config;

import com.fintrack.notification.security.JwtUtils;
import io.jsonwebtoken.Claims;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

// Intercepteur STOMP CONNECT : valide le JWT et injecte l'Authentication dans la session WebSocket.

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

  private final JwtUtils jwtUtils;

  // Authentifie les messages WebSocket avant leur diffusion.

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
      message,
      StompHeaderAccessor.class
    );

    if (
      accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())
    ) {
      String authHeader = accessor.getFirstNativeHeader("Authorization");

      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);

        if (jwtUtils.validateJwtToken(token)) {
          String username = jwtUtils.getUserNameFromJwtToken(token);
          Claims claims = jwtUtils.getClaimsFromJwtToken(token);

          @SuppressWarnings("unchecked")
          List<String> permissions = claims.get("permissions", List.class);
          Collection<SimpleGrantedAuthority> authorities =
            permissions == null
              ? List.of()
              : permissions
                  .stream()
                  .map(p -> new SimpleGrantedAuthority(p.toUpperCase()))
                  .collect(Collectors.toList());

          UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
              username,
              null,
              authorities
            );
          accessor.setUser(authentication);

          log.debug(
            "Connexion WebSocket CONNECT authentifiée pour l'utilisateur : {}",
            username
          );
        } else {
          log.warn("Connexion WebSocket CONNECT rejetée : JWT invalide");
        }
      } else {
        log.warn(
          "Connexion WebSocket CONNECT rejetée : en-tête Authorization manquant"
        );
      }
    }

    if (
      accessor != null &&
      StompCommand.SUBSCRIBE.equals(accessor.getCommand()) &&
      PRESENCE_TOPIC.equals(accessor.getDestination()) &&
      !canViewPresence(accessor)
    ) {
      log.warn(
        "Abonnement presence rejete : droits insuffisants pour {}",
        accessor.getUser() != null ? accessor.getUser().getName() : "anonyme"
      );
      return null;
    }

    return message;
  }

  private static final String PRESENCE_TOPIC = "/topic/presence";
  private static final List<String> PRESENCE_AUTHORITIES = List.of(
    "USER_VIEW_ALL",
    "USER_VIEW_AGENCY",
    "USER_VIEW_SERVICE"
  );

  // Verifie que la session porte un droit de consultation des utilisateurs.
  private boolean canViewPresence(StompHeaderAccessor accessor) {
    if (
      accessor.getUser() instanceof
      UsernamePasswordAuthenticationToken authentication
    ) {
      return authentication
        .getAuthorities()
        .stream()
        .anyMatch(granted ->
          PRESENCE_AUTHORITIES.contains(granted.getAuthority())
        );
    }
    return false;
  }
}
