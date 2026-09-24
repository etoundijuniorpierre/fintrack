// Tests frontend : verifie le comportement de use incidents.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import React from "react";
import { App } from "antd";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  useIncidents,
  useIncident,
  useCreateIncident,
  useUpdateIncident,
  useDeleteIncident,
  useValidateIncident,
  useAddComment,
  useDeleteComment,
  useIncidentStatuses,
  useReopenIncident,
  useCloneIncident,
  useResubmitIncident,
  useCancelIncident,
  useSubmitSolution,
  useDirectionValidate,
  useDirectionReject,
} from "./useIncidents";
import { incidentApi } from "../../../api/incident/incidentApi/incidentApi";
import {
  makeIncidentSummary,
  makeIncidentResponse,
  makeIncidentComment,
} from "../../../mocks/incident/incidentFixtures";
import { makeEnumResponse } from "../../../mocks/shared/summaryFixtures";
import { QUERY_KEYS } from "../../../utils/constants";

vi.mock("../../../api/incident/incidentApi/incidentApi", () => ({
  incidentApi: {
    getAll: vi.fn(),
    getById: vi.fn(),
    getByReference: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    validate: vi.fn(),
    reject: vi.fn(),
    transfer: vi.fn(),
    assign: vi.fn(),
    start: vi.fn(),
    resolve: vi.fn(),
    close: vi.fn(),
    getComments: vi.fn(),
    addComment: vi.fn(),
    deleteComment: vi.fn(),
    getHistory: vi.fn(),
    getStatuses: vi.fn(),
    getCriticalities: vi.fn(),
    getActionTypes: vi.fn(),
    getIncidentTypes: vi.fn(),
    reopen: vi.fn(),
    clone: vi.fn(),
    resubmit: vi.fn(),
    cancel: vi.fn(),
    submitSolution: vi.fn(),
    directionValidate: vi.fn(),
    directionReject: vi.fn(),
  },
}));

// Couvre les comportements du module teste.
const createTestQueryClient = () =>
  new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

const createWrapper = (queryClient = createTestQueryClient()) => {
  return ({ children }: { children: React.ReactNode }) => (
    <App>
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    </App>
  );
};

