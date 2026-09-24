// Tests frontend : verifie le comportement de endpoints.test.

import { describe, it, expect } from "vitest";
import { SETTINGS_ENDPOINTS } from "./endpoints";
import { SERVICE_BASES } from "../../base";

describe("SETTINGS_ENDPOINTS", () => {
  it("should have correct incident types endpoints", () => {
    expect(SETTINGS_ENDPOINTS.INCIDENT_TYPES.BASE).toBe(
      `${SERVICE_BASES.INCIDENT}/incident-type-configs`,
    );
    expect(SETTINGS_ENDPOINTS.INCIDENT_TYPES.ALL).toBe(
      `${SERVICE_BASES.INCIDENT}/incident-type-configs/all`,
    );
    expect(SETTINGS_ENDPOINTS.INCIDENT_TYPES.BY_ID("123")).toBe(
      `${SERVICE_BASES.INCIDENT}/incident-type-configs/123`,
    );
  });

  it("should have correct agencies endpoints", () => {
    expect(SETTINGS_ENDPOINTS.AGENCIES.BASE).toBe(
      `${SERVICE_BASES.USER}/agencies`,
    );
    expect(SETTINGS_ENDPOINTS.AGENCIES.ALL).toBe(
      `${SERVICE_BASES.USER}/agencies/all`,
    );
    expect(SETTINGS_ENDPOINTS.AGENCIES.BY_ID("456")).toBe(
      `${SERVICE_BASES.USER}/agencies/456`,
    );
    expect(SETTINGS_ENDPOINTS.AGENCIES.ASSIGN_HEAD("456", "user1")).toBe(
      `${SERVICE_BASES.USER}/agencies/456/head/user1`,
    );
  });

  it("should have correct services endpoints", () => {
    expect(SETTINGS_ENDPOINTS.SERVICES.BASE).toBe(
      `${SERVICE_BASES.USER}/departments`,
    );
    expect(SETTINGS_ENDPOINTS.SERVICES.ALL).toBe(
      `${SERVICE_BASES.USER}/departments/all`,
    );
    expect(SETTINGS_ENDPOINTS.SERVICES.BY_ID("789")).toBe(
      `${SERVICE_BASES.USER}/departments/789`,
    );
    expect(SETTINGS_ENDPOINTS.SERVICES.ASSIGN_HEAD("789", "user2")).toBe(
      `${SERVICE_BASES.USER}/departments/789/head/user2`,
    );
  });

  it("should have correct roles endpoints", () => {
    expect(SETTINGS_ENDPOINTS.ROLES.BASE).toBe(`${SERVICE_BASES.USER}/roles`);
    expect(SETTINGS_ENDPOINTS.ROLES.ALL).toBe(
      `${SERVICE_BASES.USER}/roles/all`,
    );
    expect(SETTINGS_ENDPOINTS.ROLES.BY_ID("role1")).toBe(
      `${SERVICE_BASES.USER}/roles/role1`,
    );
  });

  it("should have correct permissions endpoints", () => {
    expect(SETTINGS_ENDPOINTS.PERMISSIONS.BASE).toBe(
      `${SERVICE_BASES.USER}/permissions`,
    );
    expect(SETTINGS_ENDPOINTS.PERMISSIONS.ALL).toBe(
      `${SERVICE_BASES.USER}/permissions/all`,
    );
  });

  it("should have correct report schedules endpoints", () => {
    expect(SETTINGS_ENDPOINTS.REPORT_SCHEDULES.BASE).toBe(
      `${SERVICE_BASES.REPORTING}/report-schedules`,
    );
    expect(SETTINGS_ENDPOINTS.REPORT_SCHEDULES.ALL).toBe(
      `${SERVICE_BASES.REPORTING}/report-schedules/all`,
    );
    expect(SETTINGS_ENDPOINTS.REPORT_SCHEDULES.BY_ID("sched1")).toBe(
      `${SERVICE_BASES.REPORTING}/report-schedules/sched1`,
    );
    expect(SETTINGS_ENDPOINTS.REPORT_SCHEDULES.TOGGLE("sched1")).toBe(
      `${SERVICE_BASES.REPORTING}/report-schedules/sched1/toggle`,
    );
  });
});
