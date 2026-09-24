// Tests frontend : verifie le comportement de comments section.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, fireEvent, waitFor, act } from "@testing-library/react";
import { renderWithProviders } from "../../../../../test-utils/renderWithProviders";
import CommentsSection from "./CommentsSection";
import { makeIncidentComment } from "../../../../../mocks/incident/incidentFixtures";
import {
  useIncidentComments,
  useAddComment,
  useUpdateComment,
  useDeleteComment,
} from "../../../../../hooks/incident/useIncidents/useIncidents";
import {
  useUploadCommentAttachment,
  useIncidentAttachments,
} from "../../../../../hooks/document/useDocuments";
import { useAuth } from "../../../../../hooks/auth/useAuth";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("../../../../../hooks/auth/useAuth", () => ({
  useAuth: vi.fn(),
}));

vi.mock("../../../../../hooks/incident/useIncidents/useIncidents", () => ({
  useIncidentComments: vi.fn(),
  useAddComment: vi.fn(),
  useUpdateComment: vi.fn(),
  useDeleteComment: vi.fn(),
}));

vi.mock("../../../../../hooks/document/useDocuments", () => ({
  useUploadCommentAttachment: vi.fn(),
  useIncidentAttachments: vi.fn(),
}));

vi.mock("../../../../../utils/formatters/formatters", () => ({
  formatDateTime: () => "2024-01-01 09:30",
  sentenceCaseFormItemProps: {},
}));

const INCIDENT_ID = "incident-1";

const TRANSLATION_KEYS = {
  SUBMIT: "incidents.comments.form.submit",
  PLACEHOLDER: "incidents.comments.form.placeholder",
} as const;

const TEST_AUTHOR = {
  id: "user-1",
  username: "jdoe",
  firstName: "John",
  lastName: "Doe",
  email: "j@d.com",
};
// Definit les donnees de test test comment content.
const TEST_COMMENT_CONTENT = "This is a test comment.";
// Definit les donnees de test test valid comment.
const TEST_VALID_COMMENT = "A valid comment";

const mockMutate = vi.fn();
const mockUpdateMutate = vi.fn();
const mockDeleteMutate = vi.fn();
const mockDeleteEmpty = vi.fn();
const mockUpload = vi.fn();

