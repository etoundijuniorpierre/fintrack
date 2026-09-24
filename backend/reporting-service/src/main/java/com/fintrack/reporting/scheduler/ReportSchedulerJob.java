// Tache planifiee : execute les traitements automatiques lies a report scheduler job.

package com.fintrack.reporting.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.reporting.client.user.UserServiceClient;
import com.fintrack.reporting.client.user.dto.UserClientResponse;
import com.fintrack.reporting.model.constant.ReportGenerationType;
import com.fintrack.reporting.model.constant.ReportContentType;
import com.fintrack.reporting.model.entity.GeneratedReport;
import com.fintrack.reporting.model.entity.ReportSchedule;
import com.fintrack.reporting.repository.GeneratedReportRepository;
import com.fintrack.reporting.repository.ReportScheduleRepository;
import com.fintrack.reporting.service.impl.AsyncReportGenerator;
import com.fintrack.reporting.service.impl.AutomaticReportGroupingService;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// Porte la responsabilite applicative liee a planification de rapportr job.

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class ReportSchedulerJob {

  private final ReportScheduleRepository reportScheduleRepository;
  private final GeneratedReportRepository generatedReportRepository;
  private final AsyncReportGenerator asyncReportGenerator;
  private final AutomaticReportGroupingService automaticReportGroupingService;
  private final Clock applicationClock;
  private final UserServiceClient userServiceClient;
  private final ObjectMapper objectMapper = new ObjectMapper();

  // Genere la sortie attendue pour le domaine planification de rapportr job.

  @Scheduled(cron = "0 * * * * *", zone = "${fintrack.time-zone}")
  @Transactional
  public void generateScheduledReports() {
    runDueSchedules();
  }

  // Declenchement manuel (Super Admin) : force l'execution de tous les rapports planifies actifs
  // sans tenir compte de la date ou de l'heure prevue, et renvoie le nombre de rapports declenches.
  @Transactional
  public int triggerNow() {
    List<ReportSchedule> activeSchedules =
      reportScheduleRepository.findByIsActiveTrue();
    int triggered = 0;

    for (ReportSchedule schedule : activeSchedules) {
      try {
        log.info(
          "Déclenchement MANUEL forcé de la génération du rapport planifié pour la planification : {}",
          schedule.getId()
        );
        triggerReport(schedule);
        triggered++;
      } catch (Exception e) {
        log.error(
          "Erreur lors du déclenchement manuel de la planification {}",
          schedule.getId(),
          e
        );
      }
    }
    return triggered;
  }

  // Demarre ou initialise le traitement applicatif attendu.

  private int runDueSchedules() {
    LocalDateTime now = LocalDateTime.now(applicationClock);
    LocalTime currentTime = now.toLocalTime().withSecond(0).withNano(0);
    DayOfWeek currentDayOfWeek = now.getDayOfWeek();
    boolean isLastBusinessDayOfMonth = isLastBusinessDayOfMonth(
      now.toLocalDate()
    );

    List<ReportSchedule> activeSchedules =
      reportScheduleRepository.findByIsActiveTrue();
    int triggered = 0;

    for (ReportSchedule schedule : activeSchedules) {
      try {
        if (schedule.getSendTime() == null) continue;

        LocalTime scheduleTime = schedule
          .getSendTime()
          .withSecond(0)
          .withNano(0);

        if (!scheduleTime.equals(currentTime)) {
          continue;
        }

        boolean shouldRun = false;
        switch (schedule.getType()) {
          case DAILY:
            shouldRun = true;
            break;
          case WEEKLY:
            if (
              schedule.getWeekDay() != null &&
              schedule.getWeekDay() == currentDayOfWeek.getValue()
            ) {
              shouldRun = true;
            }
            break;
          case MONTHLY:
            // Un rapport mensuel part le dernier jour OUVRABLE (lundi-samedi) du
            // mois : si le mois se termine un dimanche, il part la veille (samedi).
            if (isLastBusinessDayOfMonth) {
              shouldRun = true;
            }
            break;
          default:
            break;
        }

        if (shouldRun) {
          log.info(
            "Déclenchement de la génération du rapport planifié pour la planification : {}",
            schedule.getId()
          );
          triggerReport(schedule);
          triggered++;
        }
      } catch (Exception e) {
        log.error(
          "Erreur lors du traitement de la planification {}",
          schedule.getId(),
          e
        );
      }
    }
    return triggered;
  }

  // Indique si la date est le dernier jour ouvrable (lundi-samedi) de son mois.
  // Seul le dimanche est non ouvrable : si le mois se termine un dimanche, le
  // dernier jour ouvrable est la veille (samedi).
  static boolean isLastBusinessDayOfMonth(LocalDate date) {
    LocalDate lastDay = date.withDayOfMonth(date.lengthOfMonth());
    LocalDate lastBusinessDay = lastDay.getDayOfWeek() == DayOfWeek.SUNDAY
      ? lastDay.minusDays(1)
      : lastDay;
    return date.equals(lastBusinessDay);
  }

  // Declenche report.

  private void triggerReport(ReportSchedule schedule) throws Exception {
    UserClientResponse creator = userServiceClient.getUserById(
      schedule.getCreatedBy()
    );
    Set<String> effectivePermissions =
      creator != null && creator.getPermissions() != null
        ? creator
            .getPermissions()
            .stream()
            .map(permission -> permission.toUpperCase(Locale.ROOT))
            .collect(Collectors.toSet())
        : Set.of();
    if (
      creator == null ||
      !creator.isActive() ||
      !effectivePermissions.contains("REPORT_GENERATE")
    ) {
      throw new SecurityException(
        "Le createur de la planification ne dispose plus de REPORT_GENERATE"
      );
    }

    GeneratedReport report = new GeneratedReport();
    LocalDate today = LocalDate.now(applicationClock);
    report.setName(schedule.getName() + " - " + today);
    report.setType(schedule.getType());
    report.setContentType(
      ReportContentType.orDefault(schedule.getContentType())
    );
    report.setFormat(schedule.getFormat());
    report.setGenerationType(ReportGenerationType.AUTOMATIC);
    report.setCreatedBy(schedule.getCreatedBy());
    applyReportPeriod(report, schedule.getType(), today);

    Map<String, Object> filters = new HashMap<>();
    Map<String, Object> innerFilters = new HashMap<>();
    innerFilters.put("view", schedule.getScope());
    filters.put("filters", innerFilters);
    filters.put(
      "requestedMetrics",
      List.of(
        "totalIncidents",
        "activeIncidents",
        "closedIncidents",
        "rejectedIncidents",
        "avgClosureHours",
        "assignedToMe"
      )
    );

    List<String> recipients = effectivePermissions.contains(
        "REPORT_SEND_EMAIL"
      )
      ? parseRecipients(schedule.getRecipientEmails())
      : List.of();
    filters.put("recipients", recipients);
    filters.put("documentLanguage", "fr");

    report.setFilters(objectMapper.writeValueAsString(filters));
    report.setAutoSendEmail(!recipients.isEmpty());
    report.setEmailRecipients(
      !recipients.isEmpty() ? objectMapper.writeValueAsString(recipients) : null
    );
    report.setStatus("PENDING");

    automaticReportGroupingService.populate(report, schedule.getScope());

    GeneratedReport saved = generatedReportRepository.save(report);

    schedule.setLastGeneratedAt(LocalDateTime.now(applicationClock));
    reportScheduleRepository.save(schedule);

    UUID reportId = saved.getId();
    runAfterCommit(() -> asyncReportGenerator.generate(reportId));
  }

  // Definit la periode couverte selon la frequence de la planification.
  private void applyReportPeriod(
    GeneratedReport report,
    com.fintrack.reporting.model.constant.ReportType type,
    LocalDate today
  ) {
    LocalDate start = switch (type) {
      case DAILY -> today;
      case WEEKLY -> today.minusDays(6);
      case MONTHLY -> today.withDayOfMonth(1);
      default -> today;
    };
    report.setPeriodStart(start.atStartOfDay());
    report.setPeriodEnd(today.atTime(23, 59, 59));
  }

  // Lit les donnees brutes du domaine planification de rapportr job dans un format exploitable.

  private List<String> parseRecipients(String recipientEmails) {
    if (recipientEmails == null || recipientEmails.isBlank()) {
      return List.of();
    }
    return Arrays.stream(recipientEmails.split("[,;\\s]+"))
      .map(String::trim)
      .filter(email -> !email.isBlank())
      .distinct()
      .toList();
  }

  // Demarre ou initialise le traitement applicatif attendu.

  private void runAfterCommit(Runnable action) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      action.run();
      return;
    }

    TransactionSynchronizationManager.registerSynchronization(
      new TransactionSynchronization() {
        @Override
        // Declenche l action differee apres validation de la transaction.
        public void afterCommit() {
          action.run();
        }
      }
    );
  }
}
