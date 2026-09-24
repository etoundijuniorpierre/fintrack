// Acces aux donnees : expose les requetes persistantes liees a user stats aggregation repository impl.

package com.fintrack.user.repository;

import com.fintrack.user.model.entity.User;
import com.fintrack.user.model.readmodel.UserStatsAggregate;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

// Fournit les acces persistants du domaine user stats aggregation repository impl.

public class UserStatsAggregationRepositoryImpl
  implements UserStatsAggregationRepository
{

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  // Fournit utilisateur statistiques aggregate au cas d usage appelant.
  public UserStatsAggregate getUserStatsAggregate(
    Specification<User> baseSpec,
    Set<String> onlineUsernames,
    LocalDateTime recentThreshold,
    int lockThreshold
  ) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Tuple> query = cb.createTupleQuery();
    Root<User> root = query.from(User.class);

    // Appliquer la specification sur la requete
    Predicate specPredicate = baseSpec.toPredicate(root, query, cb);
    if (specPredicate != null) {
      query.where(specPredicate);
    }

    // Construire les expressions d'agregation conditionnelle (SUM SELECT CASE)
    Expression<Long> totalCount = cb.count(root);

    Expression<Long> activeCount = cb.sum(
      cb
        .selectCase()
        .when(cb.equal(root.get("isActive"), true), 1L)
        .otherwise(0L)
        .as(Long.class)
    );

    Expression<Long> inactiveCount = cb.sum(
      cb
        .selectCase()
        .when(cb.equal(root.get("isActive"), false), 1L)
        .otherwise(0L)
        .as(Long.class)
    );

    Expression<Long> connectedCount = cb.sum(
      cb
        .selectCase()
        .when(
          cb.and(
            cb.equal(root.get("isActive"), true),
            root.get("username").in(onlineUsernames)
          ),
          1L
        )
        .otherwise(0L)
        .as(Long.class)
    );

    Expression<Long> neverConnectedCount = cb.sum(
      cb
        .selectCase()
        .when(cb.isNull(root.get("lastLogin")), 1L)
        .otherwise(0L)
        .as(Long.class)
    );

    Expression<Long> lockedCount = cb.sum(
      cb
        .selectCase()
        .when(
          cb.greaterThanOrEqualTo(
            root.get("failedLoginAttempts"),
            Math.max(1, lockThreshold)
          ),
          1L
        )
        .otherwise(0L)
        .as(Long.class)
    );

    Expression<Long> firstLoginPendingCount = cb.sum(
      cb
        .selectCase()
        .when(cb.equal(root.get("isFirstLogin"), true), 1L)
        .otherwise(0L)
        .as(Long.class)
    );

    Expression<Long> recentlyActiveCount = cb.sum(
      cb
        .selectCase()
        .when(cb.greaterThan(root.get("lastLogin"), recentThreshold), 1L)
        .otherwise(0L)
        .as(Long.class)
    );

    query.multiselect(
      totalCount,
      activeCount,
      inactiveCount,
      connectedCount,
      neverConnectedCount,
      lockedCount,
      firstLoginPendingCount,
      recentlyActiveCount
    );

    Tuple result = entityManager.createQuery(query).getSingleResult();

    return new UserStatsAggregate(
      getLongValue(result.get(0)),
      getLongValue(result.get(1)),
      getLongValue(result.get(2)),
      getLongValue(result.get(3)),
      getLongValue(result.get(4)),
      getLongValue(result.get(5)),
      getLongValue(result.get(6)),
      getLongValue(result.get(7))
    );
  }

  // Fournit long value a la couche appelante.

  private long getLongValue(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    return 0L;
  }
}
