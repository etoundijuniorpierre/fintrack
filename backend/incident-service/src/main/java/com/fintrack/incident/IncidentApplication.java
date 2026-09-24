// Application Spring Boot : demarre le service incident.

package com.fintrack.incident;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

// Point d'entree du microservice de gestion des incidents (clients Feign, securite par methode, execution asynchrone et planification d'execution actives)
@SpringBootApplication
@EnableFeignClients
@EnableMethodSecurity
@EnableAsync
@EnableScheduling
public class IncidentApplication {

  // Demarre ou initialise le traitement applicatif attendu.

  public static void main(String[] args) {
    SpringApplication.run(IncidentApplication.class, args);
  }
}
