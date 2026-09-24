// Tests frontend : verifie le comportement de notification api.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { notificationApi } from "./notificationApi";
import apiClient from "../../client";
import { NOTIFICATION_ENDPOINTS } from "../endpoints/endpoints";

vi.mock("../../client", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

const mockGet = vi.mocked(apiClient.get);
const mockPost = vi.mocked(apiClient.post);
const mockPatch = vi.mocked(apiClient.patch);
const mockDelete = vi.mocked(apiClient.delete);

const MOCK_NOTIFICATION = {
  id: "notif-1",
  type: "EMAIL",
  recipient: "user@example.com",
  status: "PENDING",
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
};

const MOCK_PAGED = {
  content: [MOCK_NOTIFICATION],
  totalElements: 1,
  totalPages: 1,
  size: 10,
  number: 0,
};
// Definit les donnees de test mock enum.
const MOCK_ENUM = [{ code: "EMAIL", name: "Email", description: "" }];

describe("notificationApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("getAll calls GET on NOTIFICATIONS.ALL with params", async () => {
    mockGet.mockResolvedValueOnce({ data: MOCK_PAGED });
    const params = { page: 0, size: 10 };
    const result = await notificationApi.getAll(params);
    expect(mockGet).toHaveBeenCalledWith(
      NOTIFICATION_ENDPOINTS.NOTIFICATIONS.ALL,
      { params, signal: undefined },
    );
    expect(result).toEqual(MOCK_PAGED);
  });

  it("getAllList calls GET on NOTIFICATIONS.ALL_LIST", async () => {
    mockGet.mockResolvedValueOnce({ data: [MOCK_NOTIFICATION] });
    const result = await notificationApi.getAllList();
    expect(mockGet).toHaveBeenCalledWith(
      NOTIFICATION_ENDPOINTS.NOTIFICATIONS.ALL_LIST,
      { signal: undefined },
    );
    expect(result).toEqual([MOCK_NOTIFICATION]);
  });

  it("getById calls GET on NOTIFICATIONS.BY_ID(id)", async () => {
    mockGet.mockResolvedValueOnce({ data: MOCK_NOTIFICATION });
    const result = await notificationApi.getById("notif-1");
    expect(mockGet).toHaveBeenCalledWith(
      NOTIFICATION_ENDPOINTS.NOTIFICATIONS.BY_ID("notif-1"),
      { signal: undefined },
    );
    expect(result).toEqual(MOCK_NOTIFICATION);
  });

  it("getByStatus calls GET on NOTIFICATIONS.BY_STATUS(status)", async () => {
    mockGet.mockResolvedValueOnce({ data: [MOCK_NOTIFICATION] });
    await notificationApi.getByStatus("PENDING");
    expect(mockGet).toHaveBeenCalledWith(
      NOTIFICATION_ENDPOINTS.NOTIFICATIONS.BY_STATUS("PENDING"),
      { signal: undefined },
    );
  });

  it("getByIncidentId calls GET on NOTIFICATIONS.BY_INCIDENT(incidentId)", async () => {
    mockGet.mockResolvedValueOnce({ data: [MOCK_NOTIFICATION] });
    await notificationApi.getByIncidentId("inc-1");
    expect(mockGet).toHaveBeenCalledWith(
      NOTIFICATION_ENDPOINTS.NOTIFICATIONS.BY_INCIDENT("inc-1"),
      { signal: undefined },
    );
  });

  it("getByRecipient calls GET on NOTIFICATIONS.BY_RECIPIENT(recipient)", async () => {
    mockGet.mockResolvedValueOnce({ data: [MOCK_NOTIFICATION] });
    await notificationApi.getByRecipient("user@example.com");
    expect(mockGet).toHaveBeenCalledWith(
      NOTIFICATION_ENDPOINTS.NOTIFICATIONS.BY_RECIPIENT("user@example.com"),
      { signal: undefined },
    );
  });

  it("getByRecipientPage calls GET on NOTIFICATIONS.BY_RECIPIENT_PAGE with params", async () => {
    mockGet.mockResolvedValueOnce({ data: MOCK_PAGED });
    const params = { page: 0, size: 10, sort: "createdAt,desc" };
    const result = await notificationApi.getByRecipientPage(
      "user@example.com",
      params,
    );

    expect(mockGet).toHaveBeenCalledWith(
      NOTIFICATION_ENDPOINTS.NOTIFICATIONS.BY_RECIPIENT_PAGE(
        "user@example.com",
      ),
      { params, signal: undefined },
    );
    expect(result).toEqual(MOCK_PAGED);
  });

  it("create calls POST on NOTIFICATIONS.BASE with data", async () => {
    mockPost.mockResolvedValueOnce({ data: MOCK_NOTIFICATION });
    const requestData = {
      type: "EMAIL" as const,
      recipient: "user@example.com",
    };
    const result = await notificationApi.create(requestData);
    expect(mockPost).toHaveBeenCalledWith(
      NOTIFICATION_ENDPOINTS.NOTIFICATIONS.BASE,
      requestData,
    );
    expect(result).toEqual(MOCK_NOTIFICATION);
  });

  it("markAsRead calls PATCH on NOTIFICATIONS.READ(id)", async () => {
    mockPatch.mockResolvedValueOnce({
      data: { ...MOCK_NOTIFICATION, status: "READ" },
    });
    const result = await notificationApi.markAsRead("notif-1");
    expect(mockPatch).toHaveBeenCalledWith(
      NOTIFICATION_ENDPOINTS.NOTIFICATIONS.READ("notif-1"),
    );
    expect(result.status).toBe("READ");
  });

  it("markAllAsRead calls PATCH on NOTIFICATIONS.READ_ALL", async () => {
    mockPatch.mockResolvedValueOnce({ data: undefined });
    await notificationApi.markAllAsRead();
    expect(mockPatch).toHaveBeenCalledWith(
      NOTIFICATION_ENDPOINTS.NOTIFICATIONS.READ_ALL,
    );
  });

  it("delete calls DELETE on NOTIFICATIONS.BY_ID(id)", async () => {
    mockDelete.mockResolvedValueOnce({ data: undefined });
    await notificationApi.delete("notif-1");
    expect(mockDelete).toHaveBeenCalledWith(
      NOTIFICATION_ENDPOINTS.NOTIFICATIONS.BY_ID("notif-1"),
    );
  });

  it("bulkDelete calls DELETE on NOTIFICATIONS.BULK", async () => {
    mockDelete.mockResolvedValueOnce({ data: undefined });
    await notificationApi.bulkDelete(["notif-1", "notif-2"]);
    expect(mockDelete).toHaveBeenCalledWith(
      NOTIFICATION_ENDPOINTS.NOTIFICATIONS.BULK,
      { data: ["notif-1", "notif-2"] },
    );
  });

  it("getNotificationTypes calls GET on ENUMS.NOTIFICATION_TYPES", async () => {
    mockGet.mockResolvedValueOnce({ data: MOCK_ENUM });
    const result = await notificationApi.getNotificationTypes();
    expect(mockGet).toHaveBeenCalledWith(
      NOTIFICATION_ENDPOINTS.ENUMS.NOTIFICATION_TYPES,
      { signal: undefined },
    );
    expect(result).toEqual(MOCK_ENUM);
  });

  it("getNotificationStatuses calls GET on ENUMS.NOTIFICATION_STATUSES", async () => {
    mockGet.mockResolvedValueOnce({ data: MOCK_ENUM });
    const result = await notificationApi.getNotificationStatuses();
    expect(mockGet).toHaveBeenCalledWith(
      NOTIFICATION_ENDPOINTS.ENUMS.NOTIFICATION_STATUSES,
      { signal: undefined },
    );
    expect(result).toEqual(MOCK_ENUM);
  });

  it("should throw when the API call rejects", async () => {
    mockGet.mockRejectedValueOnce(new Error("Network error"));
    await expect(notificationApi.getAll()).rejects.toThrow("Network error");
  });
});
