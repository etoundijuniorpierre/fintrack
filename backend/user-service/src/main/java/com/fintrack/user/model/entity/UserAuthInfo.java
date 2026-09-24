// Entite metier : represente les donnees persistees liees a user auth info.

package com.fintrack.user.model.entity;

import java.time.LocalDateTime;
import java.util.UUID;

// Projection interne minimale utilisee par l'authentification.
public interface UserAuthInfo {
  // Fournit id a la couche appelante.
  UUID getId();
  // Fournit username a la couche appelante.
  String getUsername();
  // Fournit password a la couche appelante.
  String getPassword();
  // Fournit temp password a la couche appelante.
  String getTempPassword();
  // Fournit active a la couche appelante.
  Boolean getActive();
  // Fournit first login a la couche appelante.
  Boolean getFirstLogin();
  // Expose les tentatives de connexion echouees a la couche appelante.
  Integer getFailedLoginAttempts();
  /** Date de dernière modification, utilisée pour l'âge du mot de passe temporaire. */
  // Fournit mise a jour at au cas d usage appelant.
  LocalDateTime getUpdatedAt();
  LocalDateTime getTempPasswordCreatedAt();
}
