// Composant backend : porte la logique liee a dashboard metrics.

package com.fintrack.incident.model.readmodel;

import com.fintrack.incident.model.entity.IncidentHistory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Modele interne des metriques du tableau de bord.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetrics {

  private long activeIncidents;
  private long closedIncidents;
  private long resolvedIncidents;
  private long treatedIncidents;
  private long rejectedIncidents;
  private long cancelledIncidents;
  private long blockedIncidents;
  private long totalIncidents;

  // Rapatriement de IncidentStats
  private long openCount;
  private long pendingCount;
  private long openCountPreviousPeriod;
  private long pendingCountPreviousPeriod;
  private long resolvedCountPreviousPeriod;
  private String period;
  private LocalDateTime effectiveDateFrom;
  private LocalDateTime effectiveDateTo;
  private LocalDateTime generatedAt;

  // Lot 2: KPIs avances (+ effectif = denominateur "base a risque" pour la lisibilite)
  private double transferRate;
  private long transferCount; // numérateur : incidents effectivement transférés
  private long transferDenominator; // incidents pris en charge sur la période
  private double slaComplianceRate;
  private long slaCompliantCount; // numérateur : clôturés dans les délais
  private long slaDenominator; // incidents clôturés sur la période
  private double resolutionReopenRate;
  private long reopenedCount; // numérateur : cycles résolus puis rouverts
  private long reopenDenominator; // incidents résolus sur la période
  private double avgTimeToFirstResponse; // heures, 1re prise en charge (VALIDATED/ASSIGNED/IN_PROGRESS)

  // Lot 3: KPI Directeur (Synthese)
  private long inflow;
  private long outflow;
  private long exitsClosed; // ventilation de outflow : clôtures
  private long exitsRejected; // ventilation de outflow : rejets
  private long exitsCancelled; // ventilation de outflow : annulations
  private long backlogReEntries; // réouvertures depuis un statut terminal
  private long netBacklog;
  private Map<String, Long> ageDistribution; // "0-3", "4-7", "8-30", ">30" (snapshot, hors période)
  private long slaBreachNow; // ouverts en dépassement maintenant (snapshot)
  private Map<String, Long> cohortOutcome; // décomposition cohorte créés: closedOnTime/closedLate/openInTime/openLate/rejected
  private List<NamedIncidentCount> workload; // charge active par personne (snapshot, encadrants)

  // Lot 5-bis: Scorecard
  private EfficiencyScorecard efficiencyScorecard;

  // Convention cohorte : suit les incidents crees sur la periode, censure comprise.
  private CohortCompletion cohortCompletion;

  // Jalon CLOTURE : creation -> closed_at (delai de bout en bout, cloture administrative incluse).
  private double avgClosureHours;
  private double medianClosureHours;
  private double p90ClosureHours;
  private long closureSampleSize;

  // Delai net : le meme jalon, deduction faite du temps ou l'horloge etait arretee
  // (blocage, attente d'actualite) — la meme horloge que le SLA.
  private double avgNetClosureHours;
  private double medianNetClosureHours;

  // Jalon RESOLUTION : creation -> resolved_at (remise en service, hors cloture administrative).
  private double avgResolutionHours;
  private double medianResolutionHours;
  private double p90ResolutionHours;
  private long resolutionSampleSize;
  private long transferredByMe;
  private long closedByMe;
  private long createdByMe;
  private long resolvedByMe;
  private Map<String, Long> distributionByType;
  private Map<String, Long> distributionByCriticality;
  private Map<String, Long> distributionByStatus;
  private List<IncidentHistory> recentActivities;
  private long assignedToMe;
  private List<ServiceIncidentCount> topServices;
  private List<NamedIncidentCount> topAgencies;
  private List<NamedIncidentCount> topResolvers;
  // Serie mensuelle (1..12) month/value, value = nombre ou heures selon la metrique.
  private List<MonthlyMetric> monthlyClosures;
  private List<MonthlyMetric> monthlyAvgClosureHours;

  // Ventilation du delai de cloture : ou passe le temps, pas seulement ou sont les volumes.
  private List<NamedDurationMetric> closureHoursByType;
  private List<NamedDurationMetric> closureHoursByCriticality;
}
