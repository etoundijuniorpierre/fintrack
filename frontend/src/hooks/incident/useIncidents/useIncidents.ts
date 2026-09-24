// Hook React pour gerer le cycle de vie et le workflow des incidents.

import {
  keepPreviousData,
  useQuery,
  useMutation,
  useQueryClient,
  type QueryClient,
} from "@tanstack/react-query";
import { App } from "antd";
import { useTranslation } from "react-i18next";
import { incidentApi } from "../../../api/incident/incidentApi/incidentApi";
import { QUERY_KEYS } from "../../../utils/constants";
import { isUuid } from "../../../utils/formatters/formatters";
import {
  getApiErrorMessage,
  getApiSuccessMessage,
} from "../../../utils/apiMessages/apiMessages";
import type {
  IncidentResponse,
  IncidentSummaryResponse,
  PagedResponse,
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
} from "../../../api/incident/types";

// Remplace l'incident modifie dans toutes les pages deja en cache.
const updateIncidentListCache = (
  current: PagedResponse<IncidentSummaryResponse> | undefined,
  incident: IncidentResponse,
) => {
  if (!current?.content) return current;
  return {
    ...current,
    content: current.content.map((item) =>
      item.id === incident.id ? { ...item, ...incident } : item,
    ),
  };
};

// Synchronise immediatement les caches detail/liste avec la reponse backend.
// Le detail est indexe a la fois sur l'UUID et sur la reference : la page peut
// avoir ete ouverte par l'un ou l'autre (URL par code ou ancien lien UUID), et
// la reponse d'une mutation fait autorite pour les deux cles.
const syncIncidentCaches = (
  queryClient: QueryClient,
  incident: IncidentResponse | undefined,
) => {
  if (!incident?.id) return;
  // Les reponses de mutation ne portent pas les champs calcules uniquement sur le
  // detail par le back (verdicts de portee, valideur attendu). On les conserve depuis
  // le cache existant pour ne pas faire disparaitre les boutons entre la mutation et
  // le refetch ; ils sont stables pour un couple (incident, utilisateur) et seront
  // rafraichis ensuite.
  const preserveDetailOnlyFields = (
    prev: IncidentResponse | undefined,
  ): IncidentResponse => ({
    ...incident,
    canValidate: incident.canValidate ?? prev?.canValidate,
    canCancel: incident.canCancel ?? prev?.canCancel,
    canRequestConfirmation:
      incident.canRequestConfirmation ?? prev?.canRequestConfirmation,
    canConfirmRelevance:
      incident.canConfirmRelevance ?? prev?.canConfirmRelevance,
    relevanceResponderRole:
      incident.relevanceResponderRole ?? prev?.relevanceResponderRole,
    relevanceResponderTarget:
      incident.relevanceResponderTarget ?? prev?.relevanceResponderTarget,
    expectedValidatorRole:
      incident.expectedValidatorRole ?? prev?.expectedValidatorRole,
    expectedValidatorTarget:
      incident.expectedValidatorTarget ?? prev?.expectedValidatorTarget,
  });
  queryClient.setQueryData<IncidentResponse | undefined>(
    QUERY_KEYS.INCIDENTS.DETAIL(incident.id),
    preserveDetailOnlyFields,
  );
  if (incident.reference) {
    queryClient.setQueryData<IncidentResponse | undefined>(
      QUERY_KEYS.INCIDENTS.DETAIL(incident.reference),
      preserveDetailOnlyFields,
    );
  }
  queryClient.setQueriesData<
    PagedResponse<IncidentSummaryResponse> | undefined
  >({ queryKey: QUERY_KEYS.INCIDENTS.ALL }, (current) =>
    updateIncidentListCache(current, incident),
  );
};

// Declenche le refetch de fond apres la mise a jour immediate du cache.
const refreshIncidentCaches = (
  queryClient: QueryClient,
  incidentId?: string,
) => {
  queryClient.invalidateQueries({ queryKey: QUERY_KEYS.INCIDENTS.ALL });
  queryClient.invalidateQueries({ queryKey: ["dashboard"] });
  if (!incidentId) return;
  queryClient.invalidateQueries({
    queryKey: QUERY_KEYS.INCIDENTS.DETAIL(incidentId),
  });
  queryClient.invalidateQueries({
    queryKey: QUERY_KEYS.INCIDENTS.HISTORY(incidentId),
  });
  queryClient.invalidateQueries({
    queryKey: QUERY_KEYS.DOCUMENTS.BY_INCIDENT(incidentId),
  });
};

