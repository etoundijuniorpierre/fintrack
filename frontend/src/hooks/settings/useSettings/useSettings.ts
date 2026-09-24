// Hooks de parametres : lecture et mutations des referentiels administrables.

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { App } from "antd";
import { useTranslation } from "react-i18next";
import { settingsApi } from "../../../api/settings/settingsApi/settingsApi";
import { QUERY_KEYS } from "../../../utils/constants";
import {
  getApiErrorMessage,
  getApiSuccessMessage,
} from "../../../utils/apiMessages/apiMessages";
import type {
  IncidentTypeConfigRequest,
  AgencyRequest,
  ServiceRequest,
  RoleRequest,
  ReportScheduleRequest,
} from "../../../api/settings/types";

// Type les donnees horodatees utilisees par l'interface.
type TimestampedRecord = {
  createdAt?: string | null;
  updatedAt?: string | null;
};

// Garde les listes de configuration dans l'ordre le plus utile a la maintenance.
const sortByMostRecentChange = <T extends TimestampedRecord>(data: T[]): T[] =>
  [...data].sort(
    (a, b) =>
      new Date(b.updatedAt || b.createdAt || 0).getTime() -
      new Date(a.updatedAt || a.createdAt || 0).getTime(),
  );

// Charge les types d'incident disponibles pour le routage metier.
export const useSettingsIncidentTypes = () => {
  return useQuery({
    queryKey: QUERY_KEYS.SETTINGS.INCIDENT_TYPES,
    queryFn: ({ signal }) => settingsApi.getIncidentTypes(signal),
    select: (data) => (Array.isArray(data) ? sortByMostRecentChange(data) : []),
  });
};

// Charge le detail d'un type d'incident cible par l'administration.
export const useIncidentType = (id: string) => {
  return useQuery({
    queryKey: [...QUERY_KEYS.SETTINGS.INCIDENT_TYPES, id],
    queryFn: ({ signal }) => settingsApi.getIncidentType(id, signal),
    enabled: Boolean(id),
  });
};

// Charge les agences referencees dans les comptes et incidents.
export const useAgencies = () => {
  return useQuery({
    queryKey: QUERY_KEYS.AGENCIES.ALL,
    queryFn: ({ signal }) => settingsApi.getAgencies(signal),
    select: (data) => (Array.isArray(data) ? sortByMostRecentChange(data) : []),
  });
};

// Charge le detail d'une agence pour consultation ou edition.
export const useAgency = (id: string) => {
  return useQuery({
    queryKey: [...QUERY_KEYS.AGENCIES.ALL, id],
    queryFn: ({ signal }) => settingsApi.getAgency(id, signal),
    enabled: Boolean(id),
  });
};

// Charge les services operationnels rattaches aux utilisateurs.
export const useDepartments = () => {
  return useQuery({
    queryKey: QUERY_KEYS.SERVICES.ALL,
    queryFn: ({ signal }) => settingsApi.getDepartments(signal),
    select: (data) => (Array.isArray(data) ? sortByMostRecentChange(data) : []),
  });
};

// Charge le detail d'un service pour consultation ou edition.
export const useDepartment = (id: string) => {
  return useQuery({
    queryKey: [...QUERY_KEYS.SERVICES.ALL, id],
    queryFn: ({ signal }) => settingsApi.getDepartment(id, signal),
    enabled: Boolean(id),
  });
};

// Charge les roles configures pour la matrice d'habilitations.
export const useRoles = () => {
  return useQuery({
    queryKey: QUERY_KEYS.ROLES.ALL,
    queryFn: ({ signal }) => settingsApi.getRoles(signal),
    select: (data) => (Array.isArray(data) ? sortByMostRecentChange(data) : []),
  });
};

// Charge le detail d'un role avec ses permissions assignees.
export const useRole = (id: string) => {
  return useQuery({
    queryKey: [...QUERY_KEYS.ROLES.ALL, id],
    queryFn: ({ signal }) => settingsApi.getRole(id, signal),
    enabled: Boolean(id),
  });
};

// Charge le catalogue des permissions disponibles.
export const usePermissions = () => {
  return useQuery({
    queryKey: QUERY_KEYS.PERMISSIONS.ALL,
    queryFn: ({ signal }) => settingsApi.getPermissions(signal),
    select: (data) => (Array.isArray(data) ? data : []),
  });
};

// Charge les planifications de rapports automatiques.
export const useReportSchedules = () => {
  return useQuery({
    queryKey: QUERY_KEYS.SETTINGS.REPORT_SCHEDULES,
    queryFn: ({ signal }) => settingsApi.getReportSchedules(signal),
    select: (data) => (Array.isArray(data) ? sortByMostRecentChange(data) : []),
  });
};

// Charge le detail d'une planification de rapport.
export const useReportSchedule = (id: string) => {
  return useQuery({
    queryKey: [...QUERY_KEYS.SETTINGS.REPORT_SCHEDULES, id],
    queryFn: ({ signal }) => settingsApi.getReportSchedule(id, signal),
    enabled: Boolean(id),
  });
};

