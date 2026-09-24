// Mapper : convertit les donnees liees a audit log entre modeles.

package com.fintrack.audit.model.mapper;

import com.fintrack.audit.client.user.UserServiceClientService;
import com.fintrack.audit.model.dto.request.AuditLogRequest;
import com.fintrack.audit.model.dto.response.AuditLogResponse;
import com.fintrack.audit.model.dto.response.AuditSample;
import com.fintrack.audit.model.dto.response.AuditStatsResponse;
import com.fintrack.audit.model.dto.response.BulkPurgeResponse;
import com.fintrack.audit.model.dto.response.RepeatedAction;
import com.fintrack.audit.model.dto.response.UserSummaryResponse;
import com.fintrack.audit.model.entity.AuditLog;
import com.fintrack.audit.model.readmodel.AuditPurgeResult;
import com.fintrack.audit.model.readmodel.AuditStats;
import com.fintrack.audit.model.readmodel.RepeatedAuditAction;
import java.util.Map;
import java.util.UUID;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

// Mapper MapStruct convertissant entre les entites d'audit et leurs DTO.
// Politique d'affichage de l'utilisateur :
// 1) On tente la resolution live via user-service pour avoir le nom a jour.
// 2) Si la resolution echoue (user supprime, service indisponible) ou ne
// fournit ni firstName ni lastName, on retombe sur le snapshot stocke
// dans le document d'audit (username, et eventuellement firstName /
// lastName figes). Cette garantie est essentielle : un audit log doit
// rester lisible meme apres suppression du compte concerne.
@Mapper(
  componentModel = "spring",
  unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class AuditLogMapper {

  @Autowired
  protected UserServiceClientService userServiceClientService;

  // Convertit une requete de creation en document d'audit.
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "timestamp", ignore = true)
  public abstract AuditLog toDocument(AuditLogRequest request);

  // Convertit un document d'audit en DTO de reponse en resolvant l'utilisateur.
  @Mapping(
    target = "user",
    source = ".",
    qualifiedByName = "auditLogToUserSummary"
  )
  public abstract AuditLogResponse toResponse(AuditLog document);

  // Convertit un document en utilisant les utilisateurs resolus en bulk pour eviter les appels par ligne.
  public AuditLogResponse toResponse(
    AuditLog document,
    Map<UUID, UserSummaryResponse> usersById
  ) {
    if (document == null) {
      return null;
    }
    UserSummaryResponse user =
      document.getUserId() != null && usersById != null
        ? usersById.get(document.getUserId())
        : null;
    if (
      user == null ||
      (isBlank(user.getFirstName()) && isBlank(user.getLastName()))
    ) {
      user = mergeWithSnapshot(user, document);
    } else if (
      isBlank(user.getUsername()) && !isBlank(document.getUsername())
    ) {
      user.setUsername(document.getUsername());
    }
    return AuditLogResponse.builder()
      .id(document.getId())
      .timestamp(document.getTimestamp())
      .user(user)
      .username(document.getUsername())
      .roles(document.getRoles())
      .action(document.getAction())
      .resourceType(document.getResourceType())
      .resourceId(document.getResourceId())
      .ipAddress(document.getIpAddress())
      .userAgent(document.getUserAgent())
      .status(document.getStatus())
      .details(document.getDetails())
      .build();
  }

  // Convertit un document d'audit en DTO d'echantillon simplifie pour les statistiques.
  @Mapping(
    target = "timestamp",
    expression = "java(document.getTimestamp() != null ? document.getTimestamp().toString() : null)"
  )
  @Mapping(
    target = "action",
    expression = "java(document.getAction() != null ? document.getAction().name() : null)"
  )
  @Mapping(
    target = "status",
    expression = "java(document.getStatus() != null ? document.getStatus().name() : null)"
  )
  public abstract AuditSample toSample(AuditLog document);

  // Convertit les donnees du domaine audit journal entre les modeles utilises.

  public AuditStatsResponse toStatsResponse(AuditStats stats) {
    if (stats == null) {
      return null;
    }
    return AuditStatsResponse.builder()
      .total(stats.getTotal())
      .byAction(stats.getByAction())
      .byStatus(stats.getByStatus())
      .sensitiveCount(stats.getSensitiveCount())
      .recentLogs(stats.getRecentLogs().stream().map(this::toSample).toList())
      .recentSensitive(
        stats.getRecentSensitive().stream().map(this::toSample).toList()
      )
      .permissionChangeHistory(
        stats.getPermissionChangeHistory().stream().map(this::toSample).toList()
      )
      .repeatedSensitiveActions(
        stats
          .getRepeatedSensitiveActions()
          .stream()
          .map(this::toRepeatedAction)
          .toList()
      )
      .failuresByResourceType(stats.getFailuresByResourceType())
      .heatmapLast7Days(stats.getHeatmapLast7Days())
      .computedAt(stats.getComputedAt())
      .build();
  }

  // Convertit les donnees du domaine audit journal entre les modeles utilises.

  public BulkPurgeResponse toBulkPurgeResponse(AuditPurgeResult result) {
    if (result == null) {
      return null;
    }
    return BulkPurgeResponse.builder()
      .scope(result.getScope())
      .deletedCount(result.getDeletedCount())
      .preservedCount(result.getPreservedCount())
      .preservedSensitive(result.getPreservedSensitive())
      .dryRun(result.isDryRun())
      .executedAt(result.getExecutedAt())
      .preservedSensitiveActions(result.getPreservedSensitiveActions())
      .build();
  }

  // Convertit les donnees du domaine audit journal entre les modeles utilises.

  protected RepeatedAction toRepeatedAction(RepeatedAuditAction action) {
    if (action == null) {
      return null;
    }
    return RepeatedAction.builder()
      .key(action.getKey())
      .username(action.getUsername())
      .action(action.getAction())
      .count(action.getCount())
      .build();
  }

  // Resout l'utilisateur du log en combinant la resolution live (user-service)
  // et le snapshot embarque dans le document. Garantit qu'on n'affichera jamais
  // "null null" tant qu'au moins un identifiant (username, prenom, nom) est connu.
  @Named("auditLogToUserSummary")
  protected UserSummaryResponse auditLogToUserSummary(AuditLog document) {
    if (document == null || document.getUserId() == null) {
      return buildSnapshotSummary(document);
    }
    UserSummaryResponse live = userServiceClientService.resolveUser(
      document.getUserId()
    );
    if (
      live == null ||
      (isBlank(live.getFirstName()) && isBlank(live.getLastName()))
    ) {
      return mergeWithSnapshot(live, document);
    }
    if (isBlank(live.getUsername()) && !isBlank(document.getUsername())) {
      live.setUsername(document.getUsername());
    }
    return live;
  }

  // Realise l'intention metier merge with instantane.

  private UserSummaryResponse mergeWithSnapshot(
    UserSummaryResponse live,
    AuditLog document
  ) {
    UserSummaryResponse snapshot = buildSnapshotSummary(document);
    if (live == null) {
      return snapshot;
    }
    if (isBlank(live.getUsername())) live.setUsername(snapshot.getUsername());
    if (isBlank(live.getFirstName())) live.setFirstName(
      snapshot.getFirstName()
    );
    if (isBlank(live.getLastName())) live.setLastName(snapshot.getLastName());
    if (isBlank(live.getEmail())) live.setEmail(snapshot.getEmail());
    // Si tous les champs nominatifs restent vides apres merge, on renvoie
    // null pour laisser le client retomber sur AuditLogResponse.username.
    if (
      live.getId() == null &&
      isBlank(live.getUsername()) &&
      isBlank(live.getFirstName()) &&
      isBlank(live.getLastName())
    ) {
      return null;
    }
    return live;
  }

  // Construit la representation attendue pour le domaine audit journal.

  private UserSummaryResponse buildSnapshotSummary(AuditLog document) {
    if (
      document == null ||
      (document.getUserId() == null && isBlank(document.getUsername()))
    ) {
      return null;
    }
    return UserSummaryResponse.builder()
      .id(document.getUserId())
      .username(document.getUsername())
      .build();
  }

  // Verifie si vide.

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
