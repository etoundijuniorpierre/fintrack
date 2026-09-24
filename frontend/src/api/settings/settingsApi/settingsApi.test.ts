// Tests frontend : verifie le comportement de parametrage api.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { settingsApi } from "./settingsApi";
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

vi.mock("../../client", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn(),
  },
}));

describe("settingsApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const mockSignal = new AbortController().signal;

  describe("Incident Types", () => {
    const mockPayload: IncidentTypeConfigRequest = {
      name: "FIRE",
      displayName: "Fire Incident",
      isActive: true,
      slaHours: 24,
      requiresValidation: true,
      requiresCauseAnalysis: false,
      emailNotificationsEnabled: false,
      closerRoles: ["ASSIGNEE"],
      validatorScope: "AGENCY_MANAGER",
    };

    const mockResponse: IncidentTypeConfigResponse = {
      id: "incident-type-1",
      name: "FIRE",
      displayName: "Fire Incident",
      isActive: true,
      slaHours: 24,
      requiresValidation: true,
      requiresCauseAnalysis: false,
      emailNotificationsEnabled: false,
      closerRoles: ["ASSIGNEE"],
      validatorScope: "AGENCY_MANAGER",
      createdAt: "2026-05-06T10:00:00Z",
      updatedAt: "2026-05-06T10:00:00Z",
    };

    it("should call GET and return incident types", async () => {
      vi.mocked(apiClient.get).mockResolvedValueOnce({ data: [mockResponse] });

      const result = await settingsApi.getIncidentTypes(mockSignal);

      expect(apiClient.get).toHaveBeenCalledWith(
        SETTINGS_ENDPOINTS.INCIDENT_TYPES.ALL,
        { signal: mockSignal },
      );
      expect(result).toEqual([mockResponse]);
    });

    it("should call POST and return the created incident type", async () => {
      vi.mocked(apiClient.post).mockResolvedValueOnce({ data: mockResponse });

      const result = await settingsApi.createIncidentType(mockPayload);

      expect(apiClient.post).toHaveBeenCalledWith(
        SETTINGS_ENDPOINTS.INCIDENT_TYPES.BASE,
        mockPayload,
      );
      expect(result).toEqual(mockResponse);
    });

    it("should call PUT and return the updated incident type", async () => {
      vi.mocked(apiClient.put).mockResolvedValueOnce({ data: mockResponse });
      const updatedPayload: IncidentTypeConfigRequest = {
        ...mockPayload,
        displayName: "Updated Fire",
      };

      const result = await settingsApi.updateIncidentType(
        "incident-type-1",
        updatedPayload,
      );

      expect(apiClient.put).toHaveBeenCalledWith(
        SETTINGS_ENDPOINTS.INCIDENT_TYPES.BY_ID("incident-type-1"),
        updatedPayload,
      );
      expect(result).toEqual(mockResponse);
    });

    it("should call DELETE for the incident type", async () => {
      vi.mocked(apiClient.delete).mockResolvedValueOnce({ data: {} });

      await settingsApi.deleteIncidentType("incident-type-1");

      expect(apiClient.delete).toHaveBeenCalledWith(
        SETTINGS_ENDPOINTS.INCIDENT_TYPES.BY_ID("incident-type-1"),
      );
    });
  });

  describe("Agencies", () => {
    const mockPayload: AgencyRequest = {
      name: "Main Agency",
      city: "Yaoundé",
      isActive: true,
    };

    const mockResponse: AgencyResponse = {
      id: "agency-1",
      name: "Main Agency",
      code: "MAIN",
      isActive: true,
      members: [],
      createdAt: "2026-05-06T10:00:00Z",
      updatedAt: "2026-05-06T10:00:00Z",
    };

    it("should call GET and return agencies", async () => {
      vi.mocked(apiClient.get).mockResolvedValueOnce({ data: [mockResponse] });

      const result = await settingsApi.getAgencies(mockSignal);

      expect(apiClient.get).toHaveBeenCalledWith(
        SETTINGS_ENDPOINTS.AGENCIES.ALL,
        { signal: mockSignal },
      );
      expect(result).toEqual([mockResponse]);
    });

    it("should call POST and return the created agency", async () => {
      vi.mocked(apiClient.post).mockResolvedValueOnce({ data: mockResponse });

      const result = await settingsApi.createAgency(mockPayload);

      expect(apiClient.post).toHaveBeenCalledWith(
        SETTINGS_ENDPOINTS.AGENCIES.BASE,
        mockPayload,
      );
      expect(result).toEqual(mockResponse);
    });

    it("should call PUT and return the updated agency", async () => {
      vi.mocked(apiClient.put).mockResolvedValueOnce({ data: mockResponse });
      const updatedPayload: AgencyRequest = {
        ...mockPayload,
        name: "Updated Agency",
      };

      const result = await settingsApi.updateAgency("agency-1", updatedPayload);

      expect(apiClient.put).toHaveBeenCalledWith(
        SETTINGS_ENDPOINTS.AGENCIES.BY_ID("agency-1"),
        updatedPayload,
      );
      expect(result).toEqual(mockResponse);
    });

    it("should call DELETE for the agency", async () => {
      vi.mocked(apiClient.delete).mockResolvedValueOnce({ data: {} });

      await settingsApi.deleteAgency("agency-1");

      expect(apiClient.delete).toHaveBeenCalledWith(
        SETTINGS_ENDPOINTS.AGENCIES.BY_ID("agency-1"),
      );
    });

    it("should call PATCH to assign an agency head", async () => {
      vi.mocked(apiClient.patch).mockResolvedValueOnce({ data: mockResponse });

      const result = await settingsApi.assignAgencyHead("agency-1", "user-1");

      expect(apiClient.patch).toHaveBeenCalledWith(
        SETTINGS_ENDPOINTS.AGENCIES.ASSIGN_HEAD("agency-1", "user-1"),
      );
      expect(result).toEqual(mockResponse);
    });
  });

  describe("Departments", () => {
    const mockPayload: ServiceRequest = {
      name: "IT Department",
      isActive: true,
    };

    const mockResponse: ServiceResponse = {
      id: "service-1",
      name: "IT Department",
      isActive: true,
      members: [],
      createdAt: "2026-05-06T10:00:00Z",
      updatedAt: "2026-05-06T10:00:00Z",
    };

    it("should call GET and return departments", async () => {
      vi.mocked(apiClient.get).mockResolvedValueOnce({ data: [mockResponse] });

      const result = await settingsApi.getDepartments(mockSignal);

      expect(apiClient.get).toHaveBeenCalledWith(
        SETTINGS_ENDPOINTS.SERVICES.ALL,
        { signal: mockSignal },
      );
      expect(result).toEqual([mockResponse]);
    });

    it("should call POST and return the created department", async () => {
      vi.mocked(apiClient.post).mockResolvedValueOnce({ data: mockResponse });

      const result = await settingsApi.createDepartment(mockPayload);

      expect(apiClient.post).toHaveBeenCalledWith(
        SETTINGS_ENDPOINTS.SERVICES.BASE,
        mockPayload,
      );
      expect(result).toEqual(mockResponse);
    });

    it("should call PUT and return the updated department", async () => {
      vi.mocked(apiClient.put).mockResolvedValueOnce({ data: mockResponse });
      const updatedPayload: ServiceRequest = {
        ...mockPayload,
        name: "Updated IT",
      };

      const result = await settingsApi.updateDepartment(
        "service-1",
        updatedPayload,
      );

      expect(apiClient.put).toHaveBeenCalledWith(
        SETTINGS_ENDPOINTS.SERVICES.BY_ID("service-1"),
        updatedPayload,
      );
      expect(result).toEqual(mockResponse);
    });

    it("should call DELETE for the department", async () => {
      vi.mocked(apiClient.delete).mockResolvedValueOnce({ data: {} });

      await settingsApi.deleteDepartment("service-1");

      expect(apiClient.delete).toHaveBeenCalledWith(
        SETTINGS_ENDPOINTS.SERVICES.BY_ID("service-1"),
      );
    });

    it("should call PATCH to assign a department head", async () => {
      vi.mocked(apiClient.patch).mockResolvedValueOnce({ data: mockResponse });

      const result = await settingsApi.assignDepartmentHead(
        "service-1",
        "user-1",
      );

      expect(apiClient.patch).toHaveBeenCalledWith(
        SETTINGS_ENDPOINTS.SERVICES.ASSIGN_HEAD("service-1", "user-1"),
      );
      expect(result).toEqual(mockResponse);
    });
  });

  describe("Roles", () => {
    const mockPayload: RoleRequest = {
      name: "Admin",
      isSystem: false,
      permissionIds: ["perm-1"],
    };

    const mockResponse: RoleResponse = {
      id: "role-1",
      name: "Admin",
      isSystem: false,
      permissions: [],
      createdAt: "2026-05-06T10:00:00Z",
      updatedAt: "2026-05-06T10:00:00Z",
    };

    it("should call GET and return roles", async () => {
      vi.mocked(apiClient.get).mockResolvedValueOnce({ data: [mockResponse] });

      const result = await settingsApi.getRoles(mockSignal);

      expect(apiClient.get).toHaveBeenCalledWith(SETTINGS_ENDPOINTS.ROLES.ALL, {
        signal: mockSignal,
      });
      expect(result).toEqual([mockResponse]);
    });

    it("should call POST and return the created role", async () => {
      vi.mocked(apiClient.post).mockResolvedValueOnce({ data: mockResponse });

      const result = await settingsApi.createRole(mockPayload);

      expect(apiClient.post).toHaveBeenCalledWith(
        SETTINGS_ENDPOINTS.ROLES.BASE,
        mockPayload,
      );
      expect(result).toEqual(mockResponse);
    });

    it("should call PUT and return the updated role", async () => {
      vi.mocked(apiClient.put).mockResolvedValueOnce({ data: mockResponse });
      const partialPayload: Partial<RoleRequest> = { name: "Super Admin" };

      const result = await settingsApi.updateRole("role-1", partialPayload);

      expect(apiClient.put).toHaveBeenCalledWith(
        SETTINGS_ENDPOINTS.ROLES.BY_ID("role-1"),
        partialPayload,
      );
      expect(result).toEqual(mockResponse);
    });

    it("should call DELETE for the role", async () => {
      vi.mocked(apiClient.delete).mockResolvedValueOnce({ data: {} });

      await settingsApi.deleteRole("role-1");

      expect(apiClient.delete).toHaveBeenCalledWith(
        SETTINGS_ENDPOINTS.ROLES.BY_ID("role-1"),
      );
    });
  });

  describe("Permissions", () => {
    const mockResponse: PermissionResponse = {
      id: "perm-1",
      name: "READ_ALL",
      createdAt: "2026-05-06T10:00:00Z",
      updatedAt: "2026-05-06T10:00:00Z",
    };

    it("should call GET and return permissions", async () => {
      vi.mocked(apiClient.get).mockResolvedValueOnce({ data: [mockResponse] });

      const result = await settingsApi.getPermissions(mockSignal);

      expect(apiClient.get).toHaveBeenCalledWith(
        SETTINGS_ENDPOINTS.PERMISSIONS.ALL,
        { signal: mockSignal },
      );
      expect(result).toEqual([mockResponse]);
    });
  });

  describe("Report Schedules", () => {
    const mockPayload: ReportScheduleRequest = {
      name: "Daily Status",
      type: "DAILY",
      format: "PDF",
      recipientEmails: ["admin@example.com"],
    };

    const mockResponse: ReportScheduleResponse = {
      id: "schedule-1",
      name: "Daily Status",
      type: "DAILY",
      format: "PDF",
      recipientEmails: ["admin@example.com"],
      isActive: true,
      createdAt: "2026-05-06T10:00:00Z",
      updatedAt: "2026-05-06T10:00:00Z",
    };

    it("should call GET and return report schedules", async () => {
      vi.mocked(apiClient.get).mockResolvedValueOnce({ data: [mockResponse] });

      const result = await settingsApi.getReportSchedules(mockSignal);

      expect(apiClient.get).toHaveBeenCalledWith(
        SETTINGS_ENDPOINTS.REPORT_SCHEDULES.ALL,
        { signal: mockSignal },
      );
      expect(result).toEqual([mockResponse]);
    });

    it("should call POST and return the created report schedule", async () => {
      vi.mocked(apiClient.post).mockResolvedValueOnce({ data: mockResponse });

      const result = await settingsApi.createReportSchedule(mockPayload);

      expect(apiClient.post).toHaveBeenCalledWith(
        SETTINGS_ENDPOINTS.REPORT_SCHEDULES.BASE,
        mockPayload,
      );
      expect(result).toEqual(mockResponse);
    });

    it("should call PUT and return the updated report schedule", async () => {
      vi.mocked(apiClient.put).mockResolvedValueOnce({ data: mockResponse });
      const partialPayload: Partial<ReportScheduleRequest> = {
        name: "Weekly Status",
        type: "WEEKLY",
      };

      const result = await settingsApi.updateReportSchedule(
        "schedule-1",
        partialPayload,
      );

      expect(apiClient.put).toHaveBeenCalledWith(
        SETTINGS_ENDPOINTS.REPORT_SCHEDULES.BY_ID("schedule-1"),
        partialPayload,
      );
      expect(result).toEqual(mockResponse);
    });

    it("should call DELETE for the report schedule", async () => {
      vi.mocked(apiClient.delete).mockResolvedValueOnce({ data: {} });

      await settingsApi.deleteReportSchedule("schedule-1");

      expect(apiClient.delete).toHaveBeenCalledWith(
        SETTINGS_ENDPOINTS.REPORT_SCHEDULES.BY_ID("schedule-1"),
      );
    });

    it("should call PATCH to toggle the report schedule and return updated data", async () => {
      vi.mocked(apiClient.patch).mockResolvedValueOnce({ data: mockResponse });

      const result = await settingsApi.toggleReportSchedule("schedule-1");

      expect(apiClient.patch).toHaveBeenCalledWith(
        SETTINGS_ENDPOINTS.REPORT_SCHEDULES.TOGGLE("schedule-1"),
      );
      expect(result).toEqual(mockResponse);
    });
  });
});
