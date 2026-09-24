// Service API du microservice incident : CRUD, workflow, commentaires, historique et enumerations.
import apiClient from "../../client";
import { INCIDENT_SERVICE_ENDPOINTS } from "..";
import type {
  IncidentSummaryResponse,
  IncidentResponse,
  IncidentCommentResponse,
  IncidentHistoryResponse,
  EnumResponse,
  IncidentTypeConfigResponse,
  IncidentRequest,
  IncidentUpdateRequest,
  ValidateRequest,
  RejectRequest,
  TransferRequest,
  AssignRequest,
  StartRequest,
  SolutionProposalRequest,
  BlockRequest,
  RelevanceRequest,
  ResumeRequest,
  TreatRequest,
  ResolveRequest,
  UnresolvedRequest,
  CloseRequest,
  ReopenRequest,
  CommentRequest,
  PagedResponse,
} from "..";

// Prepare incident api pour incident API.
export const incidentApi = {
  getAll: async (
    params?: Record<string, unknown>,
    signal?: AbortSignal,
  ): Promise<PagedResponse<IncidentSummaryResponse>> => {
    const { data } = await apiClient.get<
      PagedResponse<IncidentSummaryResponse>
    >(INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.BASE, { params, signal });
    return data;
  },

  getById: async (
    id: string,
    signal?: AbortSignal,
  ): Promise<IncidentResponse> => {
    const { data } = await apiClient.get<IncidentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.BY_ID(id),
      { signal },
    );
    return data;
  },

  getByReference: async (
    reference: string,
    signal?: AbortSignal,
  ): Promise<IncidentResponse> => {
    const { data } = await apiClient.get<IncidentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.BY_REFERENCE(reference),
      { signal },
    );
    return data;
  },

  // Telecharge la fiche de traitement PDF (cloture / conformite).
  downloadReport: async (id: string): Promise<Blob> => {
    const { data } = await apiClient.get<Blob>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.REPORT(id),
      { responseType: "blob" },
    );
    return data;
  },

  create: async (payload: IncidentRequest): Promise<IncidentResponse> => {
    const { data } = await apiClient.post<IncidentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.BASE,
      payload,
    );
    return data;
  },

  update: async (
    id: string,
    payload: IncidentUpdateRequest,
  ): Promise<IncidentResponse> => {
    const { data } = await apiClient.put<IncidentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.BY_ID(id),
      payload,
    );
    return data;
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.BY_ID(id));
  },

  validate: async (
    id: string,
    payload: ValidateRequest,
  ): Promise<IncidentResponse> => {
    const { data } = await apiClient.post<IncidentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.VALIDATE(id),
      payload,
    );
    return data;
  },

  reject: async (
    id: string,
    payload: RejectRequest,
  ): Promise<IncidentResponse> => {
    const { data } = await apiClient.post<IncidentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.REJECT(id),
      payload,
    );
    return data;
  },

  transfer: async (
    id: string,
    payload: TransferRequest,
  ): Promise<IncidentResponse> => {
    const { data } = await apiClient.post<IncidentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.TRANSFER(id),
      payload,
    );
    return data;
  },

  assign: async (
    id: string,
    payload: AssignRequest,
  ): Promise<IncidentResponse> => {
    const { data } = await apiClient.post<IncidentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.ASSIGN(id),
      payload,
    );
    return data;
  },

  start: async (
    id: string,
    payload: StartRequest,
  ): Promise<IncidentResponse> => {
    const { data } = await apiClient.post<IncidentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.START(id),
      payload,
    );
    return data;
  },

  submitSolution: async (
    id: string,
    payload: SolutionProposalRequest,
  ): Promise<IncidentResponse> => {
    const { data } = await apiClient.post<IncidentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.SUBMIT_SOLUTION(id),
      payload,
    );
    return data;
  },

  directionValidate: async (
    id: string,
    payload?: { comment?: string },
  ): Promise<IncidentResponse> => {
    const { data } = await apiClient.post<IncidentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.DIRECTION_VALIDATE(id),
      payload,
    );
    return data;
  },

  directionReject: async (
    id: string,
    payload: { rejectionReason: string },
  ): Promise<IncidentResponse> => {
    const { data } = await apiClient.post<IncidentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.DIRECTION_REJECT(id),
      payload,
    );
    return data;
  },

  block: async (
    id: string,
    payload: BlockRequest,
  ): Promise<IncidentResponse> => {
    const { data } = await apiClient.post<IncidentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.BLOCK(id),
      payload,
    );
    return data;
  },

  resume: async (
    id: string,
    payload: ResumeRequest,
  ): Promise<IncidentResponse> => {
    const { data } = await apiClient.post<IncidentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.RESUME(id),
      payload,
    );
    return data;
  },

  requestConfirmation: async (
    id: string,
    payload: RelevanceRequest,
  ): Promise<IncidentResponse> => {
    const { data } = await apiClient.post<IncidentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.REQUEST_CONFIRMATION(id),
      payload,
    );
    return data;
  },

  confirmRelevance: async (
    id: string,
    payload: RelevanceRequest,
  ): Promise<IncidentResponse> => {
    const { data } = await apiClient.post<IncidentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.CONFIRM_RELEVANCE(id),
      payload,
    );
    return data;
  },

  treat: async (
    id: string,
    payload: TreatRequest,
  ): Promise<IncidentResponse> => {
    const { data } = await apiClient.post<IncidentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.TREAT(id),
      payload,
    );
    return data;
  },

  resolve: async (
    id: string,
    payload: ResolveRequest,
  ): Promise<IncidentResponse> => {
    const { data } = await apiClient.post<IncidentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.RESOLVE(id),
      payload,
    );
    return data;
  },

  markUnresolved: async (
    id: string,
    payload: UnresolvedRequest,
  ): Promise<IncidentResponse> => {
    const { data } = await apiClient.post<IncidentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.MARK_UNRESOLVED(id),
      payload,
    );
    return data;
  },

  close: async (
    id: string,
    payload: CloseRequest,
  ): Promise<IncidentResponse> => {
    const { data } = await apiClient.post<IncidentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.CLOSE(id),
      payload,
    );
    return data;
  },

  reopen: async (
    id: string,
    payload: ReopenRequest,
  ): Promise<IncidentResponse> => {
    const { data } = await apiClient.post<IncidentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.REOPEN(id),
      payload,
    );
    return data;
  },

  clone: async (id: string): Promise<IncidentResponse> => {
    const { data } = await apiClient.post<IncidentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.CLONE(id),
    );
    return data;
  },

  resubmit: async (
    id: string,
    payload: ValidateRequest,
  ): Promise<IncidentResponse> => {
    const { data } = await apiClient.post<IncidentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.RESUBMIT(id),
      payload,
    );
    return data;
  },

  cancel: async (
    id: string,
    payload: RejectRequest,
  ): Promise<IncidentResponse> => {
    const { data } = await apiClient.post<IncidentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.CANCEL(id),
      payload,
    );
    return data;
  },

  getComments: async (
    id: string,
    signal?: AbortSignal,
  ): Promise<IncidentCommentResponse[]> => {
    const { data } = await apiClient.get<IncidentCommentResponse[]>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.COMMENTS(id),
      { signal },
    );
    return data;
  },

  addComment: async (
    id: string,
    payload: CommentRequest,
  ): Promise<IncidentCommentResponse> => {
    const { data } = await apiClient.post<IncidentCommentResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.COMMENTS(id),
      payload,
    );
    return data;
  },

  updateComment: async (
    id: string,
    commentId: string,
    payload: CommentRequest,
  ): Promise<IncidentCommentResponse> => {
    const { data } = await apiClient.put<IncidentCommentResponse>(
      `${INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.COMMENTS(id)}/${commentId}`,
      payload,
    );
    return data;
  },

  deleteComment: async (
    id: string,
    commentId: string,
    onlyIfEmpty = false,
  ): Promise<void> => {
    await apiClient.delete(
      `${INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.COMMENTS(id)}/${commentId}${onlyIfEmpty ? "?onlyIfEmpty=true" : ""}`,
    );
  },

  getHistory: async (
    id: string,
    signal?: AbortSignal,
  ): Promise<IncidentHistoryResponse[]> => {
    const { data } = await apiClient.get<IncidentHistoryResponse[]>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.HISTORY(id),
      { signal },
    );
    return data;
  },

  getStatuses: async (signal?: AbortSignal): Promise<EnumResponse[]> => {
    const { data } = await apiClient.get<EnumResponse[]>(
      INCIDENT_SERVICE_ENDPOINTS.ENUMS.STATUSES,
      { signal },
    );
    return data;
  },

  getCriticalities: async (signal?: AbortSignal): Promise<EnumResponse[]> => {
    const { data } = await apiClient.get<EnumResponse[]>(
      INCIDENT_SERVICE_ENDPOINTS.ENUMS.CRITICALITIES,
      { signal },
    );
    return data;
  },

  getActionTypes: async (signal?: AbortSignal): Promise<EnumResponse[]> => {
    const { data } = await apiClient.get<EnumResponse[]>(
      INCIDENT_SERVICE_ENDPOINTS.ENUMS.ACTION_TYPES,
      { signal },
    );
    return data;
  },

  getPeriodTypes: async (signal?: AbortSignal): Promise<EnumResponse[]> => {
    const { data } = await apiClient.get<EnumResponse[]>(
      INCIDENT_SERVICE_ENDPOINTS.ENUMS.PERIOD_TYPES,
      { signal },
    );
    return data;
  },

  getIncidentTypes: async (
    signal?: AbortSignal,
  ): Promise<IncidentTypeConfigResponse[]> => {
    const { data } = await apiClient.get<IncidentTypeConfigResponse[]>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENT_TYPE_CONFIGS.ALL,
      { signal },
    );
    return data;
  },

  getIncidentCauses: async (signal?: AbortSignal): Promise<EnumResponse[]> => {
    const { data } = await apiClient.get<EnumResponse[]>(
      INCIDENT_SERVICE_ENDPOINTS.ENUMS.CAUSES,
      { signal },
    );
    return data;
  },
};
