// Tests d'integration : verifie l'allocation reelle des references sur PostgreSQL.

package com.fintrack.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.fintrack.incident.repository.IncidentReferenceSequenceRepository;
import com.fintrack.incident.service.IncidentReferenceGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;


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
class IncidentReferenceGeneratorTest {

  @Autowired
  private IncidentReferenceSequenceRepository sequenceRepository;

  @Autowired
  private IncidentReferenceGenerator referenceGenerator;

  @Test
  @Transactional
  @DisplayName("allocateNext returns a strictly increasing number per year")
  void allocateNext_IncrementsPerYear() {
    assertThat(sequenceRepository.allocateNext(2031)).isEqualTo(1L);
    assertThat(sequenceRepository.allocateNext(2031)).isEqualTo(2L);
    assertThat(sequenceRepository.allocateNext(2031)).isEqualTo(3L);

    // Chaque annee possede son propre compteur, remis a zero.
    assertThat(sequenceRepository.allocateNext(2032)).isEqualTo(1L);
    assertThat(sequenceRepository.allocateNext(2031)).isEqualTo(4L);
  }

  @Test
  @DisplayName("Reference is formatted as FT-I-<year>-<padded number>")
  void format_ProducesBusinessReference() {
    assertThat(referenceGenerator.format(2026, 1)).isEqualTo("FT-I-2026-0001");
    assertThat(referenceGenerator.format(2026, 42)).isEqualTo("FT-I-2026-0042");
    assertThat(referenceGenerator.format(2026, 9999))
      .isEqualTo("FT-I-2026-9999");
  }

  @Test
  @DisplayName("Sorting the reference alphabetically follows chronological order")
  void references_SortChronologically() {
    // L'annee precede le numero : l'ordre alphabetique est l'ordre d'attribution.
    assertThat(referenceGenerator.format(2026, 9999))
      .isLessThan(referenceGenerator.format(2027, 1));
    assertThat(referenceGenerator.format(2026, 2))
      .isLessThan(referenceGenerator.format(2026, 10));
  }
}
