// Contrat metier : expose les operations du domaine audit purge.

package com.fintrack.audit.service;

import com.fintrack.audit.model.readmodel.AuditPurgeResult;

// Purge manuelle des logs d'audit anciens. Sensibles toujours conserves.
// La purge reelle est elle-meme auditee (action AUDIT_PURGE) au nom de l'acteur.
public interface AuditPurgeService {
  // Supprime ou invalide les donnees ciblees apres controle metier.

  AuditPurgeResult purge(
    int olderThanDays,
    int preserveLastN,
    boolean dryRun,
    String actor
  );
}
