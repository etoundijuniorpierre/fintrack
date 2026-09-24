// Contrat metier : expose les operations du domaine escalation rule.

package com.fintrack.reporting.service;

import com.fintrack.reporting.model.entity.EscalationRule;
import com.fintrack.reporting.security.UserDetailsImpl;
import java.util.List;
import java.util.Map;

// Definit le contrat escalation rule attendu par les autres couches.

public interface EscalationRuleService {
  // Fournit rules a la couche appelante.

  List<EscalationRule> getRules();
  // Applique le changement demande apres validation metier.

  EscalationRule toggleRule(String key, boolean enabled, UserDetailsImpl actor);
  // Realise l'intention metier evaluate and dispatch.

  Map<String, Object> evaluateAndDispatch(boolean dispatchNotifications);
  // Fournit recent events a la couche appelante.

  List<Map<String, Object>> getRecentEvents(int limit);
  // Construit la representation attendue pour le domaine escalation rule.

  List<Map<String, Object>> buildRuleSummary();
}
