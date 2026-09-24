// Entite metier : represente les donnees persistees liees a report schedule.

package com.fintrack.reporting.model.entity;

import com.fintrack.reporting.model.constant.ReportFormat;
import com.fintrack.reporting.model.constant.ReportContentType;
import com.fintrack.reporting.model.constant.ReportType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// Entite representant la planification d'un rapport recurrent.

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "report_schedules")
@Access(AccessType.FIELD)
public class ReportSchedule extends BaseEntity {

  @NotBlank
  @Size(max = 100)
  @Column(nullable = false, length = 100)
  private String name;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private ReportType type;

  @Enumerated(EnumType.STRING)
  @Column(name = "content_type", length = 50)
  private ReportContentType contentType = ReportContentType.OPERATIONAL;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private ReportFormat format;

  @Column(name = "recipient_emails", columnDefinition = "TEXT")
  private String recipientEmails;

  @NotNull
  @Column(name = "send_time", nullable = false)
  private LocalTime sendTime;

  @Column(name = "week_day")
  private Integer weekDay;

  @NotBlank
  @Column(nullable = false, length = 50)
  private String scope;

  @Column(name = "is_active")
  private boolean isActive = true;

  @Column(name = "last_generated_at")
  private LocalDateTime lastGeneratedAt;

  @NotNull
  @Column(name = "created_by", nullable = false)
  private UUID createdBy;
}
