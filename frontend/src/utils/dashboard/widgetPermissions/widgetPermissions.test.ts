// Tests frontend : verifie le comportement de widget permissions.test.

import { describe, expect, it } from "vitest";
import { WidgetType } from "../../../types/dashboard";
import { PERMISSIONS } from "../../permissions/permissions";
import {
  WIDGET_PERMISSION_RULES,
  getVisibleWidgets,
  isWidgetVisible,
} from "./widgetPermissions";

const incidentScopeWidgets: WidgetType[] = [
  WidgetType.INCIDENT_ACTIVE,
  WidgetType.INCIDENT_CLOSED,
  WidgetType.INCIDENT_REJECTED,
  WidgetType.INCIDENT_TOTAL,
  WidgetType.INCIDENT_AVG_RESOLUTION,
  WidgetType.INCIDENT_TYPE_DISTRIBUTION,
  WidgetType.INCIDENT_CRITICALITY_DISTRIBUTION,
  WidgetType.INCIDENT_RECENT_ACTIVITY,
  // Les series mensuelles suivent le perimetre demande depuis la refonte des
  // metriques : elles ne sont plus reservees aux porteurs de VIEW_ALL.
  WidgetType.INCIDENT_MONTHLY_CLOSURES,
  WidgetType.INCIDENT_MONTHLY_AVG_CLOSURE,
];

describe("widgetPermissions", () => {
  it("covers every dashboard widget", () => {
    Object.values(WidgetType).forEach((widget) => {
      expect(WIDGET_PERMISSION_RULES[widget]).toBeDefined();
    });
  });

  it("shows incident scope widgets for any incident view permission", () => {
    const permissions = [PERMISSIONS.INCIDENT.VIEW_SERVICE];

    incidentScopeWidgets.forEach((widget) => {
      expect(isWidgetVisible(widget, permissions)).toBe(true);
    });
  });

  it("keeps assignment and top service widgets permission-specific", () => {
    expect(
      isWidgetVisible(WidgetType.INCIDENT_ASSIGNED_TO_ME, [
        PERMISSIONS.INCIDENT.VIEW_OWN,
      ]),
    ).toBe(false);
    expect(
      isWidgetVisible(WidgetType.INCIDENT_ASSIGNED_TO_ME, [
        PERMISSIONS.INCIDENT.RESOLVE,
      ]),
    ).toBe(true);

    expect(
      isWidgetVisible(WidgetType.INCIDENT_TOP_SERVICES, [
        PERMISSIONS.INCIDENT.VIEW_AGENCY,
      ]),
    ).toBe(false);
    expect(
      isWidgetVisible(WidgetType.INCIDENT_TOP_SERVICES, [
        PERMISSIONS.INCIDENT.VIEW_SERVICE,
      ]),
    ).toBe(false);
    expect(
      isWidgetVisible(WidgetType.INCIDENT_TOP_SERVICES, [
        PERMISSIONS.INCIDENT.VIEW_ALL,
      ]),
    ).toBe(true);
  });

  it("restricts workload and scorecard to supervising scopes", () => {
    expect(
      isWidgetVisible(WidgetType.INCIDENT_WORKLOAD, [
        PERMISSIONS.INCIDENT.VIEW_OWN,
      ]),
    ).toBe(false);
    expect(
      isWidgetVisible(WidgetType.INCIDENT_WORKLOAD, [
        PERMISSIONS.INCIDENT.VIEW_SERVICE,
      ]),
    ).toBe(true);
    expect(
      isWidgetVisible(WidgetType.INCIDENT_SCORECARD, [
        PERMISSIONS.INCIDENT.VIEW_OWN,
      ]),
    ).toBe(false);
    expect(
      isWidgetVisible(WidgetType.INCIDENT_SCORECARD, [
        PERMISSIONS.INCIDENT.VIEW_ALL,
      ]),
    ).toBe(true);
  });

  it("exposes the new director widgets for incident view permissions", () => {
    [
      WidgetType.INCIDENT_AGING,
      WidgetType.INCIDENT_COHORT,
      WidgetType.INCIDENT_SLA_BREACH_NOW,
      WidgetType.INCIDENT_INFLOW_OUTFLOW,
      WidgetType.INCIDENT_CREATED_BY_ME,
      WidgetType.INCIDENT_RESOLVED_BY_ME,
    ].forEach((widget) => {
      expect(isWidgetVisible(widget, [PERMISSIONS.INCIDENT.VIEW_SERVICE])).toBe(
        true,
      );
    });
  });

  it("returns only visible widgets from the permission set", () => {
    const result = getVisibleWidgets([
      PERMISSIONS.INCIDENT.VIEW_OWN,
      PERMISSIONS.INCIDENT.TREAT,
    ]);

    expect(result).toContain(WidgetType.INCIDENT_ACTIVE);
    expect(result).toContain(WidgetType.INCIDENT_ASSIGNED_TO_ME);
    expect(result).not.toContain(WidgetType.INCIDENT_TOP_SERVICES);
  });

  it("checks permission names case-insensitively", () => {
    expect(
      isWidgetVisible(WidgetType.INCIDENT_ACTIVE, ["incident_view_own"]),
    ).toBe(true);
  });
});
