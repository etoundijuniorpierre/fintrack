// Tests frontend : verifie le comportement de view incident.test.

import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { renderWithProviders } from "../../../test-utils/renderWithProviders";
import ViewIncident from "./ViewIncident";
import { useIncident } from "../../../hooks/incident/useIncidents/useIncidents";
import type { UseQueryResult } from "@tanstack/react-query";
import type {
  IncidentResponse,
  IncidentHistoryResponse,
  IncidentCommentResponse,
} from "../../../api/incident/types";

// Fabrique une fixture de test pour view incident.test.
const makeQueryResult = <T,>(
  overrides: Partial<UseQueryResult<T, Error>>,
): UseQueryResult<T, Error> => overrides as unknown as UseQueryResult<T, Error>;

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual =
    await vi.importActual<typeof import("react-router-dom")>(
      "react-router-dom",
    );
  return {
    ...actual,
    useParams: () => ({ id: "incident-1" }),
    useNavigate: () => mockNavigate,
  };
});

const mockHasPermission = vi.fn(() => false);

vi.mock("../../../hooks/auth/useAuth", () => ({
  useAuth: () => ({
    user: { id: "user-1", username: "testuser", roles: [], permissions: [] },
    isAuthenticated: true,
    hasRole: () => false,
    hasPermission: mockHasPermission,
  }),
}));

vi.mock("../../../hooks/incident/useIncidents/useIncidents", () => ({
  useIncident: vi.fn(),
}));

vi.mock("../components/Actions/WorkflowActions", () => ({
  default: ({
    incidentId,
    currentAssigneeId,
    currentServiceId,
    defaultServiceId,
    defaultUserId,
    creatorId,
    agencyId,
    agencyName,
    creatorServiceId,
    canValidate,
    canCancel,
  }: {
    incidentId: string;
    currentAssigneeId?: string | null;
    currentServiceId?: string | null;
    defaultServiceId?: string | null;
    defaultUserId?: string | null;
    creatorId?: string | null;
    agencyId?: string | null;
    agencyName?: string | null;
    creatorServiceId?: string | null;
    canValidate?: boolean;
    canCancel?: boolean;
  }) => (
    <div
      data-testid="workflow-actions"
      data-incident-id={incidentId}
      data-current-assignee-id={currentAssigneeId ?? ""}
      data-current-service-id={currentServiceId ?? ""}
      data-default-service-id={defaultServiceId ?? ""}
      data-default-user-id={defaultUserId ?? ""}
      data-creator-id={creatorId ?? ""}
      data-agency-id={agencyId ?? ""}
      data-agency-name={agencyName ?? ""}
      data-creator-service-id={creatorServiceId ?? ""}
      data-can-validate={String(canValidate)}
      data-can-cancel={String(canCancel)}
    >
      WorkflowActions
    </div>
  ),
}));

vi.mock("../components/Comments/sections/CommentsSection", () => ({
  default: ({ incidentId }: { incidentId: string }) => (
    <div data-testid="comments-section" data-incident-id={incidentId}>
      CommentsSection
    </div>
  ),
}));

vi.mock("../components/History/HistoryTimeline", () => ({
  default: ({ incidentId }: { incidentId: string }) => (
    <div data-testid="history-timeline" data-incident-id={incidentId}>
      HistoryTimeline
    </div>
  ),
}));

vi.mock("../components/Attachments/section/AttachmentsSection", () => ({
  default: ({ incidentId }: { incidentId: string }) => (
    <div data-testid="attachments-section" data-incident-id={incidentId}>
      AttachmentsSection
    </div>
  ),
}));

vi.mock("../components/Attachments/ResolutionAttachments", () => ({
  default: () => <div data-testid="resolution-attachments" />,
}));

