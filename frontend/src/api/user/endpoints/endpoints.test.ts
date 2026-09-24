// Tests frontend : verifie le comportement de endpoints.test.

import { describe, it, expect } from "vitest";
import { USER_SERVICE_ENDPOINTS } from "./endpoints";
import { SERVICE_BASES } from "../../base";

describe("USER_SERVICE_ENDPOINTS", () => {
  it("should have correct auth endpoints", () => {
    expect(USER_SERVICE_ENDPOINTS.AUTH.LOGIN).toBe(
      `${SERVICE_BASES.USER}/auth/login`,
    );
    expect(USER_SERVICE_ENDPOINTS.AUTH.LOGOUT).toBe(
      `${SERVICE_BASES.USER}/auth/logout`,
    );
    expect(USER_SERVICE_ENDPOINTS.AUTH.REFRESH).toBe(
      `${SERVICE_BASES.USER}/auth/refresh`,
    );
    expect(USER_SERVICE_ENDPOINTS.AUTH.CHANGE_PASSWORD("user1")).toBe(
      `${SERVICE_BASES.USER}/users/user1/change-password`,
    );
  });

  it("should have correct users endpoints", () => {
    expect(USER_SERVICE_ENDPOINTS.USERS.BASE).toBe(
      `${SERVICE_BASES.USER}/users`,
    );
    expect(USER_SERVICE_ENDPOINTS.USERS.ALL).toBe(
      `${SERVICE_BASES.USER}/users/all`,
    );
    expect(USER_SERVICE_ENDPOINTS.USERS.ASSIGNABLE).toBe(
      `${SERVICE_BASES.USER}/users/assignable`,
    );
    expect(USER_SERVICE_ENDPOINTS.USERS.PROFILE("user1")).toBe(
      `${SERVICE_BASES.USER}/users/user1/profile`,
    );
    expect(USER_SERVICE_ENDPOINTS.USERS.STATS).toBe(
      `${SERVICE_BASES.USER}/users/stats`,
    );
  });

  it("should have correct roles endpoints", () => {
    expect(USER_SERVICE_ENDPOINTS.ROLES.BASE).toBe(
      `${SERVICE_BASES.USER}/roles`,
    );
    expect(USER_SERVICE_ENDPOINTS.ROLES.ALL).toBe(
      `${SERVICE_BASES.USER}/roles/all`,
    );
  });

  it("should have correct agencies endpoints", () => {
    expect(USER_SERVICE_ENDPOINTS.AGENCIES.BASE).toBe(
      `${SERVICE_BASES.USER}/agencies`,
    );
    expect(USER_SERVICE_ENDPOINTS.AGENCIES.ALL).toBe(
      `${SERVICE_BASES.USER}/agencies/all`,
    );
  });

  it("should have correct services endpoints", () => {
    expect(USER_SERVICE_ENDPOINTS.SERVICES.BASE).toBe(
      `${SERVICE_BASES.USER}/departments`,
    );
    expect(USER_SERVICE_ENDPOINTS.SERVICES.ALL).toBe(
      `${SERVICE_BASES.USER}/departments/all`,
    );
  });

  it("should have correct permissions endpoints", () => {
    expect(USER_SERVICE_ENDPOINTS.PERMISSIONS.BASE).toBe(
      `${SERVICE_BASES.USER}/permissions`,
    );
    expect(USER_SERVICE_ENDPOINTS.PERMISSIONS.ALL).toBe(
      `${SERVICE_BASES.USER}/permissions/all`,
    );
  });
});
