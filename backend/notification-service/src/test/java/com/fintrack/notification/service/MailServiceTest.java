package com.fintrack.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

class MailServiceTest {

  private final MailService mailService = new MailService(
    mock(JavaMailSender.class)
  );

  @Test
  void resolveTemplateName_UsesExplicitGeneratedReportTemplateEvenWithAttachment() {
    String template = resolveTemplateName(
      Map.of(
        "email_template",
        "generated_report",
        "attachment_content",
        "base64",
        "attachment_name",
        "rapport.pdf"
      )
    );

    assertThat(template).isEqualTo("generated-report-template.html");
  }

  @Test
  void resolveTemplateName_UsesLateIncidentsTemplateOnlyForLateIncidentReports() {
    String template = resolveTemplateName(
      Map.of(
        "email_template",
        "late_incidents_report",
        "attachment_content",
        "base64",
        "attachment_name",
        "incidents_en_retard_2026-06-09.csv"
      )
    );

    assertThat(template).isEqualTo("late-incidents-report-template.html");
  }

  @Test
  void resolveTemplateName_FallsBackToGeneratedReportForGenericAttachments() {
    String template = resolveTemplateName(
      Map.of(
        "attachment_content",
        "base64",
        "attachment_name",
        "rapport-quotidien.pdf",
        "report_name",
        "Rapport quotidien"
      )
    );

    assertThat(template).isEqualTo("generated-report-template.html");
  }

  @Test
  void resolveTemplateName_DetectsCredentialAndIncidentNotifications() {
    assertThat(
      resolveTemplateName(Map.of("temporary_password", "secret"))
    ).isEqualTo("credential-template.html");

    assertThat(
      resolveTemplateName(Map.of("incident_reference", "INC-001"))
    ).isEqualTo("incident-report-template.html");
  }

  @Test
  void resolveTemplateName_UsesDirectionTemplateForDirectionValidationEmails() {
    String template = resolveTemplateName(
      Map.of(
        "email_template",
        "direction_validation_template",
        "incident_reference",
        "INC-42",
        "proposed_solution",
        "Solution proposée"
      )
    );

    assertThat(template).isEqualTo("direction-validation-template.html");
  }

  @Test
  void resolveTemplateName_UsesGenericTemplateForNonIncidentEmails() {
    String template = resolveTemplateName(
      Map.of("subject", "Contact admin", "message", "Besoin d'aide")
    );

    assertThat(template).isEqualTo("generic-notification-template.html");
  }

  private String resolveTemplateName(Map<String, Object> params) {
    return ReflectionTestUtils.invokeMethod(
      mailService,
      "resolveTemplateName",
      params
    );
  }
}