vi.mock("../components/Form/IncidentForm", () => ({
  default: ({
    onCancel,
    onSuccess,
    initialValues,
  }: {
    onCancel: () => void;
    onSuccess: () => void;
    initialValues?: {
      incidentDate?: { format: (pattern: string) => string };
      observationDate?: { format: (pattern: string) => string };
    };
  }) => (
    <div
      data-testid="incident-form-edit"
      data-incident-date={initialValues?.incidentDate?.format("YYYY-MM-DD")}
      data-observation-date={initialValues?.observationDate?.format("YYYY-MM-DD")}
    >
      <button onClick={onCancel}>CancelEdit</button>
      <button onClick={onSuccess}>SubmitEdit</button>
    </div>
  ),
}));

const mockIncident = {
  id: "incident-1",
  title: "Test Incident Title",
  description: "Test incident description",
  status: "OPEN",
  criticality: "HIGH",
  type: { id: "type-1", name: "TECHNICAL", displayName: "Technical Issue" },
  agency: { id: "agency-1", name: "Test Agency" },
  createdBy: {
    id: "user-1",
    firstName: "John",
    lastName: "Doe",
    username: "johndoe",
  },
  assignedTo: null,
  transferredToService: null,
  transferReason: null,
  rejectReason: null,
  dueDate: null,
  incidentDate: null as string | null,
  observationDate: null as string | null,
  cause: null,
  causeDetail: null,
  resolutionDescription: null,
  createdAt: "2024-01-15T10:00:00Z",
  updatedAt: "2024-01-16T12:00:00Z",
  validatedAt: null,
  transferredAt: null,
  resolvedAt: null,
  closedAt: null,
  history: [] as IncidentHistoryResponse[],
  comments: [] as IncidentCommentResponse[],
};

// Prepare l'affichage lisible de view incident.test.
const renderViewIncident = () => renderWithProviders(<ViewIncident />);

// Fabrique un incident modifiable pour les variantes de test.
const mockEditableIncident = (overrides: Partial<typeof mockIncident> = {}) => {
  vi.mocked(useIncident).mockReturnValue(
    makeQueryResult<IncidentResponse>({
      data: {
        ...mockIncident,
        status: "PENDING_VALIDATION",
        ...overrides,
      } as unknown as IncidentResponse,
      isLoading: false,
      isError: false,
      error: null,
    }),
  );
};

