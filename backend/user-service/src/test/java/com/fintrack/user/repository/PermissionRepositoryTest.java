package com.fintrack.user.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.fintrack.user.model.entity.Permission;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class PermissionRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private PermissionRepository permissionRepository;

  @Test
  void shouldFindByName() {
    Permission permission = new Permission();
    permission.setName("USER_DELETE");
    entityManager.persist(permission);
    entityManager.flush();

    Optional<Permission> found = permissionRepository.findByName("USER_DELETE");

    assertTrue(found.isPresent());
    assertEquals("USER_DELETE", found.get().getName());
  }

  @Test
  void shouldExistsByName() {
    Permission permission = new Permission();
    permission.setName("AGENCY_WRITE");
    entityManager.persist(permission);
    entityManager.flush();

    boolean exists = permissionRepository.existsByName("AGENCY_WRITE");

    assertTrue(exists);
  }
}
