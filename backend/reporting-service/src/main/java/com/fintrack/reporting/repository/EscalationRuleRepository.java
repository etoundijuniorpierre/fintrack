// Acces aux donnees : expose les requetes persistantes liees a escalation rule.

package com.fintrack.reporting.repository;

import com.fintrack.reporting.model.entity.EscalationRule;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Definit le contrat escalation rule attendu par les autres couches.

@Repository
public interface EscalationRuleRepository
  extends JpaRepository<EscalationRule, UUID>
{
  // Recherche les rapports par rule key.

  Optional<EscalationRule> findByRuleKey(String ruleKey);
}
