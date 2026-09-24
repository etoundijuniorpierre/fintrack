// Service metier : attribue les references lisibles des incidents.

package com.fintrack.incident.service;

import com.fintrack.incident.repository.IncidentReferenceSequenceRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reference au format {@code FT-I-2026-0001} : FinTrack, Incident, annee, numero.
 *
 * <p>L'annee precede le numero, donc le tri alphabetique de la reference reproduit
 * exactement l'ordre chronologique d'attribution, sans dependre d'un champ annexe.
 * Le compteur repart a 1 chaque annee.
 */
@Service
@RequiredArgsConstructor
public class IncidentReferenceGenerator {

  private static final String PREFIX = "FT-I";
  /** 4 chiffres suffisent : le compteur est remis a zero chaque annee. */
  private static final String NUMBER_FORMAT = "%04d";

  private final IncidentReferenceSequenceRepository sequenceRepository;

  /**
   * Alloue la reference suivante. La transaction est independante : le numero reste
   * consomme meme si la creation de l'incident echoue ensuite, ce qui garantit
   * l'unicite au prix d'un trou dans la numerotation.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public String next() {
    int year = LocalDate.now().getYear();
    return format(year, sequenceRepository.allocateNext(year));
  }

  // Compose la reference affichable a partir de l'annee et du numero.
  public String format(int year, long number) {
    return PREFIX + "-" + year + "-" + String.format(NUMBER_FORMAT, number);
  }
}
