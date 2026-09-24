package com.fintrack.user.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.fintrack.user.model.entity.Agency;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class AgencyRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private AgencyRepository agencyRepository;

  @Test
  void shouldFindByName() {
    Agency agency = new Agency();
    agency.setName("Douala Branch");
    agency.setCode("DLA-01");
    entityManager.persist(agency);
    entityManager.flush();

    Optional<Agency> found = agencyRepository.findByName("Douala Branch");

    assertTrue(found.isPresent());
    assertEquals("DLA-01", found.get().getCode());
  }

  @Test
  void shouldExistsByName() {
    Agency agency = new Agency();
    agency.setName("Yaounde Branch");
    agency.setCode("YDE-01");
    entityManager.persist(agency);
    entityManager.flush();

    boolean exists = agencyRepository.existsByName(" Yaounde Branch".trim());
    assertTrue(exists);
  }
}
