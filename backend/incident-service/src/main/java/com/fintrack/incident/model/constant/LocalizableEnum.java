// Constantes metier : centralise les valeurs stables liees a localizable enum.

package com.fintrack.incident.model.constant;

// Contrat des enumerations metier exposables via l'API avec libelle et description traduits.
public interface LocalizableEnum {
  // Fournit name a la couche appelante.
  String getName();
  // Fournit name key a la couche appelante.
  String getNameKey();
  // Fournit description key a la couche appelante.
  String getDescriptionKey();
}