// Charge la liste des incidents selon les filtres fournis.
export const useIncidents = (params?: Record<string, unknown>) => {
  return useQuery({
    queryKey: [...QUERY_KEYS.INCIDENTS.ALL, params],
    queryFn: ({ signal }) => incidentApi.getAll(params, signal),
    placeholderData: keepPreviousData,
    staleTime: 2 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
};

// Charge le detail d'un incident a partir de son identifiant d'URL, qui peut
// etre soit le code metier (FT-I-2026-0001), soit l'UUID technique (anciens
// liens / incidents sans reference). La forme de l'identifiant choisit l'endpoint.
export const useIncident = (identifier: string | undefined) => {
  return useQuery({
    queryKey: QUERY_KEYS.INCIDENTS.DETAIL(identifier || ""),
    queryFn: ({ signal }) =>
      isUuid(identifier)
        ? incidentApi.getById(identifier!, signal)
        : incidentApi.getByReference(identifier!, signal),
    enabled: !!identifier,
    staleTime: 2 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
};

// Charge les commentaires associes a un incident.
export const useIncidentComments = (incidentId: string | undefined) => {
  return useQuery({
    queryKey: QUERY_KEYS.INCIDENTS.COMMENTS(incidentId || ""),
    queryFn: ({ signal }) => incidentApi.getComments(incidentId!, signal),
    enabled: !!incidentId,
    staleTime: 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
};

// Charge l'historique de workflow d'un incident.
export const useIncidentHistory = (incidentId: string | undefined) => {
  return useQuery({
    queryKey: QUERY_KEYS.INCIDENTS.HISTORY(incidentId || ""),
    queryFn: ({ signal }) => incidentApi.getHistory(incidentId!, signal),
    enabled: !!incidentId,
    staleTime: 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
};

// Charge le referentiel des statuts d'incident.
export const useIncidentStatuses = () => {
  return useQuery({
    queryKey: QUERY_KEYS.INCIDENTS.STATUSES,
    queryFn: ({ signal }) => incidentApi.getStatuses(signal),
    staleTime: 10 * 60 * 1000,
  });
};

// Charge le referentiel des criticites d'incident.
export const useCriticalities = () => {
  return useQuery({
    queryKey: QUERY_KEYS.INCIDENTS.CRITICALITIES,
    queryFn: ({ signal }) => incidentApi.getCriticalities(signal),
    staleTime: 10 * 60 * 1000,
  });
};

// Charge le referentiel des types d'action incident.
export const useActionTypes = () => {
  return useQuery({
    queryKey: QUERY_KEYS.INCIDENTS.ACTION_TYPES,
    queryFn: ({ signal }) => incidentApi.getActionTypes(signal),
    staleTime: 10 * 60 * 1000,
  });
};

// Charge le referentiel des periodes de reporting incident.
export const usePeriodTypes = () => {
  return useQuery({
    queryKey: QUERY_KEYS.INCIDENTS.PERIOD_TYPES,
    queryFn: ({ signal }) => incidentApi.getPeriodTypes(signal),
    staleTime: 10 * 60 * 1000,
  });
};

// Charge le referentiel des types d'incident.
export const useIncidentTypes = () => {
  return useQuery({
    queryKey: QUERY_KEYS.INCIDENTS.TYPES,
    queryFn: ({ signal }) => incidentApi.getIncidentTypes(signal),
    staleTime: 10 * 60 * 1000,
  });
};

// Charge le referentiel des causes d'incident.
export const useIncidentCauses = () => {
  return useQuery({
    queryKey: QUERY_KEYS.INCIDENTS.CAUSES,
    queryFn: ({ signal }) => incidentApi.getIncidentCauses(signal),
    staleTime: 10 * 60 * 1000,
  });
};

// Declenche une creation via l'API et synchronise les donnees liees.
export const useCreateIncident = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: IncidentRequest) => incidentApi.create(data),
    onSuccess: (response) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("incidents.messages.create_success");
      message.success(successMessage);
      syncIncidentCaches(queryClient, response);
      refreshIncidentCaches(queryClient, response.id);
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Declenche une mise a jour via l'API et synchronise les donnees liees.
export const useUpdateIncident = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: IncidentUpdateRequest }) =>
      incidentApi.update(id, data),
    onSuccess: (response, { id }) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("incidents.messages.update_success");
      message.success(successMessage);
      syncIncidentCaches(queryClient, response);
      refreshIncidentCaches(queryClient, id);
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Declenche une suppression via l'API et synchronise les donnees liees.
export const useDeleteIncident = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => incidentApi.delete(id),
    onSuccess: () => {
      message.success(t("incidents.messages.delete_success"));
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.INCIDENTS.ALL });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Declenche la validation d'un incident et rafraichit les caches associes.
export const useValidateIncident = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ValidateRequest }) =>
      incidentApi.validate(id, data),
    onSuccess: (response, { id }) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("incidents.workflow.messages.validate_success");
      message.success(successMessage);
      syncIncidentCaches(queryClient, response);
      refreshIncidentCaches(queryClient, id);
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Declenche le rejet d'un incident et rafraichit les caches associes.
export const useRejectIncident = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: RejectRequest }) =>
      incidentApi.reject(id, data),
    onSuccess: (response, { id }) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("incidents.workflow.messages.reject_success");
      message.success(successMessage);
      syncIncidentCaches(queryClient, response);
      refreshIncidentCaches(queryClient, id);
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Declenche le transfert d'un incident et rafraichit les caches associes.
export const useTransferIncident = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: TransferRequest }) =>
      incidentApi.transfer(id, data),
    onSuccess: (response) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("incidents.workflow.messages.transfer_success");
      message.success(successMessage);
      syncIncidentCaches(queryClient, response);
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.INCIDENTS.ALL });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Declenche l'assignation d'un incident et rafraichit les caches associes.
export const useAssignIncident = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: AssignRequest }) =>
      incidentApi.assign(id, data),
    onSuccess: (response, { id }) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("incidents.workflow.messages.assign_success");
      message.success(successMessage);
      syncIncidentCaches(queryClient, response);
      refreshIncidentCaches(queryClient, id);
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Declenche le demarrage du traitement d'un incident.
export const useStartIncident = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: StartRequest }) =>
      incidentApi.start(id, data),
    onSuccess: (response, { id }) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("incidents.workflow.messages.start_success");
      message.success(successMessage);
      syncIncidentCaches(queryClient, response);
      refreshIncidentCaches(queryClient, id);
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

