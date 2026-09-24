// Tests : grille de cartes d'incidents (etat vide, rendu, navigation detail).

import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import IncidentCards from "./IncidentCards";
import { makeIncidentSummary } from "../../../../mocks/incident/incidentFixtures";
import { incidentNavigation } from "../../../../utils/navigation/incidents/incidents";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";

const { mockNavigate } = vi.hoisted(() => ({ mockNavigate: vi.fn() }));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("../../../../utils/navigation/incidents/incidents", () => ({
  incidentNavigation: { navigateToIncidentDetail: vi.fn() },
  incidentPathIdentifier: (incident: { id: string; reference?: string }) =>
    incident.reference ?? incident.id,
}));

describe("IncidentCards", () => {
  beforeEach(() => vi.clearAllMocks());

  it("should render the empty state when there is no incident and loading is done", () => {
    renderWithProviders(<IncidentCards incidents={[]} loading={false} />);
    expect(screen.getByText("incidents.empty_description")).toBeInTheDocument();
  });

  it("should not render the empty state while loading", () => {
    renderWithProviders(<IncidentCards incidents={[]} loading={true} />);
    expect(
      screen.queryByText("incidents.empty_description"),
    ).not.toBeInTheDocument();
  });

  it("should render one card per incident with its title", () => {
    const incidents = [
      makeIncidentSummary({ id: "1", title: "Carte bloquee" }),
      makeIncidentSummary({ id: "2", title: "Virement echoue" }),
    ];
    renderWithProviders(
      <IncidentCards incidents={incidents} loading={false} />,
    );
    expect(screen.getByText("Carte bloquee")).toBeInTheDocument();
    expect(screen.getByText("Virement echoue")).toBeInTheDocument();
  });

  it('should show the "no assignee" fallback when nobody is assigned', () => {
    renderWithProviders(
      <IncidentCards
        incidents={[makeIncidentSummary({ assignedTo: undefined })]}
        loading={false}
      />,
    );
    expect(
      screen.getByText("incidents.detail.no_assigned"),
    ).toBeInTheDocument();
  });

  it("should show the assignee name, agency, creation date and due date", () => {
    renderWithProviders(
      <IncidentCards
        incidents={[
          makeIncidentSummary({
            assignedTo: {
              id: "user-2",
              username: "technical.username",
              firstName: "Jane",
              lastName: "Doe",
            },
            agency: {
              id: "agency-2",
              name: "Agence Centrale",
              code: "AC",
            },
            createdAt: "2024-01-01T08:00:00Z",
            dueDate: "2024-01-03T18:00:00Z",
          }),
        ]}
        loading={false}
      />,
    );

    expect(screen.getByText("Doe Jane")).toBeInTheDocument();
    expect(screen.queryByText("technical.username")).not.toBeInTheDocument();
    expect(screen.getByText("Agence Centrale")).toBeInTheDocument();
    expect(screen.getByText(/01\/01\/2024/)).toBeInTheDocument();
    expect(screen.getByText(/03\/01\/2024/)).toBeInTheDocument();
  });

  it("should navigate to the detail when the details button is clicked", async () => {
    renderWithProviders(
      <IncidentCards
        incidents={[makeIncidentSummary({ id: "inc-77" })]}
        loading={false}
      />,
    );
    await userEvent.click(
      screen.getByRole("button", { name: "incidents.cards.details" }),
    );
    expect(incidentNavigation.navigateToIncidentDetail).toHaveBeenCalledWith(
      mockNavigate,
      "inc-77",
    );
  });
});
