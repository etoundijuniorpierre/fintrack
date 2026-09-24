// Client inter-services : communique avec les services externes lies a super admin audit client.

package com.fintrack.reporting.client.superadmin;

import com.fintrack.reporting.client.superadmin.dto.AuditLogClientResponse;
import com.fintrack.reporting.client.superadmin.dto.AuditLogsPageClientResponse;
import com.fintrack.reporting.model.readmodel.AuditExportRow;
import java.util.List;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// Adapter de lecture des logs d'audit via les endpoints Super Admin.
@Service
@RequiredArgsConstructor
public class SuperAdminAuditClientService {

  private final SuperAdminAuditClient auditClient;
  private final MessageSource messageSource;

  private String t(String key, Object... args) {
    return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
  }

  // Fournit audit export rows au cas d usage appelant.
  public List<AuditExportRow> getAuditExportRows(
    int page,
    int size,
    String action,
    String status,
    String from,
    String to
  ) {
    AuditLogsPageClientResponse body = auditClient.getAuditLogs(
      page,
      size,
      "timestamp,desc",
      action,
      status,
      from,
      to
    );
    if (body == null) {
      throw new IllegalStateException(
        t("reporting.error.audit_service_no_response")
      );
    }
    if (body.getContent() == null) {
      return List.of();
    }
    return body.getContent().stream().map(this::toExportRow).toList();
  }

  // Convertit les donnees du domaine super-administration audit client entre les modeles utilises.

  private AuditExportRow toExportRow(AuditLogClientResponse row) {
    return AuditExportRow.builder()
      .timestamp(
        row.getTimestamp() != null ? row.getTimestamp().toString() : null
      )
      .username(row.getUsername())
      .userId(row.getUserId() != null ? row.getUserId().toString() : null)
      .action(row.getAction())
      .status(row.getStatus())
      .resourceType(row.getResourceType())
      .resourceId(row.getResourceId())
      .ipAddress(row.getIpAddress())
      .build();
  }
}
