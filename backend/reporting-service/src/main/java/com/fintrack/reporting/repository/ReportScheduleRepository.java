// Acces aux donnees : expose les requetes persistantes liees a report schedule.

package com.fintrack.reporting.repository;

import com.fintrack.reporting.model.entity.ReportSchedule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Repository d'acces aux planifications de rapports
@Repository
public interface ReportScheduleRepository
  extends JpaRepository<ReportSchedule, UUID>
{
  // Recherche les planifications de rapport par creation by.
  List<ReportSchedule> findByCreatedBy(UUID createdBy);
  // Recherche les planifications de rapport par creation by avec pagination.
  Page<ReportSchedule> findByCreatedBy(UUID createdBy, Pageable pageable);
  // Recherche toutes les planifications actives
  List<ReportSchedule> findByIsActiveTrue();
  // Recherche les planifications de rapport par creation by and is actif true.
  List<ReportSchedule> findByCreatedByAndIsActiveTrue(UUID createdBy);
}
