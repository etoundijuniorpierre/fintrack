// Service API du microservice d'audit : consultation des journaux et enumerations.
import apiClient from "../../client";
import { AUDIT_ENDPOINTS } from "../endpoints";
import type {
  AuditLogResponse,
  AuditLogsParams,
  AuditEnumResponse,
  PagedAuditResponse,
} from "../types";

// Prepare audit api pour audit API.
export const auditApi = {
  getAll: async (
    params?: AuditLogsParams,
    signal?: AbortSignal,
  ): Promise<PagedAuditResponse<AuditLogResponse>> => {
    const { data } = await apiClient.get<PagedAuditResponse<AuditLogResponse>>(
      AUDIT_ENDPOINTS.AUDIT_LOGS.ALL,
      { params, signal },
    );
    return data;
  },

  getById: async (
    id: string,
    signal?: AbortSignal,
  ): Promise<AuditLogResponse> => {
    const { data } = await apiClient.get<AuditLogResponse>(
      AUDIT_ENDPOINTS.AUDIT_LOGS.BY_ID(id),
      { signal },
    );
    return data;
  },

  getByUserId: async (
    userId: string,
    signal?: AbortSignal,
  ): Promise<AuditLogResponse[]> => {
    const { data } = await apiClient.get<AuditLogResponse[]>(
      AUDIT_ENDPOINTS.AUDIT_LOGS.BY_USER(userId),
      { signal },
    );
    return data;
  },

  getByAction: async (
    action: string,
    signal?: AbortSignal,
  ): Promise<AuditLogResponse[]> => {
    const { data } = await apiClient.get<AuditLogResponse[]>(
      AUDIT_ENDPOINTS.AUDIT_LOGS.BY_ACTION(action),
      { signal },
    );
    return data;
  },

  getByResourceType: async (
    resourceType: string,
    signal?: AbortSignal,
  ): Promise<AuditLogResponse[]> => {
    const { data } = await apiClient.get<AuditLogResponse[]>(
      AUDIT_ENDPOINTS.AUDIT_LOGS.BY_RESOURCE_TYPE(resourceType),
      { signal },
    );
    return data;
  },

  getByResource: async (
    resourceType: string,
    resourceId: string,
    signal?: AbortSignal,
  ): Promise<AuditLogResponse[]> => {
    const { data } = await apiClient.get<AuditLogResponse[]>(
      AUDIT_ENDPOINTS.AUDIT_LOGS.BY_RESOURCE(resourceType, resourceId),
      { signal },
    );
    return data;
  },

  getByStatus: async (
    status: string,
    signal?: AbortSignal,
  ): Promise<AuditLogResponse[]> => {
    const { data } = await apiClient.get<AuditLogResponse[]>(
      AUDIT_ENDPOINTS.AUDIT_LOGS.BY_STATUS(status),
      { signal },
    );
    return data;
  },

  getByRange: async (
    from: string,
    to: string,
    signal?: AbortSignal,
  ): Promise<AuditLogResponse[]> => {
    const { data } = await apiClient.get<AuditLogResponse[]>(
      AUDIT_ENDPOINTS.AUDIT_LOGS.BY_RANGE,
      { params: { from, to }, signal },
    );
    return data;
  },

  getAuditActions: async (
    signal?: AbortSignal,
  ): Promise<AuditEnumResponse[]> => {
    const { data } = await apiClient.get<AuditEnumResponse[]>(
      AUDIT_ENDPOINTS.ENUMS.AUDIT_ACTIONS,
      { signal },
    );
    return data;
  },

  getAuditStatuses: async (
    signal?: AbortSignal,
  ): Promise<AuditEnumResponse[]> => {
    const { data } = await apiClient.get<AuditEnumResponse[]>(
      AUDIT_ENDPOINTS.ENUMS.AUDIT_STATUSES,
      { signal },
    );
    return data;
  },
};
