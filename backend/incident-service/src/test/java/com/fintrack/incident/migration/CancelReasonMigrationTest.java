// Tests d'integration : verifie la reprise de donnees de V2_1 sur une vraie base.

package com.fintrack.incident.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * V2_1 deplace le motif des incidents deja annules de reject_reason vers sa colonne
 * propre. C'est la seule partie non reversible du decoupage : les autres tests Flyway
 * ne controlent que la forme (texte non destructeur, schema conforme aux entites), pas
 * l'effet des UPDATE. On rejoue donc la migration pour de vrai, sur des donnees semees
 * a l'etat d'avant.
 *
 * Les proprietes reprennent a l'identique celles des autres tests Testcontainers : le
 * contexte Spring est ainsi mutualise et le conteneur PostgreSQL partage. Demarrer un
 * second conteneur epuisait la machine (la pile applicative tourne a cote) et faisait
 * echouer le chargement de contexte des tests voisins. Le rejeu se fait dans une base
 * soeur creee pour l'occasion, pour ne pas perturber celle deja migree.
 */
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
@Import(com.fintrack.incident.TestcontainersConfiguration.class)
@ActiveProfiles("test")
class CancelReasonMigrationTest {

  private static final String REPLAY_DB = "cancel_reason_replay";
  private static final String VERSION_BEFORE_SPLIT = "2.0";

  @Autowired
  private PostgreSQLContainer postgres;

  @Test
  @DisplayName(
    "V2_1 - Moves a cancelled incident's reason to its own column without losing it"
  )
  void v2_1_MovesCancellationReasonWithoutLoss() throws SQLException {
    DataSource replay = freshReplayDatabase();

    flywayUpTo(replay, VERSION_BEFORE_SPLIT).migrate();

    UUID cancelled = UUID.randomUUID();
    UUID rejected = UUID.randomUUID();
    seedIncident(replay, cancelled, "CANCELLED", "doublon avec FT-I-2026-0007");
    seedIncident(replay, rejected, "REJECTED", "qualification insuffisante");

    flywayUpTo(replay, "2.1").migrate();

    // L'annulation recupere son motif dans la colonne qui la nomme...
    assertThat(readColumn(replay, cancelled, "cancel_reason")).isEqualTo(
      "doublon avec FT-I-2026-0007"
    );
    // ...et ne laisse pas croire a un rejet qui n'a jamais eu lieu.
    assertThat(readColumn(replay, cancelled, "reject_reason")).isNull();

    // Un vrai rejet n'est pas touche : la reprise ne cible que les annulations.
    assertThat(readColumn(replay, rejected, "reject_reason")).isEqualTo(
      "qualification insuffisante"
    );
    assertThat(readColumn(replay, rejected, "cancel_reason")).isNull();
  }

  // Cree (ou recree) une base vierge dans le conteneur partage, et pointe dessus.
  private DataSource freshReplayDatabase() throws SQLException {
    try (
      Connection admin = dataSourceFor(postgres.getDatabaseName()).getConnection();
      Statement statement = admin.createStatement()
    ) {
      statement.execute("DROP DATABASE IF EXISTS " + REPLAY_DB);
      statement.execute("CREATE DATABASE " + REPLAY_DB);
    }
    return dataSourceFor(REPLAY_DB);
  }

  private DataSource dataSourceFor(String database) {
    PGSimpleDataSource ds = new PGSimpleDataSource();
    ds.setServerNames(new String[] { postgres.getHost() });
    ds.setPortNumbers(new int[] { postgres.getFirstMappedPort() });
    ds.setDatabaseName(database);
    ds.setUser(postgres.getUsername());
    ds.setPassword(postgres.getPassword());
    return ds;
  }

  private Flyway flywayUpTo(DataSource dataSource, String targetVersion) {
    return Flyway.configure()
      .dataSource(dataSource)
      .locations("classpath:db/migration")
      .target(targetVersion)
      .load();
  }

  // Insere le minimum de colonnes NOT NULL pour qu'un incident existe.
  private void seedIncident(
    DataSource dataSource,
    UUID id,
    String status,
    String rejectReason
  ) throws SQLException {
    try (
      Connection connection = dataSource.getConnection();
      PreparedStatement statement = connection.prepareStatement(
        """
        INSERT INTO public.incidents
          (id, title, description, type_id, criticality, status,
           created_by, agency_id, reject_reason, reopen_count, created_at)
        VALUES (?, ?, ?, ?, 'MEDIUM', ?, ?, ?, ?, 0, now())
        """
      )
    ) {
      statement.setObject(1, id);
      statement.setString(2, "Incident " + status);
      statement.setString(3, "Description");
      statement.setObject(4, UUID.randomUUID());
      statement.setString(5, status);
      statement.setObject(6, UUID.randomUUID());
      statement.setObject(7, UUID.randomUUID());
      statement.setString(8, rejectReason);
      statement.executeUpdate();
    }
  }

  private String readColumn(DataSource dataSource, UUID id, String column)
    throws SQLException {
    List<String> values = new ArrayList<>();
    try (
      Connection connection = dataSource.getConnection();
      PreparedStatement statement = connection.prepareStatement(
        "SELECT " + column + " FROM public.incidents WHERE id = ?"
      )
    ) {
      statement.setObject(1, id);
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          values.add(rs.getString(1));
        }
      }
    }
    assertThat(values).as("incident %s introuvable", id).hasSize(1);
    return values.get(0);
  }
}
