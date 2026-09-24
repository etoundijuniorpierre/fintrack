// Client inter-services : communique avec les services externes lies a user client.

package com.fintrack.incident.client.user;

import com.fintrack.incident.client.user.dto.AgencyClientResponse;
import com.fintrack.incident.client.user.dto.ServiceClientResponse;
import com.fintrack.incident.client.user.dto.UserClientResponse;
import com.fintrack.incident.model.dto.response.AgencySummaryResponse;
import com.fintrack.incident.model.dto.response.ServiceSummaryResponse;
import com.fintrack.incident.model.dto.response.UserSummaryResponse;
import com.fintrack.incident.model.readmodel.ExternalAgency;
import com.fintrack.incident.model.readmodel.ExternalService;
import com.fintrack.incident.model.readmodel.ExternalUser;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
// Service qui appelle le service utilisateur et met en cache les resultats, avec repli sur des valeurs inconnu en cas d'echec
public class UserClientService {

  private final UserClient userClient;
  private final CacheManager cacheManager;

  // Préchauffe le cache avec une liste d'utilisateurs pour éviter le N+1
  public void prefetchUsers(Set<UUID> ids) {
    if (ids == null || ids.isEmpty()) return;
    Cache cache = cacheManager.getCache("users");
    if (cache == null) return;

    Set<UUID> missingIds = ids
      .stream()
      .filter(id -> cache.get(id) == null)
      .collect(Collectors.toSet());

    if (missingIds.isEmpty()) return;

    try {
      List<UserClientResponse> fetched = userClient.getUsersByIds(missingIds);
      for (UserClientResponse r : fetched) {
        UserSummaryResponse summary = new UserSummaryResponse();
        summary.setId(r.getId());
        summary.setUsername(
          r.getUsername() != null ? r.getUsername() : "unknown"
        );
        summary.setFirstName(r.getFirstName());
        summary.setLastName(r.getLastName());
        summary.setEmail(r.getEmail());
        summary.setActive(r.isActive());
        summary.setRoles(r.getRoles() != null ? r.getRoles() : Set.of());
        summary.setPermissions(
          r.getPermissions() != null ? r.getPermissions() : Set.of()
        );
        cache.put(r.getId(), summary);
      }
      // Mettre un fallback pour les IDs introuvables pour éviter de recommencer la requête à chaque ligne
      for (UUID id : missingIds) {
        if (cache.get(id) == null) {
          cache.put(id, unknownUser(id));
        }
      }
    } catch (Exception ex) {
      log.warn(
        "Impossible de pré-charger les utilisateurs depuis user-service",
        ex
      );
    }
  }

  // Prechauffe les agences liees aux incidents afin d'eviter les appels Feign par ligne.
  public void prefetchAgencies(Set<UUID> ids) {
    if (ids == null || ids.isEmpty()) return;
    Cache cache = cacheManager.getCache("agencies");
    if (cache == null) return;

    Set<UUID> missingIds = ids
      .stream()
      .filter(id -> id != null && cache.get(id) == null)
      .collect(Collectors.toSet());

    if (missingIds.isEmpty()) return;

    try {
      List<AgencyClientResponse> fetched = userClient.getAgenciesByIds(
        missingIds
      );
      for (AgencyClientResponse r : fetched) {
        if (r != null && r.getId() != null) {
          cache.put(r.getId(), toAgencySummary(r));
        }
      }
      for (UUID id : missingIds) {
        if (cache.get(id) == null) {
          cache.put(id, unknownAgency(id));
        }
      }
    } catch (Exception ex) {
      log.warn("Impossible de pre-charger les agences depuis user-service", ex);
    }
  }

  // Prechauffe les services lies aux incidents afin d'eviter les appels Feign par ligne.
  public void prefetchServices(Set<UUID> ids) {
    if (ids == null || ids.isEmpty()) return;
    Cache cache = cacheManager.getCache("services");
    if (cache == null) return;

    Set<UUID> missingIds = ids
      .stream()
      .filter(id -> id != null && cache.get(id) == null)
      .collect(Collectors.toSet());

    if (missingIds.isEmpty()) return;

    try {
      List<ServiceClientResponse> fetched = userClient.getServicesByIds(
        missingIds
      );
      for (ServiceClientResponse r : fetched) {
        if (r != null && r.getId() != null) {
          cache.put(r.getId(), toServiceSummary(r));
        }
      }
      for (UUID id : missingIds) {
        if (cache.get(id) == null) {
          cache.put(id, unknownService(id));
        }
      }
    } catch (Exception ex) {
      log.warn(
        "Impossible de pre-charger les services depuis user-service",
        ex
      );
    }
  }

