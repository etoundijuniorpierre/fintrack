// Tests frontend : verifie la conversion complete d'un type d'incident vers son formulaire.

import { describe, expect, it } from "vitest";
import type { IncidentTypeConfigResponse } from "../../../../api/settings/types";
import { mapIncidentTypeConfigToRequest } from "./incidentTypeConfigMapper";

// Construit une configuration complete representative d'une reponse backend.
const makeIncidentType = (): IncidentTypeConfigResponse => ({
  id: "type-1",
  name: "TECHNICAL_ERROR",
  displayName: "Technical Error",
  description: "Complete configuration",
  isActive: false,
  slaHours: 48,
  defaultTargetService: { id: "service-1", name: "IT Support" },
  defaultTargetUser: {
    id: "user-1",
    username: "assignee",
    email: "assignee@fintrack.com",
    firstName: "Default",
    lastName: "Assignee",
  },
  requiresValidation: true,
  requiresDirectionValidation: true,
  directionValidators: [
    {
      id: "validator-1",
      username: "validator",
      email: "validator@fintrack.com",
      firstName: "Direction",
      lastName: "Validator",
    },
  ],
  requiresCauseAnalysis: true,
  emailNotificationsEnabled: true,
  treaterRoles: ["CHEF_SERVICE"],
  resolverRoles: ["CREATOR"],
  closerRoles: ["CREATOR", "ASSIGNEE"],
  reopenerRoles: ["SOURCE_AGENCY_MANAGER"],
  validatorScope: "TARGET_SERVICE_MANAGER",
  defaultCriticality: "CRITICAL",
  createdAt: "2026-07-20T08:00:00",
  updatedAt: "2026-07-28T09:30:00",
});

describe("mapIncidentTypeConfigToRequest", () => {
  it("should preserve every configurable field without applying edit defaults", () => {
    expect(mapIncidentTypeConfigToRequest(makeIncidentType())).toEqual({
      name: "TECHNICAL_ERROR",
      displayName: "Technical Error",
      description: "Complete configuration",
      isActive: false,
      slaHours: 48,
      defaultTargetServiceId: "service-1",
      defaultTargetUserId: "user-1",
      requiresValidation: true,
      requiresDirectionValidation: true,
      directionValidatorIds: ["validator-1"],
      requiresCauseAnalysis: true,
      emailNotificationsEnabled: true,
      treaterRoles: ["CHEF_SERVICE"],
      resolverRoles: ["CREATOR"],
      closerRoles: ["CREATOR", "ASSIGNEE"],
      reopenerRoles: ["SOURCE_AGENCY_MANAGER"],
      validatorScope: "TARGET_SERVICE_MANAGER",
      defaultCriticality: "CRITICAL",
    });
  });

  it("should keep optional direction settings disabled when absent", () => {
    const incidentType = makeIncidentType();
    incidentType.requiresDirectionValidation = undefined;
    incidentType.directionValidators = undefined;

    expect(mapIncidentTypeConfigToRequest(incidentType)).toEqual(
      expect.objectContaining({
        requiresDirectionValidation: false,
        directionValidatorIds: [],
        isActive: false,
      }),
    );
  });
});
