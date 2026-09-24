// Test d'integration : verifie le rendu reel des phrases (vrai <Trans> + vraie i18n FR),
// pour garantir que la transition depart -> arrivee s'affiche bien dans une phrase naturelle.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { I18nextProvider } from "react-i18next";
import type { ReactNode } from "react";
import i18n from "../../../../i18n";
import { AllProviders } from "../../../../test-utils/AllProviders";
import HistoryTimeline from "./HistoryTimeline";
import { makeIncidentHistory } from "../../../../mocks/incident/incidentFixtures";
import {
  useIncidentHistory,
  useActionTypes,
} from "../../../../hooks/incident/useIncidents/useIncidents";
import type { IncidentHistoryResponse } from "../../../../api/incident/types";

vi.mock("../../../../hooks/incident/useIncidents/useIncidents", () => ({
  useIncidentHistory: vi.fn(),
  useActionTypes: vi.fn(),
}));

// On garde le vrai formatUserName (noms reels + repli « — » pour les actions systeme)
// et on ne fige que la date pour des assertions stables.
vi.mock("../../../../utils/formatters/formatters", async (importActual) => {
  const actual =
    await importActual<
      typeof import("../../../../utils/formatters/formatters")
    >();
  return { ...actual, formatDateTime: (date: string) => date };
});

type QueryStub<T> = { data: T | undefined; isLoading: boolean };

// Compose les providers de test avec la vraie instance i18n.
const Wrapper = ({ children }: { children: ReactNode }) => (
  <AllProviders>
    <I18nextProvider i18n={i18n}>{children}</I18nextProvider>
  </AllProviders>
);

describe("HistoryTimeline (integration, real i18n)", () => {
  beforeEach(async () => {
    vi.clearAllMocks();
    await i18n.changeLanguage("fr");
    vi.mocked(useActionTypes).mockReturnValue({
      data: [],
      isLoading: false,
    } as QueryStub<never[]> as ReturnType<typeof useActionTypes>);
  });

  const mockHistory = (entries: IncidentHistoryResponse[]) =>
    vi.mocked(useIncidentHistory).mockReturnValue({
      data: entries,
      isLoading: false,
    } as QueryStub<IncidentHistoryResponse[]> as ReturnType<
      typeof useIncidentHistory
    >);

  it("preserves full multiline comments and long unbroken values", () => {
    const longValue = "Reference".repeat(80);
    const comment = `Première ligne\n${"message".repeat(200)}\nDernière ligne`;
    mockHistory([makeIncidentHistory({ action: "UPDATE", oldValue: "Ancien", newValue: longValue, comment })]);
    render(<HistoryTimeline incidentId="incident-1" />, { wrapper: Wrapper });
    expect(screen.getByText(longValue)).toBeInTheDocument();
    expect(screen.getByText((_, element) => element?.textContent === comment)).toBeInTheDocument();
  });

  it("should render a reopening as a natural French sentence led by its actor", () => {
    mockHistory([
      makeIncidentHistory({
        user: {
          id: "u-1",
          username: "bagent",
          firstName: "Beta",
          lastName: "Agent",
        },
        action: "REOPENING",
        oldValue: "RESOLVED",
        newValue: "IN_PROGRESS",
        comment: "Erreur de saisie",
      }),
    ]);

    render(<HistoryTimeline incidentId="incident-1" />, { wrapper: Wrapper });

    // La phrase se lit comme un evenement mene par son auteur (nom = « Nom Prenom »).
    expect(screen.getByText("Agent Beta")).toBeInTheDocument();
    expect(screen.getByText(/a rouvert l'incident/)).toBeInTheDocument();
    // Les statuts sont traduits et affiches comme valeurs de la transition.
    expect(screen.getByText("Résolu")).toBeInTheDocument();
    expect(screen.getByText("En cours de traitement")).toBeInTheDocument();
    // Le motif est etiquete explicitement.
    expect(screen.getByText("Motif")).toBeInTheDocument();
    expect(screen.getByText("Erreur de saisie")).toBeInTheDocument();
  });

  it("should attribute an automatic action (no user) to the system", () => {
    mockHistory([
      makeIncidentHistory({
        user: undefined as never,
        action: "STATUS_CHANGE",
        oldValue: "ASSIGNED",
        newValue: "BLOCKED",
        comment: "Délai limite dépassé",
      }),
    ]);

    render(<HistoryTimeline incidentId="incident-1" />, { wrapper: Wrapper });

    // Action automatique : toujours un auteur, ici « Le système ».
    expect(screen.getByText("Le système")).toBeInTheDocument();
    expect(screen.getByText(/a changé le statut/)).toBeInTheDocument();
    expect(screen.getByText("Bloqué")).toBeInTheDocument();
  });

  it("should render an assignment with people names, not status pills", () => {
    mockHistory([
      makeIncidentHistory({
        action: "ASSIGNMENT",
        oldValue: "Chef Compta",
        newValue: "Agent 3 Beta",
      }),
    ]);

    render(<HistoryTimeline incidentId="incident-1" />, { wrapper: Wrapper });

    expect(screen.getByText(/a réassigné l'incident/)).toBeInTheDocument();
    expect(screen.getByText("Chef Compta")).toBeInTheDocument();
    expect(screen.getByText("Agent 3 Beta")).toBeInTheDocument();
  });

  it("should render a normalized qualification change with type names and its reason", () => {
    mockHistory([
      makeIncidentHistory({
        user: {
          id: "u-1",
          username: "yvan",
          firstName: "Yvan",
          lastName: "Agent",
        },
        action: "TYPE_CHANGE",
        oldValue: "Ancien type",
        newValue: "Nouveau type",
        comment: "Changement de service",
      }),
    ]);

    render(<HistoryTimeline incidentId="incident-1" />, { wrapper: Wrapper });

    expect(screen.getByText("Agent Yvan")).toBeInTheDocument();
    expect(screen.getByText(/a changé le type/)).toBeInTheDocument();
    expect(screen.getByText("Ancien type")).toBeInTheDocument();
    expect(screen.getByText("Nouveau type")).toBeInTheDocument();
    expect(screen.getByText("Motif")).toBeInTheDocument();
    expect(screen.getByText("Changement de service")).toBeInTheDocument();
    expect(screen.queryByText(/Qualification modifiee|typeId=/)).not.toBeInTheDocument();
  });

  it("should render a creation with only the initial status", () => {
    mockHistory([
      makeIncidentHistory({
        action: "CREATION",
        newValue: "PENDING_VALIDATION",
      }),
    ]);

    render(<HistoryTimeline incidentId="incident-1" />, { wrapper: Wrapper });

    expect(screen.getByText(/a déclaré l'incident/)).toBeInTheDocument();
    expect(screen.getByText("En attente de validation")).toBeInTheDocument();
  });
});
