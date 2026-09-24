package com.fintrack.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fintrack.user.model.entity.Agency;
import com.fintrack.user.model.entity.Role;
import com.fintrack.user.model.entity.User;
import com.fintrack.user.model.readmodel.UserStatsAggregate;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

// Exécute la vraie requête Criteria (sum(case when...) + count en une passe) sur H2,
// hors mock : valide le SQL généré, le scope dynamique et le SUM NULL sur table vide.
@DataJpaTest
@ActiveProfiles("test")
class UserStatsAggregationRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TestEntityManager entityManager;

  private final LocalDateTime now = LocalDateTime.now();
  private Role role;

  @BeforeEach
  void setUp() {
    role = new Role();
    role.setName("ROLE_USER");
    entityManager.persist(role);
  }

  private User newUser(
    String username,
    boolean active,
    LocalDateTime lastLogin,
    int failedAttempts,
    boolean firstLogin
  ) {
    User user = new User();
    user.setUsername(username);
    user.setEmail(username + "@fintrack.com");
    user.setPassword("password");
    user.setActive(active);
    user.setLastLogin(lastLogin);
    user.setFailedLoginAttempts(failedAttempts);
    user.setFirstLogin(firstLogin);
    user.setRoles(Set.of(role));
    return user;
  }

  @Test
  @DisplayName(
    "getUserStatsAggregate compte chaque dimension en une seule requête (vue all)"
  )
  void aggregate_countsEachDimension() {
    entityManager.persist(newUser("u1", true, now.minusMinutes(5), 0, false)); // connected + recentlyActive
    entityManager.persist(newUser("u2", true, now.minusHours(2), 0, false)); // recentlyActive seulement
    entityManager.persist(newUser("u3", false, now.minusHours(48), 6, false)); // locked
    entityManager.persist(newUser("u4", true, null, 0, true)); // neverConnected + firstLoginPending
    entityManager.persist(newUser("u5", false, null, 5, true)); // neverConnected + locked + firstLoginPending
    entityManager.persist(newUser("u6", false, now.minusMinutes(5), 0, false)); // recent login but inactive
    entityManager.flush();

    Specification<User> matchAll = (root, query, cb) -> cb.conjunction();
    UserStatsAggregate agg = userRepository.getUserStatsAggregate(
      matchAll,
      Set.of("u1"),
      now.minusHours(24),
      5
    );

    assertThat(agg.getTotalCount()).isEqualTo(6);
    assertThat(agg.getActiveCount()).isEqualTo(3);
    assertThat(agg.getInactiveCount()).isEqualTo(3);
    assertThat(agg.getConnectedCount()).isEqualTo(1);
    assertThat(agg.getNeverConnectedCount()).isEqualTo(2);
    assertThat(agg.getLockedCount()).isEqualTo(2);
    assertThat(agg.getFirstLoginPendingCount()).isEqualTo(2);
    assertThat(agg.getRecentlyActiveCount()).isEqualTo(3);
  }

  @Test
  @DisplayName(
    "getUserStatsAggregate respecte le scope dynamique (filtre agence, jointure réelle)"
  )
  void aggregate_respectsDynamicScope() {
    Agency agency = new Agency();
    agency.setName("Douala Branch");
    agency.setCode("DLA-01");
    entityManager.persist(agency);

    User inScope1 = newUser("a1", true, now.minusMinutes(5), 0, false);
    inScope1.setAgency(agency);
    User inScope2 = newUser("a2", false, null, 7, true);
    inScope2.setAgency(agency);
    User outOfScope = newUser("b1", true, now.minusMinutes(5), 0, false); // sans agence
    entityManager.persist(inScope1);
    entityManager.persist(inScope2);
    entityManager.persist(outOfScope);
    entityManager.flush();

    Specification<User> agencyScope = (root, query, cb) ->
      cb.equal(root.get("agency").get("id"), agency.getId());
    UserStatsAggregate agg = userRepository.getUserStatsAggregate(
      agencyScope,
      Set.of("a1", "b1"),
      now.minusHours(24),
      5
    );

    assertThat(agg.getTotalCount()).isEqualTo(2);
    assertThat(agg.getActiveCount()).isEqualTo(1);
    assertThat(agg.getInactiveCount()).isEqualTo(1);
    assertThat(agg.getConnectedCount()).isEqualTo(1);
    assertThat(agg.getLockedCount()).isEqualTo(1);
    assertThat(agg.getFirstLoginPendingCount()).isEqualTo(1);
  }

  @Test
  @DisplayName(
    "getUserStatsAggregate sur table vide renvoie des zéros (SUM NULL géré, pas de NPE)"
  )
  void aggregate_emptyTable_returnsZeros() {
    Specification<User> matchAll = (root, query, cb) -> cb.conjunction();
    UserStatsAggregate agg = userRepository.getUserStatsAggregate(
      matchAll,
      Set.of("__no_online_user__"),
      now.minusHours(24),
      5
    );

    assertThat(agg.getTotalCount()).isZero();
    assertThat(agg.getActiveCount()).isZero();
    assertThat(agg.getInactiveCount()).isZero();
    assertThat(agg.getLockedCount()).isZero();
  }

  @Test
  @DisplayName("getUserStatsAggregate utilise le seuil de verrouillage parametre")
  void aggregate_usesConfiguredLockThreshold() {
    entityManager.persist(newUser("below", true, now, 6, false));
    entityManager.persist(newUser("locked", false, now, 7, false));
    entityManager.flush();

    Specification<User> matchAll = (root, query, cb) -> cb.conjunction();
    UserStatsAggregate agg = userRepository.getUserStatsAggregate(
      matchAll,
      Set.of("__no_online_user__"),
      now.minusHours(24),
      7
    );

    assertThat(agg.getLockedCount()).isEqualTo(1);
  }
}
