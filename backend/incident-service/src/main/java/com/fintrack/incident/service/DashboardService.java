// Contrat metier : expose les operations du domaine tableau de bord.

package com.fintrack.incident.service;

import com.fintrack.incident.model.constant.PeriodType;
import com.fintrack.incident.model.readmodel.DashboardMetrics;
import com.fintrack.incident.model.readmodel.PeriodActivityIncidents;
import com.fintrack.incident.security.UserDetailsImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Service de calcul des metriques du tableau de bord.
public interface DashboardService {
  // Metriques pour la vue demandee, scopees par filtre agence/service.
  // year est utilise pour les series mensuelles (defaut = annee courante).
  DashboardMetrics getMetrics(
    String view,
    UserDetailsImpl currentUser,
    UUID filterAgencyId,
    UUID filterServiceId,
    UUID filterUserId,
    int year,
    PeriodType period,
    LocalDateTime dateFrom,
    LocalDateTime dateTo
  );

  // Incidents adossant les compteurs de flux (traites/resolus/clotures) d'une periode,
  // resolus dans la meme portee et la meme fenetre temporelle que getMetrics.
  PeriodActivityIncidents getPeriodActivity(
    String view,
    UserDetailsImpl currentUser,
    UUID filterAgencyId,
    UUID filterServiceId,
    UUID filterUserId,
    PeriodType period,
    LocalDateTime dateFrom,
    LocalDateTime dateTo
  );

  // Comparaison de plusieurs entites de meme type (USER/SERVICE/AGENCY) sur le meme perimetre temporel.
  List<ComparisonEntry> getComparison(
    String entityType,
    List<UUID> ids,
    UserDetailsImpl currentUser,
    int year,
    PeriodType period,
    LocalDateTime dateFrom,
    LocalDateTime dateTo
  );

  // Porte la responsabilite applicative liee a tableau de bord.

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  class ComparisonEntry {

    private UUID id;
    private String name;
    private DashboardMetrics metrics;
  }
}
