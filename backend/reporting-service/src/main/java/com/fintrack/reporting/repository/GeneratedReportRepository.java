// Acces aux donnees : expose les requetes persistantes liees aux rapports generes.

package com.fintrack.reporting.repository;

import com.fintrack.reporting.model.entity.GeneratedReport;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

// Repository d'acces aux rapports generes, avec support des specifications de requete dynamiques
@Repository
public interface GeneratedReportRepository
  extends
    JpaRepository<GeneratedReport, UUID>,
    JpaSpecificationExecutor<GeneratedReport> {
  // Supprime en masse les rapports dont la duree de retention est depassee.
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("delete from GeneratedReport report where report.createdAt < :cutoff")
  int deleteCreatedBefore(@Param("cutoff") LocalDateTime cutoff);
}
