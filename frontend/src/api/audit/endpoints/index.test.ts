// Tests frontend : verifie le comportement de index.test.

import { describe, it, expect } from "vitest";
import { AUDIT_ENDPOINTS } from "./index";
import { SERVICE_BASES } from "../../base";

describe("AUDIT_ENDPOINTS", () => {
  const base = SERVICE_BASES.AUDIT;

  it("should construct AUDIT_LOGS.BASE correctly", () => {
    expect(AUDIT_ENDPOINTS.AUDIT_LOGS.BASE).toBe(`${base}/audit-logs`);
  });

  it("should construct AUDIT_LOGS.ALL correctly", () => {
    expect(AUDIT_ENDPOINTS.AUDIT_LOGS.ALL).toBe(`${base}/audit-logs`);
  });

  it("should construct AUDIT_LOGS.BY_ID with the provided id", () => {
    expect(AUDIT_ENDPOINTS.AUDIT_LOGS.BY_ID("log-1")).toBe(
      `${base}/audit-logs/log-1`,
    );
  });

  it("should construct AUDIT_LOGS.BY_USER with the provided userId", () => {
    expect(AUDIT_ENDPOINTS.AUDIT_LOGS.BY_USER("user-1")).toBe(
      `${base}/audit-logs/user/user-1`,
    );
  });

  it("should construct AUDIT_LOGS.BY_ACTION with the provided action", () => {
    expect(AUDIT_ENDPOINTS.AUDIT_LOGS.BY_ACTION("LOGIN_SUCCESS")).toBe(
      `${base}/audit-logs/action/LOGIN_SUCCESS`,
    );
  });

  it("should construct AUDIT_LOGS.BY_RESOURCE_TYPE with the provided resourceType", () => {
    expect(AUDIT_ENDPOINTS.AUDIT_LOGS.BY_RESOURCE_TYPE("USER")).toBe(
      `${base}/audit-logs/resource/USER`,
    );
  });

  it("should construct AUDIT_LOGS.BY_RESOURCE with both resourceType and resourceId", () => {
    expect(AUDIT_ENDPOINTS.AUDIT_LOGS.BY_RESOURCE("USER", "user-1")).toBe(
      `${base}/audit-logs/resource/USER/user-1`,
    );
  });

  it("should construct AUDIT_LOGS.BY_STATUS with the provided status", () => {
    expect(AUDIT_ENDPOINTS.AUDIT_LOGS.BY_STATUS("SUCCESS")).toBe(
      `${base}/audit-logs/status/SUCCESS`,
    );
  });

  it("should construct AUDIT_LOGS.BY_RANGE correctly", () => {
    expect(AUDIT_ENDPOINTS.AUDIT_LOGS.BY_RANGE).toBe(
      `${base}/audit-logs/range`,
    );
  });

  it("should construct ENUMS.AUDIT_ACTIONS correctly", () => {
    expect(AUDIT_ENDPOINTS.ENUMS.AUDIT_ACTIONS).toBe(
      `${base}/enums/audit-actions`,
    );
  });

  it("should construct ENUMS.AUDIT_STATUSES correctly", () => {
    expect(AUDIT_ENDPOINTS.ENUMS.AUDIT_STATUSES).toBe(
      `${base}/enums/audit-statuses`,
    );
  });
});
