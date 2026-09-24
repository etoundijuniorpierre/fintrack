// Contrat metier : expose les operations du domaine incident history.

package com.fintrack.incident.service;

import com.fintrack.incident.model.constant.ActionType;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentHistory;
import java.util.List;
import java.util.UUID;

// Interface du service de gestion de l'historique des incidents.

public interface IncidentHistoryService {
  // Recherche les incidents par incident identifiant.
  List<IncidentHistory> findByIncidentId(UUID incidentId);
  // Realise l'intention metier record.
  IncidentHistory record(
    Incident incident,
    UUID userId,
    ActionType action,
    String oldValue,
    String newValue,
    String comment
  );
}
