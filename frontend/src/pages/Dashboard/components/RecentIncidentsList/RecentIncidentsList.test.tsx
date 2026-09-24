// Tests frontend : verifie le comportement de recent incidents list.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";
import RecentIncidentsList from "./RecentIncidentsList";
import {
  type IncidentHistoryResponse,
  ActionType,
} from "../../../../api/incident/types";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock("../../../../utils/formatters/formatters", () => ({
  formatDate: (date: string) => date,
  formatDateTime: (date: string) => date,
  formatUserName: (u: { firstName?: string; lastName?: string } | null) =>
    u ? `${u.firstName ?? ""} ${u.lastName ?? ""}`.trim() : "—",
}));

// Fabrique une fixture de test pour recent incidents list.test.
const makeIncident = (
  overrides: Partial<IncidentHistoryResponse> & { title?: string } = {},
): IncidentHistoryResponse => {
  const { title, ...rest } = overrides;
  return {
    id: "hist-1",
    incidentId: "inc-1",
    incidentTitle: title || "Serveur en panne",
    action: ActionType.CREATION,
    oldValue: "",
    newValue: "",
    createdAt: "2024-01-15T10:00:00Z",
    updatedAt: "2024-01-15T10:00:00Z",
    user: { id: "u1", username: "jdoe", firstName: "John", lastName: "Doe" },
    ...rest,
  };
};

describe("RecentIncidentsList", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should show skeleton when isLoading is true", () => {
    const { container } = renderWithProviders(
      <RecentIncidentsList incidents={[]} isLoading={true} isError={false} />,
    );
    expect(container.querySelector('[aria-busy="true"]')).toBeInTheDocument();
  });

  it("should show error alert when isError is true", () => {
    renderWithProviders(
      <RecentIncidentsList incidents={[]} isLoading={false} isError={true} />,
    );
    expect(screen.getByRole("alert")).toBeInTheDocument();
  });

  it("should show empty state when incidents list is empty", () => {
    renderWithProviders(
      <RecentIncidentsList incidents={[]} isLoading={false} isError={false} />,
    );
    expect(
      screen.getByText("dashboard.recentActivity.empty"),
    ).toBeInTheDocument();
  });

  it("should render incident titles when incidents are provided", () => {
    const incidents = [
      makeIncident({ id: "hist-1", title: "Panne réseau" }),
      makeIncident({ id: "hist-2", title: "Accès refusé" }),
    ];
    renderWithProviders(
      <RecentIncidentsList
        incidents={incidents}
        isLoading={false}
        isError={false}
      />,
    );
    expect(screen.getByText("Panne réseau")).toBeInTheDocument();
    expect(screen.getByText("Accès refusé")).toBeInTheDocument();
  });

  it("should render translated action tags when incident is displayed", () => {
    renderWithProviders(
      <RecentIncidentsList
        incidents={[makeIncident({ action: ActionType.CREATION })]}
        isLoading={false}
        isError={false}
      />,
    );
    expect(
      screen.getByText("incidents.action_type.CREATION"),
    ).toBeInTheDocument();
  });

  it("should render the formatted date when incident has a createdAt value", () => {
    renderWithProviders(
      <RecentIncidentsList
        incidents={[makeIncident({ createdAt: "2024-01-15T10:00:00Z" })]}
        isLoading={false}
        isError={false}
      />,
    );
    expect(screen.getByText("2024-01-15T10:00:00Z")).toBeInTheDocument();
  });

  it("should render all list items when multiple incidents are provided", () => {
    const incidents = Array.from({ length: 3 }, (_, i) =>
      makeIncident({ id: `hist-${i}`, title: `Incident ${i}` }),
    );
    renderWithProviders(
      <RecentIncidentsList
        incidents={incidents}
        isLoading={false}
        isError={false}
      />,
    );
    expect(screen.getAllByRole("listitem")).toHaveLength(3);
  });
});
