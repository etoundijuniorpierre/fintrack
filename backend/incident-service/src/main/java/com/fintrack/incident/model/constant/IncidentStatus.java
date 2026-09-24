// Constantes metier : centralise les valeurs stables liees a incident status.

package com.fintrack.incident.model.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Enumeration definissant les differents statuts possibles d'un incident.

@Getter
@RequiredArgsConstructor
public enum IncidentStatus implements LocalizableEnum {
  DRAFT(
    "DRAFT",
    "enum.incident_status.DRAFT.name",
    "enum.incident_status.DRAFT.description"
  ),
  OPEN(
    "OPEN",
    "enum.incident_status.OPEN.name",
    "enum.incident_status.OPEN.description"
  ),
  PENDING_VALIDATION(
    "PENDING_VALIDATION",
    "enum.incident_status.PENDING_VALIDATION.name",
    "enum.incident_status.PENDING_VALIDATION.description"
  ),
  VALIDATED(
    "VALIDATED",
    "enum.incident_status.VALIDATED.name",
    "enum.incident_status.VALIDATED.description"
  ),
  TRANSFERRED(
    "TRANSFERRED",
    "enum.incident_status.TRANSFERRED.name",
    "enum.incident_status.TRANSFERRED.description"
  ),
  ASSIGNED(
    "ASSIGNED",
    "enum.incident_status.ASSIGNED.name",
    "enum.incident_status.ASSIGNED.description"
  ),
  IN_PROGRESS(
    "IN_PROGRESS",
    "enum.incident_status.IN_PROGRESS.name",
    "enum.incident_status.IN_PROGRESS.description"
  ),
  TREATED(
    "TREATED",
    "enum.incident_status.TREATED.name",
    "enum.incident_status.TREATED.description"
  ),
  BLOCKED(
    "BLOCKED",
    "enum.incident_status.BLOCKED.name",
    "enum.incident_status.BLOCKED.description"
  ),
  RESOLVED(
    "RESOLVED",
    "enum.incident_status.RESOLVED.name",
    "enum.incident_status.RESOLVED.description"
  ),
  CLOSED(
    "CLOSED",
    "enum.incident_status.CLOSED.name",
    "enum.incident_status.CLOSED.description"
  ),
  REOPENED(
    "REOPENED",
    "enum.incident_status.REOPENED.name",
    "enum.incident_status.REOPENED.description"
  ),
  REJECTED(
    "REJECTED",
    "enum.incident_status.REJECTED.name",
    "enum.incident_status.REJECTED.description"
  ),
  CANCELLED(
    "CANCELLED",
    "enum.incident_status.CANCELLED.name",
    "enum.incident_status.CANCELLED.description"
  ),
  UNRESOLVED_PROLONGED_WAIT(
    "UNRESOLVED_PROLONGED_WAIT",
    "enum.incident_status.UNRESOLVED_PROLONGED_WAIT.name",
    "enum.incident_status.UNRESOLVED_PROLONGED_WAIT.description"
  );

  /** Identifiant technique stable (stocké en base, utilisé dans le code). */
  // Incident sorti du circuit : aucune echeance ne court plus, aucune relance ne part,
  // et il ne compte dans aucun indicateur de retard. Reference unique — les jobs et les
  // requetes doivent en deriver plutot que de reenumerer leur propre liste, sinon un
  // statut ajoute plus tard est oublie ici ou la (c'est ce qui est arrive a CANCELLED).
  public static final Set<IncidentStatus> TERMINAL_STATUSES = Collections.unmodifiableSet(
    EnumSet.of(CLOSED, REJECTED, CANCELLED)
  );

  // Incidents encore en circuit : le complement exact des statuts terminaux, pour
  // qu'un statut ajoute plus tard y entre par defaut au lieu d'etre oublie. DRAFT en
  // fait partie : ce n'est pas un brouillon de declaration mais une solution proposee
  // en attente de la Direction, au meme titre que PENDING_VALIDATION attend un
  // valideur (cf. AWAITING_VALIDATION_STATUSES). Reference unique de la notion
  // « actif » : le stock en cours, la charge par personne et les incidents assignes
  // doivent en deriver, sinon chacun compte une population differente (CANCELLED a
  // longtemps compte comme assigne).
  public static final Set<IncidentStatus> OPEN_STATUSES = Collections.unmodifiableSet(
    EnumSet.complementOf(EnumSet.copyOf(TERMINAL_STATUSES))
  );

  // Statuts ou le delai de TRAITEMENT ne court plus : l'incident est sorti du circuit,
  // ou le traitant a livre et la main est passee au valideur puis a la cloture.
  public static final Set<IncidentStatus> SLA_CLOCK_STOPPED_STATUSES = Collections.unmodifiableSet(
    EnumSet.of(CLOSED, REJECTED, CANCELLED, RESOLVED, TREATED)
  );

  // Une decision de validation est attendue : le delai de traitement ne court pas.
  // OPEN n'en est pas — il signale une validation NON requise.
  public static final Set<IncidentStatus> AWAITING_VALIDATION_STATUSES = Collections.unmodifiableSet(
    EnumSet.of(PENDING_VALIDATION, DRAFT)
  );

  // Le traitement est livre, une decision est attendue : resolution puis cloture.
  public static final Set<IncidentStatus> AWAITING_ACTION_STATUSES = Collections.unmodifiableSet(
    EnumSet.of(TREATED, RESOLVED)
  );

  // Ne peut pas etre "en retard" : horloge de traitement arretee, ou decision de
  // validation attendue — le delai du traitant n'a pas commence.
  public static final Set<IncidentStatus> NOT_LATE_STATUSES = Collections.unmodifiableSet(
    EnumSet.copyOf(
      Stream.concat(
        SLA_CLOCK_STOPPED_STATUSES.stream(),
        AWAITING_VALIDATION_STATUSES.stream()
      ).toList()
    )
  );

  private final String name;
  /** Clé i18n pour le libellé affiché à l'utilisateur. */
  private final String nameKey;
  /** Clé i18n pour la description affichée à l'utilisateur. */
  private final String descriptionKey;

  // Fournit all names a la couche appelante.

  public static String[] getAllNames() {
    return Arrays.stream(values())
      .map(IncidentStatus::getName)
      .toArray(String[]::new);
  }

  // Noms techniques : l historique compare des chaines, pas le statut courant.
  public static Set<String> namesOf(Set<IncidentStatus> statuses) {
    return statuses
      .stream()
      .map(IncidentStatus::getName)
      .collect(Collectors.toUnmodifiableSet());
  }
}