export const useSubmitSolution = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: SolutionProposalRequest;
    }) => incidentApi.submitSolution(id, data),
    onSuccess: (response, { id }) => {
      message.success(
        t("incidents.workflow.messages.submit_solution_success"),
      );
      syncIncidentCaches(queryClient, response);
      refreshIncidentCaches(queryClient, id);
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

export const useDirectionValidate = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data?: { comment?: string };
    }) => incidentApi.directionValidate(id, data),
    onSuccess: (response, { id }) => {
      message.success(
        t("incidents.workflow.messages.direction_validate_success"),
      );
      syncIncidentCaches(queryClient, response);
      refreshIncidentCaches(queryClient, id);
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

export const useDirectionReject = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: { rejectionReason: string };
    }) => incidentApi.directionReject(id, data),
    onSuccess: (response, { id }) => {
      message.success(
        t("incidents.workflow.messages.direction_reject_success"),
      );
      syncIncidentCaches(queryClient, response);
      refreshIncidentCaches(queryClient, id);
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Declenche le blocage d'un incident et rafraichit les caches associes.
export const useBlockIncident = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: BlockRequest }) =>
      incidentApi.block(id, data),
    onSuccess: (response, { id }) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("incidents.workflow.messages.block_success");
      message.success(successMessage);
      syncIncidentCaches(queryClient, response);
      refreshIncidentCaches(queryClient, id);
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Declenche la reprise d'un incident bloque.
export const useResumeIncident = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ResumeRequest }) =>
      incidentApi.resume(id, data),
    onSuccess: (response, { id }) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("incidents.workflow.messages.resume_success");
      message.success(successMessage);
      syncIncidentCaches(queryClient, response);
      refreshIncidentCaches(queryClient, id);
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Attente prolongee : le traitant reprend l'incident et interroge l'entite source.
export const useRequestConfirmation = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: RelevanceRequest }) =>
      incidentApi.requestConfirmation(id, data),
    onSuccess: (response, { id }) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("incidents.workflow.messages.request_confirmation_success");
      message.success(successMessage);
      syncIncidentCaches(queryClient, response);
      refreshIncidentCaches(queryClient, id);
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// L'entite source confirme l'actualite : l'incident repart en traitement.
export const useConfirmRelevance = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: RelevanceRequest }) =>
      incidentApi.confirmRelevance(id, data),
    onSuccess: (response, { id }) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("incidents.workflow.messages.confirm_relevance_success");
      message.success(successMessage);
      syncIncidentCaches(queryClient, response);
      refreshIncidentCaches(queryClient, id);
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Declenche l'annulation d'un incident et rafraichit les caches associes.
export const useCancelIncident = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: RejectRequest }) =>
      incidentApi.cancel(id, data),
    onSuccess: (response, { id }) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("incidents.workflow.messages.cancel_success");
      message.success(successMessage);
      syncIncidentCaches(queryClient, response);
      refreshIncidentCaches(queryClient, id);
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Declenche le passage au statut Traite (le traitant a fini son traitement).
export const useTreatIncident = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: TreatRequest }) =>
      incidentApi.treat(id, data),
    onSuccess: (response, { id }) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("incidents.workflow.messages.treat_success");
      message.success(successMessage);
      syncIncidentCaches(queryClient, response);
      refreshIncidentCaches(queryClient, id);
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Declenche la validation de la resolution d'un incident traite et rafraichit les caches.
export const useResolveIncident = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ResolveRequest }) =>
      incidentApi.resolve(id, data),
    onSuccess: (response, { id }) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("incidents.workflow.messages.resolve_success");
      message.success(successMessage);
      syncIncidentCaches(queryClient, response);
      refreshIncidentCaches(queryClient, id);
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Renvoie un incident traite en traitement (le valideur juge la resolution insuffisante).
export const useMarkUnresolvedIncident = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UnresolvedRequest }) =>
      incidentApi.markUnresolved(id, data),
    onSuccess: (response, { id }) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("incidents.workflow.messages.mark_unresolved_success");
      message.success(successMessage);
      syncIncidentCaches(queryClient, response);
      refreshIncidentCaches(queryClient, id);
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Declenche la cloture d'un incident et rafraichit les caches associes.
export const useCloseIncident = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: CloseRequest }) =>
      incidentApi.close(id, data),
    onSuccess: (response, { id }) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("incidents.workflow.messages.close_success");
      message.success(successMessage);
      syncIncidentCaches(queryClient, response);
      refreshIncidentCaches(queryClient, id);
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Declenche la reouverture d'un incident et rafraichit les caches associes.
export const useReopenIncident = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ReopenRequest }) =>
      incidentApi.reopen(id, data),
    onSuccess: (response, { id }) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("incidents.workflow.messages.reopen_success");
      message.success(successMessage);
      syncIncidentCaches(queryClient, response);
      refreshIncidentCaches(queryClient, id);
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Declenche la duplication d'un incident.
export const useCloneIncident = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id }: { id: string }) => incidentApi.clone(id),
    onSuccess: (response, { id }) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("incidents.workflow.messages.clone_success");
      message.success(successMessage);
      syncIncidentCaches(queryClient, response);
      refreshIncidentCaches(queryClient, response.id);
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.INCIDENTS.HISTORY(id),
      });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Declenche la resoumission d'un incident rejete.
