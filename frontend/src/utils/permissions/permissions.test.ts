// Tests frontend : verifie le comportement de permissions.test.

import { describe, expect, it } from "vitest";
import {
  getDashboardViewOptions,
  getIncidentViewOptions,
  getPreferredView,
  getReportScopeOptions,
  getEffectivePermissionNames,
  hasAnyPermissionName,
  PERMISSIONS,
  INCIDENT_ACCESS_PERMISSIONS,
} from "./permissions";

// Simule la fonction de traduction pour les libelles de permissions.
const t = (key: string) => key;
// Cree un verificateur de permissions pour les cas de test.
const checker = (permissions: string[]) => (permission: string) =>
  permissions.includes(permission);

describe("permissions utilities", () => {
  it("computes effective permissions as (direct union role) minus revoked", () => {
    const treat = { id: "p1", name: "INCIDENT_TREAT" };
    const view = { id: "p2", name: "USER_VIEW_ALL" };
    const names = getEffectivePermissionNames({
      permissions: [view],
      roles: [{ permissions: [treat] }],
      revokedPermissions: [treat],
    });
    expect(names).toContain("USER_VIEW_ALL");
    expect(names).not.toContain("INCIDENT_TREAT");
  });

  it("builds dashboard scope labels from permissions", () => {
    const options = getDashboardViewOptions(
      checker([PERMISSIONS.INCIDENT.VIEW_AGENCY]),
      t as never,
    );

    expect(options).toEqual([
      { label: "dashboard.views.agency", value: "agency" },
    ]);
  });

  it("keeps agency and service options when global view is available", () => {
    const options = getDashboardViewOptions(
      checker([
        PERMISSIONS.INCIDENT.VIEW_OWN,
        PERMISSIONS.INCIDENT.VIEW_AGENCY,
        PERMISSIONS.INCIDENT.VIEW_SERVICE,
        PERMISSIONS.INCIDENT.VIEW_ALL,
      ]),
      t as never,
    );

    expect(options).toContainEqual({
      label: "dashboard.views.own",
      value: "own",
    });
    expect(options).toContainEqual({
      label: "dashboard.views.agency",
      value: "agency",
    });
    expect(options).toContainEqual({
      label: "dashboard.views.service",
      value: "service",
    });
    expect(options).toContainEqual({
      label: "dashboard.views.all",
      value: "all",
    });
  });

  it("hides agency/service scopes for a non-chef admin (permission without membership)", () => {
    const options = getDashboardViewOptions(
      checker([
        PERMISSIONS.INCIDENT.VIEW_AGENCY,
        PERMISSIONS.INCIDENT.VIEW_SERVICE,
        PERMISSIONS.INCIDENT.VIEW_ALL,
      ]),
      t as never,
      { hasAgency: false, hasService: false },
    );

    expect(options).toEqual([{ label: "dashboard.views.all", value: "all" }]);
  });

  it("keeps only the membership-matching scope for a dual-role chef-admin", () => {
    const options = getDashboardViewOptions(
      checker([
        PERMISSIONS.INCIDENT.VIEW_AGENCY,
        PERMISSIONS.INCIDENT.VIEW_SERVICE,
        PERMISSIONS.INCIDENT.VIEW_ALL,
      ]),
      t as never,
      { hasAgency: true, hasService: false },
    );

    expect(options).toContainEqual({
      label: "dashboard.views.agency",
      value: "agency",
    });
    expect(options).not.toContainEqual({
      label: "dashboard.views.service",
      value: "service",
    });
    expect(options).toContainEqual({
      label: "dashboard.views.all",
      value: "all",
    });
  });

  it("builds assigned incident view only for treatment permissions", () => {
    const options = getIncidentViewOptions(
      checker([PERMISSIONS.INCIDENT.RESOLVE]),
      t as never,
    );

    expect(options).toEqual([
      { label: "incidents.views.assigned", value: "assigned" },
    ]);
  });

  it("builds report scopes from report scope permissions", () => {
    const options = getReportScopeOptions(
      checker([
        PERMISSIONS.REPORT.VIEW_OWN,
        PERMISSIONS.REPORT.GENERATE_ALL_SCOPES,
      ]),
      t as never,
    );

    expect(options).toEqual([
      { label: "reports.scope.own", value: "own" },
      { label: "reports.scope.byAgency", value: "agency" },
      { label: "reports.scope.byService", value: "service" },
      { label: "reports.scope.byUser", value: "user" },
      { label: "reports.scope.all", value: "all" },
    ]);
  });

  it("does not use report visibility to grant generation scopes", () => {
    const options = getReportScopeOptions(
      checker([PERMISSIONS.REPORT.VIEW_OWN, PERMISSIONS.REPORT.VIEW_ALL]),
      t as never,
    );

    expect(options).toEqual([
      { label: "reports.scope.own", value: "own" },
    ]);
  });

  it("selects a persisted view only when it is still available", () => {
    const options = [
      { label: "Own", value: "own" as const },
      { label: "All", value: "all" as const },
    ];

    expect(getPreferredView(options, "all", ["own"])).toBe("all");
    expect(getPreferredView(options, "service", ["all", "own"])).toBe("all");
  });

  it("checks permissions case-insensitively", () => {
    expect(
      hasAnyPermissionName(["report_export"], [PERMISSIONS.REPORT.EXPORT]),
    ).toBe(true);
  });

  it("includes ASSIGN in INCIDENT_ACCESS_PERMISSIONS", () => {
    expect(INCIDENT_ACCESS_PERMISSIONS).toContain(PERMISSIONS.INCIDENT.ASSIGN);
  });
});
