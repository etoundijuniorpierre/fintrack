// Acces aux donnees : expose les requetes persistantes liees a incident history.

package com.fintrack.incident.repository;

import com.fintrack.incident.model.constant.ActionType;
import com.fintrack.incident.model.entity.IncidentHistory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

// Repertoire Spring Data JPA pour l'acces a l'historique des incidents.

@Repository
public interface IncidentHistoryRepository
  extends JpaRepository<IncidentHistory, UUID>
{
  // Recherche les incidents par incident identifiant order by creation at asc.

  List<IncidentHistory> findByIncidentIdOrderByCreatedAtAsc(UUID incidentId);

  // Charge en une requete les utilisateurs intervenus sur une page d'incidents.
  @Query(
    "SELECT h.incident.id, h.userId FROM IncidentHistory h " +
      "WHERE h.incident.id IN :incidentIds"
  )
  List<Object[]> findParticipantRowsByIncidentIds(
    @Param("incidentIds") Set<UUID> incidentIds
  );

  // Nombre d'incidents distincts sur lesquels l'utilisateur a fait cette action.
  @Query(
    "SELECT COUNT(DISTINCT h.incident.id) FROM IncidentHistory h " +
      "WHERE h.action = :action AND h.userId = :userId"
  )
  // Calcule le nombre de incidents pour distinct incidents by action and utilisateur.
  long countDistinctIncidentsByActionAndUser(
    @Param("action") ActionType action,
    @Param("userId") UUID userId
  );

  @Query(
    "SELECT COUNT(DISTINCT h.incident.id) FROM IncidentHistory h " +
      "WHERE h.action = :action AND h.userId = :userId " +
      "AND (CAST(:from AS timestamp) IS NULL OR h.createdAt >= :from) " +
      "AND (CAST(:to AS timestamp) IS NULL OR h.createdAt <= :to)"
  )
  // Calcule le nombre de incidents pour distinct incidents by action and utilisateur periode.
  long countDistinctIncidentsByActionAndUserBetween(
    @Param("action") ActionType action,
    @Param("userId") UUID userId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Compte les incidents transferes en incluant les anciennes actions de routage.
  @Query(
    "SELECT COUNT(DISTINCT h.incident.id) FROM IncidentHistory h " +
      "WHERE h.action IN :actions AND h.userId = :userId " +
      "AND (CAST(:from AS timestamp) IS NULL OR h.createdAt >= :from) " +
      "AND (CAST(:to AS timestamp) IS NULL OR h.createdAt <= :to)"
  )
  long countDistinctIncidentsByActionsAndUserBetween(
    @Param("actions") List<ActionType> actions,
    @Param("userId") UUID userId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  @Query(
    "SELECT COUNT(DISTINCT h.incident.id) FROM IncidentHistory h " +
      "WHERE h.action = :action AND h.newValue = :newValue AND h.userId = :userId " +
      "AND (CAST(:from AS timestamp) IS NULL OR h.createdAt >= :from) " +
      "AND (CAST(:to AS timestamp) IS NULL OR h.createdAt <= :to)"
  )
  // Calcule le nombre de incidents pour distinct incidents by action and new value and utilisateur periode.
  long countDistinctIncidentsByActionAndNewValueAndUserBetween(
    @Param("action") ActionType action,
    @Param("newValue") String newValue,
    @Param("userId") UUID userId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );

  // Lot 4: Recent Activities
  @Query(
    "SELECT h FROM IncidentHistory h WHERE h.incident.createdBy = :userId OR h.userId = :userId ORDER BY h.createdAt DESC"
  )
  List<IncidentHistory> findRecentForUser(
    @Param("userId") UUID userId,
    Pageable pageable
  );

  // Selectionne les incidents recents pour recent for agence.

  @Query(
    "SELECT h FROM IncidentHistory h WHERE h.incident.agencyId = :agencyId ORDER BY h.createdAt DESC"
  )
  List<IncidentHistory> findRecentForAgency(
    @Param("agencyId") UUID agencyId,
    Pageable pageable
  );

  // Selectionne les incidents recents pour recent for service.

  @Query(
    "SELECT h FROM IncidentHistory h WHERE h.incident.creatorServiceId = :serviceId OR h.incident.transferredToService = :serviceId ORDER BY h.createdAt DESC"
  )
  List<IncidentHistory> findRecentForService(
    @Param("serviceId") UUID serviceId,
    Pageable pageable
  );
}