  // IDs des utilisateurs inactifs ; liste vide si user-service est injoignable.
  public List<UUID> getInactiveUserIds() {
    try {
      List<UUID> ids = userClient.getInactiveUserIds();
      return ids != null ? ids : List.of();
    } catch (RuntimeException ex) {
      log.warn(
        "Impossible de récupérer les utilisateurs inactifs depuis user-service",
        ex
      );
      return List.of();
    }
  }

  // Resume un utilisateur (mis en cache) ; renvoie un utilisateur inconnu si le service est injoignable
  @Cacheable(
    value = "users",
    key = "#id",
    condition = "#id != null",
    unless = "#result == null"
  )
  public UserSummaryResponse resolveUser(UUID id) {
    if (id == null) return null;
    UserClientResponse r;
    try {
      r = userClient.getUserById(id);
    } catch (RuntimeException ex) {
      log.warn(
        "Impossible de résoudre l'utilisateur {} depuis user-service",
        id,
        ex
      );
      return unknownUser(id);
    }
    UserSummaryResponse summary = new UserSummaryResponse();
    summary.setId(r.getId());
    summary.setUsername(r.getUsername() != null ? r.getUsername() : "unknown");
    summary.setFirstName(r.getFirstName());
    summary.setLastName(r.getLastName());
    summary.setEmail(r.getEmail());
    summary.setActive(r.isActive());
    summary.setRoles(r.getRoles() != null ? r.getRoles() : Set.of());
    summary.setPermissions(
      r.getPermissions() != null ? r.getPermissions() : Set.of()
    );
    return summary;
  }

  // Recupere les details complets d'un utilisateur (mis en cache)
  @Cacheable(
    value = "userDetails",
    key = "#id",
    condition = "#id != null",
    unless = "#result == null"
  )
  public ExternalUser getUser(UUID id) {
    if (id == null) return null;
    return toExternalUser(userClient.getUserById(id));
  }

  // Resume une agence (mise en cache) ; renvoie une agence inconnu si le service est injoignable
  @Cacheable(
    value = "agencies",
    key = "#id",
    condition = "#id != null",
    unless = "#result == null"
  )
  public AgencySummaryResponse resolveAgency(UUID id) {
    if (id == null) return null;
    AgencyClientResponse r;
    try {
      r = userClient.getAgencyById(id);
    } catch (RuntimeException ex) {
      log.warn(
        "Impossible de résoudre l'agence {} depuis user-service",
        id,
        ex
      );
      return unknownAgency(id);
    }
    return toAgencySummary(r);
  }

  // Recupere les details complets d'une agence (mis en cache)
  @Cacheable(
    value = "agencyDetails",
    key = "#id",
    condition = "#id != null",
    unless = "#result == null"
  )
  public ExternalAgency getAgency(UUID id) {
    if (id == null) return null;
    return toExternalAgency(userClient.getAgencyById(id));
  }

  // Resume un service (mis en cache) ; renvoie un service inconnu si le service utilisateur est injoignable
  @Cacheable(
    value = "services",
    key = "#id",
    condition = "#id != null",
    unless = "#result == null"
  )
  public ServiceSummaryResponse resolveService(UUID id) {
    if (id == null) return null;
    ServiceClientResponse r;
    try {
      r = userClient.getServiceById(id);
    } catch (RuntimeException ex) {
      log.warn(
        "Impossible de résoudre le service {} depuis user-service",
        id,
        ex
      );
      return unknownService(id);
    }
    return toServiceSummary(r);
  }

  // Recupere les details complets d'un service (mis en cache)
  @Cacheable(
    value = "serviceDetails",
    key = "#id",
    condition = "#id != null",
    unless = "#result == null"
  )
  public ExternalService getService(UUID id) {
    if (id == null) return null;
    return toExternalService(userClient.getServiceById(id));
  }

  // Resout service name a partir du contexte disponible.

  public String resolveServiceName(UUID id) {
    if (id == null) {
      return "";
    }
    ExternalService service = getService(id);
    return service != null && service.getName() != null
      ? service.getName()
      : id.toString();
  }

  // Resout agency name a partir du contexte disponible.

  public String resolveAgencyName(UUID id) {
    if (id == null) {
      return "";
    }
    ExternalAgency agency = getAgency(id);
    return agency != null && agency.getName() != null
      ? agency.getName()
      : id.toString();
  }

  /** Retourne l'identifiant du chef du service, ou null si aucun responsable n'est défini. */
  // Mis en cache comme les autres lectures d'annuaire : la portee de validation, le
  // verdict d'annulation et le valideur attendu posent tous la meme question au cours
  // d'une seule requete de detail.
  @Cacheable(
    value = "serviceHeads",
    key = "#serviceId",
    condition = "#serviceId != null",
    unless = "#result == null"
  )
  public UUID resolveServiceHead(UUID serviceId) {
    if (serviceId == null) return null;
    try {
      UserClientResponse head = userClient.getServiceHead(serviceId);
      return head != null ? head.getId() : null;
    } catch (RuntimeException ex) {
      log.warn(
        "Impossible de résoudre le chef de service {} depuis user-service",
        serviceId,
        ex
      );
      return null;
    }
  }

