// Constantes metier : centralise les valeurs stables liees a audit status.

package com.fintrack.audit.model.constant;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Statut du résultat d'une action auditée.
 */
@Getter
@RequiredArgsConstructor
// Modelise la responsabilite applicative liee a journal d'audit.
public enum AuditStatus implements LocalizableEnum {
  SUCCESS(
    "SUCCESS",
    "enum.audit_status.SUCCESS.name",
    "enum.audit_status.SUCCESS.description"
  ),
  FAILURE(
    "FAILURE",
    "enum.audit_status.FAILURE.name",
    "enum.audit_status.FAILURE.description"
  );

  /** Identifiant technique stable (stocké en base). */
  private final String name;
  /** Clé i18n pour le libellé affiché. */
  private final String nameKey;
  /** Clé i18n pour la description affichée. */
  private final String descriptionKey;

  // Fournit all names a la couche appelante.

  public static String[] getAllNames() {
    return Arrays.stream(values())
      .map(AuditStatus::getName)
      .toArray(String[]::new);
  }
}
