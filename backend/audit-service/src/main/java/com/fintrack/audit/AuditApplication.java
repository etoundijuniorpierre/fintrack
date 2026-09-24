// Application Spring Boot : demarre le service audit.

package com.fintrack.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

// Point d'entree du microservice d'audit.
@SpringBootApplication
@EnableFeignClients
public class AuditApplication {

  // Demarre l'application Spring Boot.
  public static void main(String[] args) {
    SpringApplication.run(AuditApplication.class, args);
  }
}
