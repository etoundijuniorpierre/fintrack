// Tests frontend : verifie le comportement de incidents.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import type { NavigateFunction } from "react-router-dom";
import { incidentNavigation, incidentPathIdentifier } from "./incidents";

const mockNavigate = vi.fn() as unknown as NavigateFunction;

describe("incidentNavigation", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should navigate to /dashboard/incidents when navigateToIncidents is called", () => {
    incidentNavigation.navigateToIncidents(mockNavigate);
    expect(mockNavigate).toHaveBeenCalledWith("/dashboard/incidents");
  });

  it("should navigate to /dashboard/incidents/create when navigateToIncidentCreate is called", () => {
    incidentNavigation.navigateToIncidentCreate(mockNavigate);
    expect(mockNavigate).toHaveBeenCalledWith("/dashboard/incidents/create");
  });

  it("should navigate to /dashboard/incidents/:id when navigateToIncidentDetail is called", () => {
    incidentNavigation.navigateToIncidentDetail(mockNavigate, "incident-1");
    expect(mockNavigate).toHaveBeenCalledWith(
      "/dashboard/incidents/incident-1",
    );
  });
});

describe("incidentPathIdentifier", () => {
  it("should prefer the business reference when present", () => {
    expect(
      incidentPathIdentifier({ id: "uuid-1", reference: "FT-I-2026-0001" }),
    ).toBe("FT-I-2026-0001");
  });

  it("should fall back to the UUID when no reference exists", () => {
    expect(incidentPathIdentifier({ id: "uuid-1" })).toBe("uuid-1");
    expect(incidentPathIdentifier({ id: "uuid-1", reference: null })).toBe(
      "uuid-1",
    );
  });
});
