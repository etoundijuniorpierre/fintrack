// Client inter-services : communique avec les services externes lies a audit service client.

package com.fintrack.user.client.audit;

import com.fintrack.user.client.audit.dto.AuditLogClientRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Service wrapper autour du client Feign vers l'audit-service.
 * Quand une transaction métier est en cours, l'envoi n'est déclenché qu'après
 * son commit : un rollback (échec, refus d'autorisation) annule donc l'événement
 * au lieu de laisser un audit fantôme.
 */
@Slf4j
@Service
@RequiredArgsConstructor
// Porte les regles metier du domaine service interne.
public class AuditServiceClientService {

  private final AuditLogSender auditLogSender;

  // Construit un log d'audit complet et en differe l'envoi jusqu'a validation de la transaction.
  public void audit(
    UUID userId,
    String username,
    List<String> roles,
    String action,
    String resourceType,
    String resourceId,
    String status,
    String ipAddress,
    String userAgent,
    Map<String, Object> details
  ) {
    AuditLogClientRequest request = AuditLogClientRequest.builder()
      .userId(userId)
      .username(username)
      .roles(roles)
      .action(action)
      .resourceType(resourceType)
      .resourceId(resourceId)
      .status(status)
      .ipAddress(ipAddress)
      .userAgent(userAgent)
      .details(details)
      .build();

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          // Declenche l action differee apres validation de la transaction.
          public void afterCommit() {
            auditLogSender.send(request);
          }
        }
      );
    } else {
      auditLogSender.send(request);
    }
  }

  /** Raccourci pour les actions sans IP/UserAgent (appels internes). */
  // Realise l'intention metier audit.
  public void audit(
    UUID userId,
    String username,
    List<String> roles,
    String action,
    String resourceType,
    String resourceId,
    String status
  ) {
    audit(
      userId,
      username,
      roles,
      action,
      resourceType,
      resourceId,
      status,
      null,
      null,
      null
    );
  }

  /** Raccourci pour les actions internes avec détails (ex. instantané d'une entité supprimée). */
  // Realise l'intention metier audit.
  public void audit(
    UUID userId,
    String username,
    List<String> roles,
    String action,
    String resourceType,
    String resourceId,
    String status,
    Map<String, Object> details
  ) {
    audit(
      userId,
      username,
      roles,
      action,
      resourceType,
      resourceId,
      status,
      null,
      null,
      details
    );
  }
}
