// Contrat metier : expose les operations du domaine utilisateur statistiques.

package com.fintrack.user.service;

import com.fintrack.user.model.constant.PeriodType;
import com.fintrack.user.model.readmodel.UserStats;
import com.fintrack.user.security.UserDetailsImpl;
import java.time.LocalDateTime;
import java.util.UUID;

// Interface du service de statistiques d'activite des utilisateurs.

public interface UserStatsService {
  // Fournit statistiques a la couche appelante.
  UserStats getStats(
    PeriodType period,
    LocalDateTime dateFrom,
    LocalDateTime dateTo,
    String view,
    UUID agencyId,
    UUID serviceId,
    UserDetailsImpl currentUser
  );
}
