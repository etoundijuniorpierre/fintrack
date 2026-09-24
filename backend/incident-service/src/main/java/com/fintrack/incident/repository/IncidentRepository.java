// Acces aux donnees : expose les requetes persistantes liees a incident.

package com.fintrack.incident.repository;

import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentTypeDistribution;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

// Repertoire Spring Data JPA pour l'acces aux donnees des incidents.

@Repository
public interface IncidentRepository
  extends JpaRepository<Incident, UUID>, JpaSpecificationExecutor<Incident>
{
  // Perimetre commun a la cohorte, exprime en SQL natif.
  String COHORT_SCOPE_SQL =
    "AND (CAST(:agencyId AS uuid) IS NULL OR i.agency_id = CAST(:agencyId AS uuid)) " +
    "AND (CAST(:serviceId AS uuid) IS NULL OR i.creator_service_id = CAST(:serviceId AS uuid) OR i.transferred_to_service = CAST(:serviceId AS uuid)) " +
    "AND (CAST(:userId AS uuid) IS NULL OR i.created_by = CAST(:userId AS uuid) OR i.assigned_to = CAST(:userId AS uuid) " +
    "OR EXISTS (SELECT 1 FROM incident_histories h WHERE h.incident_id = i.id AND h.user_id = CAST(:userId AS uuid)))";

  // Convention COHORTE : on suit les incidents CREES sur la periode jusqu'a leur
  // cloture, au lieu de ne regarder que ceux qui se sont clotures pendant la periode.
  // Les encore-ouverts ne sont pas ecartes : leur anciennete actuelle est une borne
  // basse (observation censuree), et un percentile n'est annonce que si assez de la
  // cohorte est effectivement cloturee pour l'atteindre.
  String COHORT_COMPLETION_SQL =
    "WITH cohort AS (" +
    "SELECT i.closed_at AS closed_at, " +
    "(EXTRACT(EPOCH FROM COALESCE(i.closed_at, CAST(:now AS timestamp))) - EXTRACT(EPOCH FROM i.created_at)) / 3600.0 AS elapsed_hours " +
    "FROM incidents i WHERE i.status NOT IN ('REJECTED', 'CANCELLED') " +
    "AND (CAST(:from AS timestamp) IS NULL OR i.created_at >= CAST(:from AS timestamp)) " +
    "AND (CAST(:to AS timestamp) IS NULL OR i.created_at <= CAST(:to AS timestamp)) " +
    COHORT_SCOPE_SQL +
    "), " +
    "sizes AS (SELECT COUNT(*) AS n, COUNT(c.closed_at) AS k FROM cohort c), " +
    "closed_ranked AS (SELECT c.elapsed_hours AS elapsed_hours, ROW_NUMBER() OVER (ORDER BY c.elapsed_hours) AS rn FROM cohort c WHERE c.closed_at IS NOT NULL), " +
    "open_ranked AS (SELECT c.elapsed_hours AS elapsed_hours, ROW_NUMBER() OVER (ORDER BY c.elapsed_hours) AS rn, COUNT(*) OVER () AS m FROM cohort c WHERE c.closed_at IS NULL) " +
    "SELECT s.n, s.k, " +
    "(SELECT MIN(cr.elapsed_hours) FROM closed_ranked cr WHERE cr.rn >= CEIL(0.5 * s.n)), " +
    "(SELECT MIN(cr.elapsed_hours) FROM closed_ranked cr WHERE cr.rn >= CEIL(0.9 * s.n)), " +
    "(SELECT MIN(o.elapsed_hours) FROM open_ranked o WHERE o.rn >= CEIL(0.5 * o.m)), " +
    "(SELECT COALESCE(MAX(c.elapsed_hours), 0) FROM cohort c) " +
    "FROM sizes s";

  // Recherche les incidents par agence identifiant.

  // Recherche un incident par son code metier lisible.
  Optional<Incident> findByReference(String reference);

  List<Incident> findByAgencyId(UUID agencyId);
  // Recherche les incidents par creation by.

  List<Incident> findByCreatedBy(UUID createdBy);
  // Recherche les incidents par assigne fin.

  List<Incident> findByAssignedTo(UUID assignedTo);
  // Compte les elements du domaine incident selon assigned to and statut not in.

  long countByAssignedToAndStatusNotIn(
    UUID assignedTo,
    List<IncidentStatus> statuses
  );
  // Selectionne les incidents recents pour dix derniers by creation by order by creation at desc.

  List<Incident> findTop10ByCreatedByOrderByCreatedAtDesc(UUID createdBy);
  // Selectionne les incidents recents pour dix derniers by agence identifiant order by creation at desc.

  List<Incident> findTop10ByAgencyIdOrderByCreatedAtDesc(UUID agencyId);

  // Selectionne les incidents recents pour recent for service.

  @Query(
    "SELECT i FROM Incident i WHERE i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId ORDER BY i.createdAt DESC"
  )
  List<Incident> findRecentForService(
    @Param("serviceId") UUID serviceId,
    Pageable pageable
  );

  // Recherche les incidents par transfert fin service.

  List<Incident> findByTransferredToService(UUID serviceId);
  // Recherche les incidents par statut.

  List<Incident> findByStatus(IncidentStatus status);
  // Compte les elements du domaine incident selon statut et date de creation dans la periode.

  long countByStatusAndCreatedAtBetween(
    IncidentStatus status,
    LocalDateTime from,
    LocalDateTime to
  );
  // Compte les elements du domaine incident selon statut et date de creation posterieure.

  long countByStatusAndCreatedAtAfter(
    IncidentStatus status,
    LocalDateTime from
  );
  // Compte les elements du domaine incident selon statut.

  long countByStatus(IncidentStatus status);
  // Compte les elements du domaine incident selon statut and assigned to.

  long countByStatusAndAssignedTo(IncidentStatus status, UUID assignedTo);

  @Query(
    "SELECT i FROM Incident i WHERE i.status NOT IN :excludedStatuses " +
      "AND i.dueDate <= :currentDate " +
      "AND (i.lastSlaReminderSentAt IS NULL OR i.lastSlaReminderSentAt <= :reminderCutoff)"
  )
  // Recherche les incidents pour depassement SLA without recent reminder.
  List<Incident> findBreachedSlaWithoutRecentReminder(
    @Param("excludedStatuses") List<IncidentStatus> excludedStatuses,
    @Param("currentDate") LocalDateTime currentDate,
    @Param("reminderCutoff") LocalDateTime reminderCutoff
  );

  // Critiques encore ouverts, declares avant la borne, sans relance recente.
  @Query(
    "SELECT i FROM Incident i WHERE i.criticality = :criticality " +
      "AND i.status IN :openStatuses " +
      "AND i.createdAt <= :declaredBefore " +
      "AND (i.lastCriticalReminderSentAt IS NULL OR i.lastCriticalReminderSentAt <= :reminderCutoff)"
  )
  List<Incident> findCriticalOpenWithoutRecentReminder(
    @Param("criticality") Criticality criticality,
    @Param("openStatuses") Collection<IncidentStatus> openStatuses,
    @Param("declaredBefore") LocalDateTime declaredBefore,
    @Param("reminderCutoff") LocalDateTime reminderCutoff
  );

  // Attentes de decision qui durent, sans relance recente. "Attente validation Direction"
  // a deux sens : sans motif de rejet la Direction est attendue, avec un motif c'est le
  // traitant qui doit resoumettre. Les deux se relancent — c'est le destinataire qui
  // change, pas la surveillance.
  @Query(
    "SELECT i FROM Incident i WHERE i.status IN :awaitingStatuses " +
      "AND i.decisionAwaitedSince IS NOT NULL " +
      "AND i.decisionAwaitedSince <= :awaitedBefore " +
      "AND (i.lastDecisionReminderSentAt IS NULL OR i.lastDecisionReminderSentAt <= :reminderCutoff)"
  )
  List<Incident> findOverdueDecisionsWithoutRecentReminder(
    @Param("awaitingStatuses") Collection<IncidentStatus> awaitingStatuses,
    @Param("awaitedBefore") LocalDateTime awaitedBefore,
    @Param("reminderCutoff") LocalDateTime reminderCutoff
  );

  // Demandes d'actualite restees sans reponse : candidates a une relance. Les
  // administrateurs en sont destinataires des le premier envoi, pour que le silence
  // de l'entite source n'immobilise pas l'incident.
  @Query(
    "SELECT i FROM Incident i WHERE i.status = :status " +
      "AND i.confirmationRequestedAt IS NOT NULL " +
      "AND i.confirmationRequestedAt <= :requestedBefore " +
      "AND (i.lastConfirmationReminderSentAt IS NULL OR i.lastConfirmationReminderSentAt <= :reminderCutoff)"
  )
  List<Incident> findPendingConfirmationsWithoutRecentReminder(
    @Param("status") IncidentStatus status,
    @Param("requestedBefore") LocalDateTime requestedBefore,
    @Param("reminderCutoff") LocalDateTime reminderCutoff
  );

  @Query(
    "SELECT i FROM Incident i WHERE i.status NOT IN :excludedStatuses " +
      "AND i.dueDate <= :currentDate"
  )
  List<Incident> findBreachedActiveIncidents(
    @Param("excludedStatuses") List<IncidentStatus> excludedStatuses,
    @Param("currentDate") LocalDateTime currentDate
  );

  // `appliesFrom` borne le dispositif d'attente prolongee aux incidents declares a
  // partir d'une date : null = pas de borne, tout le stock est concerne.
  @Query(
    "SELECT i FROM Incident i WHERE i.status = :status " +
      "AND i.blockedAt <= :cutoffDate " +
      "AND (CAST(:appliesFrom AS timestamp) IS NULL OR i.createdAt >= :appliesFrom)"
  )
  List<Incident> findBlockedIncidentsOlderThan(
    @Param("status") IncidentStatus status,
    @Param("cutoffDate") LocalDateTime cutoffDate,
    @Param("appliesFrom") LocalDateTime appliesFrom
  );

  // Calcule le nombre de incidents pour creation at periode.
  long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
  // Compte les elements du domaine incident selon date de creation posterieure.

  long countByCreatedAtAfter(LocalDateTime from);
  // Compte les elements du domaine incident selon statut et createur and date de creation dans la periode.

  long countByStatusAndCreatedByAndCreatedAtBetween(
    IncidentStatus status,
    UUID createdBy,
    LocalDateTime from,
    LocalDateTime to
  );
  // Compte les elements du domaine incident selon statut et createur and date de creation posterieure.

  long countByStatusAndCreatedByAndCreatedAtAfter(
    IncidentStatus status,
    UUID createdBy,
    LocalDateTime from
  );
  // Compte les elements du domaine incident selon statut et createur.

  long countByStatusAndCreatedBy(IncidentStatus status, UUID createdBy);
  // Compte les elements du domaine incident selon createur and date de creation dans la periode.

  long countByCreatedByAndCreatedAtBetween(
    UUID createdBy,
    LocalDateTime from,
    LocalDateTime to
  );
  // Compte les elements du domaine incident selon createur and date de creation posterieure.

  long countByCreatedByAndCreatedAtAfter(UUID createdBy, LocalDateTime from);
  // Compte les elements du domaine incident selon createur.

  long countByCreatedBy(UUID createdBy);

  // --- OWN Scope Union (cree OU assigne OU historique) ---
  @Query(
    "SELECT COUNT(i.id) FROM Incident i " +
      "WHERE (i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.userId = :userId)) " +
      "AND i.status = :status " +
      "AND (CAST(:from AS timestamp) IS NULL OR i.createdAt >= :from) " +
      "AND (CAST(:to AS timestamp) IS NULL OR i.createdAt <= :to)"
  )
  // Compte les elements du domaine incident selon statut for own.
  long countByStatusForOwn(
    @Param("status") IncidentStatus status,
    @Param("userId") UUID userId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  @Query(
    "SELECT COUNT(i.id) FROM Incident i " +
      "WHERE (i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.userId = :userId)) " +
      "AND i.status IN :statuses " +
      "AND (CAST(:from AS timestamp) IS NULL OR i.createdAt >= :from) " +
      "AND (CAST(:to AS timestamp) IS NULL OR i.createdAt <= :to)"
  )
  // Compte les elements du domaine incident selon statuses for own.
  long countByStatusesForOwn(
    @Param("statuses") List<IncidentStatus> statuses,
    @Param("userId") UUID userId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  @Query(
    "SELECT COUNT(i.id) FROM Incident i " +
      "WHERE (i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.userId = :userId)) " +
      "AND (CAST(:from AS timestamp) IS NULL OR i.createdAt >= :from) " +
      "AND (CAST(:to AS timestamp) IS NULL OR i.createdAt <= :to)"
  )
  // Calcule le nombre de incidents pour total for personnel.
  long countTotalForOwn(
    @Param("userId") UUID userId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // --- In Statuses ---
  @Query(
    "SELECT COUNT(i) FROM Incident i WHERE i.status IN :statuses AND (CAST(:from AS timestamp) IS NULL OR i.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR i.createdAt <= :to)"
  )
  long countByStatusInAll(
    @Param("statuses") List<IncidentStatus> statuses,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Compte les elements du domaine incident selon statut in agence.

  @Query(
    "SELECT COUNT(i) FROM Incident i WHERE i.status IN :statuses AND i.agencyId = :agencyId AND (CAST(:from AS timestamp) IS NULL OR i.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR i.createdAt <= :to)"
  )
  long countByStatusInAgency(
    @Param("statuses") List<IncidentStatus> statuses,
    @Param("agencyId") UUID agencyId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Compte les elements du domaine incident selon statut in service.

  @Query(
    "SELECT COUNT(i) FROM Incident i WHERE i.status IN :statuses AND (i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (CAST(:from AS timestamp) IS NULL OR i.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR i.createdAt <= :to)"
  )
  long countByStatusInService(
    @Param("statuses") List<IncidentStatus> statuses,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Incidents anterieurs a la reference : rattrapage au demarrage, par anciennete.
  List<Incident> findByReferenceIsNullOrderByCreatedAtAsc();

  // Reopened
  @Query(
    "SELECT COUNT(DISTINCT i.id) FROM Incident i JOIN i.history h WHERE (i.createdBy = :userId OR i.assignedTo = :userId OR h.userId = :userId) AND (h.action = com.fintrack.incident.model.constant.ActionType.REOPENING OR (h.action = com.fintrack.incident.model.constant.ActionType.STATUS_CHANGE AND h.newValue = 'REOPENED')) AND (CAST(:from AS timestamp) IS NULL OR h.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR h.createdAt <= :to)"
  )
  long countReopenedForOwn(
    @Param("userId") UUID userId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Calcule le nombre de incidents pour reouverture in global.

  @Query(
    "SELECT COUNT(DISTINCT i.id) FROM Incident i JOIN i.history h WHERE (h.action = com.fintrack.incident.model.constant.ActionType.REOPENING OR (h.action = com.fintrack.incident.model.constant.ActionType.STATUS_CHANGE AND h.newValue = 'REOPENED')) AND (CAST(:from AS timestamp) IS NULL OR h.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR h.createdAt <= :to)"
  )
  long countReopenedInAll(
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Calcule le nombre de incidents pour reouverture in agence.

  @Query(
    "SELECT COUNT(DISTINCT i.id) FROM Incident i JOIN i.history h WHERE i.agencyId = :agencyId AND (h.action = com.fintrack.incident.model.constant.ActionType.REOPENING OR (h.action = com.fintrack.incident.model.constant.ActionType.STATUS_CHANGE AND h.newValue = 'REOPENED')) AND (CAST(:from AS timestamp) IS NULL OR h.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR h.createdAt <= :to)"
  )
  long countReopenedInAgency(
    @Param("agencyId") UUID agencyId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Calcule le nombre de incidents pour reouverture in service.

  @Query(
    "SELECT COUNT(DISTINCT i.id) FROM Incident i JOIN i.history h WHERE (i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (h.action = com.fintrack.incident.model.constant.ActionType.REOPENING OR (h.action = com.fintrack.incident.model.constant.ActionType.STATUS_CHANGE AND h.newValue = 'REOPENED')) AND (CAST(:from AS timestamp) IS NULL OR h.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR h.createdAt <= :to)"
  )
  long countReopenedInService(
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // SLA conforme = cloture a temps (closed_at <= dueDate), filtre par closed_at sur la periode. Jalon cloture.
  @Query(
    "SELECT COUNT(i.id) FROM Incident i WHERE (i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.userId = :userId)) AND i.closedAt IS NOT NULL AND i.closedAt <= i.initialDueDate AND (CAST(:from AS timestamp) IS NULL OR i.closedAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR i.closedAt <= :to)"
  )
  long countSlaCompliantForOwn(
    @Param("userId") UUID userId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Calcule le nombre de incidents pour total in service.

  @Query(
    "SELECT COUNT(i) FROM Incident i WHERE (i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (CAST(:from AS timestamp) IS NULL OR i.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR i.createdAt <= :to)"
  )
  long countTotalInService(
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Lot 3: Age distribution
  @Query("SELECT i.createdAt FROM Incident i WHERE i.status IN :statuses")
  List<LocalDateTime> findActiveCreatedAtInAll(
    @Param("statuses") List<IncidentStatus> statuses
  );

  // Recherche les incidents pour actif creation at in agence.

  @Query(
    "SELECT i.createdAt FROM Incident i WHERE i.agencyId = :agencyId AND i.status IN :statuses"
  )
  List<LocalDateTime> findActiveCreatedAtInAgency(
    @Param("statuses") List<IncidentStatus> statuses,
    @Param("agencyId") UUID agencyId
  );

  // Recherche les incidents pour actif creation at in service.

  @Query(
    "SELECT i.createdAt FROM Incident i WHERE (i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND i.status IN :statuses"
  )
  List<LocalDateTime> findActiveCreatedAtInService(
    @Param("statuses") List<IncidentStatus> statuses,
    @Param("serviceId") UUID serviceId
  );

  // Recherche les incidents pour actif creation at for personnel.

  @Query(
    "SELECT i.createdAt FROM Incident i WHERE i.status IN :statuses AND (i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.userId = :userId))"
  )
  List<LocalDateTime> findActiveCreatedAtForOwn(
    @Param("statuses") List<IncidentStatus> statuses,
    @Param("userId") UUID userId
  );

  // Calcule le nombre de incidents pour SLA conformite in global.
  @Query(
    "SELECT COUNT(i) FROM Incident i WHERE i.closedAt IS NOT NULL AND i.closedAt <= i.initialDueDate AND (CAST(:from AS timestamp) IS NULL OR i.closedAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR i.closedAt <= :to)"
  )
  long countSlaCompliantInAll(
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Calcule le nombre de incidents pour SLA conformite in agence.

  @Query(
    "SELECT COUNT(i) FROM Incident i WHERE i.agencyId = :agencyId AND i.closedAt IS NOT NULL AND i.closedAt <= i.initialDueDate AND (CAST(:from AS timestamp) IS NULL OR i.closedAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR i.closedAt <= :to)"
  )
  long countSlaCompliantInAgency(
    @Param("agencyId") UUID agencyId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Calcule le nombre de incidents pour SLA conformite in service.

  @Query(
    "SELECT COUNT(i) FROM Incident i WHERE (i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND i.closedAt IS NOT NULL AND i.closedAt <= i.initialDueDate AND (CAST(:from AS timestamp) IS NULL OR i.closedAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR i.closedAt <= :to)"
  )
  long countSlaCompliantInService(
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Compte les elements du domaine incident selon statut and agence id and date de creation dans la periode.

  long countByStatusAndAgencyIdAndCreatedAtBetween(
    IncidentStatus status,
    UUID agencyId,
    LocalDateTime from,
    LocalDateTime to
  );
  // Compte les elements du domaine incident selon statut and agence id and date de creation posterieure.

  long countByStatusAndAgencyIdAndCreatedAtAfter(
    IncidentStatus status,
    UUID agencyId,
    LocalDateTime from
  );
  // Compte les elements du domaine incident selon statut and agence id.

  long countByStatusAndAgencyId(IncidentStatus status, UUID agencyId);
  // Compte les elements du domaine incident selon agence id and date de creation dans la periode.

  long countByAgencyIdAndCreatedAtBetween(
    UUID agencyId,
    LocalDateTime from,
    LocalDateTime to
  );
  // Compte les elements du domaine incident selon agence id and date de creation posterieure.

  long countByAgencyIdAndCreatedAtAfter(UUID agencyId, LocalDateTime from);
  // Compte les elements du domaine incident selon agence id.

  long countByAgencyId(UUID agencyId);
  // Compte les elements du domaine incident selon statut and transferred to service and date de creation dans la periode.

  long countByStatusAndTransferredToServiceAndCreatedAtBetween(
    IncidentStatus status,
    UUID serviceId,
    LocalDateTime from,
    LocalDateTime to
  );
  // Compte les elements du domaine incident selon statut and transferred to service and date de creation posterieure.

  long countByStatusAndTransferredToServiceAndCreatedAtAfter(
    IncidentStatus status,
    UUID serviceId,
    LocalDateTime from
  );
  // Compte les elements du domaine incident selon statut and transferred to service.

  long countByStatusAndTransferredToService(
    IncidentStatus status,
    UUID serviceId
  );
  // Compte les elements du domaine incident selon transferred to service and date de creation dans la periode.

  long countByTransferredToServiceAndCreatedAtBetween(
    UUID serviceId,
    LocalDateTime from,
    LocalDateTime to
  );
  // Compte les elements du domaine incident selon transferred to service and date de creation posterieure.

  long countByTransferredToServiceAndCreatedAtAfter(
    UUID serviceId,
    LocalDateTime from
  );
  // Compte les elements du domaine incident selon transferred to service.

  long countByTransferredToService(UUID serviceId);

  // Recherche les incidents pour type distribution periode.

  @Query(
    "SELECT new com.fintrack.incident.model.entity.IncidentTypeDistribution(i.typeId, '', COUNT(i)) FROM Incident i WHERE i.createdAt BETWEEN :from AND :to GROUP BY i.typeId ORDER BY COUNT(i) DESC"
  )
  List<IncidentTypeDistribution> findTypeDistributionBetween(
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Recherche les incidents pour type distribution date minimale.

  @Query(
    "SELECT new com.fintrack.incident.model.entity.IncidentTypeDistribution(i.typeId, '', COUNT(i)) FROM Incident i WHERE i.createdAt >= :from GROUP BY i.typeId ORDER BY COUNT(i) DESC"
  )
  List<IncidentTypeDistribution> findTypeDistributionAfter(
    @Param("from") LocalDateTime from
  );

  // Recherche les incidents pour type distribution global.

  @Query(
    "SELECT new com.fintrack.incident.model.entity.IncidentTypeDistribution(i.typeId, '', COUNT(i)) FROM Incident i GROUP BY i.typeId ORDER BY COUNT(i) DESC"
  )
  List<IncidentTypeDistribution> findTypeDistributionAll();

  // Recherche les incidents pour type distribution by creation by periode.

  @Query(
    "SELECT new com.fintrack.incident.model.entity.IncidentTypeDistribution(i.typeId, '', COUNT(i)) FROM Incident i WHERE i.createdBy = :userId AND i.createdAt BETWEEN :from AND :to GROUP BY i.typeId ORDER BY COUNT(i) DESC"
  )
  List<IncidentTypeDistribution> findTypeDistributionByCreatedByBetween(
    @Param("userId") UUID userId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Recherche les incidents pour type distribution by creation by date minimale.

  @Query(
    "SELECT new com.fintrack.incident.model.entity.IncidentTypeDistribution(i.typeId, '', COUNT(i)) FROM Incident i WHERE i.createdBy = :userId AND i.createdAt >= :from GROUP BY i.typeId ORDER BY COUNT(i) DESC"
  )
  List<IncidentTypeDistribution> findTypeDistributionByCreatedByAfter(
    @Param("userId") UUID userId,
    @Param("from") LocalDateTime from
  );

  // Recherche les incidents pour type distribution by creation by.

  @Query(
    "SELECT new com.fintrack.incident.model.entity.IncidentTypeDistribution(i.typeId, '', COUNT(i)) FROM Incident i WHERE i.createdBy = :userId GROUP BY i.typeId ORDER BY COUNT(i) DESC"
  )
  List<IncidentTypeDistribution> findTypeDistributionByCreatedBy(
    @Param("userId") UUID userId
  );

  // Perimetre OWN = union (cree OU assigne OU traite), aligne sur countByStatusForOwn.
  @Query(
    "SELECT new com.fintrack.incident.model.entity.IncidentTypeDistribution(i.typeId, '', COUNT(i.id)) " +
      "FROM Incident i " +
      "WHERE (i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.userId = :userId)) " +
      "AND (CAST(:from AS timestamp) IS NULL OR i.createdAt >= :from) " +
      "AND (CAST(:to AS timestamp) IS NULL OR i.createdAt <= :to) " +
      "GROUP BY i.typeId ORDER BY COUNT(i.id) DESC"
  )
  // Recherche les incidents pour type distribution for personnel.
  List<IncidentTypeDistribution> findTypeDistributionForOwn(
    @Param("userId") UUID userId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Recherche les incidents pour type distribution by agence identifiant periode.

  @Query(
    "SELECT new com.fintrack.incident.model.entity.IncidentTypeDistribution(i.typeId, '', COUNT(i)) FROM Incident i WHERE i.agencyId = :agencyId AND i.createdAt BETWEEN :from AND :to GROUP BY i.typeId ORDER BY COUNT(i) DESC"
  )
  List<IncidentTypeDistribution> findTypeDistributionByAgencyIdBetween(
    @Param("agencyId") UUID agencyId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Recherche les incidents pour type distribution by agence identifiant date minimale.

  @Query(
    "SELECT new com.fintrack.incident.model.entity.IncidentTypeDistribution(i.typeId, '', COUNT(i)) FROM Incident i WHERE i.agencyId = :agencyId AND i.createdAt >= :from GROUP BY i.typeId ORDER BY COUNT(i) DESC"
  )
  List<IncidentTypeDistribution> findTypeDistributionByAgencyIdAfter(
    @Param("agencyId") UUID agencyId,
    @Param("from") LocalDateTime from
  );

  // Recherche les incidents pour type distribution by agence identifiant.

  @Query(
    "SELECT new com.fintrack.incident.model.entity.IncidentTypeDistribution(i.typeId, '', COUNT(i)) FROM Incident i WHERE i.agencyId = :agencyId GROUP BY i.typeId ORDER BY COUNT(i) DESC"
  )
  List<IncidentTypeDistribution> findTypeDistributionByAgencyId(
    @Param("agencyId") UUID agencyId
  );

  // Recherche les incidents pour type distribution by service identifiant periode.

  @Query(
    "SELECT new com.fintrack.incident.model.entity.IncidentTypeDistribution(i.typeId, '', COUNT(i)) FROM Incident i WHERE i.transferredToService = :serviceId AND i.createdAt BETWEEN :from AND :to GROUP BY i.typeId ORDER BY COUNT(i) DESC"
  )
  List<IncidentTypeDistribution> findTypeDistributionByServiceIdBetween(
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Recherche les incidents pour type distribution by service identifiant date minimale.

  @Query(
    "SELECT new com.fintrack.incident.model.entity.IncidentTypeDistribution(i.typeId, '', COUNT(i)) FROM Incident i WHERE i.transferredToService = :serviceId AND i.createdAt >= :from GROUP BY i.typeId ORDER BY COUNT(i) DESC"
  )
  List<IncidentTypeDistribution> findTypeDistributionByServiceIdAfter(
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from
  );

  // Recherche les incidents pour type distribution by service identifiant.

  @Query(
    "SELECT new com.fintrack.incident.model.entity.IncidentTypeDistribution(i.typeId, '', COUNT(i)) FROM Incident i WHERE i.transferredToService = :serviceId GROUP BY i.typeId ORDER BY COUNT(i) DESC"
  )
  List<IncidentTypeDistribution> findTypeDistributionByServiceId(
    @Param("serviceId") UUID serviceId
  );

  // Perimetre SERVICE
  // Un service possede un incident s'il y a ete cree (creatorServiceId) OU
  // s'il lui a ete transfere (transferredToService) coherent avec la vue liste
  // (IncidentSpecification) et avec assertCanViewIncident. Une seule requete avec
  // OR evite le double comptage d'un incident a la fois cree et transfere.

  @Query(
    "SELECT COUNT(i) FROM Incident i WHERE i.status = :status AND (i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId)"
  )
  long countByStatusForService(
    @Param("status") IncidentStatus status,
    @Param("serviceId") UUID serviceId
  );

  @Query(
    "SELECT COUNT(i) FROM Incident i WHERE i.status = :status AND (i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND i.createdAt BETWEEN :from AND :to"
  )
  // Calcule le nombre de incidents pour statut for service periode.
  long countByStatusForServiceBetween(
    @Param("status") IncidentStatus status,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  @Query(
    "SELECT COUNT(i) FROM Incident i WHERE i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId"
  )
  // Calcule le nombre de incidents pour service.
  long countForService(@Param("serviceId") UUID serviceId);

  // Recherche les incidents pour service.

  @Query(
    "SELECT i FROM Incident i WHERE i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId"
  )
  List<Incident> findForService(@Param("serviceId") UUID serviceId);

  @Query(
    "SELECT COUNT(i) FROM Incident i WHERE (i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND i.createdAt BETWEEN :from AND :to"
  )
  // Calcule le nombre de incidents pour service periode.
  long countForServiceBetween(
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  @Query(
    "SELECT new com.fintrack.incident.model.entity.IncidentTypeDistribution(i.typeId, '', COUNT(i)) FROM Incident i WHERE i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId GROUP BY i.typeId ORDER BY COUNT(i) DESC"
  )
  // Recherche les incidents pour type distribution for service.
  List<IncidentTypeDistribution> findTypeDistributionForService(
    @Param("serviceId") UUID serviceId
  );

  @Query(
    "SELECT new com.fintrack.incident.model.entity.IncidentTypeDistribution(i.typeId, '', COUNT(i)) FROM Incident i WHERE (i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND i.createdAt BETWEEN :from AND :to GROUP BY i.typeId ORDER BY COUNT(i) DESC"
  )
  // Recherche les incidents pour type distribution for service periode.
  List<IncidentTypeDistribution> findTypeDistributionForServiceBetween(
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  @Query(
    "SELECT i.criticality, COUNT(i) FROM Incident i WHERE (CAST(:from AS timestamp) IS NULL OR i.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR i.createdAt <= :to) GROUP BY i.criticality"
  )
  // Calcule le nombre de incidents pour criticality.
  List<Object[]> countByCriticality(
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Compte les elements du domaine incident selon criticite and createur.

  @Query(
    "SELECT i.criticality, COUNT(i) FROM Incident i WHERE i.createdBy = :userId GROUP BY i.criticality"
  )
  List<Object[]> countByCriticalityAndCreatedBy(@Param("userId") UUID userId);

  // Perimetre OWN = union (cree OU assigne OU traite), aligne sur countByStatusForOwn.
  @Query(
    "SELECT i.criticality, COUNT(i.id) FROM Incident i " +
      "WHERE (i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.userId = :userId)) " +
      "AND (CAST(:from AS timestamp) IS NULL OR i.createdAt >= :from) " +
      "AND (CAST(:to AS timestamp) IS NULL OR i.createdAt <= :to) " +
      "GROUP BY i.criticality"
  )
  // Compte les elements du domaine incident selon criticite for own.
  List<Object[]> countByCriticalityForOwn(
    @Param("userId") UUID userId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Compte les elements du domaine incident selon criticite and agence id.

  @Query(
    "SELECT i.criticality, COUNT(i) FROM Incident i WHERE i.agencyId = :agencyId AND (CAST(:from AS timestamp) IS NULL OR i.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR i.createdAt <= :to) GROUP BY i.criticality"
  )
  List<Object[]> countByCriticalityAndAgencyId(
    @Param("agencyId") UUID agencyId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Compte les elements du domaine incident selon criticite and service id.

  @Query(
    "SELECT i.criticality, COUNT(i) FROM Incident i WHERE i.transferredToService = :serviceId GROUP BY i.criticality"
  )
  List<Object[]> countByCriticalityAndServiceId(
    @Param("serviceId") UUID serviceId
  );

  // Compte les elements du domaine incident selon criticite for service.

  @Query(
    "SELECT i.criticality, COUNT(i) FROM Incident i WHERE (i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (CAST(:from AS timestamp) IS NULL OR i.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR i.createdAt <= :to) GROUP BY i.criticality"
  )
  List<Object[]> countByCriticalityForService(
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Classement des services par incidents clotures.
  @Query(
    "SELECT COALESCE(i.transferredToService, i.creatorServiceId), COUNT(i) FROM Incident i " +
      "WHERE (i.transferredToService IS NOT NULL OR i.creatorServiceId IS NOT NULL) " +
      "AND i.closedAt IS NOT NULL " +
      "AND (CAST(:from AS timestamp) IS NULL OR i.closedAt >= :from) " +
      "AND (CAST(:to AS timestamp) IS NULL OR i.closedAt <= :to) " +
      "GROUP BY COALESCE(i.transferredToService, i.creatorServiceId) " +
      "ORDER BY COUNT(i) DESC, COALESCE(i.transferredToService, i.creatorServiceId) ASC"
  )
  // Classe les services selon le nombre d'incidents clotures.
  List<Object[]> findTopServices(
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Classement des agences par incidents clotures.
  @Query(
    "SELECT i.agencyId, COUNT(i) FROM Incident i " +
      "WHERE i.agencyId IS NOT NULL " +
      "AND i.closedAt IS NOT NULL " +
      "AND (CAST(:from AS timestamp) IS NULL OR i.closedAt >= :from) " +
      "AND (CAST(:to AS timestamp) IS NULL OR i.closedAt <= :to) " +
      "GROUP BY i.agencyId ORDER BY COUNT(i) DESC, i.agencyId ASC"
  )
  // Classe les agences selon le nombre d'incidents clotures.
  List<Object[]> findTopAgencies(
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Classement des derniers resolveurs des incidents clotures.
  @Query(
    "SELECT h.userId, COUNT(DISTINCT i.id) FROM Incident i JOIN i.history h " +
      "WHERE h.action = com.fintrack.incident.model.constant.ActionType.STATUS_CHANGE " +
      "AND h.newValue = 'RESOLVED' " +
      "AND h.userId IS NOT NULL " +
      "AND i.closedAt IS NOT NULL " +
      "AND NOT EXISTS (SELECT 1 FROM IncidentHistory laterResolution " +
      "WHERE laterResolution.incident = i " +
      "AND laterResolution.action = com.fintrack.incident.model.constant.ActionType.STATUS_CHANGE " +
      "AND laterResolution.newValue = 'RESOLVED' " +
      "AND laterResolution.createdAt > h.createdAt " +
      "AND laterResolution.createdAt <= i.closedAt) " +
      "AND (CAST(:from AS timestamp) IS NULL OR i.closedAt >= :from) " +
      "AND (CAST(:to AS timestamp) IS NULL OR i.closedAt <= :to) " +
      "GROUP BY h.userId ORDER BY COUNT(DISTINCT i.id) DESC, h.userId ASC"
  )
  // Classe les utilisateurs selon le nombre d'incidents clotures qu'ils ont resolus.
  List<Object[]> findTopResolvers(
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Resolveurs scopes a l'agence d'origine.
  @Query(
    "SELECT h.userId, COUNT(DISTINCT i.id) FROM Incident i JOIN i.history h " +
      "WHERE h.action = com.fintrack.incident.model.constant.ActionType.STATUS_CHANGE " +
      "AND h.newValue = 'RESOLVED' AND i.agencyId = :agencyId " +
      "AND h.userId IS NOT NULL " +
      "AND i.closedAt IS NOT NULL " +
      "AND NOT EXISTS (SELECT 1 FROM IncidentHistory laterResolution " +
      "WHERE laterResolution.incident = i " +
      "AND laterResolution.action = com.fintrack.incident.model.constant.ActionType.STATUS_CHANGE " +
      "AND laterResolution.newValue = 'RESOLVED' " +
      "AND laterResolution.createdAt > h.createdAt " +
      "AND laterResolution.createdAt <= i.closedAt) " +
      "AND (CAST(:from AS timestamp) IS NULL OR i.closedAt >= :from) " +
      "AND (CAST(:to AS timestamp) IS NULL OR i.closedAt <= :to) " +
      "GROUP BY h.userId ORDER BY COUNT(DISTINCT i.id) DESC, h.userId ASC"
  )
  // Classe les utilisateurs du perimetre agence.
  List<Object[]> findTopResolversByAgency(
    @Param("agencyId") UUID agencyId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Resolveurs scopes au perimetre visible du service (source ou destinataire).
  @Query(
    "SELECT h.userId, COUNT(DISTINCT i.id) FROM Incident i JOIN i.history h " +
      "WHERE h.action = com.fintrack.incident.model.constant.ActionType.STATUS_CHANGE " +
      "AND h.newValue = 'RESOLVED' " +
      "AND h.userId IS NOT NULL " +
      "AND i.closedAt IS NOT NULL " +
      "AND (i.transferredToService = :serviceId OR i.creatorServiceId = :serviceId) " +
      "AND NOT EXISTS (SELECT 1 FROM IncidentHistory laterResolution " +
      "WHERE laterResolution.incident = i " +
      "AND laterResolution.action = com.fintrack.incident.model.constant.ActionType.STATUS_CHANGE " +
      "AND laterResolution.newValue = 'RESOLVED' " +
      "AND laterResolution.createdAt > h.createdAt " +
      "AND laterResolution.createdAt <= i.closedAt) " +
      "AND (CAST(:from AS timestamp) IS NULL OR i.closedAt >= :from) " +
      "AND (CAST(:to AS timestamp) IS NULL OR i.closedAt <= :to) " +
      "GROUP BY h.userId ORDER BY COUNT(DISTINCT i.id) DESC, h.userId ASC"
  )
  // Classe les utilisateurs du perimetre service.
  List<Object[]> findTopResolversByService(
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Denominateur reouverture = incidents ayant atteint RESOLVED (resolved_at non nul) sur la periode.
  @Query(
    "SELECT COUNT(DISTINCT i.id) FROM Incident i JOIN i.history h WHERE h.action = com.fintrack.incident.model.constant.ActionType.STATUS_CHANGE AND h.newValue = 'RESOLVED' AND (CAST(:from AS timestamp) IS NULL OR h.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR h.createdAt <= :to) AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory hx WHERE hx.incident = i AND hx.userId = :userId))"
  )
  long countResolvedReached(
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );


  // Denominateur SLA + outflow = incidents clotures (closed_at) sur la periode.
  @Query(
    "SELECT COUNT(i) FROM Incident i WHERE i.closedAt IS NOT NULL AND (CAST(:from AS timestamp) IS NULL OR i.closedAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR i.closedAt <= :to) AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.userId = :userId))"
  )
  long countClosedInPeriod(
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Compte les clotures disposant d'une echeance SLA pour former le denominateur du taux.
  @Query(
    "SELECT COUNT(i) FROM Incident i WHERE i.closedAt IS NOT NULL AND i.initialDueDate IS NOT NULL AND (CAST(:from AS timestamp) IS NULL OR i.closedAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR i.closedAt <= :to) AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.userId = :userId))"
  )
  long countClosedWithSlaInPeriod(
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Resolved in period
  @Query(
    "SELECT COUNT(DISTINCT i.id) FROM Incident i JOIN i.history h WHERE h.action = com.fintrack.incident.model.constant.ActionType.STATUS_CHANGE AND h.newValue = 'RESOLVED' AND (CAST(:from AS timestamp) IS NULL OR h.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR h.createdAt <= :to) AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory hx WHERE hx.incident = i AND hx.userId = :userId))"
  )
  long countResolvedInPeriod(
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Treated in period via history (status change to TREATED)
  @Query(
    "SELECT COUNT(DISTINCT i.id) FROM Incident i JOIN i.history h WHERE h.action = com.fintrack.incident.model.constant.ActionType.STATUS_CHANGE AND h.newValue = 'TREATED' AND (CAST(:from AS timestamp) IS NULL OR h.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR h.createdAt <= :to) AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory hx WHERE hx.incident = i AND hx.userId = :userId))"
  )
  long countTreatedInPeriod(
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Rejected in period via history (status change to REJECTED)
  @Query(
    "SELECT COUNT(DISTINCT i.id) FROM Incident i JOIN i.history h WHERE h.action = com.fintrack.incident.model.constant.ActionType.STATUS_CHANGE AND h.newValue = 'REJECTED' AND (CAST(:from AS timestamp) IS NULL OR h.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR h.createdAt <= :to) AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory hx WHERE hx.incident = i AND hx.userId = :userId))"
  )
  long countRejectedInPeriod(
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Listes de flux adossees aux compteurs countTreated/Resolved/ClosedInPeriod :
  // memes predicats, retournent les incidents pour que chaque compteur soit auditable.
  @Query(
    "SELECT DISTINCT i FROM Incident i JOIN i.history h WHERE h.action = com.fintrack.incident.model.constant.ActionType.STATUS_CHANGE AND h.newValue = 'TREATED' AND (CAST(:from AS timestamp) IS NULL OR h.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR h.createdAt <= :to) AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory hx WHERE hx.incident = i AND hx.userId = :userId))"
  )
  List<Incident> findTreatedInPeriod(
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  @Query(
    "SELECT DISTINCT i FROM Incident i JOIN i.history h WHERE h.action = com.fintrack.incident.model.constant.ActionType.STATUS_CHANGE AND h.newValue = 'RESOLVED' AND (CAST(:from AS timestamp) IS NULL OR h.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR h.createdAt <= :to) AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory hx WHERE hx.incident = i AND hx.userId = :userId))"
  )
  List<Incident> findResolvedInPeriod(
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  @Query(
    "SELECT i FROM Incident i WHERE i.closedAt IS NOT NULL AND (CAST(:from AS timestamp) IS NULL OR i.closedAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR i.closedAt <= :to) AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.userId = :userId))"
  )
  List<Incident> findClosedInPeriod(
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Snapshot SLA en depassement maintenant : ouverts dont due_date < aujourd'hui (hors filtre periode).
  @Query(
    "SELECT COUNT(i) FROM Incident i WHERE i.status NOT IN :excludedStatuses AND i.dueDate < :today AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.userId = :userId))"
  )
  long countSlaBreachNow(
    @Param("excludedStatuses") Collection<IncidentStatus> excludedStatuses,
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("today") LocalDateTime today
  );

  // Charge active par personne (workload) : incidents actifs groupes par assigne (snapshot).
  @Query(
    "SELECT i.assignedTo, COUNT(i) FROM Incident i WHERE i.assignedTo IS NOT NULL AND i.status IN :openStatuses AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) GROUP BY i.assignedTo ORDER BY COUNT(i) DESC"
  )
  List<Object[]> findWorkloadScoped(
    @Param("openStatuses") Collection<IncidentStatus> openStatuses,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId
  );

  // Un incident annule n'est plus une sortie a expliquer : il a sa propre issue.
  @Query(
    "SELECT COUNT(DISTINCT i.id) FROM Incident i WHERE i.status = com.fintrack.incident.model.constant.IncidentStatus.CANCELLED AND (CAST(:from AS timestamp) IS NULL OR i.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR i.createdAt <= :to) AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.userId = :userId))"
  )
  long countCohortCancelled(
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Sorties ventilees par statut terminal, comptees par evenement et non par incident.
  @Query(
    "SELECT h.newValue, COUNT(h) FROM IncidentHistory h JOIN h.incident i WHERE h.action = com.fintrack.incident.model.constant.ActionType.STATUS_CHANGE AND h.newValue IN :terminalStatuses AND (CAST(:from AS timestamp) IS NULL OR h.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR h.createdAt <= :to) AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory hx WHERE hx.incident = i AND hx.userId = :userId)) GROUP BY h.newValue"
  )
  List<Object[]> findTerminalExitsByStatusInPeriod(
    @Param("terminalStatuses") Collection<String> terminalStatuses,
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Retours dans le stock : une reouverture depuis un statut terminal remet a traiter.
  @Query(
    "SELECT COUNT(h) FROM IncidentHistory h JOIN h.incident i WHERE h.action = com.fintrack.incident.model.constant.ActionType.REOPENING AND h.oldValue IN :terminalStatuses AND (CAST(:from AS timestamp) IS NULL OR h.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR h.createdAt <= :to) AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory hx WHERE hx.incident = i AND hx.userId = :userId))"
  )
  long countBacklogReEntriesInPeriod(
    @Param("terminalStatuses") Collection<String> terminalStatuses,
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Annulations prononcees sur la periode (convention sortie, comme les rejets).
  @Query(
    "SELECT COUNT(DISTINCT i.id) FROM Incident i JOIN i.history h WHERE h.action = com.fintrack.incident.model.constant.ActionType.STATUS_CHANGE AND h.newValue = 'CANCELLED' AND (CAST(:from AS timestamp) IS NULL OR h.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR h.createdAt <= :to) AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory hx WHERE hx.incident = i AND hx.userId = :userId))"
  )
  long countCancelledInPeriod(
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Cohorte crees sur la periode, ventiles par issue actuelle (decision 1: total = tous crees).
  @Query(
    "SELECT COUNT(i) FROM Incident i WHERE i.status = com.fintrack.incident.model.constant.IncidentStatus.CLOSED AND i.closedAt <= i.initialDueDate AND (CAST(:from AS timestamp) IS NULL OR i.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR i.createdAt <= :to) AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.userId = :userId))"
  )
  long countCohortClosedOnTime(
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Calcule le nombre de incidents pour cohort cloture late.

  @Query(
    "SELECT COUNT(i) FROM Incident i WHERE i.status = com.fintrack.incident.model.constant.IncidentStatus.CLOSED AND i.closedAt > i.initialDueDate AND (CAST(:from AS timestamp) IS NULL OR i.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR i.createdAt <= :to) AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.userId = :userId))"
  )
  long countCohortClosedLate(
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Calcule le nombre de incidents pour cohort open late.

  @Query(
    "SELECT COUNT(i) FROM Incident i WHERE i.status NOT IN :excludedStatuses AND i.dueDate < :today AND (CAST(:from AS timestamp) IS NULL OR i.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR i.createdAt <= :to) AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.userId = :userId))"
  )
  long countCohortOpenLate(
    @Param("excludedStatuses") Collection<IncidentStatus> excludedStatuses,
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("today") LocalDateTime today,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Compte les incidents de la cohorte dont l'etat actuel est rejete.
  @Query(
    "SELECT COUNT(i) FROM Incident i WHERE i.status = com.fintrack.incident.model.constant.IncidentStatus.REJECTED AND (CAST(:from AS timestamp) IS NULL OR i.createdAt >= :from) AND (CAST(:to AS timestamp) IS NULL OR i.createdAt <= :to) AND (:agencyId IS NULL OR i.agencyId = :agencyId) AND (:serviceId IS NULL OR i.creatorServiceId = :serviceId OR i.transferredToService = :serviceId) AND (:userId IS NULL OR i.createdBy = :userId OR i.assignedTo = :userId OR EXISTS (SELECT 1 FROM IncidentHistory h WHERE h.incident = i AND h.userId = :userId))"
  )
  long countCohortRejected(
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Effectif, clotures, percentiles de completion et anciennete des encore-ouverts.
  @Query(value = COHORT_COMPLETION_SQL, nativeQuery = true)
  List<Object[]> findCohortCompletionScoped(
    @Param("userId") UUID userId,
    @Param("agencyId") UUID agencyId,
    @Param("serviceId") UUID serviceId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to,
    @Param("now") LocalDateTime now
  );
}
