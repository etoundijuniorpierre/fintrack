// Tests frontend : verifie le comportement de attachment api.test.

import { beforeEach, describe, expect, it, vi } from "vitest";
import apiClient from "../../client";
import { DOCUMENT_ENDPOINTS } from "../endpoints/endpoints";
import { attachmentApi } from "./attachmentApi";

vi.mock("../../client", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

const mockGet = vi.mocked(apiClient.get);
const mockPost = vi.mocked(apiClient.post);
const mockPut = vi.mocked(apiClient.put);
const mockDelete = vi.mocked(apiClient.delete);

const MOCK_ATTACHMENT = {
  id: "att-1",
  incidentId: "inc-1",
  incident: { id: "inc-1", title: "Panne réseau", status: "OPEN" },
  filename: "proof.pdf",
  storagePath: "/attachments/att-1.pdf",
  fileSize: 1024,
  mimeType: "application/pdf",
  uploadedById: "user-1",
  uploadedBy: { id: "user-1", username: "jdoe" },
  uploadedAt: "2026-05-13T10:30:00Z",
};

const MOCK_PAGE = {
  content: [MOCK_ATTACHMENT],
  totalElements: 1,
  totalPages: 1,
  number: 0,
  size: 10,
  first: true,
  last: true,
  empty: false,
};

describe("attachmentApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("gets paged attachment metadata", async () => {
    mockGet.mockResolvedValueOnce({ data: MOCK_PAGE });
    const params = { page: 0, size: 10 };

    const result = await attachmentApi.getAll(params);

    expect(mockGet).toHaveBeenCalledWith(DOCUMENT_ENDPOINTS.ATTACHMENTS.BASE, {
      params,
      signal: undefined,
    });
    expect(result).toEqual(MOCK_PAGE);
  });

  it("gets attachments by incident id", async () => {
    mockGet.mockResolvedValueOnce({ data: [MOCK_ATTACHMENT] });

    const result = await attachmentApi.getByIncidentId("inc-1");

    expect(mockGet).toHaveBeenCalledWith(
      DOCUMENT_ENDPOINTS.ATTACHMENTS.BY_INCIDENT("inc-1"),
      { signal: undefined },
    );
    expect(result[0].incident?.title).toBe("Panne réseau");
  });

  it("gets attachments by uploader id", async () => {
    mockGet.mockResolvedValueOnce({ data: [MOCK_ATTACHMENT] });

    await attachmentApi.getByUploaderId("user-1");

    expect(mockGet).toHaveBeenCalledWith(
      DOCUMENT_ENDPOINTS.ATTACHMENTS.BY_UPLOADER("user-1"),
      { signal: undefined },
    );
  });

  it("creates attachment metadata with incident and uploader ids", async () => {
    mockPost.mockResolvedValueOnce({ data: MOCK_ATTACHMENT });
    const payload = {
      incidentId: "inc-1",
      filename: "proof.pdf",
      storagePath: "/attachments/att-1.pdf",
      fileSize: 1024,
      mimeType: "application/pdf",
      uploadedBy: "user-1",
      uploadedAt: "2026-05-13T10:30:00Z",
    };

    const result = await attachmentApi.create(payload);

    expect(mockPost).toHaveBeenCalledWith(
      DOCUMENT_ENDPOINTS.ATTACHMENTS.BASE,
      payload,
    );
    expect(result.uploadedById).toBe("user-1");
  });

  it("updates attachment metadata", async () => {
    mockPut.mockResolvedValueOnce({ data: MOCK_ATTACHMENT });
    const payload = {
      incidentId: "inc-1",
      filename: "proof.pdf",
      storagePath: "/attachments/att-1.pdf",
      fileSize: 1024,
      uploadedBy: "user-1",
      uploadedAt: "2026-05-13T10:30:00Z",
    };

    await attachmentApi.update("att-1", payload);

    expect(mockPut).toHaveBeenCalledWith(
      DOCUMENT_ENDPOINTS.ATTACHMENTS.BY_ID("att-1"),
      payload,
    );
  });

  it("deletes attachment metadata by id and by incident id", async () => {
    mockDelete.mockResolvedValue({ data: undefined });

    await attachmentApi.delete("att-1");
    await attachmentApi.deleteByIncidentId("inc-1");

    expect(mockDelete).toHaveBeenCalledWith(
      DOCUMENT_ENDPOINTS.ATTACHMENTS.BY_ID("att-1"),
    );
    expect(mockDelete).toHaveBeenCalledWith(
      DOCUMENT_ENDPOINTS.ATTACHMENTS.BY_INCIDENT("inc-1"),
    );
  });
});
