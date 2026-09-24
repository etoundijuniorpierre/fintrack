// Application Spring Boot : demarre le service document.

package com.fintrack.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

// Point d'entree du microservice document-service (gestion des pieces jointes).
@SpringBootApplication
@EnableFeignClients
public class DocumentApplication {

  // Demarre l'application Spring Boot.
  public static void main(String[] args) {
    SpringApplication.run(DocumentApplication.class, args);
  }
}
