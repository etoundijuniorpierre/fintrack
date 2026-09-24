// Tests frontend : verifie le comportement de workflow actions.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, fireEvent, waitFor } from "@testing-library/react";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";
import WorkflowActions from "./WorkflowActions";
import { useAuth } from "../../../../hooks/auth/useAuth";
import {
  useValidateIncident,
  useRejectIncident,
  useTransferIncident,
  useAssignIncident,
  useStartIncident,
  useBlockIncident,
  useResumeIncident,
  useRequestConfirmation,
  useConfirmRelevance,
  useTreatIncident,
  useResolveIncident,
  useMarkUnresolvedIncident,
  useCloseIncident,
  useReopenIncident,
  useCancelIncident,
  useIncidentComments,
  useCloneIncident,
  useResubmitIncident,
  useSubmitSolution,
  useDirectionValidate,
  useDirectionReject,
} from "../../../../hooks/incident/useIncidents/useIncidents";
import {
  useIncidentAttachments,
  useUploadAttachment,
} from "../../../../hooks/document/useDocuments";

const mockNavigateToIncidents = vi.hoisted(() => vi.fn());
const mockNavigateToIncidentDetail = vi.hoisted(() => vi.fn());

vi.mock("../../../../utils/navigation/incidents/incidents", () => ({
  incidentNavigation: {
    navigateToIncidents: mockNavigateToIncidents,
    navigateToIncidentCreate: vi.fn(),
    navigateToIncidentDetail: mockNavigateToIncidentDetail,
  },
  incidentPathIdentifier: (incident: {
    id: string;
    reference?: string | null;
  }) => incident.reference ?? incident.id,
}));

const mockDirectoryData = vi.hoisted(() => ({
  services: [] as Array<{ id: string; name: string }>,
  users: [] as Array<Record<string, unknown>>,
  incidentTypes: [] as Array<Record<string, unknown>>,
}));

const assignableUsersSpy = vi.hoisted(() => vi.fn());

// Espion sur t : rend toujours la clef (comme avant) mais garde la trace des valeurs
// d'interpolation, seul moyen de verifier ce que la modale annonce reellement.
const translateSpy = vi.hoisted(() => vi.fn((key: string) => key));

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: translateSpy }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("../../../../hooks/auth/useAuth", () => ({
  useAuth: vi.fn(() => ({
    hasPermission: () => false,
    user: { id: "user-1" },
  })),
}));

vi.mock("../../../../hooks/incident/useIncidents/useIncidents", () => ({
  useValidateIncident: vi.fn(),
  useRejectIncident: vi.fn(),
  useTransferIncident: vi.fn(),
  useAssignIncident: vi.fn(),
  useStartIncident: vi.fn(),
  useBlockIncident: vi.fn(),
  useResumeIncident: vi.fn(),
  useRequestConfirmation: vi.fn(),
  useConfirmRelevance: vi.fn(),
  useTreatIncident: vi.fn(),
  useResolveIncident: vi.fn(),
  useMarkUnresolvedIncident: vi.fn(),
  useCloseIncident: vi.fn(),
  useReopenIncident: vi.fn(),
  useCancelIncident: vi.fn(),
  useIncidentComments: vi.fn(),
  useCloneIncident: vi.fn(),
  useResubmitIncident: vi.fn(),
  useSubmitSolution: vi.fn(),
  useDirectionValidate: vi.fn(),
  useDirectionReject: vi.fn(),
  useIncidentTypes: () => ({ data: mockDirectoryData.incidentTypes }),
}));

vi.mock("../../../../hooks/service/useServices", () => ({
  useServices: () => ({ data: mockDirectoryData.services }),
}));

vi.mock("../../../../hooks/user/useUsers/useUsers", () => ({
  useUsers: () => ({ data: mockDirectoryData.users }),
  useAssignableUsers: (
    params?: { serviceId?: string },
    options?: { enabled?: boolean },
  ) => {
    assignableUsersSpy(params, options);
    return { data: mockDirectoryData.users };
  },
}));

vi.mock("../../../../hooks/document/useDocuments", () => ({
  useIncidentAttachments: vi.fn(),
  useUploadAttachment: vi.fn(),
}));

const INCIDENT_ID = "incident-1";
const USER_ID = "user-1";
const AGENCY_ID = "agency-1";

const VALIDATE_KEY = "incidents.workflow.validate";
const REJECT_KEY = "incidents.workflow.reject";
const RESOLVE_KEY = "incidents.workflow.resolve";
const BLOCK_KEY = "incidents.workflow.block";
const RESUME_KEY = "incidents.workflow.resume";
const CANCEL_KEY = "incidents.workflow.cancel";
const TRANSFER_KEY = "incidents.workflow.transfer";
const CLONE_KEY = "incidents.workflow.clone";
const RESUBMIT_KEY = "incidents.workflow.resubmit";
const REOPEN_KEY = "incidents.workflow.reopen";
const REJECT_REASON_REQUIRED =
  "incidents.workflow.modals.reject.reason_required";
const RESOLVE_DESCRIPTION_REQUIRED =
  "incidents.workflow.modals.resolve.note_required";
const BLOCK_REASON_REQUIRED = "incidents.workflow.modals.block.reason_required";
const CANCEL_REASON_REQUIRED =
  "incidents.workflow.modals.cancel.reason_required";
const VALIDATE_SERVICE_REQUIRED =
  "incidents.workflow.modals.transfer.service_required";

const openMoreActions = () => {
  const trigger = screen.queryByText("common.more_actions");
  if (trigger) {
    fireEvent.mouseOver(trigger);
  }
};

const mockValidateMutate = vi.fn();
const mockRejectMutate = vi.fn();
const mockBlockMutate = vi.fn();
const mockResumeMutate = vi.fn();
const mockCancelMutate = vi.fn();
const mockRequestConfirmationMutate = vi.fn();
const mockConfirmRelevanceMutate = vi.fn();
const mockTransferMutate = vi.fn();
const mockUploadAttachment = vi.fn();

