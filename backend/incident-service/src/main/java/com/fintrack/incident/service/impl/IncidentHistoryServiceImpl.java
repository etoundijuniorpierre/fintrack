// Service metier : coordonne les operations du domaine incident history.

package com.fintrack.incident.service.impl;

import com.fintrack.incident.model.constant.ActionType;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentHistory;
import com.fintrack.incident.repository.IncidentHistoryRepository;
import com.fintrack.incident.service.IncidentHistoryService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Implementation du service d'historisation des actions sur les incidents.

@Service
@RequiredArgsConstructor
public class IncidentHistoryServiceImpl implements IncidentHistoryService {

  private final IncidentHistoryRepository historyRepository;

  @Override
  @Transactional(readOnly = true)
  // Recherche les incidents par incident identifiant.
  public List<IncidentHistory> findByIncidentId(UUID incidentId) {
    return historyRepository.findByIncidentIdOrderByCreatedAtAsc(incidentId);
  }

  @Override
  @Transactional
  @Caching(
    evict = {
      @CacheEvict(value = "dashboardMetrics", allEntries = true),
      @CacheEvict(value = "dashboardComparisons", allEntries = true),
    }
  )
  // Enregistre une entree d'historique pour tracer l'evolution de l'incident.
  public IncidentHistory record(
    Incident incident,
    UUID userId,
    ActionType action,
    String oldValue,
    String newValue,
    String comment
  ) {
    IncidentHistory history = new IncidentHistory();
    history.setIncident(incident);
    history.setUserId(userId);
    history.setAction(action);
    history.setOldValue(oldValue);
    history.setNewValue(newValue);
    history.setComment(comment);
    return historyRepository.save(history);
  }
}
