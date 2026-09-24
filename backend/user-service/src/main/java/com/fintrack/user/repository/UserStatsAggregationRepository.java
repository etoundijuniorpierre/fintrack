// Acces aux donnees : expose les requetes persistantes liees a user stats aggregation.

package com.fintrack.user.repository;

import com.fintrack.user.model.entity.User;
import com.fintrack.user.model.readmodel.UserStatsAggregate;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

// Definit le contrat user stats aggregation attendu par les autres couches.

public interface UserStatsAggregationRepository {
  // Fournit utilisateur statistiques aggregate au cas d usage appelant.
  UserStatsAggregate getUserStatsAggregate(
    Specification<User> baseSpec,
    Set<String> onlineUsernames,
    LocalDateTime recentThreshold,
    int lockThreshold
  );
}
