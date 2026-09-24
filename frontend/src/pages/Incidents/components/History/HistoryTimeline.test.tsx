// Tests frontend : verifie le comportement de history timeline.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";
import HistoryTimeline from "./HistoryTimeline";
import { makeIncidentHistory } from "../../../../mocks/incident/incidentFixtures";
import {
  useIncidentHistory,
  useActionTypes,
} from "../../../../hooks/incident/useIncidents/useIncidents";
import type { IncidentHistoryResponse } from "../../../../api/incident/types";

// Mock react-i18next : `t` renvoie la clef, `Trans` expose clef + valeurs interpolees,
// `i18n.exists` considere toute clef comme presente (on teste la selection de variante).
vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: { exists: () => true },
  }),
  Trans: ({
    i18nKey,
    values,
  }: {
    i18nKey: string;
    values?: Record<string, string | undefined>;
  }) => (
    <span>
      <span>{i18nKey}</span>
      {values?.from ? <span>{values.from}</span> : null}
      {values?.to ? <span>{values.to}</span> : null}
    </span>
  ),
}));

vi.mock("../../../../hooks/incident/useIncidents/useIncidents", () => ({
  useIncidentHistory: vi.fn(),
  useActionTypes: vi.fn(),
}));

vi.mock("../../../../utils/formatters/formatters", () => ({
  formatDateTime: (date: string) => date,
  formatUserName: (_user: unknown, fallback?: string) => fallback ?? "user",
}));

// Type un resultat de requete simplifie pour les tests.
type QueryStub<T> = { data: T | undefined; isLoading: boolean };

const INCIDENT_ID = "incident-123";

const TEST_ACTION_TYPES = [
  { code: "CREATION", name: "Creation", description: "" },
  { code: "VALIDATION", name: "Validation", description: "" },
  { code: "COMMENT", name: "Comment", description: "" },
];

// Injecte une liste d'entrees d'historique dans le hook mocke.
const mockHistory = (entries: IncidentHistoryResponse[]) => {
  vi.mocked(useIncidentHistory).mockReturnValue({
    data: entries,
    isLoading: false,
  } as QueryStub<IncidentHistoryResponse[]> as ReturnType<
    typeof useIncidentHistory
  >);
};

