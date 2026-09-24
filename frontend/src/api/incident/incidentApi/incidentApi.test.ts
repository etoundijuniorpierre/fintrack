// Tests frontend : verifie le comportement de incident api.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import apiClient from "../../client";
import { incidentApi } from "./incidentApi";
import { INCIDENT_SERVICE_ENDPOINTS } from "..";

import {
  makeIncidentSummary,
  makeIncidentResponse,
  makeIncidentComment,
  makeIncidentHistory,
  makeIncidentTypeConfig,
} from "../../../mocks/incident/incidentFixtures";
import { makeEnumResponse } from "../../../mocks/shared/summaryFixtures";

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

// Definit les donnees de test inincident id.
const INCIDENT_ID = "incident-1";

describe("incidentApi", () => {
  it("uses the empty-only protocol only for automatic cleanup", async () => {
    mockDelete.mockResolvedValue({ data: undefined });
    await incidentApi.deleteComment(INCIDENT_ID, "comment", true);
    expect(mockDelete).toHaveBeenLastCalledWith(`${INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.COMMENTS(INCIDENT_ID)}/comment?onlyIfEmpty=true`);
    await incidentApi.deleteComment(INCIDENT_ID, "comment");
    expect(mockDelete).toHaveBeenLastCalledWith(`${INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.COMMENTS(INCIDENT_ID)}/comment`);
  });
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should call GET incidents base with params and signal when getAll is called", async () => {
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

    mockGet.mockResolvedValueOnce({ data: page });

    const result = await incidentApi.getAll({ page: 0 });

    expect(mockGet).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.BASE,
      { params: { page: 0 }, signal: undefined },
    );
    expect(result).toEqual(page);
  });

  it("should call GET incidents/:id when getById is called", async () => {
    const incident = makeIncidentResponse();
    mockGet.mockResolvedValueOnce({ data: incident });

    const result = await incidentApi.getById(INCIDENT_ID);

    expect(mockGet).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.BY_ID(INCIDENT_ID),
      { signal: undefined },
    );
    expect(result).toEqual(incident);
  });

  it("should call GET incidents/by-reference/:reference when getByReference is called", async () => {
    const incident = makeIncidentResponse();
    mockGet.mockResolvedValueOnce({ data: incident });

    const result = await incidentApi.getByReference("FT-I-2026-0001");

    expect(mockGet).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.BY_REFERENCE("FT-I-2026-0001"),
      { signal: undefined },
    );
    expect(result).toEqual(incident);
  });

  it("should call POST incidents base with payload when create is called", async () => {
    const incident = makeIncidentResponse();
    mockPost.mockResolvedValueOnce({ data: incident });

    const payload = {
      title: "Test",
      description: "Desc",
      typeId: "type-1",
      criticality: "LOW" as const,
    };
    const result = await incidentApi.create(payload);

    expect(mockPost).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.BASE,
      payload,
    );
    expect(result).toEqual(incident);
  });

  it("should call PUT incidents/:id with payload when update is called", async () => {
    const incident = makeIncidentResponse();
    mockPut.mockResolvedValueOnce({ data: incident });

    const payload = {
      title: "Updated",
      description: "Updated description",
      typeId: "type-1",
      criticality: "LOW" as const,
    };
    const result = await incidentApi.update(INCIDENT_ID, payload);

    expect(mockPut).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.BY_ID(INCIDENT_ID),
      payload,
    );
    expect(result).toEqual(incident);
  });

  it("should call DELETE incidents/:id when delete is called", async () => {
    mockDelete.mockResolvedValueOnce({});

    await incidentApi.delete(INCIDENT_ID);

    expect(mockDelete).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.BY_ID(INCIDENT_ID),
    );
  });

  it("should call POST incidents/:id/validate when validate is called", async () => {
    const incident = makeIncidentResponse();
    mockPost.mockResolvedValueOnce({ data: incident });

    const result = await incidentApi.validate(INCIDENT_ID, { comment: "ok" });

    expect(mockPost).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.VALIDATE(INCIDENT_ID),
      { comment: "ok" },
    );
    expect(result).toEqual(incident);
  });

  it("should call POST incidents/:id/reject when reject is called", async () => {
    const incident = makeIncidentResponse();
    mockPost.mockResolvedValueOnce({ data: incident });

    await incidentApi.reject(INCIDENT_ID, { reason: "invalid" });

    expect(mockPost).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.REJECT(INCIDENT_ID),
      { reason: "invalid" },
    );
  });

  it("should call POST incidents/:id/transfer when transfer is called", async () => {
    mockPost.mockResolvedValueOnce({ data: makeIncidentResponse() });

    await incidentApi.transfer(INCIDENT_ID, { targetServiceId: "svc-1" });

    expect(mockPost).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.TRANSFER(INCIDENT_ID),
      { targetServiceId: "svc-1" },
    );
  });

  it("should call POST incidents/:id/assign when assign is called", async () => {
    mockPost.mockResolvedValueOnce({ data: makeIncidentResponse() });

    await incidentApi.assign(INCIDENT_ID, { assignedTo: "user-1" });

    expect(mockPost).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.ASSIGN(INCIDENT_ID),
      { assignedTo: "user-1" },
    );
  });

  it("should call POST incidents/:id/start when start is called", async () => {
    mockPost.mockResolvedValueOnce({ data: makeIncidentResponse() });

    await incidentApi.start(INCIDENT_ID, { comment: "taking it" });

    expect(mockPost).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.START(INCIDENT_ID),
      { comment: "taking it" },
    );
  });

  it("should call POST incidents/:id/block when block is called", async () => {
    mockPost.mockResolvedValueOnce({ data: makeIncidentResponse() });

    await incidentApi.block(INCIDENT_ID, { reason: "Waiting for vendor" });

    expect(mockPost).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.BLOCK(INCIDENT_ID),
      { reason: "Waiting for vendor" },
    );
  });

  it("should call POST incidents/:id/resume when resume is called", async () => {
    mockPost.mockResolvedValueOnce({ data: makeIncidentResponse() });

    await incidentApi.resume(INCIDENT_ID, { comment: "Vendor answered" });

    expect(mockPost).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.RESUME(INCIDENT_ID),
      { comment: "Vendor answered" },
    );
  });

  it("should call POST incidents/:id/resolve when resolve is called", async () => {
    mockPost.mockResolvedValueOnce({ data: makeIncidentResponse() });

    await incidentApi.resolve(INCIDENT_ID, { resolutionNote: "Fixed" });

    expect(mockPost).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.RESOLVE(INCIDENT_ID),
      { resolutionNote: "Fixed" },
    );
  });

  it("should call POST incidents/:id/close when close is called", async () => {
    mockPost.mockResolvedValueOnce({ data: makeIncidentResponse() });

    await incidentApi.close(INCIDENT_ID, {
      closureDescription: "Fixed",
      comment: "done",
    });

    expect(mockPost).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.CLOSE(INCIDENT_ID),
      {
        closureDescription: "Fixed",
        comment: "done",
      },
    );
  });

  it("should call POST incidents/:id/reopen when reopen is called", async () => {
    mockPost.mockResolvedValueOnce({ data: makeIncidentResponse() });

    await incidentApi.reopen(INCIDENT_ID, { reason: "Need more info" });

    expect(mockPost).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.REOPEN(INCIDENT_ID),
      { reason: "Need more info" },
    );
  });

  it("should call POST incidents/:id/clone when clone is called", async () => {
    mockPost.mockResolvedValueOnce({ data: makeIncidentResponse() });

    await incidentApi.clone(INCIDENT_ID);

    expect(mockPost).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.CLONE(INCIDENT_ID),
    );
  });

  it("should call POST incidents/:id/resubmit when resubmit is called", async () => {
    mockPost.mockResolvedValueOnce({ data: makeIncidentResponse() });

    await incidentApi.resubmit(INCIDENT_ID, { comment: "fixed" });

    expect(mockPost).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.RESUBMIT(INCIDENT_ID),
      { comment: "fixed" },
    );
  });

  it("should call POST incidents/:id/cancel when cancel is called", async () => {
    mockPost.mockResolvedValueOnce({ data: makeIncidentResponse() });

    await incidentApi.cancel(INCIDENT_ID, { reason: "mistake" });

    expect(mockPost).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.CANCEL(INCIDENT_ID),
      { reason: "mistake" },
    );
  });

  it("should call GET incidents/:id/comments when getComments is called", async () => {
    const comments = [makeIncidentComment()];
    mockGet.mockResolvedValueOnce({ data: comments });

    const result = await incidentApi.getComments(INCIDENT_ID);

    expect(mockGet).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.COMMENTS(INCIDENT_ID),
      { signal: undefined },
    );
    expect(result).toEqual(comments);
  });

  it("should call POST incidents/:id/comments when addComment is called", async () => {
    const comment = makeIncidentComment();
    mockPost.mockResolvedValueOnce({ data: comment });

    const result = await incidentApi.addComment(INCIDENT_ID, {
      content: "Hello",
      isInternal: false,
    });

    expect(mockPost).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.COMMENTS(INCIDENT_ID),
      { content: "Hello", isInternal: false },
    );
    expect(result).toEqual(comment);
  });

  it("should call GET incidents/:id/history when getHistory is called", async () => {
    const history = [makeIncidentHistory()];
    mockGet.mockResolvedValueOnce({ data: history });

    const result = await incidentApi.getHistory(INCIDENT_ID);

    expect(mockGet).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.HISTORY(INCIDENT_ID),
      { signal: undefined },
    );
    expect(result).toEqual(history);
  });

  it("should call GET enums/incident-statuses when getStatuses is called", async () => {
    const statuses = [makeEnumResponse({ code: "OPEN" })];
    mockGet.mockResolvedValueOnce({ data: statuses });

    const result = await incidentApi.getStatuses();

    expect(mockGet).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.ENUMS.STATUSES,
      { signal: undefined },
    );
    expect(result).toEqual(statuses);
  });

  it("should call GET enums/criticalities when getCriticalities is called", async () => {
    mockGet.mockResolvedValueOnce({ data: [] });

    await incidentApi.getCriticalities();

    expect(mockGet).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.ENUMS.CRITICALITIES,
      { signal: undefined },
    );
  });

  it("should call GET enums/action-types when getActionTypes is called", async () => {
    mockGet.mockResolvedValueOnce({ data: [] });

    await incidentApi.getActionTypes();

    expect(mockGet).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.ENUMS.ACTION_TYPES,
      { signal: undefined },
    );
  });

  it("should call GET incident-type-configs/all when getIncidentTypes is called", async () => {
    const types = [makeIncidentTypeConfig()];
    mockGet.mockResolvedValueOnce({ data: types });

    const result = await incidentApi.getIncidentTypes();

    expect(mockGet).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENT_TYPE_CONFIGS.ALL,
      { signal: undefined },
    );
    expect(result).toEqual(types);
  });

  it("should throw when the API call rejects", async () => {
    mockGet.mockRejectedValueOnce(new Error("Network error"));
    await expect(incidentApi.getAll()).rejects.toThrow("Network error");
  });
});