describe("useIncidents hooks", () => {
  it("requests empty-only verification for automatic comment cleanup", async () => {
    vi.mocked(incidentApi.deleteComment).mockResolvedValueOnce(undefined);
    const { result } = renderHook(() => useDeleteComment(), { wrapper: createWrapper() });
    await result.current.mutateAsync({ id: "incident", commentId: "comment", onlyIfEmpty: true });
    expect(incidentApi.deleteComment).toHaveBeenCalledWith("incident", "comment", true);
  });
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should return paginated incident list when the API call succeeds", async () => {
    const page = {
      content: [makeIncidentSummary()],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 10,
      first: true,
      last: true,
      empty: false,
    };
    vi.mocked(incidentApi.getAll).mockResolvedValue(page);

    const { result } = renderHook(() => useIncidents(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(page);
  });

  it("should not fetch when id is undefined", () => {
    const { result } = renderHook(() => useIncident(undefined), {
      wrapper: createWrapper(),
    });

    expect(result.current.fetchStatus).toBe("idle");
    expect(incidentApi.getById).not.toHaveBeenCalled();
  });

  it("should fetch by UUID when the identifier is a technical UUID", async () => {
    const uuid = "11111111-1111-1111-1111-111111111111";
    const incident = makeIncidentResponse({ id: uuid });
    vi.mocked(incidentApi.getById).mockResolvedValue(incident);

    const { result } = renderHook(() => useIncident(uuid), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(incidentApi.getById).toHaveBeenCalledWith(uuid, expect.anything());
    expect(incidentApi.getByReference).not.toHaveBeenCalled();
    expect(result.current.data).toEqual(incident);
  });

  it("should fetch by reference when the identifier is a business code", async () => {
    const incident = makeIncidentResponse({ reference: "FT-I-2026-0001" });
    vi.mocked(incidentApi.getByReference).mockResolvedValue(incident);

    const { result } = renderHook(() => useIncident("FT-I-2026-0001"), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(incidentApi.getByReference).toHaveBeenCalledWith(
      "FT-I-2026-0001",
      expect.anything(),
    );
    expect(incidentApi.getById).not.toHaveBeenCalled();
    expect(result.current.data).toEqual(incident);
  });

  it("should call incidentApi.create with the payload when useCreateIncident mutates", async () => {
    const incident = makeIncidentResponse();
    vi.mocked(incidentApi.create).mockResolvedValue(incident);

    const { result } = renderHook(() => useCreateIncident(), {
      wrapper: createWrapper(),
    });

    result.current.mutate({
      title: "T",
      description: "D",
      typeId: "type-1",
      criticality: "LOW",
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(incidentApi.create).toHaveBeenCalled();
  });

  it("should call incidentApi.update with id and data when useUpdateIncident mutates", async () => {
    const incident = makeIncidentResponse({ id: "incident-1" });
    vi.mocked(incidentApi.update).mockResolvedValue(incident);

    const { result } = renderHook(() => useUpdateIncident(), {
      wrapper: createWrapper(),
    });

    const data = {
      title: "Updated",
      description: "Updated description",
      typeId: "type-1",
      criticality: "LOW" as const,
    };
    result.current.mutate({ id: "incident-1", data });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(incidentApi.update).toHaveBeenCalledWith("incident-1", data);
  });

  it("should call incidentApi.delete with id when useDeleteIncident mutates", async () => {
    vi.mocked(incidentApi.delete).mockResolvedValue(undefined);

    const { result } = renderHook(() => useDeleteIncident(), {
      wrapper: createWrapper(),
    });

    result.current.mutate("incident-1");

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(incidentApi.delete).toHaveBeenCalledWith("incident-1");
  });

  it("should call incidentApi.validate with id and data when useValidateIncident mutates", async () => {
    vi.mocked(incidentApi.validate).mockResolvedValue(makeIncidentResponse());

    const { result } = renderHook(() => useValidateIncident(), {
      wrapper: createWrapper(),
    });

    result.current.mutate({ id: "incident-1", data: { comment: "ok" } });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(incidentApi.validate).toHaveBeenCalledWith("incident-1", {
      comment: "ok",
    });
  });

  it("should update incident caches immediately when a workflow mutation succeeds", async () => {
    const queryClient = createTestQueryClient();
    const listQueryKey = [...QUERY_KEYS.INCIDENTS.ALL, { page: 0 }] as const;
    queryClient.setQueryData(
      QUERY_KEYS.INCIDENTS.DETAIL("incident-1"),
      makeIncidentResponse({ id: "incident-1", status: "PENDING_VALIDATION" }),
    );
    queryClient.setQueryData(listQueryKey, {
      content: [
        makeIncidentSummary({
          id: "incident-1",
          status: "PENDING_VALIDATION",
        }),
      ],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 10,
      first: true,
      last: true,
      empty: false,
    });
    vi.mocked(incidentApi.validate).mockResolvedValue(
      makeIncidentResponse({ id: "incident-1", status: "VALIDATED" }),
    );

    const { result } = renderHook(() => useValidateIncident(), {
      wrapper: createWrapper(queryClient),
    });

    result.current.mutate({ id: "incident-1", data: { comment: "ok" } });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(
      queryClient.getQueryData<ReturnType<typeof makeIncidentResponse>>(
        QUERY_KEYS.INCIDENTS.DETAIL("incident-1"),
      )?.status,
    ).toBe("VALIDATED");
    expect(
      queryClient.getQueryData<{
        content: ReturnType<typeof makeIncidentSummary>[];
      }>(listQueryKey)?.content[0].status,
    ).toBe("VALIDATED");
  });

  it("should call incidentApi.reopen with id and data when useReopenIncident mutates", async () => {
    vi.mocked(incidentApi.reopen).mockResolvedValue(makeIncidentResponse());

    const { result } = renderHook(() => useReopenIncident(), {
      wrapper: createWrapper(),
    });

    result.current.mutate({
      id: "incident-1",
      data: { reason: "need more info" },
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(incidentApi.reopen).toHaveBeenCalledWith("incident-1", {
      reason: "need more info",
    });
  });

  it("should call incidentApi.clone with id when useCloneIncident mutates", async () => {
    vi.mocked(incidentApi.clone).mockResolvedValue(makeIncidentResponse());

    const { result } = renderHook(() => useCloneIncident(), {
      wrapper: createWrapper(),
    });

    result.current.mutate({ id: "incident-1" });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(incidentApi.clone).toHaveBeenCalledWith("incident-1");
  });

  it("should call incidentApi.resubmit with id and data when useResubmitIncident mutates", async () => {
    vi.mocked(incidentApi.resubmit).mockResolvedValue(makeIncidentResponse());

    const { result } = renderHook(() => useResubmitIncident(), {
      wrapper: createWrapper(),
    });

    result.current.mutate({ id: "incident-1", data: { comment: "fixed" } });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(incidentApi.resubmit).toHaveBeenCalledWith("incident-1", {
      comment: "fixed",
    });
  });

  it("should call incidentApi.cancel with id and data when useCancelIncident mutates", async () => {
    vi.mocked(incidentApi.cancel).mockResolvedValue(makeIncidentResponse());

    const { result } = renderHook(() => useCancelIncident(), {
      wrapper: createWrapper(),
    });

    result.current.mutate({ id: "incident-1", data: { reason: "mistake" } });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(incidentApi.cancel).toHaveBeenCalledWith("incident-1", {
      reason: "mistake",
    });
  });

  it("should call incidentApi.addComment with id and data when useAddComment mutates", async () => {
    const comment = makeIncidentComment();
    vi.mocked(incidentApi.addComment).mockResolvedValue(comment);

    const { result } = renderHook(() => useAddComment(), {
      wrapper: createWrapper(),
    });

    result.current.mutate({
      id: "incident-1",
      data: { content: "Hello", isInternal: false },
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(incidentApi.addComment).toHaveBeenCalledWith("incident-1", {
      content: "Hello",
      isInternal: false,
    });
  });

  it("should call incidentApi.submitSolution with id and data when useSubmitSolution mutates", async () => {
    vi.mocked(incidentApi.submitSolution).mockResolvedValue(makeIncidentResponse());

    const { result } = renderHook(() => useSubmitSolution(), {
      wrapper: createWrapper(),
    });

    result.current.mutate({
      id: "incident-1",
      data: { proposedSolution: "do this and that" },
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(incidentApi.submitSolution).toHaveBeenCalledWith("incident-1", {
      proposedSolution: "do this and that",
    });
  });

  it("should call incidentApi.directionValidate with id and data when useDirectionValidate mutates", async () => {
    vi.mocked(incidentApi.directionValidate).mockResolvedValue(makeIncidentResponse());

    const { result } = renderHook(() => useDirectionValidate(), {
      wrapper: createWrapper(),
    });

    result.current.mutate({
      id: "incident-1",
      data: { comment: "approved" },
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(incidentApi.directionValidate).toHaveBeenCalledWith("incident-1", {
      comment: "approved",
    });
  });

  it("should call incidentApi.directionReject with id and data when useDirectionReject mutates", async () => {
    vi.mocked(incidentApi.directionReject).mockResolvedValue(makeIncidentResponse());

    const { result } = renderHook(() => useDirectionReject(), {
      wrapper: createWrapper(),
    });

    result.current.mutate({
      id: "incident-1",
      data: { rejectionReason: "not good enough" },
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(incidentApi.directionReject).toHaveBeenCalledWith("incident-1", {
      rejectionReason: "not good enough",
    });
  });

  it("should return statuses data when useIncidentStatuses succeeds", async () => {
    const statuses = [makeEnumResponse({ code: "OPEN" })];
    vi.mocked(incidentApi.getStatuses).mockResolvedValue(statuses);

    const { result } = renderHook(() => useIncidentStatuses(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(statuses);
  });
});
