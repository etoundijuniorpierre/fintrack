// Tests frontend : verifie le comportement de create incident.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen } from "@testing-library/react";
import { useNavigate } from "react-router-dom";
import CreateIncident from "./CreateIncident";
import { useAuth } from "../../../hooks/auth/useAuth";
import { makeIncidentResponse } from "../../../mocks/incident/incidentFixtures";
import { renderWithProviders } from "../../../test-utils/renderWithProviders";
import type { IncidentResponse } from "../../../api/incident/types";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (k: string) => k }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: vi.fn(),
    Navigate: vi.fn(({ to }: { to: string }) => (
      <div data-testid="navigate-mock" data-to={to} />
    )),
  };
});

vi.mock("../../../hooks/auth/useAuth", () => ({
  useAuth: vi.fn(),
}));

vi.mock("../components/Form/IncidentForm", () => ({
  default: ({
    onSuccess,
    onCancel,
  }: {
    onSuccess: (incident: IncidentResponse) => void;
    onCancel: () => void;
  }) => (
    <div data-testid="incident-form">
      <button
        onClick={() => onSuccess(makeIncidentResponse({ id: "incident-1" }))}
      >
        Success
      </button>
      <button onClick={onCancel}>Cancel</button>
    </div>
  ),
}));

describe("CreateIncident Page", () => {
  const mockNavigate = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useNavigate).mockReturnValue(mockNavigate);
    vi.mocked(useAuth).mockReturnValue({
      user: null,
      isAuthenticated: true,
      hasRole: vi.fn().mockReturnValue(false),
      hasPermission: vi.fn().mockReturnValue(true),
    });
  });

  it("should render the page title and form when user has permission", () => {
    renderWithProviders(<CreateIncident />);
    expect(
      screen.getAllByText("incidents.form.titles.create").length,
    ).toBeGreaterThanOrEqual(1);
    expect(screen.getByTestId("incident-form")).toBeInTheDocument();
  });

  it("should redirect to incidents list when user lacks INCIDENT_CREATE permission", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: null,
      isAuthenticated: true,
      hasRole: vi.fn().mockReturnValue(false),
      hasPermission: vi.fn().mockReturnValue(false),
    });
    renderWithProviders(<CreateIncident />);
    const navigateMock = screen.getByTestId("navigate-mock");
    expect(navigateMock).toBeInTheDocument();
    expect(navigateMock.getAttribute("data-to")).toBe("/dashboard/incidents");
  });

  it("should navigate to incident detail when form submission succeeds", () => {
    renderWithProviders(<CreateIncident />);
    screen.getByText("Success").click();
    expect(mockNavigate).toHaveBeenCalledWith(
      "/dashboard/incidents/incident-1",
    );
  });

  it("should navigate back to incidents list when cancel is clicked", () => {
    renderWithProviders(<CreateIncident />);
    screen.getByText("Cancel").click();
    expect(mockNavigate).toHaveBeenCalledWith("/dashboard/incidents");
  });

  it("should render an icon-only back button on the page header", () => {
    renderWithProviders(<CreateIncident />);
    expect(screen.getByRole("button", { name: "Return" })).toBeInTheDocument();
  });

  it("should navigate back to incidents list when the header back button is clicked", () => {
    renderWithProviders(<CreateIncident />);
    screen.getByRole("button", { name: "Return" }).click();
    expect(mockNavigate).toHaveBeenCalledWith("/dashboard/incidents");
  });
});
