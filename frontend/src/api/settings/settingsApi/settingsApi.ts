// Service API des parametres : CRUD des types d'incident, agences, services, roles et planifications de rapports.
import apiClient from "../../client";
import { SETTINGS_ENDPOINTS } from "../endpoints/endpoints";
import type {
  IncidentTypeConfigRequest,
  IncidentTypeConfigResponse,
  AgencyRequest,
  AgencyResponse,
  ServiceRequest,
  ServiceResponse,
  RoleRequest,
  RoleResponse,
  PermissionResponse,
  ReportScheduleRequest,
  ReportScheduleResponse,
} from "../types";

// Prepare settings api pour parametrage API.
export const settingsApi = {
  getIncidentTypes: async (
    signal?: AbortSignal,
  ): Promise<IncidentTypeConfigResponse[]> => {
    const { data } = await apiClient.get<IncidentTypeConfigResponse[]>(
      SETTINGS_ENDPOINTS.INCIDENT_TYPES.ALL,
      { signal },
    );
    return data;
  },

  getIncidentType: async (
    id: string,
    signal?: AbortSignal,
  ): Promise<IncidentTypeConfigResponse> => {
    const { data } = await apiClient.get<IncidentTypeConfigResponse>(
      SETTINGS_ENDPOINTS.INCIDENT_TYPES.BY_ID(id),
      { signal },
    );
    return data;
  },

  createIncidentType: async (
    payload: IncidentTypeConfigRequest,
  ): Promise<IncidentTypeConfigResponse> => {
    const { data } = await apiClient.post<IncidentTypeConfigResponse>(
      SETTINGS_ENDPOINTS.INCIDENT_TYPES.BASE,
      payload,
    );
    return data;
  },

  updateIncidentType: async (
    id: string,
    payload: IncidentTypeConfigRequest,
  ): Promise<IncidentTypeConfigResponse> => {
    const { data } = await apiClient.put<IncidentTypeConfigResponse>(
      SETTINGS_ENDPOINTS.INCIDENT_TYPES.BY_ID(id),
      payload,
    );
    return data;
  },

  deleteIncidentType: async (id: string): Promise<void> => {
    await apiClient.delete(SETTINGS_ENDPOINTS.INCIDENT_TYPES.BY_ID(id));
  },

  getAgencies: async (signal?: AbortSignal): Promise<AgencyResponse[]> => {
    const { data } = await apiClient.get<AgencyResponse[]>(
      SETTINGS_ENDPOINTS.AGENCIES.ALL,
      { signal },
    );
    return data;
  },

  getAgency: async (
    id: string,
    signal?: AbortSignal,
  ): Promise<AgencyResponse> => {
    const { data } = await apiClient.get<AgencyResponse>(
      SETTINGS_ENDPOINTS.AGENCIES.BY_ID(id),
      { signal },
    );
    return data;
  },

  createAgency: async (payload: AgencyRequest): Promise<AgencyResponse> => {
    const { data } = await apiClient.post<AgencyResponse>(
      SETTINGS_ENDPOINTS.AGENCIES.BASE,
      payload,
    );
    return data;
  },

  updateAgency: async (
    id: string,
    payload: AgencyRequest,
  ): Promise<AgencyResponse> => {
    const { data } = await apiClient.put<AgencyResponse>(
      SETTINGS_ENDPOINTS.AGENCIES.BY_ID(id),
      payload,
    );
    return data;
  },

  deleteAgency: async (id: string): Promise<void> => {
    await apiClient.delete(SETTINGS_ENDPOINTS.AGENCIES.BY_ID(id));
  },

  assignAgencyHead: async (
    id: string,
    userId: string,
  ): Promise<AgencyResponse> => {
    const { data } = await apiClient.patch<AgencyResponse>(
      SETTINGS_ENDPOINTS.AGENCIES.ASSIGN_HEAD(id, userId),
    );
    return data;
  },

  getDepartments: async (signal?: AbortSignal): Promise<ServiceResponse[]> => {
    const { data } = await apiClient.get<ServiceResponse[]>(
      SETTINGS_ENDPOINTS.SERVICES.ALL,
      { signal },
    );
    return data;
  },

  getDepartment: async (
    id: string,
    signal?: AbortSignal,
  ): Promise<ServiceResponse> => {
    const { data } = await apiClient.get<ServiceResponse>(
      SETTINGS_ENDPOINTS.SERVICES.BY_ID(id),
      { signal },
    );
    return data;
  },

  createDepartment: async (
    payload: ServiceRequest,
  ): Promise<ServiceResponse> => {
    const { data } = await apiClient.post<ServiceResponse>(
      SETTINGS_ENDPOINTS.SERVICES.BASE,
      payload,
    );
    return data;
  },

  updateDepartment: async (
    id: string,
    payload: ServiceRequest,
  ): Promise<ServiceResponse> => {
    const { data } = await apiClient.put<ServiceResponse>(
      SETTINGS_ENDPOINTS.SERVICES.BY_ID(id),
      payload,
    );
    return data;
  },

  deleteDepartment: async (id: string): Promise<void> => {
    await apiClient.delete(SETTINGS_ENDPOINTS.SERVICES.BY_ID(id));
  },

  assignDepartmentHead: async (
    id: string,
    userId: string,
  ): Promise<ServiceResponse> => {
    const { data } = await apiClient.patch<ServiceResponse>(
      SETTINGS_ENDPOINTS.SERVICES.ASSIGN_HEAD(id, userId),
    );
    return data;
  },

  getRoles: async (signal?: AbortSignal): Promise<RoleResponse[]> => {
    const { data } = await apiClient.get<RoleResponse[]>(
      SETTINGS_ENDPOINTS.ROLES.ALL,
      { signal },
    );
    return data;
  },

  getRole: async (id: string, signal?: AbortSignal): Promise<RoleResponse> => {
    const { data } = await apiClient.get<RoleResponse>(
      SETTINGS_ENDPOINTS.ROLES.BY_ID(id),
      { signal },
    );
    return data;
  },

  createRole: async (payload: RoleRequest): Promise<RoleResponse> => {
    const { data } = await apiClient.post<RoleResponse>(
      SETTINGS_ENDPOINTS.ROLES.BASE,
      payload,
    );
    return data;
  },

  updateRole: async (
    id: string,
    payload: Partial<RoleRequest>,
  ): Promise<RoleResponse> => {
    const { data } = await apiClient.put<RoleResponse>(
      SETTINGS_ENDPOINTS.ROLES.BY_ID(id),
      payload,
    );
    return data;
  },

  deleteRole: async (id: string): Promise<void> => {
    await apiClient.delete(SETTINGS_ENDPOINTS.ROLES.BY_ID(id));
  },

  getPermissions: async (
    signal?: AbortSignal,
  ): Promise<PermissionResponse[]> => {
    const { data } = await apiClient.get<PermissionResponse[]>(
      SETTINGS_ENDPOINTS.PERMISSIONS.ALL,
      { signal },
    );
    return data;
  },

  getReportSchedules: async (
    signal?: AbortSignal,
  ): Promise<ReportScheduleResponse[]> => {
    const { data } = await apiClient.get<ReportScheduleResponse[]>(
      SETTINGS_ENDPOINTS.REPORT_SCHEDULES.ALL,
      { signal },
    );
    return data;
  },

  getReportSchedule: async (
    id: string,
    signal?: AbortSignal,
  ): Promise<ReportScheduleResponse> => {
    const { data } = await apiClient.get<ReportScheduleResponse>(
      SETTINGS_ENDPOINTS.REPORT_SCHEDULES.BY_ID(id),
      { signal },
    );
    return data;
  },

  createReportSchedule: async (
    payload: ReportScheduleRequest,
  ): Promise<ReportScheduleResponse> => {
    const { data } = await apiClient.post<ReportScheduleResponse>(
      SETTINGS_ENDPOINTS.REPORT_SCHEDULES.BASE,
      payload,
    );
    return data;
  },

  updateReportSchedule: async (
    id: string,
    payload: Partial<ReportScheduleRequest>,
  ): Promise<ReportScheduleResponse> => {
    const { data } = await apiClient.put<ReportScheduleResponse>(
      SETTINGS_ENDPOINTS.REPORT_SCHEDULES.BY_ID(id),
      payload,
    );
    return data;
  },

  deleteReportSchedule: async (id: string): Promise<void> => {
    await apiClient.delete(SETTINGS_ENDPOINTS.REPORT_SCHEDULES.BY_ID(id));
  },

  toggleReportSchedule: async (id: string): Promise<ReportScheduleResponse> => {
    const { data } = await apiClient.patch<ReportScheduleResponse>(
      SETTINGS_ENDPOINTS.REPORT_SCHEDULES.TOGGLE(id),
    );
    return data;
  },
};
