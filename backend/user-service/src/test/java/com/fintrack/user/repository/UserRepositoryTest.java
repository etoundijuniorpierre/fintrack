package com.fintrack.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fintrack.user.model.entity.Permission;
import com.fintrack.user.model.entity.Role;
import com.fintrack.user.model.entity.ServiceEntity;
import com.fintrack.user.model.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TestEntityManager entityManager;

  @Test
  @DisplayName("Find user by username - Success")
  void findByUsername_userExists_returnsUser() {
    Role role = new Role();
    role.setName("ROLE_USER");
    entityManager.persist(role);

    User user = new User();
    user.setUsername("john_doe");
    user.setEmail("john@fintrack.com");
    user.setPassword("password");
    user.setRoles(Set.of(role));
    entityManager.persist(user);
    entityManager.flush();

    Optional<User> found = userRepository.findByUsername("john_doe");

    assertThat(found).isPresent();
    assertThat(found.get().getUsername()).isEqualTo("john_doe");
  }

  @Test
  @DisplayName("Authentication lookup ignores case and preserves internal spaces")
  void findAuthInfoByUsername_ignoresCaseWithInternalSpaces() {
    Role role = new Role();
    role.setName("ROLE_AUTH_CASE_TEST");
    entityManager.persist(role);

    User user = new User();
    user.setUsername("Jean Pierre");
    user.setEmail("jean.pierre@fintrack.com");
    user.setPassword("password");
    user.setRoles(Set.of(role));
    entityManager.persist(user);
    entityManager.flush();

    var found = userRepository.findAuthInfoByUsername("jean pierre");

    assertThat(found).isPresent();
    assertThat(found.get().getUsername()).isEqualTo("Jean Pierre");
  }

  @Test
  @DisplayName("Failed-attempt increment is atomic and disables at configured threshold")
  void incrementFailedLoginAttempts_incrementsAndDisablesAtThreshold() {
    Role role = new Role();
    role.setName("ROLE_LOCK_TEST");
    entityManager.persist(role);

    User user = new User();
    user.setUsername("locked_user");
    user.setEmail("locked@fintrack.com");
    user.setPassword("password");
    user.setActive(true);
    user.setFailedLoginAttempts(4);
    user.setRoles(Set.of(role));
    entityManager.persist(user);
    entityManager.flush();

    int updated = userRepository.incrementFailedLoginAttempts(
      user.getId(),
      5
    );
    User reloaded = userRepository.findById(user.getId()).orElseThrow();

    assertThat(updated).isEqualTo(1);
    assertThat(reloaded.getFailedLoginAttempts()).isEqualTo(5);
    assertThat(reloaded.isActive()).isFalse();
  }

  @Test
  @DisplayName("Check if username exists - True")
  void existsByUsername_userExists_returnsTrue() {
    Role role = new Role();
    role.setName("ROLE_USER");
    entityManager.persist(role);

    User user = new User();
    user.setUsername("jane_doe");
    user.setEmail("jane@fintrack.com");
    user.setPassword("password");
    user.setRoles(Set.of(role));
    entityManager.persist(user);
    entityManager.flush();

    boolean exists = userRepository.existsByUsername("jane_doe");

    assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("Check if username exists - False")
  void existsByUsername_userDoesNotExist_returnsFalse() {
    boolean exists = userRepository.existsByUsername("non_existent");

    assertThat(exists).isFalse();
  }

  @Test
  @DisplayName("Filter users by current WebSocket presence")
  void findFiltered_presenceState_returnsMatchingUsers() {
    Role role = new Role();
    role.setName("ROLE_PRESENCE_TEST");
    entityManager.persist(role);

    User onlineUser = new User();
    onlineUser.setUsername("online_user");
    onlineUser.setEmail("online@fintrack.com");
    onlineUser.setPassword("password");
    onlineUser.setRoles(Set.of(role));
    entityManager.persist(onlineUser);

    User offlineUser = new User();
    offlineUser.setUsername("offline_user");
    offlineUser.setEmail("offline@fintrack.com");
    offlineUser.setPassword("password");
    offlineUser.setRoles(Set.of(role));
    entityManager.persist(offlineUser);
    entityManager.flush();

    var pageable = PageRequest.of(0, 10);
    var onlinePage = userRepository.findFiltered(
      null,
      null,
      null,
      null,
      null,
      true,
      Set.of("online_user"),
      pageable
    );
    var offlinePage = userRepository.findFiltered(
      null,
      null,
      null,
      null,
      null,
      false,
      Set.of("online_user"),
      pageable
    );

    assertThat(onlinePage.getContent())
      .extracting(User::getUsername)
      .containsExactly("online_user");
    assertThat(offlinePage.getContent())
      .extracting(User::getUsername)
      .containsExactly("offline_user");
  }

  // Persiste un service actif reutilisable par les tests d'assignation.
  private ServiceEntity persistService(String name) {
    ServiceEntity service = new ServiceEntity();
    service.setName(name);
    service.setActive(true);
    entityManager.persist(service);
    return service;
  }

  // Persiste une permission par son nom canonique.
  private Permission persistPermission(String name) {
    Permission permission = new Permission();
    permission.setName(name);
    entityManager.persist(permission);
    return permission;
  }

  @Test
  @DisplayName(
    "Assignable users - Includes a service agent whose treat permission comes from a custom role"
  )
  void findAssignableUsers_treatPermissionViaCustomRole_returnsAgent() {
    ServiceEntity service = persistService("Recouvrement");
    Permission treat = persistPermission("INCIDENT_TREAT");

    Role customRole = new Role();
    customRole.setName("ROLE_RECOUVREMENT_AGENT");
    customRole.setPermissions(Set.of(treat));
    entityManager.persist(customRole);

    // Agent sans permission directe : sa capacite de traitement vient du role.
    User agent = new User();
    agent.setUsername("agent_recouvrement");
    agent.setEmail("agent@fintrack.com");
    agent.setPassword("password");
    agent.setActive(true);
    agent.setService(service);
    agent.setRoles(Set.of(customRole));
    entityManager.persist(agent);
    entityManager.flush();
    entityManager.clear();

    List<User> assignables = userRepository.findAssignableUsers(service.getId());

    assertThat(assignables)
      .extracting(User::getUsername)
      .containsExactly("agent_recouvrement");
  }

  @Test
  @DisplayName(
    "Assignable users - Excludes an agent without any treat/resolve permission"
  )
  void findAssignableUsers_noTreatPermission_excludesAgent() {
    ServiceEntity service = persistService("Comptabilite");
    Permission viewOnly = persistPermission("INCIDENT_VIEW_SERVICE");

    Role role = new Role();
    role.setName("ROLE_VIEW_ONLY");
    role.setPermissions(Set.of(viewOnly));
    entityManager.persist(role);

    User agent = new User();
    agent.setUsername("agent_lecture");
    agent.setEmail("lecture@fintrack.com");
    agent.setPassword("password");
    agent.setActive(true);
    agent.setService(service);
    agent.setRoles(Set.of(role));
    entityManager.persist(agent);
    entityManager.flush();
    entityManager.clear();

    List<User> assignables = userRepository.findAssignableUsers(service.getId());

    assertThat(assignables).isEmpty();
  }

  @Test
  @DisplayName(
    "Assignable users - Inactive agent is excluded even with treat permission"
  )
  void findAssignableUsers_inactiveAgent_excluded() {
    ServiceEntity service = persistService("Support");
    Permission treat = persistPermission("INCIDENT_TREAT");

    Role role = new Role();
    role.setName("ROLE_SUPPORT_AGENT");
    role.setPermissions(Set.of(treat));
    entityManager.persist(role);

    User agent = new User();
    agent.setUsername("agent_inactif");
    agent.setEmail("inactif@fintrack.com");
    agent.setPassword("password");
    agent.setActive(false);
    agent.setService(service);
    agent.setRoles(Set.of(role));
    entityManager.persist(agent);
    entityManager.flush();
    entityManager.clear();

    List<User> assignables = userRepository.findAssignableUsers(service.getId());

    assertThat(assignables).isEmpty();
  }

  @Test
  @DisplayName("Direction validators - Returns users with VALIDATION_DIRECTION permission")
  void findDirectionValidators_returnsOnlyUsersWithValidationDirectionPermission() {
    Permission valDirection = persistPermission("VALIDATION_DIRECTION");

    Role dirRole = new Role();
    dirRole.setName("ROLE_DIRECTEUR");
    entityManager.persist(dirRole);

    Role agentRole = new Role();
    agentRole.setName("ROLE_AGENT");
    entityManager.persist(agentRole);

    User validator = new User();
    validator.setUsername("directeur_valideur");
    validator.setEmail("directeur@fintrack.com");
    validator.setPassword("password");
    validator.setActive(true);
    validator.setPermissions(Set.of(valDirection));
    validator.setRoles(Set.of(dirRole));
    entityManager.persist(validator);

    User regularUser = new User();
    regularUser.setUsername("agent_normal");
    regularUser.setEmail("agent@fintrack.com");
    regularUser.setPassword("password");
    regularUser.setActive(true);
    regularUser.setRoles(Set.of(agentRole));
    entityManager.persist(regularUser);

    entityManager.flush();
    entityManager.clear();

    List<User> validators = userRepository.findDirectionValidators();

    assertThat(validators)
      .extracting(User::getUsername)
      .containsExactly("directeur_valideur");
  }
}
