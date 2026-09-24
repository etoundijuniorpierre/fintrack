// Contrat metier : expose les operations du domaine audit export.

package com.fintrack.reporting.service;

import com.fintrack.reporting.security.UserDetailsImpl;

// Definit le contrat audit export attendu par les autres couches.

public interface AuditExportService {
  // Realise l'intention metier export csv.

  byte[] exportCsv(
    String action,
    String status,
    String from,
    String to,
    Integer limit,
    UserDetailsImpl actor
  );
}
