// Service metier : orchestre les regles et traitements lies a async report generator.

package com.fintrack.reporting.service.impl;

import com.fintrack.reporting.model.entity.GeneratedReport;
import com.fintrack.reporting.report.ReportDocumentGenerator;
import com.fintrack.reporting.repository.GeneratedReportRepository;
import com.fintrack.reporting.service.ReportService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Génération asynchrone des rapports, isolée dans son propre bean.
 */
@Slf4j
@Component
@RequiredArgsConstructor
// Modelise la responsabilite applicative liee a rapport.
public class AsyncReportGenerator {

  private static final String REPORTS_API_PATH =
    "/api/v1/reportingService/reports/";

  private final GeneratedReportRepository repository;
  private final ObjectMapper objectMapper;
  private final ReportDocumentGenerator documentGenerator;

  @Autowired
  @Lazy
  private ReportService reportService;

  // Bornage de la stack trace conservee pour l'onglet Reporting (reglage infra).
  @Value("${fintrack.report.error-stack-max-frames:20}")
  private int errorStackMaxFrames;

  @Value("${fintrack.report.error-stack-max-chars:2048}")
  private int errorStackMaxChars;

  // Genere la sortie attendue pour le domaine async rapport generator.

  @Async
  public void generate(UUID reportId) {
    log.info(
      "Démarrage de la génération asynchrone du rapport pour l'identifiant : {}",
      reportId
    );
    try {
      GeneratedReport report = repository.findById(reportId).orElseThrow();

      // Le document est produit au telechargement ; ReportServiceImpl enrichit les
      // metriques avec le JWT courant avant d'appeler le generateur.
      Thread.sleep(2000);

      report.setStatus("AVAILABLE");
      byte[] content = documentGenerator.generate(report);
      report.setFileSize((long) content.length);
      report.setDownloadUrl(REPORTS_API_PATH + reportId + "/download");
      repository.save(report);

      log.info(
        "Génération du rapport terminée pour l'identifiant : {}",
        reportId
      );

      if (
        Boolean.TRUE.equals(report.getAutoSendEmail()) &&
        report.getEmailRecipients() != null &&
        !report.getEmailRecipients().isBlank()
      ) {
        try {
          List<String> recipients = objectMapper.readValue(
            report.getEmailRecipients(),
            new TypeReference<List<String>>() {}
          );
          reportService.sendEmail(reportId, recipients, null);
        } catch (Exception e) {
          log.error(
            "Échec d'envoi de e-mail automatique pour report {}",
            reportId,
            e
          );
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      markFailed(reportId, "Generation interrupted: " + e.getMessage());
    } catch (Exception e) {
      log.error("Échec de generer le rapport: {}", reportId, e);
      markFailed(reportId, formatError(e));
    }
  }

  // Applique le changement demande apres validation metier.

  private void markFailed(UUID reportId, String errorMessage) {
    repository.findById(reportId).ifPresent(r -> {
      r.setStatus("FAILED");
      r.setErrorMessage(errorMessage);
      repository.save(r);
    });
  }

  // Q16: serialise message + stack trace tronquee (env. 2 Kio) pour l'onglet Reporting.
  private String formatError(Throwable t) {
    StringBuilder sb = new StringBuilder();
    sb.append(t.getClass().getName())
      .append(": ")
      .append(t.getMessage())
      .append('\n');
    int frames = 0;
    for (StackTraceElement element : t.getStackTrace()) {
      sb.append("  at ").append(element.toString()).append('\n');
      if (++frames >= errorStackMaxFrames) {
        sb.append("  ... (truncated)");
        break;
      }
    }
    return sb.length() > errorStackMaxChars
      ? sb.substring(0, errorStackMaxChars) + "...(truncated)"
      : sb.toString();
  }
}
