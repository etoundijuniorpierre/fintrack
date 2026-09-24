// Application Spring Boot : demarre le service notification.

package com.fintrack.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// Classe principale de demarrage du microservice de notification.

@SpringBootApplication
@EnableFeignClients
@EnableAsync
@EnableScheduling
public class NotificationApplication {

  // Demarre ou initialise le traitement applicatif attendu.

  public static void main(String[] args) {
    SpringApplication.run(NotificationApplication.class, args);
  }
}
