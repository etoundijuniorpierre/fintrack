// Client inter-services : communique avec les services externes lies a super admin audit client.

package com.fintrack.reporting.client.superadmin.fallback;

import com.fintrack.reporting.client.superadmin.SuperAdminAuditClient;
import com.fintrack.reporting.client.superadmin.dto.AuditLogsPageClientResponse;
import com.fintrack.reporting.client.superadmin.dto.AuditStatsClientResponse;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Repli quand audit-service est indisponible : renvoie des valeurs neutres
// (null / map vide) que les appelants Super Admin traitent en mode degrade,
// au lieu de lever NoFallbackAvailableException.
@Slf4j
@Component
public class SuperAdminAuditClientFallback implements SuperAdminAuditClient {

  @Override
  // Fournit audit logs au cas d usage appelant.
  public AuditLogsPageClientResponse getAuditLogs(
    int page,
    int size,
    String sort,
    String action,
    String status,
    String from,
    String to
  ) {
    log.warn(
      "Repli : audit-service indisponible — getAuditLogs dégradé (liste vide)"
    );
    return null;
  }

  // Fournit statistiques a la couche appelante.

  @Override
  public AuditStatsClientResponse getStats(
    String from,
    String to,
    int sampleSize,
    int repeatedThreshold
  ) {
    log.warn(
      "Repli : audit-service indisponible — statistiques d'audit dégradées"
    );
    return null;
  }

  // Traite en masse purge.

  @Override
  public Map<String, Object> bulkPurge(Map<String, Object> request) {
    log.warn("Repli : audit-service indisponible — purge non exécutée");
    return Map.of();
  }
}
