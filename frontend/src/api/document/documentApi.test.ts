// Tests frontend : verifie le comportement de document api.test.

import { beforeEach, describe, expect, it, vi } from "vitest";
import apiClient from "../client";
import { DOCUMENT_ENDPOINTS } from "./endpoints/endpoints";
import { documentApi } from "./documentApi";

vi.mock("../client", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn(),
  },
}));

const mockGet = vi.mocked(apiClient.get);
const mockPost = vi.mocked(apiClient.post);
const mockDelete = vi.mocked(apiClient.delete);

const attachment = {
  id: "att-1",
  filename: "avatar.png",
  fileSize: 128,
  mimeType: "image/png",
};

describe("documentApi", () => {
  it("keeps upload IDs across lost-response retries but isolates target comments", async () => {
    const file = new File(["%PDF-1.7"], "a.pdf", { type: "application/pdf" });
    mockPost.mockRejectedValueOnce(new Error("network"));
    await expect(documentApi.upload(file, "incident", { commentId: "one" })).rejects.toThrow("network");
    mockPost.mockResolvedValue({ data: attachment });
    await documentApi.upload(file, "incident", { commentId: "one" });
    await documentApi.upload(file, "incident", { commentId: "two" });
    const ids = mockPost.mock.calls.map((call) => (call[1] as FormData).get("uploadId"));
    expect(ids[0]).toEqual(expect.any(String));
    expect(ids[1]).toBe(ids[0]);
    expect(ids[2]).not.toBe(ids[0]);
  });
  it.each([undefined, "COMMENT", "SOLUTION", "TREATMENT", "RESOLUTION", "UNRESOLVED", "CLOSURE", "CANCELLATION"])(
    "rejects an empty attachment before posting (%s)", async (category) => {
      const file = new File([], "empty.pdf", { type: "application/pdf" });
      await expect(documentApi.upload(file, "incident-1", {
        category,
        ...(category === "COMMENT" ? { commentId: "comment-1" } : {}),
      })).rejects.toThrow();
      expect(mockPost).not.toHaveBeenCalled();
    },
  );

  it("rejects an empty avatar before posting", async () => {
    await expect(documentApi.uploadAvatar(new File([], "avatar.png", { type: "image/png" }))).rejects.toThrow();
    expect(mockPost).not.toHaveBeenCalled();
  });
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("uploads an incident attachment through the upload endpoint with encoded incident id", async () => {
    mockPost.mockResolvedValueOnce({ data: attachment });
    const file = new File(["data"], "proof.png", { type: "image/png" });

    const result = await documentApi.upload(file, "incident/1");

    expect(mockPost).toHaveBeenCalledWith(
      `${DOCUMENT_ENDPOINTS.ATTACHMENTS.UPLOAD}?incidentId=incident%2F1`,
      expect.any(FormData),
    );
    expect(result).toEqual(attachment);
  });

  it("uploads a user avatar through the avatar endpoint", async () => {
    mockPost.mockResolvedValueOnce({ data: attachment });
    const file = new File(["data"], "avatar.png", { type: "image/png" });

    await documentApi.uploadAvatar(file);

    expect(mockPost).toHaveBeenCalledWith(
      DOCUMENT_ENDPOINTS.ATTACHMENTS.UPLOAD_AVATAR,
      expect.any(FormData),
    );
  });

  it("downloads an attachment as a blob and exposes the same download URL helper", async () => {
    const blob = new Blob(["avatar"], { type: "image/png" });
    mockGet.mockResolvedValueOnce({ data: blob });

    await expect(documentApi.download("att-1")).resolves.toBe(blob);

    expect(mockGet).toHaveBeenCalledWith(
      DOCUMENT_ENDPOINTS.ATTACHMENTS.DOWNLOAD("att-1"),
      { responseType: "blob" },
    );
    expect(documentApi.getDownloadUrl("att-1")).toBe(
      DOCUMENT_ENDPOINTS.ATTACHMENTS.DOWNLOAD("att-1"),
    );
  });

  it("deletes an attachment by id", async () => {
    mockDelete.mockResolvedValueOnce({ data: undefined });

    await documentApi.delete("att-1");

    expect(mockDelete).toHaveBeenCalledWith(
      DOCUMENT_ENDPOINTS.ATTACHMENTS.BY_ID("att-1"),
    );
  });
});
