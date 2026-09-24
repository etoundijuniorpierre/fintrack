// Contrat metier : expose les operations du domaine planification de rapport.

package com.fintrack.reporting.service;

import com.fintrack.reporting.model.entity.ReportSchedule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// Contrat du service de gestion des planifications de rapports
public interface ReportScheduleService {
  // Liste les planifications de rapport selon le perimetre demande.
  Page<ReportSchedule> findAll(Pageable pageable);
  // Liste les planifications de rapport selon le perimetre demande.
  List<ReportSchedule> findAll();
  // Recherche les planifications de rapport par identifiant.
  ReportSchedule findById(UUID id);
  // Recherche les planifications de rapport par creation by.
  List<ReportSchedule> findByCreatedBy(UUID createdBy);
  // Recherche les planifications de rapport par creation by avec pagination.
  Page<ReportSchedule> findByCreatedBy(UUID createdBy, Pageable pageable);
  // Recherche les planifications de rapport pour actif.
  List<ReportSchedule> findActive();
  // Recherche les planifications de rapport pour actif by creation by.
  List<ReportSchedule> findActiveByCreatedBy(UUID createdBy);
  // Prepare l'enregistrement de la ressource selon les regles metier.
  ReportSchedule create(ReportSchedule schedule, UUID createdBy);
  // Applique le changement demande apres validation metier.
  ReportSchedule update(UUID id, ReportSchedule schedule);
  // Applique le changement demande apres validation metier.
  ReportSchedule toggleActive(UUID id);
  // Supprime ou invalide les donnees ciblees apres controle metier.
  void delete(UUID id);
}
