// Contrat metier : expose les operations du domaine configuration systeme.

package com.fintrack.reporting.service;

import com.fintrack.reporting.model.readmodel.superadmin.EmailNotificationSettings;
import com.fintrack.reporting.model.readmodel.superadmin.SystemThresholds;
import com.fintrack.reporting.security.UserDetailsImpl;
import java.util.LinkedHashMap;
import java.util.Map;

// Service applicatif des seuils systeme modifiables.
public interface SystemConfigService {
  // Fournit seuils a la couche appelante.

  SystemThresholds getThresholds();
  // Fournit seuil long a la couche appelante.

  long getThresholdLong(String key, long fallback);
  // Applique le changement demande apres validation metier.

  SystemThresholds updateThresholds(
    SystemThresholds thresholds,
    UserDetailsImpl actor
  );

  // Fournit les reglages e-mail par evenement (interrupteurs + exclusions).
  EmailNotificationSettings getEmailNotificationSettings();

  // Applique la mise a jour des reglages e-mail apres validation metier.
  EmailNotificationSettings updateEmailNotificationSettings(
    EmailNotificationSettings settings,
    UserDetailsImpl actor
  );

  // Realise l'intention metier instantane.

  default Map<String, Object> snapshot() {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("thresholds", getThresholds());
    snapshot.put("emailNotifications", getEmailNotificationSettings());
    return snapshot;
  }
}
