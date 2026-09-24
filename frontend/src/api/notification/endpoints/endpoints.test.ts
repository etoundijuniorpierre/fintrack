// Tests frontend : verifie le comportement de endpoints.test.

import { describe, it, expect } from "vitest";
import { NOTIFICATION_ENDPOINTS } from "./endpoints";
import { SERVICE_BASES } from "../../base";

describe("NOTIFICATION_ENDPOINTS", () => {
  const base = SERVICE_BASES.NOTIFICATION;

  it("should construct NOTIFICATIONS.BASE correctly", () => {
    expect(NOTIFICATION_ENDPOINTS.NOTIFICATIONS.BASE).toBe(
      `${base}/notifications`,
    );
  });

  it("should construct NOTIFICATIONS.ALL correctly", () => {
    expect(NOTIFICATION_ENDPOINTS.NOTIFICATIONS.ALL).toBe(
      `${base}/notifications`,
    );
  });

  it("should construct NOTIFICATIONS.ALL_LIST correctly", () => {
    expect(NOTIFICATION_ENDPOINTS.NOTIFICATIONS.ALL_LIST).toBe(
      `${base}/notifications/all`,
    );
  });

  it("should construct NOTIFICATIONS.BY_ID with the provided id", () => {
    expect(NOTIFICATION_ENDPOINTS.NOTIFICATIONS.BY_ID("notif-1")).toBe(
      `${base}/notifications/notif-1`,
    );
  });

  it("should construct NOTIFICATIONS.BY_STATUS with the provided status", () => {
    expect(NOTIFICATION_ENDPOINTS.NOTIFICATIONS.BY_STATUS("PENDING")).toBe(
      `${base}/notifications/status/PENDING`,
    );
  });

  it("should construct NOTIFICATIONS.BY_INCIDENT with the provided incidentId", () => {
    expect(NOTIFICATION_ENDPOINTS.NOTIFICATIONS.BY_INCIDENT("incident-1")).toBe(
      `${base}/notifications/incident/incident-1`,
    );
  });

  it("should construct NOTIFICATIONS.BY_RECIPIENT with the provided recipient", () => {
    expect(
      NOTIFICATION_ENDPOINTS.NOTIFICATIONS.BY_RECIPIENT("user@example.com"),
    ).toContain("user@example.com");
  });

  it("should construct NOTIFICATIONS.READ with the provided id", () => {
    expect(NOTIFICATION_ENDPOINTS.NOTIFICATIONS.READ("notif-1")).toBe(
      `${base}/notifications/notif-1/read`,
    );
  });

  it("should construct NOTIFICATIONS.READ_ALL correctly", () => {
    expect(NOTIFICATION_ENDPOINTS.NOTIFICATIONS.READ_ALL).toBe(
      `${base}/notifications/read-all`,
    );
  });

  it("should construct NOTIFICATIONS.BULK correctly", () => {
    expect(NOTIFICATION_ENDPOINTS.NOTIFICATIONS.BULK).toBe(
      `${base}/notifications/bulk`,
    );
  });

  it("should construct ENUMS.NOTIFICATION_TYPES correctly", () => {
    expect(NOTIFICATION_ENDPOINTS.ENUMS.NOTIFICATION_TYPES).toBe(
      `${base}/enums/notification-types`,
    );
  });

  it("should construct ENUMS.NOTIFICATION_STATUSES correctly", () => {
    expect(NOTIFICATION_ENDPOINTS.ENUMS.NOTIFICATION_STATUSES).toBe(
      `${base}/enums/notification-statuses`,
    );
  });
});
