// Tests frontend : verifie le comportement de endpoints.test.

import { describe, it, expect } from "vitest";
import { INCIDENT_SERVICE_ENDPOINTS } from "./endpoints";
import { SERVICE_BASES } from "../../base";

// Definit les donnees de test base.
const BASE = SERVICE_BASES.INCIDENT;
// Definit les donnees de test id.
const ID = "incident-1";

describe("INCIDENT_SERVICE_ENDPOINTS", () => {
  it("should construct INCIDENTS.BASE from SERVICE_BASES.INCIDENT", () => {
    expect(INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.BASE).toBe(`${BASE}/incidents`);
  });

  it("should construct INCIDENTS.BY_ID with the provided id", () => {
    expect(INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.BY_ID(ID)).toBe(
      `${BASE}/incidents/${ID}`,
    );
  });

  it("should construct INCIDENTS.BY_REFERENCE with the encoded reference", () => {
    expect(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.BY_REFERENCE("FT-I-2026-0001"),
    ).toBe(`${BASE}/incidents/by-reference/FT-I-2026-0001`);
  });

  it("should construct INCIDENTS.VALIDATE with the provided id", () => {
    expect(INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.VALIDATE(ID)).toBe(
      `${BASE}/incidents/${ID}/validate`,
    );
  });

  it("should construct INCIDENTS.REJECT with the provided id", () => {
    expect(INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.REJECT(ID)).toBe(
      `${BASE}/incidents/${ID}/reject`,
    );
  });

  it("should construct INCIDENTS.TRANSFER with the provided id", () => {
    expect(INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.TRANSFER(ID)).toBe(
      `${BASE}/incidents/${ID}/transfer`,
    );
  });

  it("should construct INCIDENTS.ASSIGN with the provided id", () => {
    expect(INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.ASSIGN(ID)).toBe(
      `${BASE}/incidents/${ID}/assign`,
    );
  });

  it("should construct INCIDENTS.START with the provided id", () => {
    expect(INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.START(ID)).toBe(
      `${BASE}/incidents/${ID}/start`,
    );
  });

  it("should construct INCIDENTS.BLOCK with the provided id", () => {
    expect(INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.BLOCK(ID)).toBe(
      `${BASE}/incidents/${ID}/block`,
    );
  });

  it("should construct INCIDENTS.RESUME with the provided id", () => {
    expect(INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.RESUME(ID)).toBe(
      `${BASE}/incidents/${ID}/resume`,
    );
  });

  it("should construct INCIDENTS.REQUEST_CONFIRMATION with the provided id", () => {
    expect(INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.REQUEST_CONFIRMATION(ID)).toBe(
      `${BASE}/incidents/${ID}/request-confirmation`,
    );
  });

  it("should construct INCIDENTS.CONFIRM_RELEVANCE with the provided id", () => {
    expect(INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.CONFIRM_RELEVANCE(ID)).toBe(
      `${BASE}/incidents/${ID}/confirm-relevance`,
    );
  });

  it("should construct INCIDENTS.RESOLVE with the provided id", () => {
    expect(INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.RESOLVE(ID)).toBe(
      `${BASE}/incidents/${ID}/resolve`,
    );
  });

  it("should construct INCIDENTS.CLOSE with the provided id", () => {
    expect(INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.CLOSE(ID)).toBe(
      `${BASE}/incidents/${ID}/close`,
    );
  });

  it("should construct INCIDENTS.REOPEN with the provided id", () => {
    expect(INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.REOPEN(ID)).toBe(
      `${BASE}/incidents/${ID}/reopen`,
    );
  });

  it("should construct INCIDENTS.CLONE with the provided id", () => {
    expect(INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.CLONE(ID)).toBe(
      `${BASE}/incidents/${ID}/clone`,
    );
  });

  it("should construct INCIDENTS.RESUBMIT with the provided id", () => {
    expect(INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.RESUBMIT(ID)).toBe(
      `${BASE}/incidents/${ID}/resubmit`,
    );
  });

  it("should construct INCIDENTS.CANCEL with the provided id", () => {
    expect(INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.CANCEL(ID)).toBe(
      `${BASE}/incidents/${ID}/cancel`,
    );
  });

  it("should construct INCIDENTS.COMMENTS with the provided id", () => {
    expect(INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.COMMENTS(ID)).toBe(
      `${BASE}/incidents/${ID}/comments`,
    );
  });

  it("should construct INCIDENTS.HISTORY with the provided id", () => {
    expect(INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.HISTORY(ID)).toBe(
      `${BASE}/incidents/${ID}/history`,
    );
  });

  it("should construct ENUMS.STATUSES correctly", () => {
    expect(INCIDENT_SERVICE_ENDPOINTS.ENUMS.STATUSES).toBe(
      `${BASE}/enums/incident-statuses`,
    );
  });

  it("should construct ENUMS.CRITICALITIES correctly", () => {
    expect(INCIDENT_SERVICE_ENDPOINTS.ENUMS.CRITICALITIES).toBe(
      `${BASE}/enums/criticalities`,
    );
  });

  it("should construct ENUMS.ACTION_TYPES correctly", () => {
    expect(INCIDENT_SERVICE_ENDPOINTS.ENUMS.ACTION_TYPES).toBe(
      `${BASE}/enums/action-types`,
    );
  });

  it("should construct DASHBOARD.METRICS correctly", () => {
    expect(INCIDENT_SERVICE_ENDPOINTS.DASHBOARD.METRICS).toBe(
      `${BASE}/dashboard/metrics`,
    );
  });

  it("should construct DASHBOARD.COMPARISON correctly", () => {
    expect(INCIDENT_SERVICE_ENDPOINTS.DASHBOARD.COMPARISON).toBe(
      `${BASE}/dashboard/metrics/comparison`,
    );
  });
});
