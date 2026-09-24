// Composant backend : porte la logique liee a user stats aggregate.

package com.fintrack.user.model.readmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Porte la responsabilite applicative liee a utilisateur statistiques aggregate.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsAggregate {

  private long totalCount;
  private long activeCount;
  private long inactiveCount;
  private long connectedCount;
  private long neverConnectedCount;
  private long lockedCount;
  private long firstLoginPendingCount;
  private long recentlyActiveCount;
}
