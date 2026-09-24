// Constantes metier : centralise les valeurs stables liees a incident action.

package com.fintrack.incident.model.constant;

// Actions metier autorisees dans le workflow d'un incident.
public enum IncidentAction {
  UPDATE,
  DELETE,
  VALIDATE,
  REJECT,
  TRANSFER,
  ASSIGN,
  START,
  BLOCK,
  RESUME,
  REQUEST_CONFIRMATION,
  CONFIRM_RELEVANCE,
  TREAT,
  MARK_UNRESOLVED,
  RESOLVE,
  CLOSE,
  REOPEN,
  RESUBMIT,
  CLONE,
  CANCEL,
  SUBMIT_SOLUTION,
  DIRECTION_VALIDATE,
  DIRECTION_REJECT,
}
