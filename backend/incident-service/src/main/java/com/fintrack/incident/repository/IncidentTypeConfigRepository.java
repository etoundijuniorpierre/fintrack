// Acces aux donnees : expose les requetes persistantes liees a incident type config.

package com.fintrack.incident.repository;

import com.fintrack.incident.model.entity.IncidentTypeConfig;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Repertoire Spring Data JPA pour la configuration des types d'incidents.

@Repository
public interface IncidentTypeConfigRepository
  extends JpaRepository<IncidentTypeConfig, UUID>
{
  // Recherche les types incident par nom.

  Optional<IncidentTypeConfig> findByName(String name);
  // Verifie l'existence de name.

  boolean existsByName(String name);
  // Liste les elements du domaine incident type configuration.

  List<IncidentTypeConfig> findAll();
}
