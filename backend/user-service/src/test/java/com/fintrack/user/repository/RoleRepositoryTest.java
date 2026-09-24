package com.fintrack.user.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.fintrack.user.model.entity.Role;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class RoleRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private RoleRepository roleRepository;

  @Test
  void shouldFindByName() {
    Role role = new Role();
    role.setName("ROLE_MANAGER");
    entityManager.persist(role);
    entityManager.flush();

    Optional<Role> found = roleRepository.findByName("ROLE_MANAGER");

    assertTrue(found.isPresent());
    assertEquals("ROLE_MANAGER", found.get().getName());
  }

  @Test
  void shouldExistsByName() {
    Role role = new Role();
    role.setName("ROLE_AUDITOR");
    entityManager.persist(role);
    entityManager.flush();

    boolean exists = roleRepository.existsByName("ROLE_AUDITOR");

    assertTrue(exists);
  }
}
