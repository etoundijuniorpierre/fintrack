// Tests frontend : verifie le comportement de resolution attachments.

import { screen } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import ResolutionAttachments from "./ResolutionAttachments";
import { useIncidentAttachments } from "../../../../hooks/document/useDocuments";
import {
  useIncident,
  useIncidentHistory,
} from "../../../../hooks/incident/useIncidents/useIncidents";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    // Les cles parametrees sont rendues "cle:valeur" pour rester assertables.
    t: (key: string, options?: Record<string, unknown>) =>
      options && typeof options.cycle !== "undefined"
        ? `${key}:${options.cycle}`
        : key,
  }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("../../../../hooks/document/useDocuments", () => ({
  useIncidentAttachments: vi.fn(),
}));

vi.mock("../../../../hooks/incident/useIncidents/useIncidents", () => ({
  useIncident: vi.fn(),
  useIncidentHistory: vi.fn(),
}));

vi.mock("../Comments/CommentAttachments", () => ({
  default: ({
    attachments,
  }: {
    attachments: { id: string; filename: string }[];
  }) => (
    <div data-testid="mock-comment-attachments">
      {attachments.map((a) => (
        <span key={a.id}>{a.filename}</span>
      ))}
    </div>
  ),
}));

const TREATMENT_ATTACHMENT = {
  id: "tre-1",
  filename: "treatment.pdf",
  category: "TREATMENT",
  uploadedAt: "2026-03-10T10:00:00",
};
const RESOLUTION_ATTACHMENT = {
  id: "res-1",
  filename: "resolution.pdf",
  category: "RESOLUTION",
  uploadedAt: "2026-03-10T11:00:00",
};
const CLOSURE_ATTACHMENT = {
  id: "clo-1",
  filename: "closure.pdf",
  category: "CLOSURE",
  uploadedAt: "2026-03-10T12:00:00",
};
const UNRESOLVED_ATTACHMENT = {
  id: "unr-1",
  filename: "refus.pdf",
  category: "UNRESOLVED",
  uploadedAt: "2026-03-10T11:30:00",
};
const CANCELLATION_ATTACHMENT = {
  id: "can-1",
  filename: "cancellation.pdf",
  category: "CANCELLATION",
  uploadedAt: "2026-03-10T11:45:00",
};
const OTHER_ATTACHMENT = {
  id: "oth-1",
  filename: "other.pdf",
  category: "OTHER",
  uploadedAt: "2026-03-10T13:00:00",
};

const setup = (attachments: unknown[] = [], history: unknown[] = []) => {
  vi.mocked(useIncidentAttachments).mockReturnValue({
    data: attachments,
  } as never);
  vi.mocked(useIncidentHistory).mockReturnValue({ data: history } as never);
  renderWithProviders(<ResolutionAttachments incidentId="inc-1" />);
};

describe("ResolutionAttachments", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useIncident).mockReturnValue({ data: undefined } as never);
  });

  it("should render nothing without workflow attachments", () => {
    setup([OTHER_ATTACHMENT]);
    expect(
      screen.queryByText("incidents.attachments.treatment_title"),
    ).toBeNull();
    expect(
      screen.queryByText("incidents.attachments.resolution_title"),
    ).toBeNull();
    expect(screen.queryByText("incidents.attachments.closure_title")).toBeNull();
  });

  it("should render only resolution attachments", () => {
    setup([RESOLUTION_ATTACHMENT, OTHER_ATTACHMENT]);
    expect(
      screen.getByText("incidents.attachments.resolution_title"),
    ).toBeInTheDocument();
    expect(screen.queryByText("incidents.attachments.closure_title")).toBeNull();
    expect(screen.getByText("resolution.pdf")).toBeInTheDocument();
  });

  it("should render only closure attachments", () => {
    setup([CLOSURE_ATTACHMENT, OTHER_ATTACHMENT]);
    expect(
      screen.queryByText("incidents.attachments.resolution_title"),
    ).toBeNull();
    expect(
      screen.getByText("incidents.attachments.closure_title"),
    ).toBeInTheDocument();
    expect(screen.getByText("closure.pdf")).toBeInTheDocument();
  });

  // Les trois comptes rendus du workflow ont chacun leur propre groupe de PJ.
  it("should render treatment, resolution and closure as distinct groups", () => {
    setup([TREATMENT_ATTACHMENT, RESOLUTION_ATTACHMENT, CLOSURE_ATTACHMENT]);
    expect(
      screen.getByText("incidents.attachments.treatment_title"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("incidents.attachments.resolution_title"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("incidents.attachments.closure_title"),
    ).toBeInTheDocument();
    expect(screen.getByText("treatment.pdf")).toBeInTheDocument();
    expect(screen.getByText("resolution.pdf")).toBeInTheDocument();
    expect(screen.getByText("closure.pdf")).toBeInTheDocument();
  });

  // Un refus de resolution n'est pas une resolution : ses justificatifs ont leur
  // propre groupe et ne polluent pas le compte rendu de resolution.
  it("should keep resolution refusal attachments in their own group", () => {
    setup([RESOLUTION_ATTACHMENT, UNRESOLVED_ATTACHMENT]);
    expect(
      screen.getByText("incidents.attachments.resolution_title"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("incidents.attachments.unresolved_title"),
    ).toBeInTheDocument();
    expect(screen.getByText("resolution.pdf")).toBeInTheDocument();
    expect(screen.getByText("refus.pdf")).toBeInTheDocument();
  });

  it("should keep cancellation attachments in their own group", () => {
    setup([CANCELLATION_ATTACHMENT]);

    expect(
      screen.getByText("incidents.attachments.cancellation_title"),
    ).toBeInTheDocument();
    expect(screen.getByText("cancellation.pdf")).toBeInTheDocument();
  });

  // Une reouverture ouvre un nouveau cycle : les PJ deposees avant sont conservees
  // mais isolees sous leur rang de cycle, jamais melangees au cycle courant.
  it("should isolate attachments uploaded before a reopening into a previous cycle", () => {
    const beforeReopen = {
      id: "res-old",
      filename: "old-resolution.pdf",
      category: "RESOLUTION",
      uploadedAt: "2026-03-10T09:00:00",
    };
    const afterReopen = {
      id: "res-new",
      filename: "new-resolution.pdf",
      category: "RESOLUTION",
      uploadedAt: "2026-03-12T09:00:00",
    };

    setup(
      [beforeReopen, afterReopen],
      [{ action: "REOPENING", createdAt: "2026-03-11T09:00:00" }],
    );

    // Cycle courant affiche a plat, cycle anterieur replie derriere son libelle.
    expect(screen.getByText("new-resolution.pdf")).toBeInTheDocument();
    expect(
      screen.getByText("incidents.attachments.previous_cycle:1"),
    ).toBeInTheDocument();
  });

  // Sans reouverture, tout appartient au cycle courant : aucun repli ne doit apparaitre.
  it("should not render a previous cycle group when the incident was never reopened", () => {
    setup([RESOLUTION_ATTACHMENT]);
    expect(
      screen.queryByText("incidents.attachments.previous_cycle:1"),
    ).toBeNull();
    expect(screen.getByText("resolution.pdf")).toBeInTheDocument();
  });
});
