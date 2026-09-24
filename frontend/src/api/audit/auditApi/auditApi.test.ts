// Tests frontend : verifie le comportement de audit api.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { auditApi } from "./auditApi";
import apiClient from "../../client";
import { AUDIT_ENDPOINTS } from "../endpoints";

vi.mock("../../client", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

const mockGet = vi.mocked(apiClient.get);

const MOCK_LOG = {
  id: "log-1",
  timestamp: "2024-01-01T00:00:00Z",
  username: "jdoe",
  roles: ["ADMIN"],
  action: "LOGIN_SUCCESS",
  resourceType: "USER",
  status: "SUCCESS",
};

// Definit les donnees de test mock paged.
const MOCK_PAGED = {
  content: [MOCK_LOG],
  totalElements: 1,
  totalPages: 1,
  size: 10,
  number: 0,
};
// Definit les donnees de test mock enum.
const MOCK_ENUM = [
  { code: "LOGIN_SUCCESS", name: "Login Success", description: "" },
];

describe("auditApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("getAll calls GET on AUDIT_LOGS.ALL with params", async () => {
    mockGet.mockResolvedValueOnce({ data: MOCK_PAGED });
    const params = { page: 0, size: 10 };
    const result = await auditApi.getAll(params);
    expect(mockGet).toHaveBeenCalledWith(AUDIT_ENDPOINTS.AUDIT_LOGS.ALL, {
      params,
      signal: undefined,
    });
    expect(result).toEqual(MOCK_PAGED);
  });

  it("getById calls GET on AUDIT_LOGS.BY_ID(id)", async () => {
    mockGet.mockResolvedValueOnce({ data: MOCK_LOG });
    const result = await auditApi.getById("log-1");
    expect(mockGet).toHaveBeenCalledWith(
      AUDIT_ENDPOINTS.AUDIT_LOGS.BY_ID("log-1"),
      { signal: undefined },
    );
    expect(result).toEqual(MOCK_LOG);
  });

  it("getByUserId calls GET on AUDIT_LOGS.BY_USER(userId)", async () => {
    mockGet.mockResolvedValueOnce({ data: [MOCK_LOG] });
    const result = await auditApi.getByUserId("user-1");
    expect(mockGet).toHaveBeenCalledWith(
      AUDIT_ENDPOINTS.AUDIT_LOGS.BY_USER("user-1"),
      { signal: undefined },
    );
    expect(result).toEqual([MOCK_LOG]);
  });

  it("getByAction calls GET on AUDIT_LOGS.BY_ACTION(action)", async () => {
    mockGet.mockResolvedValueOnce({ data: [MOCK_LOG] });
    await auditApi.getByAction("LOGIN_SUCCESS");
    expect(mockGet).toHaveBeenCalledWith(
      AUDIT_ENDPOINTS.AUDIT_LOGS.BY_ACTION("LOGIN_SUCCESS"),
      { signal: undefined },
    );
  });

  it("getByResourceType calls GET on AUDIT_LOGS.BY_RESOURCE_TYPE(resourceType)", async () => {
    mockGet.mockResolvedValueOnce({ data: [MOCK_LOG] });
    await auditApi.getByResourceType("USER");
    expect(mockGet).toHaveBeenCalledWith(
      AUDIT_ENDPOINTS.AUDIT_LOGS.BY_RESOURCE_TYPE("USER"),
      { signal: undefined },
    );
  });

  it("getByResource calls GET on AUDIT_LOGS.BY_RESOURCE(resourceType, resourceId)", async () => {
    mockGet.mockResolvedValueOnce({ data: [MOCK_LOG] });
    await auditApi.getByResource("USER", "user-1");
    expect(mockGet).toHaveBeenCalledWith(
      AUDIT_ENDPOINTS.AUDIT_LOGS.BY_RESOURCE("USER", "user-1"),
      { signal: undefined },
    );
  });

  it("getByStatus calls GET on AUDIT_LOGS.BY_STATUS(status)", async () => {
    mockGet.mockResolvedValueOnce({ data: [MOCK_LOG] });
    await auditApi.getByStatus("SUCCESS");
    expect(mockGet).toHaveBeenCalledWith(
      AUDIT_ENDPOINTS.AUDIT_LOGS.BY_STATUS("SUCCESS"),
      { signal: undefined },
    );
  });

  it("getByRange calls GET on AUDIT_LOGS.BY_RANGE with from/to params", async () => {
    mockGet.mockResolvedValueOnce({ data: [MOCK_LOG] });
    await auditApi.getByRange("2024-01-01", "2024-01-31");
    expect(mockGet).toHaveBeenCalledWith(AUDIT_ENDPOINTS.AUDIT_LOGS.BY_RANGE, {
      params: { from: "2024-01-01", to: "2024-01-31" },
      signal: undefined,
    });
  });

  it("getAuditActions calls GET on ENUMS.AUDIT_ACTIONS", async () => {
    mockGet.mockResolvedValueOnce({ data: MOCK_ENUM });
    const result = await auditApi.getAuditActions();
    expect(mockGet).toHaveBeenCalledWith(AUDIT_ENDPOINTS.ENUMS.AUDIT_ACTIONS, {
      signal: undefined,
    });
    expect(result).toEqual(MOCK_ENUM);
  });

  it("getAuditStatuses calls GET on ENUMS.AUDIT_STATUSES", async () => {
    mockGet.mockResolvedValueOnce({ data: MOCK_ENUM });
    const result = await auditApi.getAuditStatuses();
    expect(mockGet).toHaveBeenCalledWith(AUDIT_ENDPOINTS.ENUMS.AUDIT_STATUSES, {
      signal: undefined,
    });
    expect(result).toEqual(MOCK_ENUM);
  });

  it("passes AbortSignal to GET requests", async () => {
    mockGet.mockResolvedValueOnce({ data: MOCK_PAGED });
    const signal = new AbortController().signal;
    await auditApi.getAll(undefined, signal);
    expect(mockGet).toHaveBeenCalledWith(AUDIT_ENDPOINTS.AUDIT_LOGS.ALL, {
      params: undefined,
      signal,
    });
  });

  it("should throw when the API call rejects", async () => {
    mockGet.mockRejectedValueOnce(new Error("Network error"));
    await expect(auditApi.getAll()).rejects.toThrow("Network error");
  });
});
