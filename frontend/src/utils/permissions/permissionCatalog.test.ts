// Tests : catalogue des permissions (domaine fonctionnel, cle i18n, regroupement).

import { describe, it, expect } from "vitest";
import {
  PERMISSION_GROUPS,
  PERMISSION_CATALOG,
  getPermissionGroup,
  permissionDescriptionKey,
  groupPermissions,
} from "./permissionCatalog";

describe("PERMISSION_CATALOG", () => {
  it("should map each code to its functional group", () => {
    expect(PERMISSION_CATALOG["INCIDENT_CREATE"]).toBe("incident");
    expect(PERMISSION_CATALOG["USER_VIEW_ALL"]).toBe("user");
    expect(PERMISSION_CATALOG["ROLE_CREATE"]).toBe("role");
  });

  it("should only use groups declared in PERMISSION_GROUPS", () => {
    for (const group of Object.values(PERMISSION_CATALOG)) {
      expect(PERMISSION_GROUPS).toContain(group);
    }
  });
});

describe("getPermissionGroup", () => {
  it("should return the known group for a referenced code", () => {
    expect(getPermissionGroup("INCIDENT_CREATE")).toBe("incident");
  });

  it("should fall back to settings for an unknown code", () => {
    expect(getPermissionGroup("UNKNOWN_CODE")).toBe("settings");
  });
});

describe("permissionDescriptionKey", () => {
  it("should build the expected i18n key", () => {
    expect(permissionDescriptionKey("INCIDENT_CREATE")).toBe(
      "help.permissions.INCIDENT_CREATE",
    );
  });
});

describe("groupPermissions", () => {
  it("should group codes by domain in display order", () => {
    const result = groupPermissions(["USER_VIEW_ALL", "INCIDENT_CREATE"]);
    expect(result.map((g) => g.group)).toEqual(["incident", "user"]);
  });

  it("should sort codes within each domain", () => {
    const [incidentGroup] = groupPermissions([
      "INCIDENT_VIEW_ALL",
      "INCIDENT_CREATE",
    ]);
    expect(incidentGroup.codes).toEqual([
      "INCIDENT_CREATE",
      "INCIDENT_VIEW_ALL",
    ]);
  });

  it("should omit domains with no code", () => {
    const result = groupPermissions(["INCIDENT_CREATE"]);
    expect(result).toHaveLength(1);
    expect(result[0].group).toBe("incident");
  });

  it("should return an empty array for an empty list", () => {
    expect(groupPermissions([])).toEqual([]);
  });
});