describe("CommentsSection", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUpload.mockReset().mockResolvedValue({ id: "attachment-1" });
    mockDeleteEmpty.mockReset().mockResolvedValue(undefined);
    vi.mocked(useAuth).mockReturnValue({
      user: { id: TEST_AUTHOR.id },
    } as unknown as ReturnType<typeof useAuth>);
    vi.mocked(useAddComment).mockReturnValue({
      mutate: mockMutate,
      isPending: false,
    } as unknown as ReturnType<typeof useAddComment>);
    vi.mocked(useUpdateComment).mockReturnValue({
      mutate: mockUpdateMutate,
    } as unknown as ReturnType<typeof useUpdateComment>);
    vi.mocked(useDeleteComment).mockReturnValue({
      mutate: mockDeleteMutate,
      mutateAsync: mockDeleteEmpty,
    } as unknown as ReturnType<typeof useDeleteComment>);
    vi.mocked(useUploadCommentAttachment).mockReturnValue({
      mutateAsync: mockUpload,
      isPending: false,
    } as unknown as ReturnType<typeof useUploadCommentAttachment>);
    vi.mocked(useIncidentAttachments).mockReturnValue({
      data: [],
      isLoading: false,
    } as unknown as ReturnType<typeof useIncidentAttachments>);
    vi.mocked(useIncidentComments).mockReturnValue({
      data: [],
      isLoading: false,
    } as unknown as ReturnType<typeof useIncidentComments>);
  });

  it("should render author full name and comment content when comments are loaded", () => {
    vi.mocked(useIncidentComments).mockReturnValue({
      data: [
        makeIncidentComment({
          author: TEST_AUTHOR,
          content: TEST_COMMENT_CONTENT,
          isInternal: false,
        }),
      ],
      isLoading: false,
    } as ReturnType<typeof useIncidentComments>);

    renderWithProviders(<CommentsSection incidentId={INCIDENT_ID} />);

    expect(screen.getByText("John Doe")).toBeInTheDocument();
    expect(screen.getByText(TEST_COMMENT_CONTENT)).toBeInTheDocument();
  });

  const selectFiles = async (files: File[]) => {
    fireEvent.change(document.querySelector('input[type="file"]')!, { target: { files } });
    await waitFor(() => expect(screen.getByText(files[0].name)).toBeInTheDocument());
  };

  const completeComment = async () => {
    await act(async () => {
      await mockMutate.mock.calls[0][1].onSuccess({ id: "created-comment" });
    });
  };

  it("rejects empty selected and pasted files without creating a comment", async () => {
    renderWithProviders(<CommentsSection incidentId={INCIDENT_ID} />);
    const empty = new File([], "empty.png", { type: "image/png" });
    await act(async () => {
      fireEvent.change(document.querySelector('input[type="file"]')!, { target: { files: [empty] } });
      fireEvent.paste(screen.getByPlaceholderText(TRANSLATION_KEYS.PLACEHOLDER), {
        clipboardData: { items: [{ kind: "file", type: "image/png", getAsFile: () => empty }] },
      });
    });
    expect(screen.queryByText("empty.png")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: TRANSLATION_KEYS.SUBMIT })).toBeDisabled();
    expect(mockMutate).not.toHaveBeenCalled();
    expect(mockUpload).not.toHaveBeenCalled();
  });

  it("removes an attachment-only comment when every upload fails and retains the files", async () => {
    mockUpload.mockRejectedValue(new Error("Upload failed"));
    renderWithProviders(<CommentsSection incidentId={INCIDENT_ID} />);
    await selectFiles([new File(["x"], "proof.pdf", { type: "application/pdf" })]);
    fireEvent.click(screen.getByRole("button", { name: TRANSLATION_KEYS.SUBMIT }));
    await completeComment();
    expect(mockDeleteEmpty).toHaveBeenCalledWith({ id: INCIDENT_ID, commentId: "created-comment", onlyIfEmpty: true });
    expect(screen.getByText("proof.pdf")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: TRANSLATION_KEYS.SUBMIT })).toBeEnabled();
  });

  it("continues after a failed upload and retries only failed files on the same comment", async () => {
    mockUpload.mockRejectedValueOnce(new Error("Upload failed"));
    renderWithProviders(<CommentsSection incidentId={INCIDENT_ID} />);
    const files = [new File(["x"], "failed.pdf"), new File(["y"], "sent.pdf")];
    await selectFiles(files);
    fireEvent.click(screen.getByRole("button", { name: TRANSLATION_KEYS.SUBMIT }));
    await completeComment();
    expect(mockUpload).toHaveBeenCalledTimes(2);
    expect(mockDeleteEmpty).not.toHaveBeenCalled();
    expect(screen.queryByText("sent.pdf")).not.toBeInTheDocument();
    expect(screen.getByText("failed.pdf")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: TRANSLATION_KEYS.SUBMIT }));
    await waitFor(() => expect(screen.queryByText("failed.pdf")).not.toBeInTheDocument());
    expect(mockMutate).toHaveBeenCalledTimes(1);
    expect(mockUpload).toHaveBeenNthCalledWith(3, { file: files[0], commentId: "created-comment" });
  });

  it("keeps a text comment when its attachments fail", async () => {
    mockUpload.mockRejectedValue(new Error("Upload failed"));
    renderWithProviders(<CommentsSection incidentId={INCIDENT_ID} />);
    fireEvent.change(screen.getByPlaceholderText(TRANSLATION_KEYS.PLACEHOLDER), { target: { value: "Carnet du client" } });
    await selectFiles([new File(["x"], "proof.pdf")]);
    fireEvent.click(screen.getByRole("button", { name: TRANSLATION_KEYS.SUBMIT }));
    await completeComment();
    expect(mockDeleteEmpty).not.toHaveBeenCalled();
    expect(screen.getByText("proof.pdf")).toBeInTheDocument();
  });

  it("reuses the comment if cleanup fails and prevents duplicate submissions", async () => {
    mockUpload.mockRejectedValueOnce(new Error("Upload failed"));
    mockDeleteEmpty.mockRejectedValueOnce(new Error("Delete failed"));
    renderWithProviders(<CommentsSection incidentId={INCIDENT_ID} />);
    await selectFiles([new File(["x"], "proof.pdf")]);
    const button = screen.getByRole("button", { name: TRANSLATION_KEYS.SUBMIT });
    fireEvent.click(button);
    fireEvent.click(button);
    expect(mockMutate).toHaveBeenCalledTimes(1);
    await completeComment();
    fireEvent.click(button);
    await waitFor(() => expect(screen.queryByText("proof.pdf")).not.toBeInTheDocument());
    expect(mockMutate).toHaveBeenCalledTimes(1);
    expect(mockUpload).toHaveBeenLastCalledWith(expect.objectContaining({ commentId: "created-comment" }));
  });

  it("shows an explicit absence of files for an old orphan placeholder", () => {
    vi.mocked(useIncidentComments).mockReturnValue({
      data: [makeIncidentComment({ content: "📎 Pièce(s) jointe(s)" })],
    } as ReturnType<typeof useIncidentComments>);
    renderWithProviders(<CommentsSection incidentId={INCIDENT_ID} />);
    expect(screen.getByText("incidents.attachments.missing_files")).toBeInTheDocument();
    expect(screen.queryByText("📎 Pièce(s) jointe(s)")).not.toBeInTheDocument();
  });

  it("should disable send and not call addComment when the textarea is empty", () => {
    renderWithProviders(<CommentsSection incidentId={INCIDENT_ID} />);

    const sendButton = screen.getByRole("button", {
      name: TRANSLATION_KEYS.SUBMIT,
    });
    expect(sendButton).toBeDisabled();

    fireEvent.click(sendButton);
    expect(mockMutate).not.toHaveBeenCalled();
  });

  it("should not call addComment when the textarea contains only whitespace", () => {
    renderWithProviders(<CommentsSection incidentId={INCIDENT_ID} />);

    fireEvent.change(
      screen.getByPlaceholderText(TRANSLATION_KEYS.PLACEHOLDER),
      { target: { value: "   " } },
    );

    const sendButton = screen.getByRole("button", {
      name: TRANSLATION_KEYS.SUBMIT,
    });
    expect(sendButton).toBeDisabled();

    fireEvent.click(sendButton);
    expect(mockMutate).not.toHaveBeenCalled();
  });

  it("should call addComment mutate with correct payload when valid content is submitted", async () => {
    renderWithProviders(<CommentsSection incidentId={INCIDENT_ID} />);

    fireEvent.change(
      screen.getByPlaceholderText(TRANSLATION_KEYS.PLACEHOLDER),
      { target: { value: TEST_VALID_COMMENT } },
    );

    fireEvent.click(
      screen.getByRole("button", { name: TRANSLATION_KEYS.SUBMIT }),
    );

    await waitFor(() => expect(mockMutate).toHaveBeenCalledOnce());

    expect(mockMutate).toHaveBeenCalledWith(
      {
        id: INCIDENT_ID,
        data: expect.objectContaining({ content: TEST_VALID_COMMENT }),
      },
      expect.any(Object),
    );
  });

  it("should clear textarea after successful comment submission", async () => {
    renderWithProviders(<CommentsSection incidentId={INCIDENT_ID} />);

    fireEvent.change(
      screen.getByPlaceholderText(TRANSLATION_KEYS.PLACEHOLDER),
      { target: { value: TEST_VALID_COMMENT } },
    );

    fireEvent.click(
      screen.getByRole("button", { name: TRANSLATION_KEYS.SUBMIT }),
    );

    await waitFor(() => expect(mockMutate).toHaveBeenCalled());

    const onSuccess = mockMutate.mock.calls[0][1]?.onSuccess;
    expect(onSuccess).toBeDefined();
    onSuccess();

    await waitFor(() =>
      expect(
        (
          screen.getByPlaceholderText(
            TRANSLATION_KEYS.PLACEHOLDER,
          ) as HTMLTextAreaElement
        ).value,
      ).toBe(""),
    );
  });

  it("should render loading state when comments are being fetched", () => {
    vi.mocked(useIncidentComments).mockReturnValue({
      data: undefined,
      isLoading: true,
    } as unknown as ReturnType<typeof useIncidentComments>);

    renderWithProviders(<CommentsSection incidentId={INCIDENT_ID} />);

    expect(document.querySelector(".ant-spin")).toBeInTheDocument();
  });

  it("should render empty state when there are no comments", () => {
    vi.mocked(useIncidentComments).mockReturnValue({
      data: [],
      isLoading: false,
    } as unknown as ReturnType<typeof useIncidentComments>);

    renderWithProviders(<CommentsSection incidentId={INCIDENT_ID} />);

    expect(
      screen.getByText("incidents.comments.no_comments"),
    ).toBeInTheDocument();
  });

  // Prepare un unique commentaire de l'auteur courant, editable par defaut.
  const mockOwnComment = (overrides = {}) => {
    vi.mocked(useIncidentComments).mockReturnValue({
      data: [
        makeIncidentComment({
          id: "comment-1",
          author: TEST_AUTHOR,
          content: "My comment",
          createdAt: "2024-01-01T10:00:00Z",
          ...overrides,
        }),
      ],
      isLoading: false,
    } as ReturnType<typeof useIncidentComments>);
  };

  it("should show edit and delete actions on the author's editable comment", () => {
    mockOwnComment();
    renderWithProviders(<CommentsSection incidentId={INCIDENT_ID} />);

    expect(screen.getByLabelText("common.edit")).toBeInTheDocument();
    expect(screen.getByLabelText("common.delete")).toBeInTheDocument();
  });

  it("should hide edit and delete actions for a comment authored by someone else", () => {
    vi.mocked(useIncidentComments).mockReturnValue({
      data: [
        makeIncidentComment({
          id: "comment-1",
          author: { ...TEST_AUTHOR, id: "user-2" },
          content: "Other comment",
          createdAt: "2024-01-01T10:00:00Z",
        }),
      ],
      isLoading: false,
    } as ReturnType<typeof useIncidentComments>);

    renderWithProviders(<CommentsSection incidentId={INCIDENT_ID} />);

    expect(screen.queryByLabelText("common.edit")).not.toBeInTheDocument();
    expect(screen.queryByLabelText("common.delete")).not.toBeInTheDocument();
  });

  it("should hide edit and delete actions when the comment has a reply", () => {
    vi.mocked(useIncidentComments).mockReturnValue({
      data: [
        makeIncidentComment({
          id: "comment-1",
          author: TEST_AUTHOR,
          content: "Parent",
          createdAt: "2024-01-01T10:00:00Z",
        }),
        makeIncidentComment({
          id: "reply-1",
          author: TEST_AUTHOR,
          parentCommentId: "comment-1",
          content: "Reply",
          createdAt: "2024-01-01T10:30:00Z",
        }),
      ],
      isLoading: false,
    } as ReturnType<typeof useIncidentComments>);

    renderWithProviders(<CommentsSection incidentId={INCIDENT_ID} />);

    // Le parent porte une reponse : seule la reponse (sans enfant) reste editable.
    expect(screen.getAllByLabelText("common.edit")).toHaveLength(1);
  });

  it("should hide edit and delete actions when another user commented after", () => {
    vi.mocked(useIncidentComments).mockReturnValue({
      data: [
        makeIncidentComment({
          id: "comment-1",
          author: TEST_AUTHOR,
          content: "First comment",
          createdAt: "2024-01-01T10:00:00Z",
        }),
        makeIncidentComment({
          id: "comment-2",
          author: { ...TEST_AUTHOR, id: "user-2", username: "other" },
          content: "Second comment",
          createdAt: "2024-01-01T11:00:00Z",
        }),
      ],
      isLoading: false,
    } as ReturnType<typeof useIncidentComments>);

    renderWithProviders(<CommentsSection incidentId={INCIDENT_ID} />);

    expect(screen.queryByLabelText("common.edit")).not.toBeInTheDocument();
  });

  it("should hide edit and delete actions once the incident is closed", () => {
    mockOwnComment();
    renderWithProviders(
      <CommentsSection incidentId={INCIDENT_ID} incidentStatus="CLOSED" />,
    );

    expect(screen.queryByLabelText("common.edit")).not.toBeInTheDocument();
    expect(screen.queryByLabelText("common.delete")).not.toBeInTheDocument();
  });

  it("should call updateComment mutate when an edit is saved", () => {
    mockOwnComment();
    renderWithProviders(<CommentsSection incidentId={INCIDENT_ID} />);

    fireEvent.click(screen.getByLabelText("common.edit"));
    const textarea = screen.getByDisplayValue("My comment");
    fireEvent.change(textarea, { target: { value: "Edited content" } });
    fireEvent.click(screen.getByText("common.save"));

    expect(mockUpdateMutate).toHaveBeenCalledWith(
      expect.objectContaining({
        id: INCIDENT_ID,
        commentId: "comment-1",
        data: expect.objectContaining({ content: "Edited content" }),
      }),
    );
  });

  it("should call deleteComment mutate after confirming deletion", async () => {
    mockOwnComment();
    renderWithProviders(<CommentsSection incidentId={INCIDENT_ID} />);

    fireEvent.click(screen.getByLabelText("common.delete"));
    fireEvent.click(await screen.findByText("common.yes"));

    expect(mockDeleteMutate).toHaveBeenCalledWith({
      id: INCIDENT_ID,
      commentId: "comment-1",
    });
  });
});
