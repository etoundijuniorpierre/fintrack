// DTO : transporte les donnees liees a user stats client entre les couches.

package com.fintrack.reporting.client.superadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Transporte les donnees liees a utilisateur statistiques client entre services.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsClientResponse {

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
