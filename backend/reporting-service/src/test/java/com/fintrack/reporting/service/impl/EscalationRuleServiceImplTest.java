package com.fintrack.reporting.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fintrack.reporting.client.audit.AuditServiceClientService;
import com.fintrack.reporting.client.incident.IncidentServiceClientService;
import com.fintrack.reporting.client.notification.BilingualText;
import com.fintrack.reporting.client.notification.NotificationClientService;
import com.fintrack.reporting.client.superadmin.SuperAdminNotificationClientService;
import com.fintrack.reporting.client.superadmin.SuperAdminUserClientService;
import com.fintrack.reporting.model.entity.EscalationRule;
import com.fintrack.reporting.model.readmodel.EscalationIncident;
import com.fintrack.reporting.repository.EscalationRuleEventRepository;
import com.fintrack.reporting.repository.EscalationRuleRepository;
import com.fintrack.reporting.repository.GeneratedReportRepository;
import com.fintrack.reporting.service.SystemConfigService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.support.StaticMessageSource;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EscalationRuleServiceImplTest {

  @Mock
  private EscalationRuleRepository ruleRepository;

  @Mock
  private EscalationRuleEventRepository eventRepository;

  @Mock
  private IncidentServiceClientService incidentService;

  @Mock
  private SuperAdminNotificationClientService notificationClientService;

  @Mock
  private SuperAdminUserClientService userClientService;

  @Mock
  private GeneratedReportRepository reportRepository;

  @Mock
  private NotificationClientService outboundNotificationClientService;

  @Mock
  private AuditServiceClientService auditService;

  @Mock
  private SystemConfigService systemConfigService;

  private EscalationRuleServiceImpl service;

  @BeforeEach
  void setUp() {
    StaticMessageSource messages = new StaticMessageSource();
    messages.setUseCodeAsDefaultMessage(true);
    messages.addMessage(
      "notification.escalation.internal.content",
      java.util.Locale.FRENCH,
      "{0} incident(s)."
    );
    messages.addMessage(
      "notification.escalation.internal.incidents",
      java.util.Locale.FRENCH,
      "Incidents concernés : {0}."
    );
    messages.addMessage(
      "notification.escalation.internal.incidents_more",
      java.util.Locale.FRENCH,
      "et {0} autre(s)."
    );

    service =
      new EscalationRuleServiceImpl(
        ruleRepository,
        eventRepository,
        incidentService,
        notificationClientService,
        userClientService,
        reportRepository,
        outboundNotificationClientService,
        auditService,
        systemConfigService,
        messages
      );

    when(systemConfigService.getThresholdLong(anyString(), any(Long.class)))
      .thenAnswer(invocation -> invocation.getArgument(1, Long.class));
    when(reportRepository.findAll()).thenReturn(List.of());
    when(notificationClientService.countFailedNotifications()).thenReturn(0L);
    when(userClientService.resolveUsernamesByRoles(any(), any()))
      .thenReturn(List.of("directeur"));
    when(ruleRepository.findByRuleKey(anyString()))
      .thenAnswer(invocation -> Optional.of(enabledRule(invocation.getArgument(0))));
  }

  private EscalationRule enabledRule(String key) {
    EscalationRule rule = new EscalationRule();
    rule.setRuleKey(key);
    rule.setEnabled(true);
    rule.setAutomated(true);
    rule.setOwnerRole("ADMIN");
    return rule;
  }

  // Non critique : seule la regle multi-transfert se declenche, assertion deterministe.
  private EscalationIncident transferredIncident(
    String reference,
    String title
  ) {
    return EscalationIncident.builder()
      .reference(reference)
      .title(title)
      .criticality("HIGH")
      .status("IN_PROGRESS")
      .transferCount(3)
      .createdAt(LocalDateTime.now().minusDays(2).toString())
      .build();
  }

  @Test
  @DisplayName("An escalation alert names the incidents it counts")
  void escalationAlert_NamesTheIncidents() {
    when(incidentService.getEscalationIncidents())
      .thenReturn(
        List.of(
          transferredIncident("FT-I-2026-0001", "Écart de caisse"),
          transferredIncident("FT-I-2026-0002", "Guichet hors service")
        )
      );

    service.evaluateAndDispatch(true);

    ArgumentCaptor<BilingualText> content = ArgumentCaptor.forClass(
      BilingualText.class
    );
    ArgumentCaptor<Map<String, Object>> details = ArgumentCaptor.forClass(
      Map.class
    );
    verify(outboundNotificationClientService)
      .sendInternal(
        eq("directeur"),
        any(),
        content.capture(),
        details.capture(),
        anyString()
      );

    // Le destinataire lit les references dans le message, sans aller les chercher.
    assertThat(content.getValue().fr())
      .contains("FT-I-2026-0001 — Écart de caisse")
      .contains("FT-I-2026-0002 — Guichet hors service");
    assertThat(details.getValue().get("incidents"))
      .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
      .hasSize(2);
  }

  @Test
  @DisplayName(
    "A long batch names the first incidents and announces the remainder"
  )
  void escalationAlert_TruncatesLongBatches() {
    when(incidentService.getEscalationIncidents())
      .thenReturn(
        IntStream
          .rangeClosed(1, 13)
          .mapToObj(i ->
            transferredIncident(
              String.format("FT-I-2026-%04d", i),
              "Incident " + i
            )
          )
          .toList()
      );

    service.evaluateAndDispatch(true);

    ArgumentCaptor<BilingualText> content = ArgumentCaptor.forClass(
      BilingualText.class
    );
    verify(outboundNotificationClientService)
      .sendInternal(
        eq("directeur"),
        any(),
        content.capture(),
        any(),
        anyString()
      );

    // Tronquer en silence rendrait l'alerte fausse : le reste est annonce.
    assertThat(content.getValue().fr())
      .contains("FT-I-2026-0010")
      .doesNotContain("FT-I-2026-0011")
      .contains("et 3 autre(s).");
  }
}
