// Contrat metier : expose les operations du domaine mail.

package com.fintrack.notification.service;

import com.fintrack.notification.model.entity.Notification;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * Envoi effectif des e-mails via SMTP (Spring Mail). Provider-agnostique :
 * le transport est piloté par les propriétés spring.mail.* (Gmail, Brevo, SES…).
 */
@Slf4j
@Service
@RequiredArgsConstructor
// Porte les regles metier du domaine service interne.
public class MailService {

  private final JavaMailSender mailSender;

  // Adresse d'authentification SMTP (sert aussi d'expediteur par defaut).
  @Value("${spring.mail.username:}")
  private String mailUsername;

  // Expediteur affiche ; si vide, on retombe sur l'adresse d'authentification.
  @Value("${spring.mail.from:}")
  private String fromEmail;

  @Value("${fintrack.mail.from-name:FinTrack}")
  private String fromName;

  private static final String CREDENTIAL_TEMPLATE = "credential-template.html";
  private static final String INCIDENT_TEMPLATE =
    "incident-report-template.html";
  private static final String GENERIC_NOTIFICATION_TEMPLATE =
    "generic-notification-template.html";
  private static final String GENERATED_REPORT_TEMPLATE =
    "generated-report-template.html";
  private static final String LATE_INCIDENTS_REPORT_TEMPLATE =
    "late-incidents-report-template.html";
  private static final String DIRECTION_VALIDATION_TEMPLATE =
    "direction-validation-template.html";

  private final Map<String, String> templateCache = new HashMap<>();