export const useResubmitIncident = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ValidateRequest }) =>
      incidentApi.resubmit(id, data),
    onSuccess: (response, { id }) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("incidents.workflow.messages.resubmit_success");
      message.success(successMessage);
      syncIncidentCaches(queryClient, response);
      refreshIncidentCaches(queryClient, id);
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Ajoute un commentaire a un incident et rafraichit commentaires et historique.
export const useAddComment = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: CommentRequest }) =>
      incidentApi.addComment(id, data),
    onSuccess: (response, { id }) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("incidents.comments.messages.add_success");
      message.success(successMessage);
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.INCIDENTS.COMMENTS(id),
      });
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.INCIDENTS.HISTORY(id),
      });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

export const useUpdateComment = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      commentId,
      data,
    }: {
      id: string;
      commentId: string;
      data: CommentRequest;
    }) => incidentApi.updateComment(id, commentId, data),
    onSuccess: (response, { id }) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("incidents.comments.messages.update_success");
      message.success(successMessage);
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.INCIDENTS.COMMENTS(id),
      });
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.INCIDENTS.HISTORY(id),
      });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

export const useDeleteComment = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, commentId, onlyIfEmpty }: { id: string; commentId: string; onlyIfEmpty?: boolean }) =>
      onlyIfEmpty ? incidentApi.deleteComment(id, commentId, true) : incidentApi.deleteComment(id, commentId),
    onSuccess: (_, { id }) => {
      message.success(t("incidents.comments.messages.delete_success"));
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.INCIDENTS.COMMENTS(id),
      });
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.INCIDENTS.HISTORY(id),
      });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};
