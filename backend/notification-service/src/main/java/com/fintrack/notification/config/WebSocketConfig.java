// Configuration Spring : declare les regles techniques liees a web socket.

package com.fintrack.notification.config;

import com.fintrack.notification.constant.ApiConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// Configuration WebSocket STOMP/SockJS pour les notifications temps reel.

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Value("${fintrack.allowed-origins}")
  private String allowedOrigins;

  private final WebSocketAuthInterceptor webSocketAuthInterceptor;

  // Declare le comportement technique attendu par l'infrastructure.

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    // Battements actifs des deux cotes : les intermediaires (Nginx) ne coupent
    // plus les connexions inactives et les clients detectent les liens morts.
    // Lecture tolerante a 60s : les navigateurs limitent les timers des onglets
    // en arriere-plan, un client inactif ne doit pas passer hors ligne a tort.
    config
      .enableSimpleBroker("/topic")
      .setHeartbeatValue(new long[] { 10000, 60000 })
      .setTaskScheduler(webSocketHeartbeatScheduler());
    config.setApplicationDestinationPrefixes("/app");
  }

  // Ordonnanceur dedie aux battements STOMP du broker simple.
  @Bean
  public ThreadPoolTaskScheduler webSocketHeartbeatScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(1);
    scheduler.setThreadNamePrefix("ws-heartbeat-");
    scheduler.initialize();
    return scheduler;
  }

  // Declare les points d'entree STOMP du WebSocket.

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
      .addEndpoint(ApiConstants.API_BASE_PATH + "/ws")
      .setAllowedOrigins(allowedOrigins.split(","))
      .withSockJS();
  }

  // Declare le comportement technique attendu par l'infrastructure.

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(webSocketAuthInterceptor);
  }
}
