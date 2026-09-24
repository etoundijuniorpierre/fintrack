// Client inter-services : communique avec les services externes lies a audit service client.

package com.fintrack.reporting.client.audit;

import com.fintrack.reporting.client.audit.dto.AuditLogClientRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
// Construit les journaux d'audit et n'en declenche l'envoi qu'apres validation
// effective de la transaction metier (un rollback annule l'evenement).
public class AuditServiceClientService {

  private final AuditLogSender auditLogSender;

  // Construit un journal d'audit complet. Si une transaction est en cours, l'envoi
  // est differe a apres son commit : un rollback (echec, refus d'autorisation)
  // annule donc l'evenement au lieu de laisser un audit fantome.
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
    AuditLogClientRequest request = AuditLogClientRequest.builder()
      .userId(userId)
      .username(username)
      .roles(roles)
      .action(action)
      .resourceType(resourceType)
      .resourceId(resourceId)
      .status(status)
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

  // Variante sans details complementaires
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
      null
    );
  }
}
