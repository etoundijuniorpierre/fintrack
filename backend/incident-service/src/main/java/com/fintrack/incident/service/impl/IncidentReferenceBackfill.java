// Rattrapage : attribue une reference aux incidents anterieurs a la fonctionnalite.

package com.fintrack.incident.service.impl;

import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.repository.IncidentRepository;
import com.fintrack.incident.repository.IncidentReferenceSequenceRepository;
import com.fintrack.incident.service.IncidentReferenceGenerator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Les incidents crees avant l'introduction de la reference n'en ont aucune. On leur en
 * attribue une au demarrage, dans l'ordre de creation, pour que la numerotation reflete
 * la chronologie reelle.
 *
 * <p>Idempotent : seuls les incidents sans reference sont touches, donc les demarrages
 * suivants ne font rien.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentReferenceBackfill implements ApplicationRunner {

  private final IncidentRepository incidentRepository;
  private final IncidentReferenceSequenceRepository sequenceRepository;
  private final IncidentReferenceGenerator referenceGenerator;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    List<Incident> pending = incidentRepository.findByReferenceIsNullOrderByCreatedAtAsc();
    if (pending.isEmpty()) {
      return;
    }

    log.info("Attribution d'une référence à {} incident(s) existant(s).", pending.size());
    for (Incident incident : pending) {
      // L'annee de creation, pas l'annee courante : la reference doit rester coherente
      // avec la date de l'incident.
      int year = incident.getCreatedAt() != null
        ? incident.getCreatedAt().getYear()
        : java.time.LocalDate.now().getYear();
      incident.setReference(
        referenceGenerator.format(year, sequenceRepository.allocateNext(year))
      );
    }
    incidentRepository.saveAll(pending);
  }
}
