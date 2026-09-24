// Composant backend : porte la logique liee a repeated audit action.

package com.fintrack.audit.model.readmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Modele interne d'une action sensible repetee par un utilisateur.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepeatedAuditAction {

  private String key;
  private String username;
  private String action;
  private long count;
}
