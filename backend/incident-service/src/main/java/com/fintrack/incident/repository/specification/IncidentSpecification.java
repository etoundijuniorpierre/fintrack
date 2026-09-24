// Acces aux donnees : expose les requetes persistantes liees a incident specification.

package com.fintrack.incident.repository.specification;

import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentHistory;
import com.fintrack.incident.model.entity.IncidentSearchCriteria;
import com.fintrack.incident.security.UserDetailsImpl;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

// Specification JPA pour la recherche et le filtrage dynamique des incidents.

public class IncidentSpecification {

  // Statuts qui sortent de la file de travail de l'assigne. Derive de la reference
  // unique : enumerer CLOTURE et REJETE a la main laissait les incidents ANNULES dans
  // "Assignes a moi", alors que le compteur du tableau de bord les avait deja retires --
  // la liste et son compteur annoncaient deux nombres differents.
  private static final Set<IncidentStatus> NON_ACTIVE_ASSIGNED_STATUSES =
    IncidentStatus.TERMINAL_STATUSES;

  // Fournit filter specification a la couche appelante.

  public static Specification<Incident> getFilterSpecification(
    IncidentSearchCriteria request,
    UserDetailsImpl currentUser
  ) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      if ("own".equalsIgnoreCase(request.getView())) {
        predicates.add(cb.equal(root.get("createdBy"), currentUser.getId()));
      } else if ("assigned".equalsIgnoreCase(request.getView())) {
        predicates.add(cb.equal(root.get("assignedTo"), currentUser.getId()));
        // On exclut les incidents clotures / rejetes de la vue "a traiter".
        // Sans ce filtre, ils continueraient a apparaitre dans la file de
        // l'agent qui s'en est occupe, alors qu'ils sont deja resolus.
        predicates.add(
          cb.not(root.get("status").in(NON_ACTIVE_ASSIGNED_STATUSES))
        );
      } else if ("agency".equalsIgnoreCase(request.getView())) {
        // Sans agence rattachee, aucun incident ne doit remonter (fail-closed)
        // plutot que de generer un "= NULL" au comportement imprevisible.
        if (currentUser.getAgencyId() == null) {
          predicates.add(cb.disjunction());
        } else {
          predicates.add(
            cb.equal(root.get("agencyId"), currentUser.getAgencyId())
          );
        }
      } else if ("service".equalsIgnoreCase(request.getView())) {
        if (currentUser.getServiceId() == null) {
          predicates.add(cb.disjunction());
        } else {
          // Coherent avec assertCanViewIncident : le chef de service voit
          // les incidents crees dans son service OU transferes vers lui.
          predicates.add(
            cb.or(
              cb.equal(
                root.get("creatorServiceId"),
                currentUser.getServiceId()
              ),
              cb.equal(
                root.get("transferredToService"),
                currentUser.getServiceId()
              )
            )
          );
        }
      }