  /** Retourne l'identifiant du chef d'agence, ou null si aucun responsable n'est défini. */
  @Cacheable(
    value = "agencyHeads",
    key = "#agencyId",
    condition = "#agencyId != null",
    unless = "#result == null"
  )
  public UUID resolveAgencyHead(UUID agencyId) {
    if (agencyId == null) return null;
    try {
      UserClientResponse head = userClient.getAgencyHead(agencyId);
      return head != null ? head.getId() : null;
    } catch (RuntimeException ex) {
      log.warn(
        "Impossible de résoudre le chef d'agence {} depuis user-service",
        agencyId,
        ex
      );
      return null;
    }
  }

  /** Retourne la liste des administrateurs (ADMIN / SUPER_ADMIN). */
  // Mise en cache : les administrateurs sont destinataires de presque chaque
  // notification, et un lot de relances redemandait l'annuaire complet a chaque
  // incident. Une liste vide n'est pas mise en cache : elle signale une panne de
  // l'annuaire, pas une absence d'administrateurs.
  @Cacheable(value = "admins", key = "'all'", unless = "#result == null || #result.isEmpty()")
  public List<ExternalUser> resolveAdmins() {
    try {
      List<UserClientResponse> admins = userClient.getAdmins();
      return admins != null
        ? admins.stream().map(this::toExternalUser).toList()
        : List.of();
    } catch (RuntimeException ex) {
      log.warn(
        "Impossible de résoudre les administrateurs depuis user-service",
        ex
      );
      return List.of();
    }
  }

  // Convertit les donnees du domaine utilisateur client entre les modeles utilises.

  private ExternalUser toExternalUser(UserClientResponse response) {
    if (response == null) {
      return null;
    }
    return ExternalUser.builder()
      .id(response.getId())
      .username(response.getUsername())
      .firstName(response.getFirstName())
      .lastName(response.getLastName())
      .email(response.getEmail())
      .active(response.isActive())
      .roles(response.getRoles() != null ? response.getRoles() : Set.of())
      .permissions(
        response.getPermissions() != null ? response.getPermissions() : Set.of()
      )
      .agencyId(response.getAgencyId())
      .serviceId(response.getServiceId())
      .managedServiceIds(
        response.getManagedServiceIds() != null
          ? response.getManagedServiceIds()
          : Set.of()
      )
      .build();
  }

  // Convertit les donnees du domaine utilisateur client entre les modeles utilises.

  private ExternalAgency toExternalAgency(AgencyClientResponse response) {
    if (response == null) {
      return null;
    }
    return ExternalAgency.builder()
      .id(response.getId())
      .name(response.getName())
      .code(response.getCode())
      .active(response.isActive())
      .build();
  }

  // Convertit les donnees du domaine utilisateur client entre les modeles utilises.

  private ExternalService toExternalService(ServiceClientResponse response) {
    if (response == null) {
      return null;
    }
    return ExternalService.builder()
      .id(response.getId())
      .name(response.getName())
      .description(response.getDescription())
      .active(response.isActive())
      .build();
  }

  // Convertit une agence distante vers le resume attendu par les DTO incidents.
  private AgencySummaryResponse toAgencySummary(AgencyClientResponse r) {
    AgencySummaryResponse summary = new AgencySummaryResponse();
    summary.setId(r.getId());
    summary.setName(r.getName() != null ? r.getName() : "unknown");
    summary.setCode(r.getCode());
    return summary;
  }

  // Convertit un service distant vers le resume attendu par les DTO incidents.
  private ServiceSummaryResponse toServiceSummary(ServiceClientResponse r) {
    ServiceSummaryResponse summary = new ServiceSummaryResponse();
    summary.setId(r.getId());
    summary.setName(r.getName() != null ? r.getName() : "unknown");
    summary.setDescription(r.getDescription());
    return summary;
  }

  // Realise l'intention metier unknown user.

  private UserSummaryResponse unknownUser(UUID id) {
    UserSummaryResponse summary = new UserSummaryResponse();
    summary.setId(id);
    summary.setUsername("unknown");
    summary.setRoles(Set.of());
    summary.setPermissions(Set.of());
    return summary;
  }

  // Realise l'intention metier unknown agency.

  private AgencySummaryResponse unknownAgency(UUID id) {
    AgencySummaryResponse summary = new AgencySummaryResponse();
    summary.setId(id);
    summary.setName("unknown");
    return summary;
  }

  // Realise l'intention metier unknown service.

  private ServiceSummaryResponse unknownService(UUID id) {
    ServiceSummaryResponse summary = new ServiceSummaryResponse();
    summary.setId(id);
    summary.setName("unknown");
    return summary;
  }
}
