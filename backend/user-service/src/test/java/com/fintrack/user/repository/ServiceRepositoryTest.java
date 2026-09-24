package com.fintrack.user.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.fintrack.user.model.entity.ServiceEntity;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class ServiceRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private ServiceRepository serviceRepository;

  @Test
  void shouldFindByName() {
    ServiceEntity service = new ServiceEntity();
    service.setName("Human Resources");
    service.setDescription("HR Department");
    entityManager.persist(service);
    entityManager.flush();

    Optional<ServiceEntity> found = serviceRepository.findByName(
      "Human Resources"
    );

    assertTrue(found.isPresent());
    assertEquals("Human Resources", found.get().getName());
  }

  @Test
  void shouldExistsByName() {
    ServiceEntity service = new ServiceEntity();
    service.setName("Accounting");
    entityManager.persist(service);
    entityManager.flush();

    boolean exists = serviceRepository.existsByName("Accounting");

    assertTrue(exists);
  }
}
