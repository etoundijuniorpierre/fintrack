// Tests frontend : verifie le comportement de dashboard.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { dashboardNavigation } from "./dashboard";
import type { NavigateFunction } from "react-router-dom";

const mockNavigate = vi.fn() as unknown as NavigateFunction;

describe("dashboardNavigation", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should navigate to /dashboard when navigateToDashboard is called", () => {
    dashboardNavigation.navigateToDashboard(mockNavigate);
    expect(mockNavigate).toHaveBeenCalledWith("/dashboard");
  });

  it("should navigate to /dashboard/incidents when navigateToIncidents is called", () => {
    dashboardNavigation.navigateToIncidents(mockNavigate);
    expect(mockNavigate).toHaveBeenCalledWith("/dashboard/incidents");
  });

  it("should navigate to /dashboard/reports when navigateToReports is called", () => {
    dashboardNavigation.navigateToReports(mockNavigate);
    expect(mockNavigate).toHaveBeenCalledWith("/dashboard/reports");
  });

  it("should navigate to /dashboard/settings when navigateToSettings is called", () => {
    dashboardNavigation.navigateToSettings(mockNavigate);
    expect(mockNavigate).toHaveBeenCalledWith("/dashboard/settings");
  });

  it("should navigate to /dashboard/audit when navigateToAudit is called", () => {
    dashboardNavigation.navigateToAudit(mockNavigate);
    expect(mockNavigate).toHaveBeenCalledWith("/dashboard/audit");
  });
});