describe("WorkflowActions", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockDirectoryData.services = [];
    mockDirectoryData.users = [];
    mockDirectoryData.incidentTypes = [];
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: () => false,
    } as unknown as ReturnType<typeof useAuth>);
    vi.mocked(useIncidentComments).mockReturnValue({
      data: [],
      isLoading: false,
    } as unknown as ReturnType<typeof useIncidentComments>);
    vi.mocked(useIncidentAttachments).mockReturnValue({
      data: [],
    } as unknown as ReturnType<typeof useIncidentAttachments>);
    vi.mocked(useUploadAttachment).mockReturnValue({
      mutateAsync: mockUploadAttachment,
      isPending: false,
    } as unknown as ReturnType<typeof useUploadAttachment>);

    const noOpMutation = { mutate: vi.fn(), isPending: false };
    vi.mocked(useValidateIncident).mockReturnValue({
      mutate: mockValidateMutate,
      isPending: false,
    } as unknown as ReturnType<typeof useValidateIncident>);
    vi.mocked(useRejectIncident).mockReturnValue({
      mutate: mockRejectMutate,
      isPending: false,
    } as unknown as ReturnType<typeof useRejectIncident>);
    vi.mocked(useTransferIncident).mockReturnValue({
      mutate: mockTransferMutate,
      isPending: false,
    } as unknown as ReturnType<typeof useTransferIncident>);
    vi.mocked(useAssignIncident).mockReturnValue(
      noOpMutation as unknown as ReturnType<typeof useAssignIncident>,
    );
    vi.mocked(useStartIncident).mockReturnValue(
      noOpMutation as unknown as ReturnType<typeof useStartIncident>,
    );
    vi.mocked(useBlockIncident).mockReturnValue({
      mutate: mockBlockMutate,
      isPending: false,
    } as unknown as ReturnType<typeof useBlockIncident>);
    vi.mocked(useResumeIncident).mockReturnValue({
      mutate: mockResumeMutate,
      isPending: false,
    } as unknown as ReturnType<typeof useResumeIncident>);
    vi.mocked(useRequestConfirmation).mockReturnValue({
      mutate: mockRequestConfirmationMutate,
      isPending: false,
    } as unknown as ReturnType<typeof useRequestConfirmation>);
    vi.mocked(useConfirmRelevance).mockReturnValue({
      mutate: mockConfirmRelevanceMutate,
      isPending: false,
    } as unknown as ReturnType<typeof useConfirmRelevance>);
    vi.mocked(useTreatIncident).mockReturnValue(
      noOpMutation as unknown as ReturnType<typeof useTreatIncident>,
    );
    vi.mocked(useResolveIncident).mockReturnValue(
      noOpMutation as unknown as ReturnType<typeof useResolveIncident>,
    );
    vi.mocked(useMarkUnresolvedIncident).mockReturnValue(
      noOpMutation as unknown as ReturnType<typeof useMarkUnresolvedIncident>,
    );
    vi.mocked(useCloseIncident).mockReturnValue(
      noOpMutation as unknown as ReturnType<typeof useCloseIncident>,
    );
    vi.mocked(useReopenIncident).mockReturnValue(
      noOpMutation as unknown as ReturnType<typeof useReopenIncident>,
    );
    vi.mocked(useCancelIncident).mockReturnValue({
      mutate: mockCancelMutate,
      isPending: false,
    } as unknown as ReturnType<typeof useCancelIncident>);
    vi.mocked(useCloneIncident).mockReturnValue(
      noOpMutation as unknown as ReturnType<typeof useCloneIncident>,
    );
    vi.mocked(useResubmitIncident).mockReturnValue(
      noOpMutation as unknown as ReturnType<typeof useResubmitIncident>,
    );
    vi.mocked(useSubmitSolution).mockReturnValue(
      noOpMutation as unknown as ReturnType<typeof useSubmitSolution>,
    );
    vi.mocked(useDirectionValidate).mockReturnValue(
      noOpMutation as unknown as ReturnType<typeof useDirectionValidate>,
    );
    vi.mocked(useDirectionReject).mockReturnValue(
      noOpMutation as unknown as ReturnType<typeof useDirectionReject>,
    );
  });

  it("should render no buttons when user has no workflow permissions", () => {
    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="OPEN"
        defaultServiceId="service-default"
        canValidate
      />,
    );

    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  it("should render validate button when user has INCIDENT_VALIDATE permission", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) =>
        p === "INCIDENT_VALIDATE" || p === "INCIDENT_VIEW_ALL",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="OPEN"
        defaultServiceId="service-default"
        canValidate
      />,
    );

    expect(
      screen.getByRole("button", { name: VALIDATE_KEY }),
    ).toBeInTheDocument();
  });

  it("should show validate when the backend grants it via canValidate", () => {
    // La portee de validation est calculee cote back (verdict expose par le detail).
    // Un chef service sans droit admin voit Valider des lors que canValidate est vrai.
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID, roles: ["CHEF_SERVICE"] },
      hasPermission: (p: string) => p === "INCIDENT_VALIDATE",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions incidentId={INCIDENT_ID} status="OPEN" canValidate />,
    );

    expect(
      screen.getByRole("button", { name: VALIDATE_KEY }),
    ).toBeInTheDocument();
  });

  it("should hide validate when the backend denies it via canValidate", () => {
    // Meme avec la permission INCIDENT_VALIDATE, si le back refuse la portee
    // (canValidate=false) le bouton ne doit jamais apparaitre.
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID, roles: ["CHEF_SERVICE"] },
      hasPermission: (p: string) => p === "INCIDENT_VALIDATE",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="OPEN"
        canValidate={false}
      />,
    );

    expect(
      screen.queryByRole("button", { name: VALIDATE_KEY }),
    ).not.toBeInTheDocument();
  });

  it("should render reject action in the dropdown when user has INCIDENT_REJECT permission", async () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) =>
        p === "INCIDENT_REJECT" || p === "INCIDENT_VIEW_ALL",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions incidentId={INCIDENT_ID} status="OPEN" canValidate />,
    );

    openMoreActions();
    await waitFor(() =>
      expect(screen.getByText(REJECT_KEY)).toBeInTheDocument(),
    );
  });

  it("should render both actions flat without the dropdown when only two are available", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) =>
        p === "INCIDENT_VALIDATE" ||
        p === "INCIDENT_REJECT" ||
        p === "INCIDENT_VIEW_ALL",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions incidentId={INCIDENT_ID} status="OPEN" canValidate />,
    );

    expect(
      screen.getByRole("button", { name: VALIDATE_KEY }),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: REJECT_KEY })).toBeInTheDocument();
    expect(screen.queryByText("common.more_actions")).not.toBeInTheDocument();
  });

  it("should open the validate modal when validate button is clicked", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) =>
        p === "INCIDENT_VALIDATE" || p === "INCIDENT_VIEW_ALL",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions incidentId={INCIDENT_ID} status="OPEN" canValidate />,
    );

    fireEvent.click(screen.getByRole("button", { name: VALIDATE_KEY }));

    expect(
      screen.getByText("incidents.workflow.modals.validate.title"),
    ).toBeInTheDocument();
  });

  it("should call validate mutation when validate modal is confirmed", async () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) =>
        p === "INCIDENT_VALIDATE" || p === "INCIDENT_VIEW_ALL",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="OPEN"
        defaultServiceId="service-default"
        canValidate
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: VALIDATE_KEY }));
    fireEvent.click(
      screen.getByText("incidents.workflow.modals.validate.confirm"),
    );

    await waitFor(() =>
      expect(mockValidateMutate).toHaveBeenCalledWith(
        {
          id: INCIDENT_ID,
          data: {
            comment: undefined,
            targetServiceId: undefined,
            targetUserId: undefined,
          },
        },
        expect.objectContaining({ onSuccess: expect.any(Function) }),
      ),
    );
  });

  it("should show error and not call validate mutation when routing service is missing", async () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) =>
        p === "INCIDENT_VALIDATE" || p === "INCIDENT_VIEW_ALL",
    } as unknown as ReturnType<typeof useAuth>);

    // No defaultServiceId, so requiresValidationRouting = true
    renderWithProviders(
      <WorkflowActions incidentId={INCIDENT_ID} status="OPEN" canValidate />,
    );

    fireEvent.click(screen.getByRole("button", { name: VALIDATE_KEY }));
    fireEvent.click(
      screen.getByText("incidents.workflow.modals.validate.confirm"),
    );

    await waitFor(() =>
      expect(screen.getByText(VALIDATE_SERVICE_REQUIRED)).toBeInTheDocument(),
    );

    expect(mockValidateMutate).not.toHaveBeenCalled();
  });

  it("should show error and not call mutation when reject reason is empty", async () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) =>
        p === "INCIDENT_REJECT" || p === "INCIDENT_VIEW_ALL",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions incidentId={INCIDENT_ID} status="OPEN" canValidate />,
    );

    openMoreActions();
    await waitFor(() =>
      expect(screen.getByText(REJECT_KEY)).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByText(REJECT_KEY));
    fireEvent.click(
      screen.getByText("incidents.workflow.modals.reject.confirm"),
    );

    await waitFor(() =>
      expect(screen.getByText(REJECT_REASON_REQUIRED)).toBeInTheDocument(),
    );

    expect(mockRejectMutate).not.toHaveBeenCalled();
  });

  it("should require a description before resolving an incident", async () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) => p === "INCIDENT_RESOLVE",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="TREATED"
        currentAssigneeId={USER_ID}
        resolverRoles={["ASSIGNEE"]}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: RESOLVE_KEY }));
    fireEvent.click(
      screen.getByText("incidents.workflow.modals.resolve.confirm"),
    );

    await waitFor(() =>
      expect(screen.getByText(RESOLVE_DESCRIPTION_REQUIRED)).toBeInTheDocument(),
    );

    expect(vi.mocked(useResolveIncident)().mutate).not.toHaveBeenCalled();
  });

  it("should show resolve to user with INCIDENT_RESOLVE permission when they are the assignee", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) => p === "INCIDENT_RESOLVE",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="TREATED"
        currentAssigneeId={USER_ID}
        resolverRoles={["ASSIGNEE"]}
      />,
    );

    expect(
      screen.getByRole("button", { name: RESOLVE_KEY }),
    ).toBeInTheDocument();
  });

  it("should show block in the dropdown for a user with INCIDENT_TREAT permission while incident is in progress", async () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) => p === "INCIDENT_TREAT",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="IN_PROGRESS"
        currentAssigneeId={USER_ID}
      />,
    );

    openMoreActions();
    await waitFor(() =>
      expect(screen.getByText(BLOCK_KEY)).toBeInTheDocument(),
    );
  });

  it("should require a reason before blocking an incident", async () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) => p === "INCIDENT_TREAT",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="IN_PROGRESS"
        currentAssigneeId={USER_ID}
      />,
    );

    openMoreActions();
    await waitFor(() =>
      expect(screen.getByText(BLOCK_KEY)).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByText(BLOCK_KEY));
    fireEvent.click(
      screen.getByText("incidents.workflow.modals.block.confirm"),
    );

    await waitFor(() =>
      expect(screen.getByText(BLOCK_REASON_REQUIRED)).toBeInTheDocument(),
    );

    expect(mockBlockMutate).not.toHaveBeenCalled();
  });

  it("should show resume only to the current assignee while incident is blocked", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) => p === "INCIDENT_TREAT",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="BLOCKED"
        currentAssigneeId={USER_ID}
      />,
    );

    expect(
      screen.getByRole("button", { name: RESUME_KEY }),
    ).toBeInTheDocument();
  });

  it("should render clone action in the dropdown when user has INCIDENT_CREATE permission on a CLOSED incident", async () => {
    vi.mocked(useAuth).mockReturnValue({
      hasPermission: (p: string) => p === "INCIDENT_CREATE",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions incidentId={INCIDENT_ID} status="CLOSED" />,
    );

    openMoreActions();
    await waitFor(() =>
      expect(screen.getByText(CLONE_KEY)).toBeInTheDocument(),
    );
  });

  it.each(["REJECTED", "CANCELLED"])(
    "should render clone action on a %s incident",
    async (status) => {
      vi.mocked(useAuth).mockReturnValue({
        hasPermission: (p: string) => p === "INCIDENT_CREATE",
      } as unknown as ReturnType<typeof useAuth>);

      renderWithProviders(
        <WorkflowActions incidentId={INCIDENT_ID} status={status} />,
      );

      openMoreActions();
      await waitFor(() =>
        expect(screen.getByText(CLONE_KEY)).toBeInTheDocument(),
      );
    },
  );

  it("should render resubmit action for the creator with INCIDENT_CREATE permission on a REOPENED incident", async () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) => p === "INCIDENT_CREATE",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="REOPENED"
        creatorId={USER_ID}
      />,
    );

    openMoreActions();
    await waitFor(() =>
      expect(screen.getByText(RESUBMIT_KEY)).toBeInTheDocument(),
    );
  });

  it("should hide resubmit action for a non-creator even with INCIDENT_CREATE permission on a REOPENED incident", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) => p === "INCIDENT_CREATE",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="REOPENED"
        creatorId="other-creator"
      />,
    );

    expect(screen.queryByText(RESUBMIT_KEY)).not.toBeInTheDocument();
    expect(screen.queryByText("common.more_actions")).not.toBeInTheDocument();
  });

  // ===== reopen : portee acteur sur un incident REJETE (createur ou rejeteur) =====

  it("should hide reopen on a REJECTED incident when user is neither creator nor rejector", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) => p === "INCIDENT_REOPEN",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="REJECTED"
        creatorId="other-creator"
        reopenRejectorId="other-rejector"
      />,
    );

    expect(
      screen.queryByRole("button", { name: REOPEN_KEY }),
    ).not.toBeInTheDocument();
  });

  it("should show reopen on a REJECTED incident when user is the creator", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) => p === "INCIDENT_REOPEN",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="REJECTED"
        creatorId={USER_ID}
        reopenRejectorId="other-rejector"
      />,
    );

    expect(
      screen.getByRole("button", { name: REOPEN_KEY }),
    ).toBeInTheDocument();
  });

  it("should show reopen on a REJECTED incident when user is the rejector", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) => p === "INCIDENT_REOPEN",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="REJECTED"
        creatorId="other-creator"
        reopenRejectorId={USER_ID}
      />,
    );

    expect(
      screen.getByRole("button", { name: REOPEN_KEY }),
    ).toBeInTheDocument();
  });

  // Réouverture après traitement (RÉSOLU) : mêmes acteurs que la clôture (closerType).

  it("should hide reopen on a RESOLVED incident from the assignee by default (reopener = agency manager)", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) => p === "INCIDENT_REOPEN",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="RESOLVED"
        creatorId="other-creator"
        currentAssigneeId={USER_ID}
      />,
    );

    expect(
      screen.queryByRole("button", { name: REOPEN_KEY }),
    ).not.toBeInTheDocument();
  });

  it("should show reopen on a RESOLVED incident to the source agency manager by default", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID, agencyId: AGENCY_ID },
      hasPermission: (p: string) =>
        p === "INCIDENT_REOPEN" || p === "INCIDENT_VIEW_AGENCY",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="RESOLVED"
        creatorId="other-creator"
        currentAssigneeId="other-assignee"
        agencyId={AGENCY_ID}
      />,
    );

    expect(
      screen.getByRole("button", { name: REOPEN_KEY }),
    ).toBeInTheDocument();
  });

  it("should show reopen on a RESOLVED incident to the assignee when reopenerRoles includes ASSIGNEE", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) => p === "INCIDENT_REOPEN",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="RESOLVED"
        creatorId="other-creator"
        currentAssigneeId={USER_ID}
        reopenerRoles={["ASSIGNEE"]}
      />,
    );

    expect(
      screen.getByRole("button", { name: REOPEN_KEY }),
    ).toBeInTheDocument();
  });

  it("should hide reopen on a RESOLVED incident from a non-creator when reopenerRoles is CREATOR", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) => p === "INCIDENT_REOPEN",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="RESOLVED"
        creatorId="other-creator"
        currentAssigneeId={USER_ID}
        reopenerRoles={["CREATOR"]}
      />,
    );

    expect(
      screen.queryByRole("button", { name: REOPEN_KEY }),
    ).not.toBeInTheDocument();
  });

  it("should show reopen on a RESOLVED incident for a global-view admin regardless of reopener scope", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) =>
        p === "INCIDENT_REOPEN" || p === "INCIDENT_VIEW_ALL",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="RESOLVED"
        creatorId="other-creator"
        currentAssigneeId="other-assignee"
      />,
    );

    expect(
      screen.getByRole("button", { name: REOPEN_KEY }),
    ).toBeInTheDocument();
  });

  it("should show self-assign when current user manages the target service", async () => {
    vi.mocked(useAuth).mockReturnValue({
      hasPermission: (p: string) => p === "INCIDENT_TREAT",
      user: {
        id: USER_ID,
        isActive: true,
        permissions: ["INCIDENT_TREAT"],
        serviceId: "service-other",
        managedServiceIds: ["service-1"],
      },
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="IN_PROGRESS"
        currentServiceId="service-1"
        agencyId="agency-1"
      />,
    );

    openMoreActions();
    await waitFor(() =>
      expect(
        screen.getByText("incidents.workflow.self_assign"),
      ).toBeInTheDocument(),
    );
  });

  it("requests assignable users scoped to the incident's handling service", async () => {
    // Un assigne valide doit exister dans le service traitant, sinon le garde-fou
    // masque le bouton (pas d'action possible => pas de bouton).
    mockDirectoryData.users = [
      {
        id: "user-2",
        firstName: "Alex",
        lastName: "Doe",
        username: "adoe",
      },
    ];
    vi.mocked(useAuth).mockReturnValue({
      user: {
        id: USER_ID,
        isActive: true,
        serviceId: "service-1",
        managedServiceIds: ["service-1"],
      },
      hasPermission: (p: string) => p === "INCIDENT_ASSIGN",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="ASSIGNED"
        currentServiceId="service-1"
        agencyId="agency-1"
      />,
    );

    openMoreActions();
    fireEvent.click(await screen.findByText("incidents.workflow.assign"));

    expect(assignableUsersSpy).toHaveBeenCalledWith(
      { serviceId: "service-1" },
      { enabled: true },
    );
  });

  it("lists an agent whose treat permission comes only from a custom role", async () => {
    vi.mocked(useAuth).mockReturnValue({
      user: {
        id: USER_ID,
        isActive: true,
        serviceId: "service-1",
        managedServiceIds: ["service-1"],
      },
      hasPermission: (p: string) => p === "INCIDENT_ASSIGN",
    } as unknown as ReturnType<typeof useAuth>);


    mockDirectoryData.users = [
      {
        id: "agent-role-only",
        username: "agent_role",
        firstName: "Awa",
        lastName: "Ndiaye",
        isActive: true,
      },
    ];

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="ASSIGNED"
        currentServiceId="service-1"
        agencyId="agency-1"
      />,
    );

    openMoreActions();
    fireEvent.click(await screen.findByText("incidents.workflow.assign"));
    fireEvent.mouseDown(
      screen.getByText("incidents.workflow.modals.assign.user_placeholder"),
    );

    await waitFor(() =>
      expect(screen.getByText("Awa Ndiaye (agent_role)")).toBeInTheDocument(),
    );
  });

  it("should hide self-assign when current user is already the assignee", async () => {
    vi.mocked(useAuth).mockReturnValue({
      hasPermission: (p: string) => p === "INCIDENT_TREAT",
      user: {
        id: USER_ID,
        isActive: true,
        permissions: ["INCIDENT_TREAT"],
        serviceId: "service-1",
        managedServiceIds: [],
      },
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="ASSIGNED"
        currentAssigneeId={USER_ID}
        currentServiceId="service-1"
        agencyId="agency-1"
      />,
    );

    const moreActionsBtn = screen.queryByText("common.more_actions");
    if (moreActionsBtn) {
      fireEvent.mouseOver(moreActionsBtn);
    }
    expect(
      screen.queryByText("incidents.workflow.self_assign"),
    ).not.toBeInTheDocument();
  });

  it("should hide self-assign when current user is admin and is already the assignee", async () => {
    vi.mocked(useAuth).mockReturnValue({
      hasPermission: (p: string) => p === "INCIDENT_VIEW_ALL" || p === "INCIDENT_TREAT",
      user: {
        id: USER_ID,
        isActive: true,
        permissions: ["INCIDENT_VIEW_ALL", "INCIDENT_TREAT"],
        serviceId: "service-1",
        managedServiceIds: [],
      },
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="ASSIGNED"
        currentAssigneeId={USER_ID}
        currentServiceId="service-1"
        agencyId="agency-1"
      />,
    );

    const moreActionsBtn = screen.queryByText("common.more_actions");
    if (moreActionsBtn) {
      fireEvent.mouseOver(moreActionsBtn);
    }
    expect(
      screen.queryByText("incidents.workflow.self_assign"),
    ).not.toBeInTheDocument();
  });

  it("should hide self-assign for an admin who is not in the handling service", async () => {
    // Un admin (VIEW_ALL) hors du service désigné ne peut pas se prendre l'incident :
    // le backend le rejetterait, donc pas de bouton « Prendre en charge ».
    vi.mocked(useAuth).mockReturnValue({
      hasPermission: (p: string) =>
        p === "INCIDENT_VIEW_ALL" || p === "INCIDENT_TREAT",
      user: {
        id: USER_ID,
        isActive: true,
        permissions: ["INCIDENT_VIEW_ALL", "INCIDENT_TREAT"],
        serviceId: "service-other",
        managedServiceIds: [],
      },
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="ASSIGNED"
        currentServiceId="service-1"
        agencyId="agency-1"
      />,
    );

    const moreActionsBtn = screen.queryByText("common.more_actions");
    if (moreActionsBtn) {
      fireEvent.mouseOver(moreActionsBtn);
    }
    expect(
      screen.queryByText("incidents.workflow.self_assign"),
    ).not.toBeInTheDocument();
  });

  it("should hide assign when the handling service has no assignable user", async () => {
    // Aucun assigné valide dans le service traitant => pas de bouton « Assigner »
    // (dropdown vide = bouton mort), même pour un admin.
    mockDirectoryData.users = [];
    vi.mocked(useAuth).mockReturnValue({
      hasPermission: (p: string) =>
        p === "INCIDENT_VIEW_ALL" || p === "INCIDENT_ASSIGN",
      user: {
        id: USER_ID,
        isActive: true,
        permissions: ["INCIDENT_VIEW_ALL", "INCIDENT_ASSIGN"],
        serviceId: "service-other",
        managedServiceIds: [],
      },
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="ASSIGNED"
        currentServiceId="service-1"
        agencyId="agency-1"
      />,
    );

    const moreActionsBtn = screen.queryByText("common.more_actions");
    if (moreActionsBtn) {
      fireEvent.mouseOver(moreActionsBtn);
    }
    expect(
      screen.queryByText("incidents.workflow.assign"),
    ).not.toBeInTheDocument();
  });

  it("should require a reason before canceling an incident", async () => {
    vi.mocked(useAuth).mockReturnValue({
      user: {
        id: USER_ID,
        agencyId: "agency-1",
        managedAgencyId: "agency-1",
        roles: ["chef_agence"],
      },
      hasPermission: (p: string) =>
        p === "INCIDENT_CANCEL" || p === "INCIDENT_VIEW_AGENCY",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="VALIDATED"
        currentAssigneeId="assigned-agent"
        agencyId="agency-1"
        canCancel
      />,
    );

    openMoreActions();
    await waitFor(() =>
      expect(screen.getByText(CANCEL_KEY)).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByText(CANCEL_KEY));
    fireEvent.click(
      screen.getByText("incidents.workflow.modals.cancel.confirm"),
    );

    await waitFor(() =>
      expect(screen.getByText(CANCEL_REASON_REQUIRED)).toBeInTheDocument(),
    );

    expect(mockCancelMutate).not.toHaveBeenCalled();
  });

  it("should upload cancellation attachments before canceling an incident", async () => {
    vi.mocked(useAuth).mockReturnValue({
      user: {
        id: USER_ID,
        agencyId: AGENCY_ID,
        managedAgencyId: AGENCY_ID,
        roles: ["chef_agence"],
      },
      hasPermission: (p: string) =>
        p === "INCIDENT_CANCEL" || p === "INCIDENT_VIEW_AGENCY",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="VALIDATED"
        agencyId={AGENCY_ID}
        canCancel
      />,
    );

    openMoreActions();
    await waitFor(() =>
      expect(screen.getByText(CANCEL_KEY)).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByText(CANCEL_KEY));

    const file = new File(["proof"], "cancellation.pdf", {
      type: "application/pdf",
    });
    const uploadInputs = document.querySelectorAll<HTMLInputElement>(
      'input[type="file"]',
    );
    const uploadInput = uploadInputs.item(uploadInputs.length - 1);
    expect(uploadInput).not.toBeNull();
    fireEvent.change(uploadInput!, { target: { files: [new File([], "empty.pdf", { type: "application/pdf" }), file] } });
    await waitFor(() => expect(screen.getByText("incidents.attachments.empty_file")).toBeInTheDocument());
    expect(screen.queryByText("empty.pdf")).not.toBeInTheDocument();
    await waitFor(() =>
      expect(screen.getByText("cancellation.pdf")).toBeInTheDocument(),
    );
    fireEvent.change(screen.getByRole("textbox"), {
      target: { value: "Incident devenu sans objet" },
    });
    fireEvent.click(
      screen.getByText("incidents.workflow.modals.cancel.confirm"),
    );

    await waitFor(() =>
      expect(mockUploadAttachment).toHaveBeenCalledWith({
        file,
        category: "CANCELLATION",
      }),
    );
    expect(mockUploadAttachment).toHaveBeenCalledTimes(1);
    expect(mockCancelMutate).toHaveBeenCalledWith(
      {
        id: INCIDENT_ID,
        data: { reason: "Incident devenu sans objet" },
      },
      expect.any(Object),
    );
    // Une erreur metier laisse la modale ouverte : ses fichiers recus ne repartent pas.
    fireEvent.click(screen.getByText("incidents.workflow.modals.cancel.confirm"));
    await waitFor(() => expect(mockCancelMutate).toHaveBeenCalledTimes(2));
    expect(mockUploadAttachment).toHaveBeenCalledTimes(1);
  });

  it("retries only unacknowledged workflow files after a partial failure", async () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID, agencyId: AGENCY_ID, managedAgencyId: AGENCY_ID, roles: ["chef_agence"] },
      hasPermission: (p: string) => p === "INCIDENT_CANCEL" || p === "INCIDENT_VIEW_AGENCY",
    } as unknown as ReturnType<typeof useAuth>);
    mockUploadAttachment.mockResolvedValueOnce({ id: "first" }).mockRejectedValueOnce(new Error("network"))
      .mockResolvedValueOnce({ id: "second" });
    renderWithProviders(<WorkflowActions incidentId={INCIDENT_ID} status="VALIDATED" agencyId={AGENCY_ID} canCancel />);
    openMoreActions();
    await waitFor(() => expect(screen.getByText(CANCEL_KEY)).toBeInTheDocument());
    fireEvent.click(screen.getByText(CANCEL_KEY));
    const one = new File(["proof"], "first.pdf", { type: "application/pdf" });
    const two = new File(["proof"], "second.pdf", { type: "application/pdf" });
    const inputs = document.querySelectorAll<HTMLInputElement>('input[type="file"]');
    fireEvent.change(inputs.item(inputs.length - 1), { target: { files: [one, two] } });
    await waitFor(() => expect(screen.getByText("second.pdf")).toBeInTheDocument());
    fireEvent.change(screen.getByRole("textbox"), { target: { value: "Incident devenu sans objet" } });
    fireEvent.click(screen.getByText("incidents.workflow.modals.cancel.confirm"));
    await waitFor(() => expect(mockUploadAttachment).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(screen.queryByText("first.pdf")).not.toBeInTheDocument());
    expect(mockCancelMutate).not.toHaveBeenCalled();
    expect(screen.getByText("second.pdf")).toBeInTheDocument();
    fireEvent.click(screen.getByText("incidents.workflow.modals.cancel.confirm"));
    await waitFor(() => expect(mockCancelMutate).toHaveBeenCalledTimes(1));
    expect(mockUploadAttachment).toHaveBeenCalledTimes(3);
    expect(mockUploadAttachment).toHaveBeenLastCalledWith({ file: two, category: "CANCELLATION" });
  });

  // La portée (chef d'agence / chef de service) est arbitrée par le back et arrive
  // via canCancel : le composant ne fait plus que suivre ce verdict.
  it("should hide cancellation when the backend verdict denies it", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: {
        id: USER_ID,
        agencyId: AGENCY_ID,
        managedAgencyId: AGENCY_ID,
        roles: ["chef_agence"],
      },
      hasPermission: (p: string) =>
        p === "INCIDENT_CANCEL" || p === "INCIDENT_VIEW_AGENCY",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="VALIDATED"
        agencyId={AGENCY_ID}
        canCancel={false}
      />,
    );

    openMoreActions();
    expect(screen.queryByText(CANCEL_KEY)).not.toBeInTheDocument();
  });

  it.each(["OPEN", "PENDING_VALIDATION"])(
    "should hide cancellation on %s even when the verdict allows it",
    (status) => {
      vi.mocked(useAuth).mockReturnValue({
        user: { id: USER_ID, managedAgencyId: AGENCY_ID },
        hasPermission: (p: string) => p === "INCIDENT_CANCEL",
      } as unknown as ReturnType<typeof useAuth>);

      renderWithProviders(
        <WorkflowActions
          incidentId={INCIDENT_ID}
          status={status}
          agencyId={AGENCY_ID}
          canCancel
        />,
      );

      // Avant validation la sortie s'appelle rejet : les deux portes sont disjointes.
      openMoreActions();
      expect(screen.queryByText(CANCEL_KEY)).not.toBeInTheDocument();
    },
  );

  it("should expose cancellation to the target service head after assignment", async () => {
    vi.mocked(useAuth).mockReturnValue({
      user: {
        id: USER_ID,
        managedServiceIds: ["service-1"],
        roles: ["chef_service"],
      },
      hasPermission: (p: string) => p === "INCIDENT_CANCEL",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="ASSIGNED"
        currentServiceId="service-1"
        agencyId={AGENCY_ID}
        canCancel
      />,
    );

    openMoreActions();
    await waitFor(() =>
      expect(screen.getByText(CANCEL_KEY)).toBeInTheDocument(),
    );
  });

  it("should hide cancellation once treatment is in progress", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: {
        id: USER_ID,
        managedServiceIds: ["service-1"],
        roles: ["chef_service"],
      },
      hasPermission: (p: string) => p === "INCIDENT_CANCEL",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="IN_PROGRESS"
        currentServiceId="service-1"
        agencyId={AGENCY_ID}
        canCancel
      />,
    );

    openMoreActions();
    expect(screen.queryByText(CANCEL_KEY)).not.toBeInTheDocument();
  });

  it.each(["BLOCKED", "UNRESOLVED_PROLONGED_WAIT"])(
    "should expose cancellation to the target service head for %s",
    async (status) => {
      vi.mocked(useAuth).mockReturnValue({
        user: {
          id: USER_ID,
          managedServiceIds: ["service-1"],
          roles: ["chef_service"],
        },
        hasPermission: (p: string) => p === "INCIDENT_CANCEL",
      } as unknown as ReturnType<typeof useAuth>);

      renderWithProviders(
        <WorkflowActions
          incidentId={INCIDENT_ID}
          status={status}
          currentServiceId="service-1"
          agencyId={AGENCY_ID}
          canCancel
          proposedSolution="Proposition conservee"
        />,
      );

      openMoreActions();
      const expectedKey =
        status === "UNRESOLVED_PROLONGED_WAIT"
          ? "incidents.workflow.no_longer_relevant"
          : CANCEL_KEY;
      await waitFor(() =>
        expect(screen.getByText(expectedKey)).toBeInTheDocument(),
      );
    },
  );

  // ===== closerType : conditionne l affichage du bouton de cloture =====

  it("should show close button when closerType is CREATOR and user is the creator", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) => p === "INCIDENT_CLOSE",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="RESOLVED"
        creatorId={USER_ID}
        closerRoles={["CREATOR"]}
      />,
    );

    expect(
      screen.getByRole("button", { name: "incidents.workflow.close" }),
    ).toBeInTheDocument();
  });

  it("should hide close button when closerType is CREATOR and user is not the creator", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) => p === "INCIDENT_CLOSE",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="RESOLVED"
        creatorId="other-creator"
        closerRoles={["CREATOR"]}
      />,
    );

    expect(
      screen.queryByRole("button", { name: "incidents.workflow.close" }),
    ).not.toBeInTheDocument();
  });

  it("should show close button when closerType is ASSIGNEE and user is the assignee", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) => p === "INCIDENT_CLOSE",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="RESOLVED"
        currentAssigneeId={USER_ID}
        creatorId="other-creator"
        closerRoles={["ASSIGNEE"]}
      />,
    );

    expect(
      screen.getByRole("button", { name: "incidents.workflow.close" }),
    ).toBeInTheDocument();
  });

  it("should hide close button when closerType is ASSIGNEE and user is not the assignee", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) => p === "INCIDENT_CLOSE",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="RESOLVED"
        currentAssigneeId="other-user"
        creatorId="other-creator"
        closerRoles={["ASSIGNEE"]}
      />,
    );

    expect(
      screen.queryByRole("button", { name: "incidents.workflow.close" }),
    ).not.toBeInTheDocument();
  });

  it("should show close button when closerType is BOTH and user is the creator (not assignee)", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) => p === "INCIDENT_CLOSE",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="RESOLVED"
        creatorId={USER_ID}
        currentAssigneeId="other-user"
        closerRoles={["CREATOR", "ASSIGNEE"]}
      />,
    );

    expect(
      screen.getByRole("button", { name: "incidents.workflow.close" }),
    ).toBeInTheDocument();
  });

  it("should show close button when closerType is BOTH and user is the assignee (not creator)", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) => p === "INCIDENT_CLOSE",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="RESOLVED"
        creatorId="other-creator"
        currentAssigneeId={USER_ID}
        closerRoles={["CREATOR", "ASSIGNEE"]}
      />,
    );

    expect(
      screen.getByRole("button", { name: "incidents.workflow.close" }),
    ).toBeInTheDocument();
  });

  it("should show close button when user has global view (admin) regardless of closerType", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (p: string) =>
        p === "INCIDENT_CLOSE" || p === "INCIDENT_VIEW_ALL",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="RESOLVED"
        creatorId="other-creator"
        currentAssigneeId="other-user"
        closerRoles={["ASSIGNEE"]}
      />,
    );

    expect(
      screen.getByRole("button", { name: "incidents.workflow.close" }),
    ).toBeInTheDocument();
  });

  it("should hide the creator agency transfer option when creatorServiceId is set", async () => {
    mockDirectoryData.services = [{ id: "service-2", name: "Support" }];
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (permission: string) =>
        permission === "INCIDENT_TRANSFER" ||
        permission === "INCIDENT_VIEW_ALL",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="VALIDATED"
        agencyId="agency-1"
        agencyName="Agency Alpha"
        creatorServiceId="service-creator"
      />,
    );

    openMoreActions();
    await waitFor(() => expect(screen.getByText(TRANSFER_KEY)).toBeInTheDocument());
    fireEvent.click(screen.getByText(TRANSFER_KEY));

    expect(
      screen.queryByText("incidents.workflow.modals.transfer.agency_group"),
    ).not.toBeInTheDocument();
    expect(screen.queryByText("Agency Alpha")).not.toBeInTheDocument();
  });

  it("should show the target-service type field and send newTypeId on transfer", async () => {
    mockDirectoryData.services = [{ id: "service-2", name: "Support" }];
    mockDirectoryData.incidentTypes = [
      {
        id: "type-a",
        displayName: "Panne réseau",
        isActive: true,
        defaultTargetService: { id: "service-2" },
      },
      {
        id: "type-b",
        displayName: "Hors service",
        isActive: true,
        defaultTargetService: { id: "service-999" },
      },
    ];
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (permission: string) =>
        permission === "INCIDENT_TRANSFER" ||
        permission === "INCIDENT_VIEW_ALL",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="VALIDATED"
        currentServiceId="service-1"
        agencyId="agency-1"
      />,
    );

    openMoreActions();
    await waitFor(() =>
      expect(screen.getByText(TRANSFER_KEY)).toBeInTheDocument(),
    );
    fireEvent.click(screen.getByText(TRANSFER_KEY));

    // Choix du service destinataire : le champ de type du service cible apparait directement.
    fireEvent.mouseDown(screen.getByRole("combobox"));
    fireEvent.click(await screen.findByText("Support"));
    // Le champ de type apparait directement et propose le type du service destinataire.
    expect(
      await screen.findByText(
        "incidents.workflow.modals.transfer.new_type",
      ),
    ).toBeInTheDocument();

    fireEvent.click(
      screen.getByText("incidents.workflow.modals.transfer.confirm"),
    );

    await waitFor(() =>
      expect(mockTransferMutate).toHaveBeenCalledWith(
        {
          id: INCIDENT_ID,
          data: {
            targetServiceId: "service-2",
            newTypeId: "type-a",
          },
        },
        expect.anything(),
      ),
    );
  });

  it("should transfer to the creator agency when the creator has no service", async () => {
    mockDirectoryData.services = [{ id: "service-2", name: "Support" }];
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID },
      hasPermission: (permission: string) =>
        permission === "INCIDENT_TRANSFER" ||
        permission === "INCIDENT_VIEW_ALL",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="VALIDATED"
        agencyId="agency-1"
        agencyName="Agency Alpha"
      />,
    );

    openMoreActions();
    await waitFor(() => expect(screen.getByText(TRANSFER_KEY)).toBeInTheDocument());
    fireEvent.click(screen.getByText(TRANSFER_KEY));
    fireEvent.mouseDown(screen.getByRole("combobox"));
    fireEvent.click(await screen.findByText("Agency Alpha"));
    fireEvent.click(
      screen.getByText("incidents.workflow.modals.transfer.confirm"),
    );

    await waitFor(() =>
      expect(mockTransferMutate).toHaveBeenCalledWith(
        {
          id: INCIDENT_ID,
          data: {
            targetAgencyId: "agency-1",
            reason: undefined,
            comment: undefined,
          },
        },
        expect.objectContaining({ onSuccess: expect.any(Function) }),
      ),
    );

    const onSuccess = mockTransferMutate.mock.calls[0][1]?.onSuccess;
    expect(onSuccess).toBeDefined();
    onSuccess?.();
    expect(mockNavigateToIncidents).toHaveBeenCalledWith(
      expect.any(Function),
      { replace: true },
    );
  });

  describe("DRAFT status (prior Direction validation flow)", () => {
    it("should show only validation/rejection buttons for Direction when status is DRAFT and no rejection reason is set", async () => {
      vi.mocked(useAuth).mockReturnValue({
        user: { id: "user-direction" },
        hasPermission: (p: string) => p === "VALIDATION_DIRECTION",
      } as unknown as ReturnType<typeof useAuth>);

      renderWithProviders(
        <WorkflowActions
          incidentId={INCIDENT_ID}
          status="DRAFT"
          currentAssigneeId={USER_ID}
          requiresDirectionValidation={true}
          directionRejectionReason={null}
        />,
      );

      expect(
        screen.getByRole("button", { name: "incidents.workflow.direction_validate" }),
      ).toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: "incidents.workflow.direction_reject" }),
      ).toBeInTheDocument();

      expect(
        screen.queryByRole("button", { name: "incidents.workflow.start" }),
      ).not.toBeInTheDocument();
    });

    it("should hide all buttons for assignee when status is DRAFT and no rejection reason is set", () => {
      vi.mocked(useAuth).mockReturnValue({
        user: { id: USER_ID },
        hasPermission: (p: string) => p === "INCIDENT_TREAT" || p === "INCIDENT_RESOLVE",
      } as unknown as ReturnType<typeof useAuth>);

      renderWithProviders(
        <WorkflowActions
          incidentId={INCIDENT_ID}
          status="DRAFT"
          currentAssigneeId={USER_ID}
          requiresDirectionValidation={true}
          directionRejectionReason={null}
        />,
      );

      expect(screen.queryAllByRole("button")).toHaveLength(0);
    });

    it("should show resubmit button for assignee when status is DRAFT and rejection reason is set", () => {
      vi.mocked(useAuth).mockReturnValue({
        user: { id: USER_ID, agencyId: "agency-1" },
        hasPermission: (p: string) =>
          p === "INCIDENT_TREAT" ||
          p === "INCIDENT_RESOLVE" ||
          p === "INCIDENT_VIEW_AGENCY",
      } as unknown as ReturnType<typeof useAuth>);

      renderWithProviders(
        <WorkflowActions
          incidentId={INCIDENT_ID}
          status="DRAFT"
          currentAssigneeId={USER_ID}
          creatorId={USER_ID}
          requiresDirectionValidation={true}
          directionRejectionReason="Rejected due to cost"
          agencyId="agency-1"
        />,
      );

      expect(
        screen.getByRole("button", { name: "incidents.workflow.request_new_validation" }),
      ).toBeInTheDocument();
    });

    it("should hide validation/rejection buttons for Direction when status is DRAFT and rejection reason is set", () => {
      vi.mocked(useAuth).mockReturnValue({
        user: { id: "user-direction" },
        hasPermission: (p: string) => p === "VALIDATION_DIRECTION",
      } as unknown as ReturnType<typeof useAuth>);

      renderWithProviders(
        <WorkflowActions
          incidentId={INCIDENT_ID}
          status="DRAFT"
          currentAssigneeId={USER_ID}
          requiresDirectionValidation={true}
          directionRejectionReason="Rejected due to cost"
        />,
      );

      expect(screen.queryAllByRole("button")).toHaveLength(0);
    });

    it("should only display SOLUTION attachments in submit solution modal, filtering out comments and other categories", () => {
      vi.mocked(useAuth).mockReturnValue({
        user: { id: USER_ID, agencyId: "agency-1" },
        hasPermission: (p: string) =>
          p === "INCIDENT_TREAT" ||
          p === "INCIDENT_RESOLVE" ||
          p === "INCIDENT_VIEW_AGENCY",
      } as unknown as ReturnType<typeof useAuth>);

      vi.mocked(useIncidentAttachments).mockReturnValue({
        data: [
          {
            id: "att-comment",
            filename: "comment_attachment.pdf",
            commentId: "comment-1",
            category: null,
            fileSize: 1024,
          },
          {
            id: "att-declaration",
            filename: "declaration_attachment.pdf",
            commentId: null,
            category: "INCIDENT",
            fileSize: 2048,
          },
          {
            id: "att-solution",
            filename: "solution_attachment.pdf",
            commentId: null,
            category: "SOLUTION",
            fileSize: 4096,
          },
        ],
      } as unknown as ReturnType<typeof useIncidentAttachments>);

      renderWithProviders(
        <WorkflowActions
          incidentId={INCIDENT_ID}
          status="DRAFT"
          currentAssigneeId={USER_ID}
          creatorId={USER_ID}
          requiresDirectionValidation={true}
          directionRejectionReason="Rejected due to cost"
          agencyId="agency-1"
        />,
      );

      fireEvent.click(
        screen.getByRole("button", {
          name: "incidents.workflow.request_new_validation",
        }),
      );

      expect(screen.getByText("solution_attachment.pdf")).toBeInTheDocument();
      expect(
        screen.queryByText("comment_attachment.pdf"),
      ).not.toBeInTheDocument();
      expect(
        screen.queryByText("declaration_attachment.pdf"),
      ).not.toBeInTheDocument();
    });
  });
});