describe("ViewIncident", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockHasPermission.mockReturnValue(false);
    vi.mocked(useIncident).mockReturnValue(
      makeQueryResult<IncidentResponse>({
        data: mockIncident as unknown as IncidentResponse,
        isLoading: false,
        isError: false,
        error: null,
      }),
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe("loading and error states", () => {
    it("should render a loading skeleton when incident data is loading", () => {
      vi.mocked(useIncident).mockReturnValue(
        makeQueryResult<IncidentResponse>({
          data: undefined,
          isLoading: true,
          isError: false,
          error: null,
        }),
      );

      const { container } = renderViewIncident();

      expect(container.querySelector(".ant-skeleton")).toBeInTheDocument();
    });

    it("should render an error result when the incident fetch fails", () => {
      vi.mocked(useIncident).mockReturnValue(
        makeQueryResult<IncidentResponse>({
          data: undefined,
          isLoading: false,
          isError: true,
          error: new Error("Network error"),
        }),
      );

      renderViewIncident();

      expect(screen.getByText("common.error")).toBeInTheDocument();
      expect(screen.queryByTestId("split-layout")).not.toBeInTheDocument();
    });

    it("should render the split-layout when incident data is available", () => {
      renderViewIncident();
      expect(screen.getByTestId("split-layout")).toBeInTheDocument();
    });
  });

  describe("page header", () => {
    it("should render the incident title as a heading when data is loaded", () => {
      renderViewIncident();
      expect(
        screen.getByRole("heading", { name: mockIncident.title }),
      ).toBeInTheDocument();
    });

    it('should render a back button (aria-label="Return") when data is loaded', () => {
      renderViewIncident();
      expect(
        screen.getByRole("button", { name: "Return" }),
      ).toBeInTheDocument();
    });

    it("should navigate to incidents list when back button is clicked", async () => {
      renderViewIncident();
      await userEvent.click(screen.getByRole("button", { name: "Return" }));
      expect(mockNavigate).toHaveBeenCalled();
    });
  });

  describe("SplitLayout two-column structure", () => {
    it("should render the split-layout container when data is loaded", () => {
      renderViewIncident();
      expect(screen.getByTestId("split-layout")).toBeInTheDocument();
    });

    it("should render the main section when data is loaded", () => {
      renderViewIncident();
      expect(screen.getByTestId("split-layout-main")).toBeInTheDocument();
    });

    it("should render the sidebar section when data is loaded", () => {
      renderViewIncident();
      expect(screen.getByTestId("split-layout-sidebar")).toBeInTheDocument();
    });

    it("should render main section before sidebar in DOM order", () => {
      renderViewIncident();
      const container = screen.getByTestId("split-layout");
      const children = Array.from(container.children);
      const mainIndex = children.findIndex(
        (el) => el.getAttribute("data-testid") === "split-layout-main",
      );
      const sidebarIndex = children.findIndex(
        (el) => el.getAttribute("data-testid") === "split-layout-sidebar",
      );
      expect(mainIndex).toBeLessThan(sidebarIndex);
    });
  });

  describe("main section content", () => {
    it("should render the incident description in the main section", () => {
      renderViewIncident();
      const main = screen.getByTestId("split-layout-main");
      expect(main).toHaveTextContent(mockIncident.description);
    });

    it("should display incident and observation dates without a time", () => {
      mockEditableIncident({
        incidentDate: "2024-01-15",
        observationDate: "2024-01-16",
      });

      renderViewIncident();

      expect(screen.getByText("15/01/2024")).toBeInTheDocument();
      expect(screen.getByText("16/01/2024")).toBeInTheDocument();
      expect(screen.queryByText(/00:00/)).not.toBeInTheDocument();
    });

    it("should render the incident type display name in the main section", () => {
      renderViewIncident();
      const main = screen.getByTestId("split-layout-main");
      expect(main).toHaveTextContent(mockIncident.type.displayName);
    });

    it("should render the created-by user full name in the main section", () => {
      renderViewIncident();
      const main = screen.getByTestId("split-layout-main");
      // formatUserName rend « NOM Prenom » : c'est la convention d'affichage.
      expect(main).toHaveTextContent(
        `${mockIncident.createdBy.lastName} ${mockIncident.createdBy.firstName}`,
      );
    });

    it("should render the agency name in the main section", () => {
      renderViewIncident();
      const main = screen.getByTestId("split-layout-main");
      expect(main).toHaveTextContent(mockIncident.agency.name);
    });

    it("should render CommentsSection inside the main section", () => {
      renderViewIncident();
      const main = screen.getByTestId("split-layout-main");
      expect(main).toContainElement(screen.getByTestId("comments-section"));
    });

    it("should pass the correct incidentId to CommentsSection", () => {
      renderViewIncident();
      expect(screen.getByTestId("comments-section")).toHaveAttribute(
        "data-incident-id",
        "incident-1",
      );
    });
  });

  describe("sidebar section content", () => {
    it("should render HistoryTimeline inside the sidebar section", () => {
      renderViewIncident();
      const sidebar = screen.getByTestId("split-layout-sidebar");
      expect(sidebar).toContainElement(screen.getByTestId("history-timeline"));
    });

    it("should pass the correct incidentId to HistoryTimeline", () => {
      renderViewIncident();
      expect(screen.getByTestId("history-timeline")).toHaveAttribute(
        "data-incident-id",
        "incident-1",
      );
    });

    it("should not render HistoryTimeline in the main section", () => {
      renderViewIncident();
      const main = screen.getByTestId("split-layout-main");
      expect(main).not.toContainElement(screen.getByTestId("history-timeline"));
    });
  });

  describe("WorkflowActions", () => {
    it("should render WorkflowActions with the correct incidentId", () => {
      renderViewIncident();
      expect(screen.getByTestId("workflow-actions")).toHaveAttribute(
        "data-incident-id",
        "incident-1",
      );
    });

    it("should pass assignment and routing context to WorkflowActions", () => {
      vi.mocked(useIncident).mockReturnValue(
        makeQueryResult<IncidentResponse>({
          data: {
            ...mockIncident,
            assignedTo: {
              id: "assignee-1",
              firstName: "Aline",
              lastName: "Solver",
              username: "asolver",
            },
            transferredToService: { id: "service-1", name: "IT" },
            creatorServiceId: "service-creator",
            type: {
              ...mockIncident.type,
              defaultTargetService: { id: "service-default", name: "Support" },
              defaultTargetUser: {
                id: "user-default",
                firstName: "Default",
                lastName: "Owner",
                username: "owner",
              },
            },
          } as unknown as IncidentResponse,
          isLoading: false,
          isError: false,
          error: null,
        }),
      );

      renderViewIncident();

      expect(screen.getByTestId("workflow-actions")).toHaveAttribute(
        "data-current-assignee-id",
        "assignee-1",
      );
      expect(screen.getByTestId("workflow-actions")).toHaveAttribute(
        "data-current-service-id",
        "service-1",
      );
      expect(screen.getByTestId("workflow-actions")).toHaveAttribute(
        "data-default-service-id",
        "service-default",
      );
      expect(screen.getByTestId("workflow-actions")).toHaveAttribute(
        "data-default-user-id",
        "user-default",
      );
      expect(screen.getByTestId("workflow-actions")).toHaveAttribute(
        "data-creator-id",
        "user-1",
      );
      expect(screen.getByTestId("workflow-actions")).toHaveAttribute(
        "data-agency-id",
        "agency-1",
      );
      expect(screen.getByTestId("workflow-actions")).toHaveAttribute(
        "data-agency-name",
        "Test Agency",
      );
      expect(screen.getByTestId("workflow-actions")).toHaveAttribute(
        "data-creator-service-id",
        "service-creator",
      );
    });
  });

  describe("backend verdicts wiring", () => {
    it("should forward the cancellation verdict to the workflow actions", () => {
      // Le verdict est calcule par le back : s'il n'est pas transmis, le bouton
      // Annuler ne s'affiche jamais, quelle que soit la portee de l'utilisateur.
      mockEditableIncident({ canCancel: true } as unknown as Partial<
        typeof mockIncident
      >);

      renderViewIncident();

      expect(screen.getByTestId("workflow-actions")).toHaveAttribute(
        "data-can-cancel",
        "true",
      );
    });

    it("should forward the validation verdict to the workflow actions", () => {
      mockEditableIncident({ canValidate: true } as unknown as Partial<
        typeof mockIncident
      >);

      renderViewIncident();

      expect(screen.getByTestId("workflow-actions")).toHaveAttribute(
        "data-can-validate",
        "true",
      );
    });
  });

  describe("pending relevance confirmation", () => {
    it("should announce that the source entity still has to answer", () => {
      // Sans ce bandeau, l'incident semble simplement immobile : rien ne dit
      // qu'une reponse est attendue, ni de qui.
      mockEditableIncident({
        status: "UNRESOLVED_PROLONGED_WAIT",
        confirmationRequestedAt: "2026-08-20T10:00:00",
      } as unknown as Partial<typeof mockIncident>);

      renderViewIncident();

      expect(
        screen.getByText("incidents.workflow.pending_confirmation.title"),
      ).toBeInTheDocument();
    });

    it("should stay silent when no confirmation is awaited", () => {
      mockEditableIncident({ status: "UNRESOLVED_PROLONGED_WAIT" });

      renderViewIncident();

      expect(
        screen.queryByText("incidents.workflow.pending_confirmation.title"),
      ).not.toBeInTheDocument();
    });
  });

  describe("expected validator", () => {
    it("should name the service manager expected to validate", () => {
      mockEditableIncident({
        type: {
          id: "type-1",
          name: "TECHNICAL",
          displayName: "Technical Issue",
          requiresValidation: true,
        },
        expectedValidatorRole: "SERVICE_MANAGER",
        expectedValidatorTarget: "Comptabilité",
      } as unknown as Partial<typeof mockIncident>);

      renderViewIncident();

      expect(
        screen.getByText("incidents.validator_roles.SERVICE_MANAGER_named"),
      ).toBeInTheDocument();
    });

    it("should show the admin validator even when the type normally skips validation", () => {
      mockEditableIncident({
        type: {
          id: "type-1",
          name: "TECHNICAL",
          displayName: "Technical Issue",
          requiresValidation: false,
        },
        expectedValidatorRole: "ADMIN",
      } as unknown as Partial<typeof mockIncident>);

      renderViewIncident();

      expect(
        screen.getByText("incidents.validator_roles.ADMIN"),
      ).toBeInTheDocument();
    });
  });

  describe("conditional rendering", () => {
    it("should render assignedTo field when incident has an assigned user", () => {
      vi.mocked(useIncident).mockReturnValue(
        makeQueryResult<IncidentResponse>({
          data: {
            ...mockIncident,
            assignedTo: {
              id: "user-2",
              firstName: "Jane",
              lastName: "Smith",
              username: "janesmith",
            },
          } as unknown as IncidentResponse,
          isLoading: false,
          isError: false,
          error: null,
        }),
      );

      renderViewIncident();

      expect(screen.getByText("Jane Smith")).toBeInTheDocument();
    });

    it("should not render assignedTo field when incident has no assigned user", () => {
      renderViewIncident();
      expect(
        screen.queryByText("incidents.form.labels.assigned_to"),
      ).not.toBeInTheDocument();
    });

    it("should render rejectReason field when incident has a reject reason", () => {
      vi.mocked(useIncident).mockReturnValue(
        makeQueryResult<IncidentResponse>({
          data: {
            ...mockIncident,
            rejectReason: "Not valid",
          } as unknown as IncidentResponse,
          isLoading: false,
          isError: false,
          error: null,
        }),
      );

      renderViewIncident();

      expect(screen.getByText("Not valid")).toBeInTheDocument();
    });

    it("should render proposedSolutionTitle alert when incident has proposed solution before validation", () => {
      vi.mocked(useIncident).mockReturnValue(
        makeQueryResult<IncidentResponse>({
          data: {
            ...mockIncident,
            status: "DRAFT",
            proposedSolution: "Solution de test",
          } as unknown as IncidentResponse,
          isLoading: false,
          isError: false,
          error: null,
        }),
      );

      renderViewIncident();

      expect(screen.getByText("Solution de test")).toBeInTheDocument();
      expect(
        screen.getByText("incidents.workflow.modals.submit_solution.proposedSolutionTitle"),
      ).toBeInTheDocument();
    });

    it("should render treatmentSolutionTitle alert when incident has proposed solution after validation", () => {
      vi.mocked(useIncident).mockReturnValue(
        makeQueryResult<IncidentResponse>({
          data: {
            ...mockIncident,
            status: "IN_PROGRESS",
            proposedSolution: "Solution de test",
          } as unknown as IncidentResponse,
          isLoading: false,
          isError: false,
          error: null,
        }),
      );

      renderViewIncident();

      expect(screen.getByText("Solution de test")).toBeInTheDocument();
      expect(
        screen.getByText("incidents.workflow.modals.submit_solution.treatmentSolutionTitle"),
      ).toBeInTheDocument();
    });

    it("should render directionRejectionReason alert when incident has direction rejection reason", () => {
      vi.mocked(useIncident).mockReturnValue(
        makeQueryResult<IncidentResponse>({
          data: {
            ...mockIncident,
            directionRejectionReason: "Procédure incomplète",
          } as unknown as IncidentResponse,
          isLoading: false,
          isError: false,
          error: null,
        }),
      );

      renderViewIncident();

      expect(screen.getByText("Procédure incomplète")).toBeInTheDocument();
    });
  });

  describe("inline edit mode", () => {
    it("should not show the edit form in view mode by default", () => {
      renderViewIncident();
      expect(
        screen.queryByTestId("incident-form-edit"),
      ).not.toBeInTheDocument();
    });

    it("should render edit button for the creator when the incident status allows correction", () => {
      mockEditableIncident();
      mockHasPermission.mockReturnValue(true);

      renderViewIncident();

      expect(screen.getByText("common.edit")).toBeInTheDocument();
    });

    it("should not render edit button for a non-creator without update permission", () => {
      mockEditableIncident({
        createdBy: {
          id: "other-user",
          firstName: "Other",
          lastName: "User",
          username: "other",
        },
      });

      renderViewIncident();

      expect(screen.queryByText("common.edit")).not.toBeInTheDocument();
    });

    it("should not render edit button for a non-creator even with update permission", () => {
      mockEditableIncident({
        createdBy: {
          id: "other-user",
          firstName: "Other",
          lastName: "User",
          username: "other",
        },
      });
      mockHasPermission.mockReturnValue(true);
      renderViewIncident();
      expect(screen.queryByText("common.edit")).not.toBeInTheDocument();
    });

    it("should show inline IncidentForm and hide SplitLayout when edit button is clicked", async () => {
      mockEditableIncident();
      mockHasPermission.mockReturnValue(true);
      renderViewIncident();

      await userEvent.click(screen.getByText("common.edit"));

      expect(screen.getByTestId("incident-form-edit")).toBeInTheDocument();
      expect(screen.queryByTestId("split-layout")).not.toBeInTheDocument();
    });

    it("should preserve the business dates when opening the edit form", async () => {
      mockEditableIncident({
        incidentDate: "2024-01-14",
        observationDate: "2024-01-15",
      });
      mockHasPermission.mockReturnValue(true);
      renderViewIncident();

      await userEvent.click(screen.getByText("common.edit"));

      expect(screen.getByTestId("incident-form-edit")).toHaveAttribute(
        "data-incident-date",
        "2024-01-14",
      );
      expect(screen.getByTestId("incident-form-edit")).toHaveAttribute(
        "data-observation-date",
        "2024-01-15",
      );
    });

    it("should return to view mode and show SplitLayout when cancel is clicked inside edit form", async () => {
      mockEditableIncident();
      mockHasPermission.mockReturnValue(true);
      renderViewIncident();

      await userEvent.click(screen.getByText("common.edit"));
      expect(screen.getByTestId("incident-form-edit")).toBeInTheDocument();

      await userEvent.click(screen.getByText("CancelEdit"));

      expect(
        screen.queryByTestId("incident-form-edit"),
      ).not.toBeInTheDocument();
      expect(screen.getByTestId("split-layout")).toBeInTheDocument();
    });

    it("should return to view mode when edit form submits successfully", async () => {
      mockEditableIncident();
      mockHasPermission.mockReturnValue(true);
      renderViewIncident();

      await userEvent.click(screen.getByText("common.edit"));
      await userEvent.click(screen.getByText("SubmitEdit"));

      expect(
        screen.queryByTestId("incident-form-edit"),
      ).not.toBeInTheDocument();
      expect(screen.getByTestId("split-layout")).toBeInTheDocument();
    });

    it("should not navigate when edit button is clicked (inline, not routed)", async () => {
      mockEditableIncident();
      mockHasPermission.mockReturnValue(true);
      renderViewIncident();

      await userEvent.click(screen.getByText("common.edit"));

      expect(mockNavigate).not.toHaveBeenCalled();
    });
  });

  describe("end-to-end user flow", () => {
    it("should display incident details and allow navigation back to incidents list", async () => {
      renderViewIncident();

      expect(
        screen.getByRole("heading", { name: mockIncident.title }),
      ).toBeInTheDocument();
      expect(screen.getByTestId("split-layout-main")).toHaveTextContent(
        mockIncident.description,
      );
      expect(screen.getByTestId("split-layout-sidebar")).toContainElement(
        screen.getByTestId("history-timeline"),
      );

      await userEvent.click(screen.getByRole("button", { name: "Return" }));

      expect(mockNavigate).toHaveBeenCalled();
    });
  });
});
