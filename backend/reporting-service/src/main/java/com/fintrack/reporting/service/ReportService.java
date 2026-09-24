// Contrat de service : definit les operations metier liees aux rapports.

package com.fintrack.reporting.service;

import com.fintrack.reporting.model.entity.GeneratedReport;
import com.fintrack.reporting.model.entity.ReportDownload;
import com.fintrack.reporting.security.UserDetailsImpl;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// Contrat du service de generation, telechargement, envoi et relance des rapports.
public interface ReportService {
  // Liste les rapports visibles selon les droits et le perimetre choisi.
  Page<GeneratedReport> findAll(
    UserDetailsImpl currentUser,
    String scope,
    String keyword,
    String type,
    String generationType,
    String format,
    String status,
    Boolean missingFile,
    Pageable pageable
  );
  // Planifie la generation manuelle d'un rapport pour l'utilisateur courant.
  GeneratedReport generate(GeneratedReport report, UserDetailsImpl currentUser);
  // Prepare le fichier telechargeable d'un rapport autorise.
  ReportDownload download(UUID id, UserDetailsImpl currentUser);
  // Envoie un rapport genere aux destinataires valides.
  void sendEmail(UUID id, List<String> recipients, UserDetailsImpl currentUser);
  // Supprime un rapport apres verification des droits d'acces.
  void delete(UUID id, UserDetailsImpl currentUser);
  // Relance la generation d'un rapport existant.
  GeneratedReport rerun(UUID id, UserDetailsImpl currentUser);
}
