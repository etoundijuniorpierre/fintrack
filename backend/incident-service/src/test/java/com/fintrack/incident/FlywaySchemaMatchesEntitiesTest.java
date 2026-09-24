// Tests d'integration : confronte le schema produit par Flyway au modele JPA.

package com.fintrack.incident;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
  properties = {
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration",
    "spring.flyway.baseline-on-migrate=false",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    "spring.sql.init.mode=never",
  }
)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class FlywaySchemaMatchesEntitiesTest {

  @Test
  @DisplayName(
    "Flyway migrations alone satisfy Hibernate schema validation on a clean database"
  )
  void migrationsSatisfyEntityModel() {
  }
}
