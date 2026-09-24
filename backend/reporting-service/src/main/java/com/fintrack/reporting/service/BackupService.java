// Contrat metier : expose l'execution des sauvegardes systeme.

package com.fintrack.reporting.service;

import com.fintrack.reporting.model.readmodel.superadmin.BackupOutcome;

// Sauvegarde des deux bases puis synchronisation vers Backblaze B2.
public interface BackupService {
  BackupOutcome run();
}
