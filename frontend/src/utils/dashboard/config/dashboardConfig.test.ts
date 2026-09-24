// Tests frontend : verifie le comportement de tableau de bord config.test.

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { type DashboardConfig, WidgetType } from "../../../types/dashboard";
import { PERMISSIONS } from "../../permissions/permissions";
import {
  getEffectiveWidgets,
  loadDashboardConfig,
  resetDashboardConfig,
  saveDashboardConfig,
} from "./dashboardConfig";

// Fabrique une fixture de test pour tableau de bord config.test.
const makeConfig = (
  userId: string,
  visibleWidgets: WidgetType[],
): DashboardConfig => ({
  userId,
  visibleWidgets,
  lastUpdated: "2024-01-01T00:00:00.000Z",
});

// Reproduit la cle de stockage attendue par les assertions.
const storageKey = (userId: string) => `dashboard_config_${userId}`;

describe("dashboardConfig", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it("loads null when no config exists or when JSON is invalid", () => {
    expect(loadDashboardConfig("user-1")).toBeNull();

    localStorage.setItem(storageKey("user-1"), "not-json");
    expect(loadDashboardConfig("user-1")).toBeNull();
  });

  it("saves and resets a dashboard config per user", () => {
    const config = makeConfig("user-1", [WidgetType.INCIDENT_ACTIVE]);

    saveDashboardConfig(config);
    expect(loadDashboardConfig("user-1")).toEqual(config);

    resetDashboardConfig("user-1");
    expect(loadDashboardConfig("user-1")).toBeNull();
  });

  it("returns all permitted widgets when no config is saved", () => {
    const result = getEffectiveWidgets("user-1", [
      PERMISSIONS.INCIDENT.VIEW_ALL,
      PERMISSIONS.INCIDENT.VALIDATE,
      PERMISSIONS.INCIDENT.RESOLVE,
    ]);

    expect(result).toContain(WidgetType.INCIDENT_ACTIVE);
    expect(result).toContain(WidgetType.INCIDENT_REJECTED);
    expect(result).toContain(WidgetType.INCIDENT_ASSIGNED_TO_ME);
    expect(result).toContain(WidgetType.INCIDENT_TOP_SERVICES);
  });

  it("filters saved widgets by current permissions", () => {
    const config = makeConfig("user-1", [
      WidgetType.INCIDENT_ACTIVE,
      WidgetType.INCIDENT_REJECTED,
      WidgetType.INCIDENT_TOP_SERVICES,
    ]);
    saveDashboardConfig(config);

    expect(
      getEffectiveWidgets("user-1", [PERMISSIONS.INCIDENT.VIEW_OWN]),
    ).toEqual([WidgetType.INCIDENT_ACTIVE, WidgetType.INCIDENT_REJECTED]);
  });

  it("keeps user configs isolated", () => {
    const config1 = makeConfig("user-1", [WidgetType.INCIDENT_ACTIVE]);
    const config2 = makeConfig("user-2", [WidgetType.INCIDENT_TOTAL]);

    saveDashboardConfig(config1);
    saveDashboardConfig(config2);

    expect(loadDashboardConfig("user-1")).toEqual(config1);
    expect(loadDashboardConfig("user-2")).toEqual(config2);
  });
});