      if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
        predicates.add(root.get("status").in(request.getStatuses()));
      }
      if (request.getTypeIds() != null && !request.getTypeIds().isEmpty()) {
        predicates.add(root.get("typeId").in(request.getTypeIds()));
      }
      if (
        request.getCriticalities() != null &&
        !request.getCriticalities().isEmpty()
      ) {
        predicates.add(root.get("criticality").in(request.getCriticalities()));
      }
      if (request.getAssignedTo() != null) {
        predicates.add(
          cb.equal(root.get("assignedTo"), request.getAssignedTo())
        );
      }
      if (Boolean.TRUE.equals(request.getUnassignedOnly())) {
        predicates.add(cb.isNull(root.get("assignedTo")));
      }
      // Qualite des donnees : incidents sans agence (lien Super Admin).
      if (Boolean.TRUE.equals(request.getMissingAgency())) {
        predicates.add(cb.isNull(root.get("agencyId")));
      }
      // Qualite des donnees : incidents sans service (ni createur, ni cible).
      if (Boolean.TRUE.equals(request.getMissingService())) {
        predicates.add(
          cb.and(
            cb.isNull(root.get("creatorServiceId")),
            cb.isNull(root.get("transferredToService"))
          )
        );
      }
      // Qualite des donnees : incidents assignes a un utilisateur inactif.
      // Les IDs inactifs sont resolus en amont (service). Aucun inactif => aucun resultat.
      if (Boolean.TRUE.equals(request.getAssignedToInactive())) {
        if (
          request.getInactiveAssigneeIds() == null ||
          request.getInactiveAssigneeIds().isEmpty()
        ) {
          predicates.add(cb.disjunction());
        } else {
          predicates.add(
            root.get("assignedTo").in(request.getInactiveAssigneeIds())
          );
        }
      }
      if (request.getCreatedBy() != null) {
        predicates.add(cb.equal(root.get("createdBy"), request.getCreatedBy()));
      }
      // Utilisateur cible d'un rapport : incidents qu'il a crees, qui lui sont
      // assignes, OU dans lesquels il est intervenu (historique). Union alignee
      // sur le perimetre "own" : capte aussi les incidents resolus / clotures par
      // lui meme apres reassignation a un autre agent.
      // Ensembles effectifs : le champ singulier et sa variante multi-valeurs
      // sont combines (un singleton se comporte exactement comme l'ancien equal).
      Set<UUID> subjects = effectiveIds(
        request.getSubjectUserId(),
        request.getSubjectUserIds()
      );
      Set<UUID> agencies = effectiveIds(
        request.getAgencyId(),
        request.getAgencyIds()
      );
      Set<UUID> services = effectiveIds(
        request.getServiceId(),
        request.getServiceIds()
      );
      if (!subjects.isEmpty()) {
        Subquery<Integer> touchedInHistory = query.subquery(Integer.class);
        Root<IncidentHistory> history = touchedInHistory.from(
          IncidentHistory.class
        );
        touchedInHistory.select(cb.literal(1));
        touchedInHistory.where(
          cb.equal(history.get("incident"), root),
          history.get("userId").in(subjects)
        );
        predicates.add(
          cb.or(
            root.get("createdBy").in(subjects),
            root.get("assignedTo").in(subjects),
            cb.exists(touchedInHistory)
          )
        );
      }
      if (!agencies.isEmpty()) {
        predicates.add(root.get("agencyId").in(agencies));
      }
      if (!services.isEmpty()) {
        predicates.add(
          cb.or(
            root.get("creatorServiceId").in(services),
            root.get("transferredToService").in(services)
          )
        );
      }
      LocalDateTime periodFrom = request.getStartDate() != null
        ? request.getStartDate().atStartOfDay()
        : null;
      LocalDateTime periodTo = request.getEndDate() != null
        ? request.getEndDate().atTime(23, 59, 59)
        : null;
      if (!subjects.isEmpty()) {
        // Rapport par utilisateur : la periode encadre l'activite (creation,
        // resolution OU cloture) et non la seule date de creation, afin de
        // refleter "ce qu'il a traite" sur la periode.
        List<Predicate> activityInPeriod = new ArrayList<>();
        for (String dateField : List.of("createdAt", "resolvedAt", "closedAt")) {
          List<Predicate> bounds = new ArrayList<>();
          if (periodFrom != null) {
            bounds.add(cb.greaterThanOrEqualTo(root.get(dateField), periodFrom));
          }
          if (periodTo != null) {
            bounds.add(cb.lessThanOrEqualTo(root.get(dateField), periodTo));
          }
          if (!bounds.isEmpty()) {
            activityInPeriod.add(cb.and(bounds.toArray(new Predicate[0])));
          }
        }
        if (!activityInPeriod.isEmpty()) {
          predicates.add(cb.or(activityInPeriod.toArray(new Predicate[0])));
        }
      } else {
        if (periodFrom != null) {
          predicates.add(
            cb.greaterThanOrEqualTo(root.get("createdAt"), periodFrom)
          );
        }
        if (periodTo != null) {
          predicates.add(
            cb.lessThanOrEqualTo(root.get("createdAt"), periodTo)
          );
        }
      }
      if (
        request.getKeyword() != null && !request.getKeyword().trim().isEmpty()
      ) {
        String likePattern =
          "%" + request.getKeyword().trim().toLowerCase() + "%";
        // La recherche couvre code + titre + description + traitement + resolution : retrouver
        // un cas similaire deja traite (capitalisation / base de connaissance).
        predicates.add(
          cb.or(
            cb.like(cb.lower(root.get("reference")), likePattern),
            cb.like(cb.lower(root.get("title")), likePattern),
            cb.like(cb.lower(root.get("description")), likePattern),
            cb.like(cb.lower(root.get("treatmentDescription")), likePattern),
            cb.like(cb.lower(root.get("resolutionDescription")), likePattern)
          )
        );
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  // Fusionne un identifiant singulier et sa variante multi-valeurs en un ensemble
  // effectif ; un singleton reproduit exactement l'ancien filtre par egalite.
  private static Set<UUID> effectiveIds(UUID single, Set<UUID> multiple) {
    Set<UUID> ids = new HashSet<>();
    if (single != null) {
      ids.add(single);
    }
    if (multiple != null) {
      multiple.stream().filter(java.util.Objects::nonNull).forEach(ids::add);
    }
    return ids;
  }
}
