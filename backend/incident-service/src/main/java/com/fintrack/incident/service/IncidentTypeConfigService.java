// Contrat metier : expose les operations du domaine incident type configuration.

package com.fintrack.incident.service;

import com.fintrack.incident.model.entity.IncidentTypeConfig;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// Interface du service de configuration des types d'incidents.

public interface IncidentTypeConfigService {
  // Liste les types d'incident selon le perimetre demande.
  Page<IncidentTypeConfig> findAll(Pageable pageable);
  // Liste les types d'incident selon le perimetre demande.
  List<IncidentTypeConfig> findAll();
  // Recherche les types incident par identifiant.
  IncidentTypeConfig findById(UUID id);
  // Recherche les types incident par nom.
  IncidentTypeConfig findByName(String name);
  // Prepare l'enregistrement de la ressource selon les regles metier.
  IncidentTypeConfig create(IncidentTypeConfig typeConfig, UUID actorId);
  // Applique le changement demande apres validation metier.
  IncidentTypeConfig update(
    UUID id,
    IncidentTypeConfig typeConfig,
    UUID actorId
  );
  // Supprime ou invalide les donnees ciblees apres controle metier.
  void delete(UUID id, UUID actorId);
}
