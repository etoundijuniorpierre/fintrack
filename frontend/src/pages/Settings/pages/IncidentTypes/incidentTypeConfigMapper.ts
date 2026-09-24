// Mapper frontend : transforme un type d'incident recu en formulaire de configuration complet.

import type {
  IncidentTypeConfigRequest,
  IncidentTypeConfigResponse,
} from "../../../../api/settings/types";

// Preserve chaque valeur configuree lors du passage en mode edition ou d'une action rapide.
export const mapIncidentTypeConfigToRequest = (
  incidentType: IncidentTypeConfigResponse,
): IncidentTypeConfigRequest => ({
  name: incidentType.name,
  displayName: incidentType.displayName,
  description: incidentType.description,
  isActive: incidentType.isActive,
  slaHours: incidentType.slaHours,
  defaultTargetServiceId: incidentType.defaultTargetService?.id,
  defaultTargetUserId: incidentType.defaultTargetUser?.id,
  requiresValidation: incidentType.requiresValidation,
  requiresDirectionValidation:
    incidentType.requiresDirectionValidation ?? false,
  directionValidatorIds:
    incidentType.directionValidators?.map((validator) => validator.id) ?? [],
  requiresCauseAnalysis: incidentType.requiresCauseAnalysis,
  emailNotificationsEnabled: incidentType.emailNotificationsEnabled,
  treaterRoles: incidentType.treaterRoles ?? [],
  resolverRoles: incidentType.resolverRoles ?? [],
  closerRoles: incidentType.closerRoles ?? [],
  reopenerRoles: incidentType.reopenerRoles ?? [],
  validatorScope: incidentType.validatorScope,
  defaultCriticality: incidentType.defaultCriticality,
});
