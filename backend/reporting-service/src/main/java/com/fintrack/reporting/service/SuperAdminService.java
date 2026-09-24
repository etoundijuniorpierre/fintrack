// Contrat metier : expose les operations du domaine super-administration.

package com.fintrack.reporting.service;

import com.fintrack.reporting.model.dto.response.superadmin.BackupTriggerResponse;
import com.fintrack.reporting.model.readmodel.superadmin.SuperAdminSnapshot;
import com.fintrack.reporting.model.readmodel.superadmin.SystemThresholds;
import com.fintrack.reporting.security.UserDetailsImpl;

// Definit le contrat super admin attendu par les autres couches.

public interface SuperAdminService {
  // Fournit vue d ensemble a la couche appelante.

  SuperAdminSnapshot getOverview();
  // Fournit gouvernance a la couche appelante.

  SuperAdminSnapshot getGovernance();
  // Fournit systeme sante a la couche appelante.

  SuperAdminSnapshot getSystemHealth();
  // Fournit controles qualite a la couche appelante.

  SuperAdminSnapshot getControlsQuality();
  // Fournit operations a la couche appelante.

  SuperAdminSnapshot getOperations();
  // Fournit audit vue d ensemble a la couche appelante.

  SuperAdminSnapshot getAuditOverview();
  // Fournit configuration systeme a la couche appelante.

  SuperAdminSnapshot getSystemConfig();
  // Fournit reporting vue d ensemble a la couche appelante.

  SuperAdminSnapshot getReportingOverview();
  // Applique le changement demande apres validation metier.

  SuperAdminSnapshot updateThresholds(
    SystemThresholds thresholds,
    UserDetailsImpl actor
  );

  // Fournit les reglages e-mail par evenement a la couche appelante.
  com.fintrack.reporting.model.readmodel.superadmin.EmailNotificationSettings getEmailNotificationSettings();

  // Applique la mise a jour des reglages e-mail apres validation metier.
  com.fintrack.reporting.model.readmodel.superadmin.EmailNotificationSettings updateEmailNotificationSettings(
    com.fintrack.reporting.model.readmodel.superadmin.EmailNotificationSettings settings,
    UserDetailsImpl actor
  );
  // Realise l'intention metier export audit csv.

  byte[] exportAuditCsv(
    String action,
    String status,
    String from,
    String to,
    Integer limit,
    UserDetailsImpl actor
  );
  // Invalide cache.

  void invalidateCache(UserDetailsImpl actor);

  // Declenche la sauvegarde manuelle et la synchronisation Backblaze B2 (audite).
  BackupTriggerResponse triggerBackup(
    UserDetailsImpl actor
  );
}
