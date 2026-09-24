// Tests d'integration legers : verifie la resolution multilingue des notifications.
package com.fintrack.incident.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

class NotificationMessageSourceTest {

  private ResourceBundleMessageSource messageSource;

  @BeforeEach
  void setUp() {
    messageSource = new ResourceBundleMessageSource();
    messageSource.setBasenames("i18n/messages", "i18n/notifications");
    messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
    messageSource.setFallbackToSystemLocale(false);
  }

  @Test
  @DisplayName(
    "notifications - Should resolve transferred incident labels in French"
  )
  void transferredNotification_French_ResolvesLabels() {
    assertThat(
      messageSource.getMessage(
        "notification.incident.transferred.subject",
        new Object[] { "Caisse", "Agent" },
        Locale.FRENCH
      )
    ).contains("Caisse");
    assertThat(
      messageSource.getMessage(
        "notification.incident.transferred.content",
        new Object[] { "Inc-1", "Caisse", "Agent" },
        Locale.FRENCH
      )
    )
      .contains("Caisse")
      .doesNotContain("notification.incident.transferred.content");
  }

  @Test
  @DisplayName(
    "notifications - Should resolve transferred incident labels in English"
  )
  void transferredNotification_English_ResolvesLabels() {
    assertThat(
      messageSource.getMessage(
        "notification.incident.transferred.subject",
        new Object[] { "Cash desk", "Agent" },
        Locale.ENGLISH
      )
    ).contains("Cash desk");
    assertThat(
      messageSource.getMessage(
        "notification.incident.transferred.content",
        new Object[] { "Inc-1", "Cash desk", "Agent" },
        Locale.ENGLISH
      )
    )
      .contains("Cash desk")
      .doesNotContain("notification.incident.transferred.content");
  }
}