  /**
   * Envoie un e-mail via SMTP. Lève une exception en cas d'échec afin que l'appelant
   * puisse gérer le réessai.
   */
  // Transmet service interne au canal de notification demande.
  public void send(Notification notification) {
    String to = notification.getRecipient();
    if (!StringUtils.hasText(to)) {
      throw new IllegalArgumentException("Destinataire e-mail manquant");
    }

    validateConfiguration(to);

    try {
      Map<String, Object> templateParams = new HashMap<>();
      if (notification.getTemplateParams() != null) {
        templateParams.putAll(notification.getTemplateParams());
      }

      // Defaults
      String subject =
        notification.getSubject() != null
          ? notification.getSubject()
          : "Notification FinTrack";
      templateParams.putIfAbsent("subject", subject);
      templateParams.putIfAbsent(
        "message",
        notification.getContent() != null ? notification.getContent() : ""
      );
      templateParams.putIfAbsent("name", to.split("@")[0]);
      templateParams.putIfAbsent("firstName", "");
      templateParams.putIfAbsent(
        "date",
        LocalDateTime.now().format(
          DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        )
      );

      // Garde la compatibilite avec les blocs conditionnels des anciens modeles.
      templateParams.putIfAbsent("display_incident_details", "none");
      templateParams.putIfAbsent("display_action_details", "none");
      templateParams.putIfAbsent("display_sender_details", "none");
      templateParams.putIfAbsent("display_rule_details", "none");
      templateParams.putIfAbsent("subject_color", "#111827");

      String htmlContent = generateHtml(templateParams);

      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(
        message,
        MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
        StandardCharsets.UTF_8.name()
      );

      String sender = StringUtils.hasText(fromEmail) ? fromEmail : mailUsername;
      helper.setFrom(sender, fromName);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlContent, true);

      // Piece jointe eventuelle (Base64).
      if (
        templateParams.containsKey("attachment_content") &&
        templateParams.containsKey("attachment_name")
      ) {
        String contentBase64 = String.valueOf(
          templateParams.get("attachment_content")
        );
        String fileName = String.valueOf(templateParams.get("attachment_name"));
        byte[] bytes = Base64.getDecoder().decode(contentBase64);
        helper.addAttachment(
          fileName,
          new ByteArrayDataSource(bytes, guessContentType(fileName))
        );
      }

      mailSender.send(message);
      log.info("E-mail envoyé via SMTP à {}", to);
    } catch (Exception e) {
      log.error(
        "Échec de l'envoi de l'e-mail via SMTP à {} : {}",
        to,
        e.getMessage()
      );
      throw new RuntimeException("Échec de l'envoi de l'e-mail", e);
    }
  }

  // Genere la sortie attendue pour le domaine mail.

  private String generateHtml(Map<String, Object> params) throws IOException {
    String templateName = resolveTemplateName(params);
    String templateStr = getTemplateContent(templateName);

    // Replace all {{key}} with value
    for (Map.Entry<String, Object> entry : params.entrySet()) {
      if (entry.getValue() != null) {
        String regex = "\\{\\{" + Pattern.quote(entry.getKey()) + "\\}\\}";
        templateStr = templateStr.replaceAll(
          regex,
          Matcher.quoteReplacement(String.valueOf(entry.getValue()))
        );
      }
    }

    // Clean up unmatched placeholders
    templateStr = templateStr.replaceAll("\\{\\{[^}]*\\}\\}", "");

    return templateStr;
  }

  // Resout template name a partir du contexte disponible.

  private String resolveTemplateName(Map<String, Object> params) {
    if (params == null) {
      return INCIDENT_TEMPLATE;
    }

    String explicitTemplate = firstTextParam(
      params,
      "email_template",
      "template_name",
      "template"
    );
    if (StringUtils.hasText(explicitTemplate)) {
      String templateName = resolveExplicitTemplate(explicitTemplate);
      if (templateName != null) {
        return templateName;
      }
      log.warn(
        "Clé de modèle d'e-mail inconnue '{}', résolution automatique utilisée en repli",
        explicitTemplate
      );
    }

    if (params.containsKey("temporary_password")) {
      return CREDENTIAL_TEMPLATE;
    }
    if (params.containsKey("attachment_content")) {
      return looksLikeLateIncidentsReport(params)
        ? LATE_INCIDENTS_REPORT_TEMPLATE
        : GENERATED_REPORT_TEMPLATE;
    }
    if (looksLikeIncidentNotification(params)) {
      return INCIDENT_TEMPLATE;
    }
    return GENERIC_NOTIFICATION_TEMPLATE;
  }

  // Resout explicit template a partir du contexte disponible.

  private String resolveExplicitTemplate(String templateKey) {
    String normalized = templateKey
      .trim()
      .toLowerCase(Locale.ROOT)
      .replace(".html", "")
      .replace('-', '_');

    return switch (normalized) {
      case
        "credential",
        "credentials",
        "credential_template" -> CREDENTIAL_TEMPLATE;
      case
        "incident",
        "incident_report",
        "incident_notification",
        "incident_report_template" -> INCIDENT_TEMPLATE;
      case
        "generic",
        "notification",
        "generic_notification",
        "generic_notification_template" -> GENERIC_NOTIFICATION_TEMPLATE;
      case
        "generated_report",
        "report",
        "report_generated",
        "generated_report_template" -> GENERATED_REPORT_TEMPLATE;
      case
        "late_incidents_report",
        "late_incidents",
        "overdue_incidents_report",
        "late_incidents_report_template" -> LATE_INCIDENTS_REPORT_TEMPLATE;
      case
        "direction_validation",
        "direction_validation_template" -> DIRECTION_VALIDATION_TEMPLATE;
      default -> null;
    };
  }

  // Realise l'intention metier first text param.

  private String firstTextParam(Map<String, Object> params, String... names) {
    for (String name : names) {
      Object value = params.get(name);
      if (value != null && StringUtils.hasText(String.valueOf(value))) {
        return String.valueOf(value);
      }
    }
    return null;
  }

  // Verifie si late incidents report.

  private boolean looksLikeLateIncidentsReport(Map<String, Object> params) {
    StringBuilder signals = new StringBuilder();
    appendParam(signals, params, "attachment_name");
    appendParam(signals, params, "subject");
    appendParam(signals, params, "message");
    appendParam(signals, params, "report_name");

    String text = signals.toString().toLowerCase(Locale.ROOT);
    return (
      text.contains("incidents_en_retard") ||
      text.contains("incidents en retard") ||
      text.contains("late incidents") ||
      text.contains("overdue incidents")
    );
  }

  // Verifie si incident notification.

  private boolean looksLikeIncidentNotification(Map<String, Object> params) {
    return (
      params.containsKey("incident_reference") ||
      params.containsKey("incident_title") ||
      params.containsKey("incident_status") ||
      "block".equalsIgnoreCase(
        String.valueOf(params.get("display_incident_details"))
      )
    );
  }

  // Ajoute param.

  private void appendParam(
    StringBuilder builder,
    Map<String, Object> params,
    String name
  ) {
    Object value = params.get(name);
    if (value != null) {
      builder.append(' ').append(value);
    }
  }

  // Fournit template content a la couche appelante.

  private String getTemplateContent(String templateName) throws IOException {
    if (templateCache.containsKey(templateName)) {
      return templateCache.get(templateName);
    }

    // Charger via le classloader de la classe (LaunchedClassLoader en fat-jar Spring Boot).
    // L'envoi tourne sur ForkJoinPool.commonPool, dont le context classloader (system)
    // ne voit pas BOOT-INF/classes/ : sans ca, le template est introuvable a l'execution.
    ClassPathResource resource = new ClassPathResource(
      "templates/" + templateName,
      getClass().getClassLoader()
    );
    try (
      Reader reader = new InputStreamReader(
        resource.getInputStream(),
        StandardCharsets.UTF_8
      )
    ) {
      String content = FileCopyUtils.copyToString(reader);
      templateCache.put(templateName, content);
      return content;
    }
  }

  // Type MIME deduit de l'extension pour les pieces jointes.
  private String guessContentType(String fileName) {
    if (fileName == null) {
      return "application/octet-stream";
    }
    String lower = fileName.toLowerCase();
    if (lower.endsWith(".pdf")) return "application/pdf";
    if (lower.endsWith(".csv")) return "text/csv";
    if (
      lower.endsWith(".xlsx")
    ) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
    if (lower.endsWith(".txt")) return "text/plain";
    if (lower.endsWith(".json")) return "application/json";
    if (lower.endsWith(".png")) return "image/png";
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
    return "application/octet-stream";
  }

  // Verifie que les regles metier autorisent l operation sur notification.

  private void validateConfiguration(String recipient) {
    if (!StringUtils.hasText(mailUsername)) {
      log.error(
        "SMTP non configuré (spring.mail.username vide). E-mail non envoyé à {}",
        recipient
      );
      throw new IllegalStateException("La configuration SMTP est incomplete");
    }
  }
}
