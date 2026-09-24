// Constantes metier : centralise les valeurs stables liees a action type.

package com.fintrack.incident.model.constant;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Enumeration definissant les types d'actions sur les incidents.

@Getter
@RequiredArgsConstructor
public enum ActionType implements LocalizableEnum {
  CREATION(
    "CREATION",
    "enum.action_type.CREATION.name",
    "enum.action_type.CREATION.description"
  ),
  VALIDATION(
    "VALIDATION",
    "enum.action_type.VALIDATION.name",
    "enum.action_type.VALIDATION.description"
  ),
  TRANSFER(
    "TRANSFER",
    "enum.action_type.TRANSFER.name",
    "enum.action_type.TRANSFER.description"
  ),
  STATUS_CHANGE(
    "STATUS_CHANGE",
    "enum.action_type.STATUS_CHANGE.name",
    "enum.action_type.STATUS_CHANGE.description"
  ),
  COMMENT(
    "COMMENT",
    "enum.action_type.COMMENT.name",
    "enum.action_type.COMMENT.description"
  ),
  UPDATE(
    "UPDATE",
    "enum.action_type.UPDATE.name",
    "enum.action_type.UPDATE.description"
  ),
  ASSIGNMENT(
    "ASSIGNMENT",
    "enum.action_type.ASSIGNMENT.name",
    "enum.action_type.ASSIGNMENT.description"
  ),
  ROUTING(
    "ROUTING",
    "enum.action_type.ROUTING.name",
    "enum.action_type.ROUTING.description"
  ),
  TYPE_CHANGE(
    "TYPE_CHANGE",
    "enum.action_type.TYPE_CHANGE.name",
    "enum.action_type.TYPE_CHANGE.description"
  ),
  CRITICALITY_CHANGE(
    "CRITICALITY_CHANGE",
    "enum.action_type.CRITICALITY_CHANGE.name",
    "enum.action_type.CRITICALITY_CHANGE.description"
  ),
  CONFIRMATION_REQUEST(
    "CONFIRMATION_REQUEST",
    "enum.action_type.CONFIRMATION_REQUEST.name",
    "enum.action_type.CONFIRMATION_REQUEST.description"
  ),
  REOPENING(
    "REOPENING",
    "enum.action_type.REOPENING.name",
    "enum.action_type.REOPENING.description"
  ),
  RESOLUTION_REJECTED(
    "RESOLUTION_REJECTED",
    "enum.action_type.RESOLUTION_REJECTED.name",
    "enum.action_type.RESOLUTION_REJECTED.description"
  );

  private final String name;
  private final String nameKey;
  private final String descriptionKey;

  public static String[] getAllNames() {
    return Arrays.stream(values())
      .map(ActionType::getName)
      .toArray(String[]::new);
  }
}
