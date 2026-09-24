// Client inter-services : communique avec les services externes lies a audit service client.

package com.fintrack.incident.client.audit;

import com.fintrack.incident.client.audit.dto.AuditLogClientRequest;
import com.fintrack.incident.security.UserDetailsImpl;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
// Service utilitaire qui construit les journaux d'audit et n'en declenche l'envoi
// qu'apres validation effective de la transaction metier.
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
    String resolvedUsername = username;
    List<String> resolvedRoles = roles;

    Authentication authentication =
      SecurityContextHolder.getContext().getAuthentication();
    if (
      authentication != null &&
      authentication.getPrincipal() instanceof UserDetailsImpl userDetails
    ) {
      if (resolvedUsername == null) {
        resolvedUsername = userDetails.getUsername();
      }
      if (resolvedRoles == null || resolvedRoles.isEmpty()) {
        resolvedRoles = userDetails
          .getAuthorities()
          .stream()
          .map(GrantedAuthority::getAuthority)
          .collect(Collectors.toList());
      }
    }

    AuditLogClientRequest request = AuditLogClientRequest.builder()
      .userId(userId)
      .username(resolvedUsername)
      .roles(resolvedRoles)
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
