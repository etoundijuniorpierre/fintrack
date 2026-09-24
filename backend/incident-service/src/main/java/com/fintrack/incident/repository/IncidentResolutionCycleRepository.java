// Repository : acces aux donnees persistees du domaine incident resolution cycle.

package com.fintrack.incident.repository;

import com.fintrack.incident.model.entity.IncidentResolutionCycle;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

// Expose les operations de persistance et les mesures de delai sur les cycles.
// Les delais se lisent ici, et non sur les colonnes scalaires de Incident : celles-ci
// ne decrivent que le cycle courant et sont remises a zero par une reouverture.
@Repository
public interface IncidentResolutionCycleRepository
  extends JpaRepository<IncidentResolutionCycle, UUID> {
  // Perimetre commun aux agregats de delai : agence, service ou intervenant.
  String SCOPE_SQL =
    "AND (CAST(:agencyId AS uuid) IS NULL OR i.agency_id = CAST(:agencyId AS uuid)) " +
    "AND (CAST(:serviceId AS uuid) IS NULL OR i.creator_service_id = CAST(:serviceId AS uuid) OR i.transferred_to_service = CAST(:serviceId AS uuid)) " +
    "AND (CAST(:userId AS uuid) IS NULL OR i.created_by = CAST(:userId AS uuid) OR i.assigned_to = CAST(:userId AS uuid) " +
    "OR EXISTS (SELECT 1 FROM incident_histories h WHERE h.incident_id = i.id AND h.user_id = CAST(:userId AS uuid)))";

  // Une seule passe : moyenne, percentiles, effectif et variante nette.
  String STATS_SELECT =
    "SELECT COALESCE(AVG(d.gross_hours), 0), " +
    "COALESCE(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY d.gross_hours), 0), " +
    "COALESCE(PERCENTILE_CONT(0.9) WITHIN GROUP (ORDER BY d.gross_hours), 0), " +
    "COUNT(*), " +
    "COALESCE(AVG(d.net_hours), 0), " +
    "COALESCE(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY d.net_hours), 0) FROM (";

  String CLOSURE_STATS_SQL =
    STATS_SELECT +
    "SELECT (EXTRACT(EPOCH FROM c.closed_at) - EXTRACT(EPOCH FROM c.started_at)) / 3600.0 AS gross_hours, " +
    "GREATEST((EXTRACT(EPOCH FROM c.closed_at) - EXTRACT(EPOCH FROM c.started_at)) / 3600.0 - c.paused_minutes / 60.0, 0) AS net_hours " +
    "FROM incident_resolution_cycles c JOIN incidents i ON i.id = c.incident_id " +
    "WHERE c.closed_at IS NOT NULL " +
    "AND (CAST(:from AS timestamp) IS NULL OR c.closed_at >= CAST(:from AS timestamp)) " +
    "AND (CAST(:to AS timestamp) IS NULL OR c.closed_at <= CAST(:to AS timestamp)) " +
    SCOPE_SQL +
    ") d";

  String RESOLUTION_STATS_SQL =
    STATS_SELECT +
    "SELECT (EXTRACT(EPOCH FROM c.resolved_at) - EXTRACT(EPOCH FROM c.started_at)) / 3600.0 AS gross_hours, " +
    "GREATEST((EXTRACT(EPOCH FROM c.resolved_at) - EXTRACT(EPOCH FROM c.started_at)) / 3600.0 - c.paused_minutes / 60.0, 0) AS net_hours " +
    "FROM incident_resolution_cycles c JOIN incidents i ON i.id = c.incident_id " +
    "WHERE c.resolved_at IS NOT NULL " +
    "AND (CAST(:from AS timestamp) IS NULL OR c.resolved_at >= CAST(:from AS timestamp)) " +
    "AND (CAST(:to AS timestamp) IS NULL OR c.resolved_at <= CAST(:to AS timestamp)) " +
    SCOPE_SQL +
    ") d";

  // Cycle courant d'un incident : le dernier ouvert.
  Optional<IncidentResolutionCycle> findFirstByIncidentIdOrderByCycleNoDesc(
    UUID incidentId
  );

  // Denominateur reouverture : cycles ayant atteint la resolution sur la periode.
  // Lu sur le cycle et non sur l'historique : une resolution rouverte puis atteinte a
  // nouveau est deux resolutions, pas une, et l'historique ne savait pas les separer.
  @Query(
    "SELECT COUNT(c) FROM IncidentResolutionCycle c JOIN c.incident i WHERE c.resolvedAt IS NOT NULL AND (CAST(:from AS timestamp) IS NULL OR c.resolvedAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR c.resolvedAt <= :to) AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.userId = :userId))"
  )
  long countResolvedCyclesScoped(
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Numerateur reouverture : parmi ces cycles, ceux dont l'issue est une reouverture.
  // C'est le seul lecteur de `outcome`, et la raison d'etre de la colonne : le cycle
  // sait comment il s'est termine, la fiche de l'incident ne montre que le dernier.
  @Query(
    "SELECT COUNT(c) FROM IncidentResolutionCycle c JOIN c.incident i WHERE c.resolvedAt IS NOT NULL AND c.outcome = com.fintrack.incident.model.constant.ResolutionCycleOutcome.REOPENED AND (CAST(:from AS timestamp) IS NULL OR c.resolvedAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR c.resolvedAt <= :to) AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.userId = :userId))"
  )
  long countReopenedAfterResolutionScoped(
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Jalon CLOTURE : moyenne, p50, p90, effectif et delai net, agreges en base en une
  // passe. Les percentiles se calculent en SQL, jamais en rapatriant les lignes.
  @Query(
    value = CLOSURE_STATS_SQL,
    nativeQuery = true
  )
  List<Object[]> findClosureStatsScoped(
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Jalon RESOLUTION : meme agregation, borne sur la date de resolution.
  @Query(
    value = RESOLUTION_STATS_SQL,
    nativeQuery = true
  )
  List<Object[]> findResolutionStatsScoped(
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // 1re reponse = prise en charge du cycle. Rapportee a l'ouverture du cycle, une
  // reouverture ne gonfle plus le delai de la duree du cycle precedent.
  @Query(
    "SELECT c.startedAt, c.validatedAt FROM IncidentResolutionCycle c JOIN c.incident i WHERE c.validatedAt IS NOT NULL AND (CAST(:from AS timestamp) IS NULL OR c.validatedAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR c.validatedAt <= :to) AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.userId = :userId))"
  )
  List<Object[]> findFirstResponseDatesScoped(
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Denominateur transfert : cycles pris en charge sur la periode. Lu sur le cycle et
  // non sur i.validated_at, que la reouverture remet a NULL : un rapport passe cessait
  // sinon de compter l'incident des qu'il etait rouvert.
  @Query(
    "SELECT COUNT(c) FROM IncidentResolutionCycle c JOIN c.incident i WHERE c.validatedAt IS NOT NULL AND (CAST(:from AS timestamp) IS NULL OR c.validatedAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR c.validatedAt <= :to) AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.userId = :userId))"
  )
  long countTakenInChargeScoped(
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Numerateur transfert : cycles pris en charge sur la periode dont l'incident a ete
  // REROUTE, c'est-a-dire envoye vers un service different de celui vise a la creation.
  // Le routage initial ne compte pas : c'est le fonctionnement normal, pas un ecart.
  // La comparaison se fait sur les lignes ROUTING, qui portent les identifiants de
  // service, et reste bornee a :to pour qu'un rapport passe ne bouge plus.
  @Query(
    "SELECT COUNT(c) FROM IncidentResolutionCycle c JOIN c.incident i WHERE c.validatedAt IS NOT NULL AND (CAST(:from AS timestamp) IS NULL OR c.validatedAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR c.validatedAt <= :to) AND EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.action = com.fintrack.incident.model.constant.ActionType.ROUTING AND h.newValue IS NOT NULL AND (i.initialTargetServiceId IS NULL OR h.newValue <> CAST(i.initialTargetServiceId AS String)) AND h.createdAt >= c.validatedAt AND (c.closedAt IS NULL OR h.createdAt <= c.closedAt) AND (CAST(:to AS timestamp) IS NULL OR h.createdAt <= :to)) AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory hx WHERE hx.incident = i AND hx.userId = :userId))"
  )
  long countTransferredScoped(
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Nombre de cycles clotures par mois, sur le perimetre demande.
  @Query(
    "SELECT EXTRACT(MONTH FROM c.closedAt) AS m, COUNT(c) FROM IncidentResolutionCycle c JOIN c.incident i WHERE c.closedAt IS NOT NULL AND EXTRACT(YEAR FROM c.closedAt) = :year AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.userId = :userId)) GROUP BY EXTRACT(MONTH FROM c.closedAt) ORDER BY EXTRACT(MONTH FROM c.closedAt)"
  )
  List<Object[]> findMonthlyClosureCountsScoped(
    @Param("year") int year,
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId
  );

  // Decomposition mensuelle du meme perimetre que avgClosureHours : la courbe suit le KPI.
  @Query(
    "SELECT EXTRACT(MONTH FROM c.closedAt) AS m, AVG( CAST(EXTRACT(EPOCH FROM c.closedAt) AS double) - CAST(EXTRACT(EPOCH FROM c.startedAt) AS double) ) / 3600.0 FROM IncidentResolutionCycle c JOIN c.incident i WHERE c.closedAt IS NOT NULL AND EXTRACT(YEAR FROM c.closedAt) = :year AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.userId = :userId)) GROUP BY EXTRACT(MONTH FROM c.closedAt) ORDER BY EXTRACT(MONTH FROM c.closedAt)"
  )
  List<Object[]> findMonthlyAvgClosureHoursScoped(
    @Param("year") int year,
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId
  );

  // Delai de cloture ventile par type d'incident (agregation en base, effectif inclus).
  @Query(
    "SELECT i.typeId, AVG( CAST(EXTRACT(EPOCH FROM c.closedAt) AS double) - CAST(EXTRACT(EPOCH FROM c.startedAt) AS double) ) / 3600.0, COUNT(c) FROM IncidentResolutionCycle c JOIN c.incident i WHERE c.closedAt IS NOT NULL AND (CAST(:from AS timestamp) IS NULL OR c.closedAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR c.closedAt <= :to) AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.userId = :userId)) GROUP BY i.typeId"
  )
  List<Object[]> findClosureHoursByTypeScoped(
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Delai de cloture ventile par criticite.
  @Query(
    "SELECT i.criticality, AVG( CAST(EXTRACT(EPOCH FROM c.closedAt) AS double) - CAST(EXTRACT(EPOCH FROM c.startedAt) AS double) ) / 3600.0, COUNT(c) FROM IncidentResolutionCycle c JOIN c.incident i WHERE c.closedAt IS NOT NULL AND (CAST(:from AS timestamp) IS NULL OR c.closedAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR c.closedAt <= :to) AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.userId = :userId)) GROUP BY i.criticality"
  )
  List<Object[]> findClosureHoursByCriticalityScoped(
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );
}