// Cree un type d'incident et rafraichit le referentiel associe.
export const useCreateIncidentType = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: IncidentTypeConfigRequest) =>
      settingsApi.createIncidentType(data),
    onSuccess: (response) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("settings.incidentTypes.messages.create_success");
      message.success(successMessage);
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.SETTINGS.INCIDENT_TYPES,
      });
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.INCIDENTS.TYPES,
      });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Cree une agence et rafraichit les listes dependantes.
export const useCreateAgency = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: AgencyRequest) => settingsApi.createAgency(data),
    onSuccess: (response) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("settings.agencies.messages.create_success");
      message.success(successMessage);
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.AGENCIES.ALL });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Cree un service operationnel et rafraichit le referentiel services.
export const useCreateDepartment = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: ServiceRequest) => settingsApi.createDepartment(data),
    onSuccess: (response) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("settings.services.messages.create_success");
      message.success(successMessage);
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.SERVICES.ALL });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Cree un role et recharge la matrice d'habilitations.
export const useCreateRole = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: RoleRequest) => settingsApi.createRole(data),
    onSuccess: (response) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("settings.roles.messages.create_success");
      message.success(successMessage);
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.ROLES.ALL });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Cree une planification de rapport et recharge la liste des automatisations.
export const useCreateReportSchedule = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: ReportScheduleRequest) =>
      settingsApi.createReportSchedule(data),
    onSuccess: (response) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("settings.reportSchedules.messages.create_success");
      message.success(successMessage);
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.SETTINGS.REPORT_SCHEDULES,
      });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Met a jour un type d'incident et synchronise le cache de configuration.
export const useUpdateIncidentType = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: IncidentTypeConfigRequest;
    }) => settingsApi.updateIncidentType(id, data),
    onSuccess: (response) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("settings.incidentTypes.messages.update_success");
      message.success(successMessage);
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.SETTINGS.INCIDENT_TYPES,
      });
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.INCIDENTS.TYPES,
      });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Met a jour une agence et synchronise le cache associe.
export const useUpdateAgency = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: AgencyRequest }) =>
      settingsApi.updateAgency(id, data),
    onSuccess: (response) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("settings.agencies.messages.update_success");
      message.success(successMessage);
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.AGENCIES.ALL });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Met a jour un service et synchronise le cache associe.
export const useUpdateDepartment = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ServiceRequest }) =>
      settingsApi.updateDepartment(id, data),
    onSuccess: (response) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("settings.services.messages.update_success");
      message.success(successMessage);
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.SERVICES.ALL });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Met a jour un role et recharge les permissions affichees.
export const useUpdateRole = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<RoleRequest> }) =>
      settingsApi.updateRole(id, data),
    onSuccess: (response) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("settings.roles.messages.update_success");
      message.success(successMessage);
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.ROLES.ALL });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Met a jour une planification de rapport et recharge les automatisations.
export const useUpdateReportSchedule = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: Partial<ReportScheduleRequest>;
    }) => settingsApi.updateReportSchedule(id, data),
    onSuccess: (response) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("settings.reportSchedules.messages.update_success");
      message.success(successMessage);
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.SETTINGS.REPORT_SCHEDULES,
      });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Supprime un type d'incident et recharge le referentiel.
export const useDeleteIncidentType = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => settingsApi.deleteIncidentType(id),
    onSuccess: () => {
      message.success(t("settings.incidentTypes.messages.delete_success"));
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.SETTINGS.INCIDENT_TYPES,
      });
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.INCIDENTS.TYPES,
      });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Supprime une agence et recharge les listes dependantes.
export const useDeleteAgency = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => settingsApi.deleteAgency(id),
    onSuccess: () => {
      message.success(t("settings.agencies.messages.delete_success"));
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.AGENCIES.ALL });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Supprime un service et recharge le referentiel services.
export const useDeleteDepartment = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => settingsApi.deleteDepartment(id),
    onSuccess: () => {
      message.success(t("settings.services.messages.delete_success"));
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.SERVICES.ALL });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Supprime un role et recharge la matrice d'habilitations.
export const useDeleteRole = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => settingsApi.deleteRole(id),
    onSuccess: () => {
      message.success(t("settings.roles.messages.delete_success"));
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.ROLES.ALL });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Supprime une planification de rapport et recharge les automatisations.
export const useDeleteReportSchedule = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => settingsApi.deleteReportSchedule(id),
    onSuccess: () => {
      message.success(t("settings.reportSchedules.messages.delete_success"));
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.SETTINGS.REPORT_SCHEDULES,
      });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Active ou desactive une planification de rapport.
export const useToggleReportSchedule = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => settingsApi.toggleReportSchedule(id),
    onSuccess: (response) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("settings.reportSchedules.messages.toggle_success");
      message.success(successMessage);
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.SETTINGS.REPORT_SCHEDULES,
      });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Assigne le responsable d'une agence.
export const useAssignAgencyHead = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, userId }: { id: string; userId: string }) =>
      settingsApi.assignAgencyHead(id, userId),
    onSuccess: (response) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("settings.agencies.messages.assign_head_success");
      message.success(successMessage);
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.AGENCIES.ALL });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Assigne le responsable d'un service.
export const useAssignDepartmentHead = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, userId }: { id: string; userId: string }) =>
      settingsApi.assignDepartmentHead(id, userId),
    onSuccess: (response) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("settings.services.messages.assign_head_success");
      message.success(successMessage);
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.SERVICES.ALL });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};
