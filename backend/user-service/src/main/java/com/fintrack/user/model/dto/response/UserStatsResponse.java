// DTO : transporte les donnees liees a user stats entre les couches.

package com.fintrack.user.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse detaillant les statistiques de productivite utilisateur.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsResponse {

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
