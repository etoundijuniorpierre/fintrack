// Client API pour la gestion des pieces jointes et documents.

import apiClient from "../../client";
import { DOCUMENT_ENDPOINTS } from "../endpoints/endpoints";
import type {
  AttachmentMetadataPageResponse,
  AttachmentMetadataRequest,
  AttachmentMetadataResponse,
} from "../types";

// Prepare attachment api pour attachment API.
export const attachmentApi = {
  getAll: async (
    params?: { page?: number; size?: number },
    signal?: AbortSignal,
  ): Promise<AttachmentMetadataPageResponse> => {
    const { data } = await apiClient.get<AttachmentMetadataPageResponse>(
      DOCUMENT_ENDPOINTS.ATTACHMENTS.BASE,
      { params, signal },
    );
    return data;
  },

  getAllList: async (
    signal?: AbortSignal,
  ): Promise<AttachmentMetadataResponse[]> => {
    const { data } = await apiClient.get<AttachmentMetadataResponse[]>(
      DOCUMENT_ENDPOINTS.ATTACHMENTS.ALL,
      { signal },
    );
    return data;
  },

  getById: async (
    id: string,
    signal?: AbortSignal,
  ): Promise<AttachmentMetadataResponse> => {
    const { data } = await apiClient.get<AttachmentMetadataResponse>(
      DOCUMENT_ENDPOINTS.ATTACHMENTS.BY_ID(id),
      { signal },
    );
    return data;
  },

  getByIncidentId: async (
    incidentId: string,
    signal?: AbortSignal,
  ): Promise<AttachmentMetadataResponse[]> => {
    const { data } = await apiClient.get<AttachmentMetadataResponse[]>(
      DOCUMENT_ENDPOINTS.ATTACHMENTS.BY_INCIDENT(incidentId),
      { signal },
    );
    return data;
  },

  getByUploaderId: async (
    uploadedById: string,
    signal?: AbortSignal,
  ): Promise<AttachmentMetadataResponse[]> => {
    const { data } = await apiClient.get<AttachmentMetadataResponse[]>(
      DOCUMENT_ENDPOINTS.ATTACHMENTS.BY_UPLOADER(uploadedById),
      { signal },
    );
    return data;
  },

  create: async (
    payload: AttachmentMetadataRequest,
  ): Promise<AttachmentMetadataResponse> => {
    const { data } = await apiClient.post<AttachmentMetadataResponse>(
      DOCUMENT_ENDPOINTS.ATTACHMENTS.BASE,
      payload,
    );
    return data;
  },

  update: async (
    id: string,
    payload: AttachmentMetadataRequest,
  ): Promise<AttachmentMetadataResponse> => {
    const { data } = await apiClient.put<AttachmentMetadataResponse>(
      DOCUMENT_ENDPOINTS.ATTACHMENTS.BY_ID(id),
      payload,
    );
    return data;
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(DOCUMENT_ENDPOINTS.ATTACHMENTS.BY_ID(id));
  },

  deleteByIncidentId: async (incidentId: string): Promise<void> => {
    await apiClient.delete(
      DOCUMENT_ENDPOINTS.ATTACHMENTS.BY_INCIDENT(incidentId),
    );
  },
};
