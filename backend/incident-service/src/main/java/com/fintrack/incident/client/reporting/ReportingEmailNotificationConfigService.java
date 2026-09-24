// Client inter-services : lit les reglages e-mail par evenement aupres du
// reporting-service et les met en cache brievement (source unique de verite,
// modifiable en direct par le Super Admin sans redeploiement).

package com.fintrack.incident.client.reporting;

import com.fintrack.common.notification.EmailNotificationEvent;
import com.fintrack.incident.client.reporting.dto.EmailNotificationEventSettingClient;
import com.fintrack.incident.client.reporting.dto.EmailNotificationSettingsClient;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// Expose l'etat "e-mail autorise / utilisateurs exclus" pour chaque evenement.
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportingEmailNotificationConfigService {

  private final ReportingSystemConfigClient reportingSystemConfigClient;

  // Cache court : les notifications sont frequentes, on evite un appel Feign par
  // envoi tout en propageant les changements de config en moins d'une minute.
  private static final long TTL_MS = 60_000;
  private volatile Map<String, EmailNotificationEventSettingClient> cache =
    Map.of();
  private volatile long cachedAt = 0L;

  private Map<String, EmailNotificationEventSettingClient> settings() {
    long now = System.currentTimeMillis();
    if (now - cachedAt <= TTL_MS && !cache.isEmpty()) {
      return cache;
    }
    try {
      EmailNotificationSettingsClient response =
        reportingSystemConfigClient.getEmailNotifications();
      Map<String, EmailNotificationEventSettingClient> next = new HashMap<>();
      if (response != null && response.getEvents() != null) {
        for (EmailNotificationEventSettingClient event : response.getEvents()) {
          if (event != null && event.getEvent() != null) {
            next.put(event.getEvent(), event);
          }
        }
      }
      cache = next;
      cachedAt = now;
    } catch (RuntimeException ex) {
      log.debug(
        "Reglages e-mail indisponibles, conservation du cache precedent : {}",
        ex.toString()
      );
    }
    return cache;
  }

  // Vrai si l'envoi e-mail est autorise pour l'evenement (defaut : autorise).
  public boolean isEmailEnabled(EmailNotificationEvent event) {
    EmailNotificationEventSettingClient setting = settings().get(event.name());
    return setting == null || setting.isEnabled();
  }

  // Utilisateurs a exclure de la reception e-mail pour l'evenement (meme admins).
  public Set<UUID> excludedRecipients(EmailNotificationEvent event) {
    EmailNotificationEventSettingClient setting = settings().get(event.name());
    if (setting == null || setting.getExcludedUserIds() == null) {
      return Set.of();
    }
    return new HashSet<>(setting.getExcludedUserIds());
  }
}
