// Acces aux donnees : expose les requetes persistantes liees a escalation rule event.

package com.fintrack.reporting.repository;

import com.fintrack.reporting.model.entity.EscalationRuleEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Definit le contrat escalation rule event attendu par les autres couches.

@Repository
public interface EscalationRuleEventRepository
  extends JpaRepository<EscalationRuleEvent, UUID>
{
  // Recherche les rapports par rule key order by evaluated at desc.

  List<EscalationRuleEvent> findByRuleKeyOrderByEvaluatedAtDesc(
    String ruleKey,
    Pageable pageable
  );
  // Selectionne les rapports recents pour top50 by order by evaluated at desc.

  List<EscalationRuleEvent> findTop50ByOrderByEvaluatedAtDesc();
}