describe("WorkflowActions - relevance confirmation", () => {
  const renderProlongedWait = (props: Record<string, unknown>) => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: USER_ID, agencyId: AGENCY_ID, roles: ["agent"] },
      hasPermission: (p: string) =>
        p === "INCIDENT_TREAT" ||
        p === "INCIDENT_VALIDATE" ||
        p === "INCIDENT_CANCEL" ||
        p === "INCIDENT_VIEW_AGENCY",
    } as unknown as ReturnType<typeof useAuth>);

    renderWithProviders(
      <WorkflowActions
        incidentId={INCIDENT_ID}
        status="UNRESOLVED_PROLONGED_WAIT"
        currentAssigneeId={USER_ID}
        agencyId={AGENCY_ID}
        {...props}
      />,
    );
  };

  it("should hide both steps until the backend grants them", () => {
    // Sans verdict, la portee n'est pas connue du navigateur : aucun bouton.
    renderProlongedWait({});

    expect(
      screen.queryByText("incidents.workflow.request_confirmation"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText("incidents.workflow.confirm_relevance"),
    ).not.toBeInTheDocument();
  });

  it("should ask the source entity for a confirmation when granted", async () => {
    renderProlongedWait({ canRequestConfirmation: true });

    fireEvent.click(screen.getByText("incidents.workflow.request_confirmation"));
    fireEvent.click(
      screen.getByText(
        "incidents.workflow.modals.request_confirmation.confirm",
      ),
    );

    await waitFor(() =>
      expect(mockRequestConfirmationMutate).toHaveBeenCalledWith(
        expect.objectContaining({ id: INCIDENT_ID }),
        expect.anything(),
      ),
    );
  });

  it("should name who is awaited instead of saying the source entity", () => {
    // « L'entité source » ne dit a personne s'il est concerne : la modale nomme
    // le responsable, comme la fiche nomme deja le valideur attendu.
    renderProlongedWait({
      canRequestConfirmation: true,
      relevanceResponderRole: "SERVICE_MANAGER",
      relevanceResponderTarget: "Comptabilité",
    });

    fireEvent.click(screen.getByText("incidents.workflow.request_confirmation"));

    expect(
      screen.getByText("incidents.workflow.modals.request_confirmation.hint"),
    ).toBeInTheDocument();
    expect(translateSpy).toHaveBeenCalledWith(
      "incidents.workflow.modals.request_confirmation.hint",
      { responder: "incidents.validator_roles.SERVICE_MANAGER_named" },
    );
  });

  it("should call the cancellation \"no longer relevant\" in this context", async () => {
    renderProlongedWait({ canCancel: true });

    openMoreActions();

    await waitFor(() =>
      expect(
        screen.getByText("incidents.workflow.no_longer_relevant"),
      ).toBeInTheDocument(),
    );
    expect(
      screen.queryByText("incidents.workflow.cancel"),
    ).not.toBeInTheDocument();
  });

  it("should confirm the relevance when granted", async () => {
    renderProlongedWait({ canConfirmRelevance: true });

    fireEvent.click(screen.getByText("incidents.workflow.confirm_relevance"));
    fireEvent.click(
      screen.getByText("incidents.workflow.modals.confirm_relevance.confirm"),
    );

    await waitFor(() =>
      expect(mockConfirmRelevanceMutate).toHaveBeenCalledWith(
        expect.objectContaining({ id: INCIDENT_ID }),
        expect.anything(),
      ),
    );
  });
});
