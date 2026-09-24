// Entite metier : represente les donnees persistees liees a report download.

package com.fintrack.reporting.model.entity;

import com.fintrack.reporting.model.constant.ReportFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

// Modele interne representant le fichier genere pret au telechargement.
@Data
@AllArgsConstructor
public class ReportDownload {

  private byte[] content;
  private String filename;
  private ReportFormat format;
}
