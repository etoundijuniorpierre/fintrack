// Application Spring Boot : demarre le service reporting.

package com.fintrack.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

// Point d'entree de l'application reporting-service (rapports et tableaux de bord)
@SpringBootApplication
@EnableFeignClients
@EnableAsync
public class ReportingApplication {

  // Demarre le contexte Spring Boot du service de reporting
  public static void main(String[] args) {
    SpringApplication.run(ReportingApplication.class, args);
  }
}
