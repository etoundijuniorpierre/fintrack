// Application Spring Boot : demarre le service user.

package com.fintrack.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// Point d'entree du microservice user-service : gestion des utilisateurs, roles, agences et authentification.
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableFeignClients
@EnableAsync
public class UserApplication {

  // Demarre l'application Spring Boot.
  public static void main(String[] args) {
    SpringApplication.run(UserApplication.class, args);
  }
}
