package com.fintrack.reporting.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.fintrack.reporting.client.audit.AuditServiceClientService;
import com.fintrack.common.notification.EmailNotificationEvent;
import com.fintrack.reporting.model.entity.SystemSetting;
import com.fintrack.reporting.model.readmodel.superadmin.EmailNotificationEventSetting;
import com.fintrack.reporting.model.readmodel.superadmin.EmailNotificationSettings;
import com.fintrack.reporting.repository.SystemSettingRepository;
import com.fintrack.reporting.security.UserDetailsImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import tools.jackson.databind.json.JsonMapper;

// Verifie le stockage/lecture des reglages e-mail par evenement.
@ExtendWith(MockitoExtension.class)
class SystemConfigEmailNotificationsTest {

  @Mock
  private SystemSettingRepository repository;

  @Mock
  private AuditServiceClientService auditService;

  @Mock
  private MessageSource messageSource;

  @Mock
  private UserDetailsImpl actor;

  private SystemConfigServiceImpl service;
  private final Map<String, SystemSetting> store = new HashMap<>();

  @BeforeEach
  void setUp() {
    service = new SystemConfigServiceImpl(
      repository,
      auditService,
      messageSource,
      new JsonMapper()
    );
    lenient()
      .when(repository.findBySettingKey(anyString()))
      .thenAnswer(inv -> Optional.ofNullable(store.get(inv.getArgument(0))));
    lenient()
      .when(repository.save(any(SystemSetting.class)))
      .thenAnswer(inv -> {
        SystemSetting setting = inv.getArgument(0);
        store.put(setting.getSettingKey(), setting);
        return setting;
      });
    lenient().when(actor.getId()).thenReturn(UUID.randomUUID());
    lenient()
      .when(actor.getAuthorities())
      .thenAnswer(inv -> List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
  }

  @Test
  void defaultsFollowEachEventOwnDefaultWithoutExclusions() {
    EmailNotificationSettings settings = service.getEmailNotificationSettings();

    assertThat(settings.getEvents())
      .hasSize(EmailNotificationEvent.values().length);
    // Sans reglage explicite, chaque evenement porte son propre defaut : actif, sauf
    // ceux qui se declarent coupes (le rappel d'action attendue reste interne).
    assertThat(settings.getEvents())
      .allSatisfy(event -> {
        assertThat(event.isEnabled()).isEqualTo(
          EmailNotificationEvent.fromName(event.getEvent()).isEnabledByDefault()
        );
        assertThat(event.getExcludedUserIds()).isEmpty();
      });
    assertThat(
      byEvent(settings, EmailNotificationEvent.PENDING_ACTION_REMINDER)
        .isEnabled()
    ).isFalse();
    // Direction : interrupteur seul, pas de liste d'exclusion.
    assertThat(byEvent(settings, EmailNotificationEvent.DIRECTION_SUBMITTED)
        .isSupportsExclusion())
      .isFalse();
    assertThat(byEvent(settings, EmailNotificationEvent.SLA_REMINDER)
        .isSupportsExclusion())
      .isTrue();
  }

  @Test
  void updatePersistsToggleAndExclusionList() {
    UUID excluded = UUID.randomUUID();
    EmailNotificationSettings input = EmailNotificationSettings.builder()
      .events(
        new ArrayList<>(
          List.of(
            EmailNotificationEventSetting.builder()
              .event(EmailNotificationEvent.LATE_INCIDENTS_DAILY_REPORT.name())
              .enabled(false)
              .build(),
            EmailNotificationEventSetting.builder()
              .event(EmailNotificationEvent.SLA_REMINDER.name())
              .enabled(true)
              .excludedUserIds(List.of(excluded, excluded))
              .build()
          )
        )
      )
      .build();

    EmailNotificationSettings result =
      service.updateEmailNotificationSettings(input, actor);

    assertThat(
      byEvent(result, EmailNotificationEvent.LATE_INCIDENTS_DAILY_REPORT)
        .isEnabled()
    ).isFalse();
    // Dedoublonnage de la liste d'exclusion.
    assertThat(
      byEvent(result, EmailNotificationEvent.SLA_REMINDER).getExcludedUserIds()
    ).containsExactly(excluded);
    // Persistance effective (relecture depuis le store).
    assertThat(
      byEvent(
        service.getEmailNotificationSettings(),
        EmailNotificationEvent.LATE_INCIDENTS_DAILY_REPORT
      ).isEnabled()
    ).isFalse();
  }

  private EmailNotificationEventSetting byEvent(
    EmailNotificationSettings settings,
    EmailNotificationEvent event
  ) {
    return settings
      .getEvents()
      .stream()
      .filter(e -> e.getEvent().equals(event.name()))
      .findFirst()
      .orElseThrow();
  }
}
