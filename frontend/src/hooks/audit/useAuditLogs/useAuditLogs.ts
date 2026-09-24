// Hook React pour consulter et filtrer les journaux d'audit.

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { auditApi } from "../../../api/audit";
import { QUERY_KEYS } from "../../../utils/constants";
import type { AuditLogsParams } from "../../../api/audit";

// Charge les journaux d'audit selon les filtres fournis.
export const useAuditLogs = (params?: AuditLogsParams) => {
  return useQuery({
    queryKey: [...QUERY_KEYS.AUDIT.ALL, params],
    queryFn: ({ signal }) => auditApi.getAll(params, signal),
    placeholderData: keepPreviousData,
  });
};

// Charge le detail d'une entree d'audit.
export const useAuditLog = (id: string | undefined) => {
  return useQuery({
    queryKey: QUERY_KEYS.AUDIT.DETAIL(id!),
    queryFn: ({ signal }) => auditApi.getById(id!, signal),
    enabled: !!id,
  });
};

// Charge les journaux d'audit d'un utilisateur.
export const useAuditLogsByUser = (userId: string | undefined) => {
  return useQuery({
    queryKey: QUERY_KEYS.AUDIT.BY_USER(userId!),
    queryFn: ({ signal }) => auditApi.getByUserId(userId!, signal),
    enabled: !!userId,
  });
};

// Charge les journaux d'audit associes a une action.
export const useAuditLogsByAction = (action: string | undefined) => {
  return useQuery({
    queryKey: QUERY_KEYS.AUDIT.BY_ACTION(action!),
    queryFn: ({ signal }) => auditApi.getByAction(action!, signal),
    enabled: !!action,
  });
};

// Charge les journaux d'audit associes a un type de ressource.
export const useAuditLogsByResourceType = (
  resourceType: string | undefined,
) => {
  return useQuery({
    queryKey: QUERY_KEYS.AUDIT.BY_RESOURCE_TYPE(resourceType!),
    queryFn: ({ signal }) => auditApi.getByResourceType(resourceType!, signal),
    enabled: !!resourceType,
  });
};

// Charge les journaux d'audit associes a un statut.
export const useAuditLogsByStatus = (status: string | undefined) => {
  return useQuery({
    queryKey: QUERY_KEYS.AUDIT.BY_STATUS(status!),
    queryFn: ({ signal }) => auditApi.getByStatus(status!, signal),
    enabled: !!status,
  });
};

// Charge le referentiel des actions d'audit.
export const useAuditActions = () => {
  return useQuery({
    queryKey: QUERY_KEYS.AUDIT.ACTIONS,
    queryFn: ({ signal }) => auditApi.getAuditActions(signal),
    staleTime: 10 * 60 * 1000,
  });
};

// Charge le referentiel des statuts d'audit.
export const useAuditStatuses = () => {
  return useQuery({
    queryKey: QUERY_KEYS.AUDIT.STATUSES,
    queryFn: ({ signal }) => auditApi.getAuditStatuses(signal),
    staleTime: 10 * 60 * 1000,
  });
};
