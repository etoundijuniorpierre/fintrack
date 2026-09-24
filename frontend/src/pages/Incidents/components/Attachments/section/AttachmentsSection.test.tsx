// Tests : section pieces jointes (etat vide, liste, permissions d'ajout selon createur/statut).

import { fireEvent, screen, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import AttachmentsSection from "./AttachmentsSection";
import {
  useIncidentAttachments,
  useUploadAttachment,
  useDeleteAttachment,
} from "../../../../../hooks/document/useDocuments";
import { useAuth } from "../../../../../hooks/auth/useAuth";
import { renderWithProviders } from "../../../../../test-utils/renderWithProviders";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("../../../../../hooks/document/useDocuments", () => ({
  useIncidentAttachments: vi.fn(),
  useUploadAttachment: vi.fn(),
  useDeleteAttachment: vi.fn(),
}));

vi.mock("../../../../../hooks/auth/useAuth", () => ({ useAuth: vi.fn() }));

const ATTACHMENT = {
  id: "att-1",
  filename: "rib.pdf",
  fileSize: 2048,
  uploadedAt: "2024-05-10T09:00:00Z",
  uploadedBy: { firstName: "John", lastName: "Doe" },
};

const setup = ({
  attachments = [] as unknown[],
  userId = "u1",
  creatorId = "u1" as string | null,
  incidentStatus = "OPEN" as string | undefined,
  onAttachmentsChange,
  deleteMutate = vi.fn(),
}: {
  attachments?: unknown[];
  userId?: string;
  creatorId?: string | null;
  incidentStatus?: string;
  onAttachmentsChange?: () => void;
  deleteMutate?: ReturnType<typeof vi.fn>;
} = {}) => {
  vi.mocked(useIncidentAttachments).mockReturnValue({
    data: attachments,
    isLoading: false,
  } as never);
  vi.mocked(useUploadAttachment).mockReturnValue({
    mutate: vi.fn(),
    isPending: false,
  } as never);
  vi.mocked(useDeleteAttachment).mockReturnValue({
    mutate: deleteMutate,
    isPending: false,
  } as never);
  vi.mocked(useAuth).mockReturnValue({
    user: { id: userId },
    isAuthenticated: true,
    hasRole: vi.fn(),
    hasPermission: vi.fn(),
  } as never);
  renderWithProviders(
    <AttachmentsSection
      incidentId="inc-1"
      incidentStatus={incidentStatus}
      creatorId={creatorId}
      onAttachmentsChange={onAttachmentsChange}
    />,
  );
};

describe("AttachmentsSection", () => {
  beforeEach(() => vi.clearAllMocks());

  it("rejects an empty file without uploading it", async () => {
    setup();
    fireEvent.change(document.querySelector('input[type="file"]')!, {
      target: { files: [new File([], "empty.pdf", { type: "application/pdf" })] },
    });
    await waitFor(() => expect(screen.getByText("incidents.attachments.empty_file")).toBeInTheDocument());
    expect(vi.mocked(useUploadAttachment).mock.results[0].value.mutate).not.toHaveBeenCalled();
  });

  it.each([
    "SOLUTION",
    "TREATMENT",
    "RESOLUTION",
    "UNRESOLVED",
    "CLOSURE",
    "CANCELLATION",
  ])(
    "should not display %s attachments among declaration attachments",
    (category) => {
      setup({
        attachments: [{ ...ATTACHMENT, id: "wf-1", filename: "workflow.pdf", category }],
      });
      expect(screen.queryByText("workflow.pdf")).not.toBeInTheDocument();
      expect(
        screen.getByText("incidents.attachments.no_attachments"),
      ).toBeInTheDocument();
    },
  );

  it.each([undefined, "INCIDENT", "INCIDENT_ATTACHMENT"])(
    "should display declaration attachments with category %s",
    (category) => {
      setup({
        attachments: [{ ...ATTACHMENT, category }],
      });
      expect(screen.getByText("rib.pdf")).toBeInTheDocument();
    },
  );

  it("should render the empty state when there are no attachments", () => {
    setup({ attachments: [] });
    expect(
      screen.getByText("incidents.attachments.no_attachments"),
    ).toBeInTheDocument();
  });

  it("should render one item per attachment with its filename", () => {
    setup({ attachments: [ATTACHMENT] });
    expect(screen.getByText("rib.pdf")).toBeInTheDocument();
  });

  it("should show the add button when the user is the creator and the status is editable", () => {
    setup({ userId: "u1", creatorId: "u1", incidentStatus: "OPEN" });
    expect(
      screen.getByRole("button", { name: "incidents.attachments.add_button" }),
    ).toBeInTheDocument();
  });

  it("should show the role-locked note and hide the add button for a non-creator", () => {
    setup({ userId: "other", creatorId: "u1", incidentStatus: "OPEN" });
    expect(
      screen.getByText("incidents.attachments.locked_role"),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", {
        name: "incidents.attachments.add_button",
      }),
    ).not.toBeInTheDocument();
  });

  it("should show the status-locked note when creator but the incident is already closed", () => {
    setup({ userId: "u1", creatorId: "u1", incidentStatus: "RESOLVED" });
    expect(
      screen.getByText("incidents.attachments.locked_status"),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", {
        name: "incidents.attachments.add_button",
      }),
    ).not.toBeInTheDocument();
  });

  it("should report a removal as a change of the incident", () => {
    const onAttachmentsChange = vi.fn();
    // La suppression est deja enregistree cote serveur ; le formulaire doit quand
    // meme l'apprendre, sinon son bouton de validation reste grise.
    const deleteMutate = vi.fn(
      (_id: string, options?: { onSuccess?: () => void }) =>
        options?.onSuccess?.(),
    );
    setup({
      attachments: [ATTACHMENT],
      userId: "u1",
      creatorId: "u1",
      incidentStatus: "OPEN",
      onAttachmentsChange,
      deleteMutate,
    });

    fireEvent.click(
      screen.getByRole("button", { name: "incidents.attachments.delete" }),
    );
    fireEvent.click(screen.getByRole("button", { name: "OK" }));

    expect(deleteMutate).toHaveBeenCalledWith(
      "att-1",
      expect.objectContaining({ onSuccess: onAttachmentsChange }),
    );
    expect(onAttachmentsChange).toHaveBeenCalledTimes(1);
  });

  it("should expose the delete action to the creator", () => {
    setup({
      attachments: [ATTACHMENT],
      userId: "u1",
      creatorId: "u1",
      incidentStatus: "OPEN",
    });
    expect(
      screen.getByRole("button", { name: "incidents.attachments.delete" }),
    ).toBeInTheDocument();
  });
});
