// Hook React pour charger et gerer les pieces jointes.

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { App } from "antd";
import { useTranslation } from "react-i18next";
import { documentApi } from "../../api/document/documentApi";
import { QUERY_KEYS } from "../../utils/constants";
import { getApiErrorMessage } from "../../utils/apiMessages/apiMessages";

// Charge les pieces jointes d'un incident.
export const useIncidentAttachments = (incidentId: string | undefined) => {
  return useQuery({
    queryKey: QUERY_KEYS.DOCUMENTS.BY_INCIDENT(incidentId || ""),
    queryFn: () => documentApi.getByIncidentId(incidentId!),
    enabled: !!incidentId,
  });
};

// Televerse une piece jointe puis rafraichit la liste de l'incident.
export const useUploadAttachment = (incidentId: string) => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    // category : INCIDENT par defaut, ou RESOLUTION / CLOSURE.
    mutationFn: ({ file, category }: { file: File; category?: string }) =>
      documentApi.upload(file, incidentId, { category }),
    onSuccess: () => {
      message.success(t("incidents.attachments.messages.upload_success"));
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.DOCUMENTS.BY_INCIDENT(incidentId),
      });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Televerse une piece jointe rattachee a un commentaire.
export const useUploadCommentAttachment = (incidentId: string) => {
  const { message } = App.useApp();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ file, commentId }: { file: File; commentId: string }) =>
      documentApi.upload(file, incidentId, { commentId }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.DOCUMENTS.BY_INCIDENT(incidentId),
      });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Declenche une suppression via l'API et synchronise les donnees liees.
export const useDeleteAttachment = (incidentId: string) => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (attachmentId: string) => documentApi.delete(attachmentId),
    onSuccess: () => {
      message.success(t("incidents.attachments.messages.delete_success"));
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.DOCUMENTS.BY_INCIDENT(incidentId),
      });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};
