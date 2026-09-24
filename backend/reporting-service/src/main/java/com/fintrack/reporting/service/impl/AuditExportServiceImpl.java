// Service metier : coordonne les operations du domaine audit export.

package com.fintrack.reporting.service.impl;

import com.fintrack.reporting.client.audit.AuditServiceClientService;
import com.fintrack.reporting.client.superadmin.SuperAdminAuditClientService;
import com.fintrack.reporting.client.superadmin.SuperAdminUserClient;
import com.fintrack.reporting.client.superadmin.dto.SuperAdminUserClientResponse;
import com.fintrack.reporting.model.readmodel.AuditExportRow;
import com.fintrack.reporting.security.UserDetailsImpl;
import com.fintrack.reporting.service.AuditExportService;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

// Implemente les regles metier du domaine audit export.

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditExportServiceImpl implements AuditExportService {

  private final SuperAdminAuditClientService auditClientService;
  private final SuperAdminUserClient userClient;
  private final AuditServiceClientService auditService;
  private final MessageSource messageSource;

  // Taille de page lors du streaming de l'export d'audit (reglage infra).
  @Value("${fintrack.audit.export-page-size:500}")
  private int auditExportPageSize;

  // Realise l'intention metier export csv.

  @Override
  public byte[] exportCsv(
    String action,
    String status,
    String from,
    String to,
    Integer limit,
    UserDetailsImpl actor
  ) {
    requireSuperAdmin(actor);
    long maxRows = sanitizeLimit(limit);
    Locale locale = LocaleContextHolder.getLocale();
    StringBuilder csv = new StringBuilder();
    csv.append('\uFEFF');
    csv.append(header(locale)).append("\r\n");

    int page = 0;
    int size = auditExportPageSize;
    long emitted = 0;
    Map<String, SuperAdminUserClientResponse> usersByUsername = null;
    while (emitted < maxRows) {
      List<AuditExportRow> rows = auditClientService.getAuditExportRows(
        // Realise l'intention metier vide to null.
        page,
        size,
        blankToNull(action),
        blankToNull(status),
        blankToNull(from),
        blankToNull(to)
      );
      if (rows.isEmpty()) {
        break;
      }
      for (AuditExportRow row : rows) {
        if (emitted >= maxRows) break;
        if (needsUserEnrichment(row) && usersByUsername == null) {
          usersByUsername = loadUsersByUsername();
        }
        AuditExportRow enriched = enrichRow(row, usersByUsername);
        csv.append(formatRow(enriched, locale)).append("\r\n");
        emitted++;
      }
      if (rows.size() < size) {
        break;
      }
      page++;
    }
    Map<String, Object> details = new LinkedHashMap<>();
    details.put("rows", emitted);
    details.put("action", action);
    details.put("status", status);
    details.put("from", from);
    details.put("to", to);
    details.put("limit", maxRows);
    audit(actor, "AUDIT_EXPORT", "audit_logs", "export", details);
    return csv.toString().getBytes(StandardCharsets.UTF_8);
  }

  // Securise une valeur du domaine audit export avant exposition.

  private long sanitizeLimit(Integer requestedLimit) {
    if (requestedLimit == null) {
      return 5000;
    }
    return Math.max(100, Math.min(requestedLimit.longValue(), 100000));
  }

  // Formate row.

  private String header(Locale locale) {
    return String.join(
      ";",
      escapeCsv(t("audit.export.header.timestamp", locale)),
      escapeCsv(t("audit.export.header.username", locale)),
      escapeCsv(t("audit.export.header.userId", locale)),
      escapeCsv(t("audit.export.header.action", locale)),
      escapeCsv(t("audit.export.header.status", locale)),
      escapeCsv(t("audit.export.header.resourceType", locale)),
      escapeCsv(t("audit.export.header.resourceId", locale)),
      escapeCsv(t("audit.export.header.ipAddress", locale))
    );
  }

  // Formate une ligne CSV localisee et compatible avec Excel.
  private String formatRow(AuditExportRow row, Locale locale) {
    return String.join(
      ";",
      escapeCsv(row.getTimestamp()),
      escapeCsv(row.getUsername()),
      escapeCsv(row.getUserId()),
      escapeCsv(
        t(
          "enum.audit_action." + row.getAction() + ".name",
          row.getAction(),
          locale
        )
      ),
      escapeCsv(
        t(
          "enum.audit_status." + row.getStatus() + ".name",
          row.getStatus(),
          locale
        )
      ),
      escapeCsv(row.getResourceType()),
      escapeCsv(row.getResourceId()),
      escapeCsv(row.getIpAddress())
    );
  }

  // Realise l'intention metier escape csv.

  private String escapeCsv(Object value) {
    if (value == null) return "";
    String text = value.toString().replace("\"", "\"\"");
    if (
      text.contains(";") ||
      text.contains(",") ||
      text.contains("\n") ||
      text.contains("\r") ||
      text.contains("\"")
    ) {
      return "\"" + text + "\"";
    }
    return text;
  }

  // Complete les anciens logs de connexion quand le username permet de retrouver le compte.
  private AuditExportRow enrichRow(
    AuditExportRow row,
    Map<String, SuperAdminUserClientResponse> usersByUsername
  ) {
    if (row == null || usersByUsername == null || row.getUsername() == null) {
      return row;
    }
    SuperAdminUserClientResponse user = usersByUsername.get(
      row.getUsername().toLowerCase(Locale.ROOT)
    );
    if (user == null || user.getId() == null) {
      if (isLoginUserEvent(row) && isBlank(row.getResourceId())) {
        row.setResourceId(row.getUsername());
      }
      return row;
    }
    if (isBlank(row.getUserId())) {
      row.setUserId(user.getId().toString());
    }
    if (isLoginUserEvent(row) && isBlank(row.getResourceId())) {
      row.setResourceId(user.getId().toString());
    }
    return row;
  }

  // Indique si la ligne a besoin d'une resolution utilisateur groupée.
  private boolean needsUserEnrichment(AuditExportRow row) {
    return (
      row != null &&
      !isBlank(row.getUsername()) &&
      (isBlank(row.getUserId()) ||
        (isLoginUserEvent(row) && isBlank(row.getResourceId())))
    );
  }

  // Charge les utilisateurs une seule fois pour enrichir les exports existants.
  private Map<String, SuperAdminUserClientResponse> loadUsersByUsername() {
    Map<String, SuperAdminUserClientResponse> users = new HashMap<>();
    try {
      for (SuperAdminUserClientResponse user : userClient.getUsers()) {
        if (user.getUsername() != null) {
          users.put(user.getUsername().toLowerCase(Locale.ROOT), user);
        }
      }
    } catch (RuntimeException ex) {
      log.warn(
        "Impossible d'enrichir l'export d'audit avec user-service : {}",
        ex.toString()
      );
    }
    return users;
  }

  // Verifie si la ligne concerne l'utilisateur d'une session.
  private boolean isLoginUserEvent(AuditExportRow row) {
    return (
      row != null &&
      "USER".equalsIgnoreCase(row.getResourceType()) &&
      ("LOGIN_SUCCESS".equalsIgnoreCase(row.getAction()) ||
        "LOGIN_FAILURE".equalsIgnoreCase(row.getAction()) ||
        "LOGOUT".equalsIgnoreCase(row.getAction()))
    );
  }

  // Retourne un libelle localise avec repli stable.
  // Libelle localise ; la cle sert de repli pour reveler toute traduction manquante.
  private String t(String key, Locale locale) {
    return messageSource.getMessage(key, null, key, locale);
  }

  private String t(String key, String fallback, Locale locale) {
    return messageSource.getMessage(key, null, fallback, locale);
  }

  private String t(String key) {
    return messageSource.getMessage(
      key,
      null,
      key,
      LocaleContextHolder.getLocale()
    );
  }

  // Verifie si une valeur texte est absente.
  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  // Realise l'intention metier vide to null.

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  // Exige super admin.

  private void requireSuperAdmin(UserDetailsImpl actor) {
    if (actor == null) {
      throw new AccessDeniedException(t("reporting.error.super_admin_role_required"));
    }
    boolean ok = actor
      .getAuthorities()
      .stream()
      .map(GrantedAuthority::getAuthority)
      .anyMatch(
        role -> "ROLE_SUPER_ADMIN".equals(role) || "SUPER_ADMIN".equals(role)
      );
    if (!ok) {
      throw new AccessDeniedException(t("reporting.error.super_admin_role_required"));
    }
  }

  // Trace l'action metier realisee sur le domaine audit export.

  private void audit(
    UserDetailsImpl actor,
    String action,
    String resourceType,
    String resourceId,
    Map<String, Object> details
  ) {
    try {
      UUID userId = actor != null ? actor.getId() : null;
      String username = actor != null ? actor.getUsername() : "system";
      List<String> roles =
        actor != null
          ? actor
              .getAuthorities()
              .stream()
              .map(GrantedAuthority::getAuthority)
              .toList()
          : List.of();
      auditService.audit(
        userId,
        username,
        roles,
        action,
        resourceType,
        resourceId,
        "SUCCESS",
        details
      );
    } catch (RuntimeException ex) {
      log.warn(
        "Échec d'émission de l'audit pour {} : {}",
        action,
        ex.toString()
      );
    }
  }
}