describe("HistoryTimeline", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useActionTypes).mockReturnValue({
      data: TEST_ACTION_TYPES,
      isLoading: false,
    } as QueryStub<typeof TEST_ACTION_TYPES> as ReturnType<
      typeof useActionTypes
    >);
    mockHistory([]);
  });

  it("should render history entries with username, date and time when data is loaded", () => {
    mockHistory([
      makeIncidentHistory({
        user: {
          id: "user-1",
          username: "jdoe",
          firstName: "John",
          lastName: "Doe",
        },
        action: "CREATION",
        createdAt: "2024-01-01T08:00:00Z",
      }),
    ]);

    renderWithProviders(<HistoryTimeline incidentId={INCIDENT_ID} />);

    expect(screen.getByText(/jdoe/)).toBeInTheDocument();
    expect(screen.getByText(/2024-01-01T08:00:00Z/)).toBeInTheDocument();
  });

  it("should render a natural sentence describing the action with both values", () => {
    mockHistory([
      makeIncidentHistory({
        action: "VALIDATION",
        oldValue: "PENDING_VALIDATION",
        newValue: "VALIDATED",
      }),
    ]);

    renderWithProviders(<HistoryTimeline incidentId={INCIDENT_ID} />);

    // La phrase specifique a l'action, avec la transition depart -> arrivee.
    expect(
      screen.getByText("incidents.history.sentence.VALIDATION"),
    ).toBeInTheDocument();
    expect(screen.getByText("PENDING_VALIDATION")).toBeInTheDocument();
    expect(screen.getByText("VALIDATED")).toBeInTheDocument();
  });

  it("should render entries in descending chronological order when multiple entries exist", () => {
    const oldest = makeIncidentHistory({
      id: "history-1",
      createdAt: "2024-01-01T08:00:00Z",
      action: "CREATION",
    });
    const middle = makeIncidentHistory({
      id: "history-2",
      createdAt: "2024-01-02T08:00:00Z",
      action: "VALIDATION",
    });
    const newest = makeIncidentHistory({
      id: "history-3",
      createdAt: "2024-01-03T08:00:00Z",
      action: "COMMENT",
    });

    mockHistory([oldest, middle, newest]);

    renderWithProviders(<HistoryTimeline incidentId={INCIDENT_ID} />);

    const sentences = screen.getAllByText(
      /incidents\.history\.sentence\.(CREATION|VALIDATION|COMMENT)_plain/,
    );

    expect(sentences[0].textContent).toContain("COMMENT");
    expect(sentences[1].textContent).toContain("VALIDATION");
    expect(sentences[2].textContent).toContain("CREATION");
  });

  it("should render empty state when there is no history", () => {
    mockHistory([]);

    renderWithProviders(<HistoryTimeline incidentId={INCIDENT_ID} />);

    expect(
      screen.getByText("incidents.history.no_history"),
    ).toBeInTheDocument();
  });

  it("should render old and new values when both are present in the history entry", () => {
    mockHistory([
      makeIncidentHistory({
        action: "STATUS_CHANGE",
        oldValue: "OPEN",
        newValue: "VALIDATED",
      }),
    ]);

    renderWithProviders(<HistoryTimeline incidentId={INCIDENT_ID} />);

    expect(screen.getByText("OPEN")).toBeInTheDocument();
    expect(screen.getByText("VALIDATED")).toBeInTheDocument();
  });

  it("should label a workflow comment as a reason", () => {
    mockHistory([
      makeIncidentHistory({ action: "TRANSFER", comment: "Wrong service" }),
    ]);

    renderWithProviders(<HistoryTimeline incidentId={INCIDENT_ID} />);

    // Un commentaire sur une action de workflow est un motif justificatif.
    expect(
      screen.getByText("incidents.history.reason_label"),
    ).toBeInTheDocument();
    expect(screen.getByText("Wrong service")).toBeInTheDocument();
  });

  it("should label a creation comment as a note rather than a reason", () => {
    mockHistory([
      makeIncidentHistory({ action: "CREATION", comment: "Cloned incident" }),
    ]);

    renderWithProviders(<HistoryTimeline incidentId={INCIDENT_ID} />);

    // A la creation le commentaire est descriptif : « Note », pas « Motif ».
    expect(
      screen.getByText("incidents.history.note_label"),
    ).toBeInTheDocument();
    expect(
      screen.queryByText("incidents.history.reason_label"),
    ).not.toBeInTheDocument();
  });

  it("should use the assignment sentence when values are people, not statuses", () => {
    mockHistory([
      makeIncidentHistory({
        action: "ASSIGNMENT",
        oldValue: "Chef Compta",
        newValue: "Agent 3 Beta",
      }),
    ]);

    renderWithProviders(<HistoryTimeline incidentId={INCIDENT_ID} />);

    expect(
      screen.getByText("incidents.history.sentence.ASSIGNMENT"),
    ).toBeInTheDocument();
    expect(screen.getByText("Chef Compta")).toBeInTheDocument();
    expect(screen.getByText("Agent 3 Beta")).toBeInTheDocument();
  });

  it("should use the destination-only sentence when there is no starting value", () => {
    mockHistory([
      makeIncidentHistory({
        action: "CREATION",
        newValue: "PENDING_VALIDATION",
      }),
    ]);

    renderWithProviders(<HistoryTimeline incidentId={INCIDENT_ID} />);

    // Pas de valeur de depart a la creation : variante « _to ».
    expect(
      screen.getByText("incidents.history.sentence.CREATION_to"),
    ).toBeInTheDocument();
    expect(screen.getByText("PENDING_VALIDATION")).toBeInTheDocument();
  });

  it("should use the criticality sentence on a criticality change", () => {
    mockHistory([
      makeIncidentHistory({
        action: "CRITICALITY_CHANGE",
        oldValue: "LOW",
        newValue: "HIGH",
      }),
    ]);

    renderWithProviders(<HistoryTimeline incidentId={INCIDENT_ID} />);

    // La requalification de criticite a sa propre phrase, distincte de UPDATE.
    expect(
      screen.getByText("incidents.history.sentence.CRITICALITY_CHANGE"),
    ).toBeInTheDocument();
    expect(screen.getByText("LOW")).toBeInTheDocument();
    expect(screen.getByText("HIGH")).toBeInTheDocument();
  });
});
