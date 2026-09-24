// Read-model : ensemble des reglages e-mail par evenement, expose a l'UI Super
// Admin et lu en runtime par les services emetteurs.

package com.fintrack.reporting.model.readmodel.superadmin;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Catalogue des reglages e-mail configurables.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationSettings {

  @Builder.Default
  private List<EmailNotificationEventSetting> events = new ArrayList<>();
}
