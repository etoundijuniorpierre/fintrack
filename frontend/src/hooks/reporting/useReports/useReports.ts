// Hook React pour generer et telecharger les rapports d'incidents.

import {
  keepPreviousData,
  useQuery,
  useMutation,
  useQueryClient,
} from "@tanstack/react-query";
import apiClient from "../../../api/client";
import { App } from "antd";
import { useTranslation } from "react-i18next";
import { REPORTING_ENDPOINTS } from "../../../api/reporting/endpoints/endpoints";
import { tokenManager } from "../../../utils/tokenManager/tokenManager";
import { API_BASE_URL } from "../../../api/routes";
import type {
  ReportGenerateRequest,
  ReportPageResponse,
  ReportResponse,
} from "../../../api/reporting/types";

const EMPTY_REPORT_PARAMS: Record<string, unknown> = {};

// Charge les rapports generes selon la portee et les filtres selectionnes.
export const useReports = (
  scope?: string,
  params?: Record<string, unknown>,
) => {
  const effectiveParams = params ?? EMPTY_REPORT_PARAMS;
  return useQuery({
    queryKey: ["reports", "generated", scope ?? "default", effectiveParams],
    queryFn: async () => {
      const { data } = await apiClient.get<ReportPageResponse>(
        REPORTING_ENDPOINTS.REPORTS.BASE,
        {
          params: {
            ...effectiveParams,
            ...(scope && scope !== "all" ? { scope } : {}),
          },
        },
      );
      return data;
    },
    placeholderData: keepPreviousData,
    refetchInterval: (query) => {
      const hasPending = query.state.data?.content.some(
        (r) => r.status === "PENDING" || r.status === "GENERATING",
      );
      return hasPending ? 3000 : false;
    },
  });
};

// Lance la generation d'un rapport et rafraichit la liste.
export const useGenerateReport = () => {
  const queryClient = useQueryClient();
  const { message } = App.useApp();
  const { t } = useTranslation();

  return useMutation({
    mutationFn: async (
      request: ReportGenerateRequest,
    ): Promise<ReportResponse> => {
      const { data } = await apiClient.post<ReportResponse>(
        REPORTING_ENDPOINTS.REPORTS.GENERATE,
        request,
      );
      return data;
    },
    onSuccess: () => {
      message.success(t("reports.messages.generateStarted"));
      queryClient.invalidateQueries({ queryKey: ["reports", "generated"] });
    },
    onError: () => {
      message.error(t("reports.errors.generateFailed"));
    },
  });
};

// Declenche une suppression via l'API et synchronise les donnees liees.
export const useDeleteReport = () => {
  const queryClient = useQueryClient();
  const { message } = App.useApp();
  const { t } = useTranslation();

  return useMutation({
    mutationFn: async (id: string): Promise<void> => {
      await apiClient.delete(REPORTING_ENDPOINTS.REPORTS.DELETE(id));
    },
    onSuccess: () => {
      message.success(t("reports.messages.deleteSuccess"));
      queryClient.invalidateQueries({ queryKey: ["reports", "generated"] });
    },
    onError: () => {
      message.error(t("reports.errors.deleteFailed"));
    },
  });
};

// Envoie un rapport par e-mail et rafraichit son etat.
export const useSendEmailReport = () => {
  const queryClient = useQueryClient();
  const { message } = App.useApp();
  const { t } = useTranslation();

  return useMutation({
    mutationFn: async ({
      id,
      recipients,
    }: {
      id: string;
      recipients: string[];
    }): Promise<void> => {
      await apiClient.post(REPORTING_ENDPOINTS.REPORTS.SEND_EMAIL(id), {
        recipients,
      });
    },
    onSuccess: () => {
      message.success(t("reports.messages.email_sent"));
      queryClient.invalidateQueries({ queryKey: ["reports", "generated"] });
    },
    onError: () => {
      message.error(t("reports.messages.email_error"));
    },
  });
};

// Relance la generation d'un rapport en erreur.
export const useRetryReport = () => {
  const queryClient = useQueryClient();
  const { message } = App.useApp();
  const { t } = useTranslation();

  return useMutation({
    mutationFn: async (id: string): Promise<ReportResponse> => {
      const { data } = await apiClient.post<ReportResponse>(
        REPORTING_ENDPOINTS.REPORTS.RETRY(id),
      );
      return data;
    },
    onSuccess: () => {
      message.success(t("reports.messages.retryStarted"));
      queryClient.invalidateQueries({ queryKey: ["reports", "generated"] });
    },
    onError: () => {
      message.error(t("reports.errors.retryFailed"));
    },
  });
};

// Telecharge le fichier d'un rapport (blob).
export const useDownloadReport = () =>
  useMutation({
    mutationFn: async (id: string): Promise<Blob> => {
      const token = tokenManager.getToken();
      const response = await fetch(
        `${API_BASE_URL}${REPORTING_ENDPOINTS.REPORTS.DOWNLOAD(id)}`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
            Accept:
              "application/octet-stream, application/pdf, application/vnd.ms-excel, application/json, */*",
          },
        },
      );
      if (!response.ok) {
        throw new Error("Download failed");
      }
      return response.blob();
    },
  });
