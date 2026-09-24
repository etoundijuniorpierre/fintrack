// Entite metier : represente les donnees persistees d'un rapport genere.

package com.fintrack.reporting.model.entity;

import com.fintrack.reporting.model.constant.ReportFormat;
import com.fintrack.reporting.model.constant.ReportContentType;
import com.fintrack.reporting.model.constant.ReportGenerationType;
import com.fintrack.reporting.model.constant.ReportType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// Entite representant un fichier de rapport genere et archive.

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "generated_reports")
@Access(AccessType.FIELD)
public class GeneratedReport extends BaseEntity {

  @NotBlank
  @Column(nullable = false)
  private String name;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReportType type;

  @Enumerated(EnumType.STRING)
  @Column(name = "content_type", length = 50)
  private ReportContentType contentType = ReportContentType.OPERATIONAL;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReportFormat format;

  @Enumerated(EnumType.STRING)
  @Column(name = "generation_type")
  private ReportGenerationType generationType = ReportGenerationType.MANUAL;

  @Column(name = "period_start")
  private LocalDateTime periodStart;

  @Column(name = "period_end")
  private LocalDateTime periodEnd;

  @Column(nullable = false)
  private String status; // AVAILABLE, PENDING, FAILED

  @Column(name = "file_path")
  private String filePath;

  @Column(name = "file_size")
  private Long fileSize;

  @Column(name = "download_url")
  private String downloadUrl;

  @Column(name = "filters", columnDefinition = "TEXT")
  private String filters; // JSON string

  @Column(name = "metrics", columnDefinition = "TEXT")
  private String metrics; // JSON string

  @NotNull
  @Column(name = "created_by", nullable = false)
  private UUID createdBy;

  @Column(name = "agency_id")
  private UUID agencyId;

  @Column(name = "service_id")
  private UUID serviceId;

  @Column(name = "auto_send_email")
  private Boolean autoSendEmail;

  @Column(name = "email_recipients", columnDefinition = "TEXT")
  private String emailRecipients;

  // Stocke le detail d'echec du rapport lorsque le statut vaut FAILED.
  // Renseigne par ReportGenerationService lors du catch, lisible par les Super Admins
  // depuis l'onglet Reporting pour diagnostiquer sans aller dans les logs serveur.
  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;
}
