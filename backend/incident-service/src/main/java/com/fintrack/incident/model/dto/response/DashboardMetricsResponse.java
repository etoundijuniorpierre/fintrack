// DTO : transporte les donnees liees a dashboard metrics entre les couches.

package com.fintrack.incident.model.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse contenant les metriques du tableau de bord.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsResponse {

  private long activeIncidents;
  private long closedIncidents;
  private long resolvedIncidents;
  private long treatedIncidents;
  private long rejectedIncidents;
  private long cancelledIncidents;
  private long blockedIncidents;
  private long totalIncidents;

  private long openCount;
  private long pendingCount;
  private long openCountPreviousPeriod;
  private long pendingCountPreviousPeriod;
  private long resolvedCountPreviousPeriod;
  private String period;
  private LocalDateTime effectiveDateFrom;
  private LocalDateTime effectiveDateTo;
  private LocalDateTime generatedAt;

  // Lot 2 KPIs (+ effectif/denominateur)
  private double transferRate;
  private long transferCount;
  private long transferDenominator;
  private double slaComplianceRate;
  private long slaCompliantCount;
  private long slaDenominator;
  private double resolutionReopenRate;
  private long reopenedCount;
  private long reopenDenominator;
  private double avgTimeToFirstResponse;

  // Lot 3: KPI Directeur
  private long inflow;
  private long outflow;
  private long exitsClosed;
  private long exitsRejected;
  private long exitsCancelled;
  private long backlogReEntries;
  private long netBacklog;
  private Map<String, Long> ageDistribution;
  private long slaBreachNow;
  private Map<String, Long> cohortOutcome;
  private List<NamedIncidentCountResponse> workload;

  // Lot 5-bis: Scorecard
  private EfficiencyScorecardResponse efficiencyScorecard;

  // Convention cohorte : suit les incidents crees sur la periode, censure comprise.
  private CohortCompletionResponse cohortCompletion;

  // Jalon CLOTURE : creation -> closed_at.
  private double avgClosureHours;
  private double medianClosureHours;
  private double p90ClosureHours;
  private long closureSampleSize;

  // Delai net : hors temps ou l'horloge SLA etait arretee.
  private double avgNetClosureHours;
  private double medianNetClosureHours;

  // Jalon RESOLUTION : creation -> resolved_at.
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
  private List<IncidentHistoryResponse> recentActivities;
  private long assignedToMe;
  private List<ServiceIncidentCountResponse> topServices;
  private List<NamedIncidentCountResponse> topAgencies;
  private List<NamedIncidentCountResponse> topResolvers;
  private List<MonthlyMetricResponse> monthlyClosures;
  private List<MonthlyMetricResponse> monthlyAvgClosureHours;
  private List<NamedDurationMetricResponse> closureHoursByType;
  private List<NamedDurationMetricResponse> closureHoursByCriticality;
}
