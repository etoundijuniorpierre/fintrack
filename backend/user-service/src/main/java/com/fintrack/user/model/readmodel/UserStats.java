// Composant backend : porte la logique liee a user stats.

package com.fintrack.user.model.readmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Modele interne portant les statistiques utilisateur calculees par le service.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStats {

  private long totalCount;
  private long activeCount;
  private long inactiveCount;
  private long connectedCount;
  private long neverConnectedCount;
  private long lockedCount;
  private long firstLoginPendingCount;
  private long recentlyActiveCount;
  private long newUsersInPeriod;
}
