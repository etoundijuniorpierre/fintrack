// Constantes metier : miroir, cote reporting, des ensembles de statuts d'incident.

package com.fintrack.reporting.model.constant;

import java.util.Set;

// reporting-service ne connait les statuts que par leur nom, transmis par Feign : il ne
// peut pas importer l'enum d'incident-service. Ces ensembles en sont le miroir, declares
// UNE fois. Les reenumerer sur place est ce qui avait laisse ANNULE compter comme un
// incident actif ici, alors qu'incident-service l'avait deja retire de ses compteurs.
public final class IncidentStatusSets {

  // Sorti du circuit : ne compte dans aucun stock, ne peut pas etre en retard.
  // Miroir de IncidentStatus.TERMINAL_STATUSES.
  public static final Set<String> TERMINAL = Set.of(
    "CLOSED",
    "REJECTED",
    "CANCELLED"
  );

  // Statuts ou le delai de traitement court encore : seuls ceux-la peuvent etre "en
  // retard". Complement exact de IncidentStatus.NOT_LATE_STATUSES cote incident-service.
  // TRAITE et RESOLU en sont absents (le traitant a livre), les attentes de validation
  // aussi (le delai du traitant n'a pas commence).
  public static final Set<String> CLOCK_RUNNING = Set.of(
    "OPEN",
    "VALIDATED",
    "TRANSFERRED",
    "ASSIGNED",
    "IN_PROGRESS",
    "BLOCKED",
    "REOPENED",
    "UNRESOLVED_PROLONGED_WAIT"
  );

  private IncidentStatusSets() {}
}
