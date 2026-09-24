package com.fintrack.incident;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
// Conteneur PostgreSQL partage par tous les tests d'integration : les classes qui
// l'importent avec les memes proprietes se partagent le contexte Spring, donc le meme
// conteneur. En demarrer un second sature la machine quand la pile applicative tourne.
public class TestcontainersConfiguration {

  @Bean
  @ServiceConnection
  public PostgreSQLContainer postgresContainer() {
    return new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));
  }
}
