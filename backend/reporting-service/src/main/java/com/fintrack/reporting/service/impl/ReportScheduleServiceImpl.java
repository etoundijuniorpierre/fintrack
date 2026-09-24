// Service metier : coordonne les operations du domaine planification de rapport.

package com.fintrack.reporting.service.impl;

import com.fintrack.reporting.client.audit.AuditServiceClientService;
import com.fintrack.reporting.client.audit.constant.AuditAction;
import com.fintrack.reporting.client.audit.constant.AuditStatus;
import com.fintrack.reporting.exception.BusinessRuleViolationException;
import com.fintrack.reporting.exception.EntityNotFoundException;
import com.fintrack.reporting.exception.ErrorCode;
import com.fintrack.reporting.model.constant.ReportFormat;
import com.fintrack.reporting.model.constant.ReportType;
import com.fintrack.reporting.model.constant.ReportContentType;
import com.fintrack.reporting.model.entity.ReportSchedule;
import com.fintrack.reporting.repository.ReportScheduleRepository;
import com.fintrack.reporting.service.ReportScheduleService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Implementation du service de planification des rapports.

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportScheduleServiceImpl implements ReportScheduleService {

  private final ReportScheduleRepository reportScheduleRepository;
  private final AuditServiceClientService auditServiceClientService;
  private final MessageSource messageSource;

  private String t(String key, Object... args) {
    return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
  }

  @Override
  @Transactional(readOnly = true)
  // Liste les planifications de rapport selon le perimetre demande.
  public Page<ReportSchedule> findAll(Pageable pageable) {
    return reportScheduleRepository.findAll(pageable);
  }

  @Override
  @Transactional(readOnly = true)
  // Liste les planifications de rapport selon le perimetre demande.
  public List<ReportSchedule> findAll() {
    return reportScheduleRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  // Recherche les planifications de rapport par identifiant.
  public ReportSchedule findById(UUID id) {
    return reportScheduleRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException(
          ErrorCode.REPORT_SCHEDULE_NOT_FOUND,
          t("reporting.error.schedule_not_found", id)
        )
      );
  }

  @Override
  @Transactional(readOnly = true)
  // Recherche les planifications de rapport par creation by.
  public List<ReportSchedule> findByCreatedBy(UUID createdBy) {
    return reportScheduleRepository.findByCreatedBy(createdBy);
  }

  // Recherche les planifications de rapport par creation by avec pagination.

  @Override
  @Transactional(readOnly = true)
  public Page<ReportSchedule> findByCreatedBy(
    UUID createdBy,
    Pageable pageable
  ) {
    return reportScheduleRepository.findByCreatedBy(createdBy, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  // Recherche les planifications de rapport pour actif.
  public List<ReportSchedule> findActive() {
    return reportScheduleRepository.findByIsActiveTrue();
  }

  // Recherche les planifications de rapport pour actif by creation by.

  @Override
  @Transactional(readOnly = true)
  public List<ReportSchedule> findActiveByCreatedBy(UUID createdBy) {
    return reportScheduleRepository.findByCreatedByAndIsActiveTrue(createdBy);
  }

  @Override
  @Transactional
  // Cree un element du domaine planification de rapport apres validation metier.
  public ReportSchedule create(ReportSchedule schedule, UUID createdBy) {
    assertScheduleAllowedTypeAndFormat(schedule);
    schedule.setContentType(
      ReportContentType.orDefault(schedule.getContentType())
    );
    schedule.setCreatedBy(createdBy);
    schedule.setActive(true);
    log.info(
      "Création de planification de rapport '{}' pour user: {}",
      schedule.getName(),
      createdBy
    );
    ReportSchedule saved = reportScheduleRepository.save(schedule);
    auditServiceClientService.audit(
      createdBy,
      null,
      null,
      AuditAction.REPORT_ACCESS.getName(),
      "REPORT_SCHEDULE",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName()
    );
    return saved;
  }

  @Override
  @Transactional
  // Met a jour un element du domaine planification de rapport avec les donnees validees.
  public ReportSchedule update(UUID id, ReportSchedule details) {
    assertScheduleAllowedTypeAndFormat(details);
    ReportSchedule schedule = findById(id);
    schedule.setName(details.getName());
    schedule.setType(details.getType());
    if (details.getContentType() != null) {
      schedule.setContentType(details.getContentType());
    }
    schedule.setFormat(details.getFormat());
    schedule.setRecipientEmails(details.getRecipientEmails());
    schedule.setSendTime(details.getSendTime());
    schedule.setWeekDay(details.getWeekDay());
    schedule.setScope(details.getScope());
    return reportScheduleRepository.saveAndFlush(schedule);
  }

  @Override
  @Transactional
  // Bascule l'activation d'une planification de rapport.
  public ReportSchedule toggleActive(UUID id) {
    ReportSchedule schedule = findById(id);
    schedule.setActive(!schedule.isActive());
    log.info(
      "Statut actif de la planification de rapport {} basculé à : {}",
      id,
      schedule.isActive()
    );
    return reportScheduleRepository.saveAndFlush(schedule);
  }

  @Override
  @Transactional
  // Supprime un element du domaine planification de rapport apres controle metier.
  public void delete(UUID id) {
    if (!reportScheduleRepository.existsById(id)) {
      throw new EntityNotFoundException(
        ErrorCode.REPORT_SCHEDULE_NOT_FOUND,
        t("reporting.error.schedule_not_found", id)
      );
    }
    reportScheduleRepository.deleteById(id);
    log.info("Supprimé planification de rapport avec id: {}", id);
  }

  // Un schedule n'accepte que DAILY/WEEKLY/MONTHLY + PDF/EXCEL + scope defini.
  private static final Set<String> ALLOWED_SCOPES = Set.of(
    "own",
    "agency",
    "service",
    "user",
    "all"
  );

  // Verifie que les regles metier autorisent l operation sur planification de rapport.

  private void assertScheduleAllowedTypeAndFormat(ReportSchedule schedule) {
    ReportType type = schedule.getType();
    if (type == null || type == ReportType.CUSTOM) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        t("reporting.error.schedule_invalid_type")
      );
    }
    ReportFormat format = schedule.getFormat();
    if (format == null || format == ReportFormat.JSON) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        t("reporting.error.schedule_invalid_format")
      );
    }
    String scope = schedule.getScope();
    if (scope == null || !ALLOWED_SCOPES.contains(scope.toLowerCase())) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        t("reporting.error.schedule_invalid_scope")
      );
    }
    if (type == ReportType.WEEKLY) {
      Integer weekDay = schedule.getWeekDay();
      if (weekDay == null || weekDay < 1 || weekDay > 7) {
        throw new BusinessRuleViolationException(
          ErrorCode.INVALID_INPUT,
          t("reporting.error.schedule_invalid_weekday")
        );
      }
    }
  }
}
