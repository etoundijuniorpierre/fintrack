// Actions de workflow d'un incident : boutons et modales pour valider, rejeter, transferer, assigner, resoudre, cloturer, rouvrir.
import {
  Button,
  Form,
  Input,
  InputNumber,
  Select,
  Dropdown,
  Upload,
  App,
  Alert,
  Typography,
} from "antd";
import type { MenuProps, UploadFile } from "antd";
import { Modal } from "../../../../components/ui";
import { useQueryClient } from "@tanstack/react-query";
import { documentApi } from "../../../../api/document/documentApi";
import { QUERY_KEYS } from "../../../../utils/constants";
import { getApiErrorMessage } from "../../../../utils/apiMessages/apiMessages";
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  CopyOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  QuestionCircleOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  SwapOutlined,
  UserAddOutlined,
  UserSwitchOutlined,
  MoreOutlined,
  UploadOutlined,
  PaperClipOutlined,
  DeleteOutlined,
  UndoOutlined,
} from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import type { ReactNode } from "react";
import { memo, useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  useValidateIncident,
  useRejectIncident,
  useTransferIncident,
  useAssignIncident,
  useStartIncident,
  useBlockIncident,
  useResumeIncident,
  useRequestConfirmation,
  useConfirmRelevance,
  useTreatIncident,
  useResolveIncident,
  useMarkUnresolvedIncident,
  useCloseIncident,
  useReopenIncident,
  useCloneIncident,
  useResubmitIncident,
  useCancelIncident,
  useSubmitSolution,
  useDirectionValidate,
  useDirectionReject,
  useIncidentTypes,
} from "../../../../hooks/incident/useIncidents/useIncidents";
import { useAuth } from "../../../../hooks/auth/useAuth";
import { useServices } from "../../../../hooks/service/useServices";
import { useAssignableUsers } from "../../../../hooks/user/useUsers/useUsers";
import {
  useUploadAttachment,
  useIncidentAttachments,
} from "../../../../hooks/document/useDocuments";
import {
  ACCEPTED_ATTACHMENT_ACCEPT,
  MAX_ATTACHMENT_SIZE_BYTES,
  isAcceptedAttachment,
  isAttachmentSizeValid,
} from "../../../../utils/attachments/attachmentValidation";

const { Text } = Typography;
import {
  IncidentStatus,
  WorkflowAction,
  IncidentCause,
} from "../../../../api/incident/enums/enums";
import type {
  IncidentActorRole,
  IncidentValidatorRole,
} from "../../../../api/incident/types";
import {
  PERMISSIONS,
  hasAnyPermission,
} from "../../../../utils/permissions/permissions";
import {
  incidentNavigation,
  incidentPathIdentifier,
} from "../../../../utils/navigation/incidents/incidents";
import styles from "./WorkflowActions.module.scss";

const { TextArea } = Input;

const STATUS_ALLOWED_ACTIONS = new Map<string, WorkflowAction[]>([
  [
    IncidentStatus.OPEN,
    [
      WorkflowAction.VALIDATE,
      WorkflowAction.REJECT,
      WorkflowAction.ASSIGN,
      WorkflowAction.SELF_ASSIGN,
    ],
  ],
  [
    IncidentStatus.PENDING_VALIDATION,
    [WorkflowAction.VALIDATE, WorkflowAction.REJECT],
  ],
  [
    IncidentStatus.VALIDATED,
    [
      WorkflowAction.TRANSFER,
      WorkflowAction.ASSIGN,
      WorkflowAction.SELF_ASSIGN,
    ],
  ],
  [
    IncidentStatus.TRANSFERRED,
    [
      WorkflowAction.TRANSFER,
      WorkflowAction.ASSIGN,
      WorkflowAction.SELF_ASSIGN,
    ],
  ],
  [
    IncidentStatus.ASSIGNED,
    [
      WorkflowAction.TRANSFER,
      WorkflowAction.ASSIGN,
      WorkflowAction.SELF_ASSIGN,
      // START (traitement direct) et SUBMIT_SOLUTION (soumission pour validation
      // prealable) sont mutuellement exclusifs : le filtre selectionne l'un ou l'autre
      // selon requiresDirectionValidation du type d'incident.
      WorkflowAction.START,
      WorkflowAction.SUBMIT_SOLUTION,
      WorkflowAction.CANCEL,
    ],
  ],
  [
    IncidentStatus.IN_PROGRESS,
    [
      WorkflowAction.TRANSFER,
      WorkflowAction.BLOCK,
      WorkflowAction.TREAT,
      WorkflowAction.ASSIGN,
      WorkflowAction.SELF_ASSIGN,
    ],
  ],
  [
    IncidentStatus.TREATED,
    [WorkflowAction.RESOLVE, WorkflowAction.MARK_UNRESOLVED],
  ],
  [IncidentStatus.BLOCKED, [WorkflowAction.TRANSFER, WorkflowAction.RESUME]],
  [IncidentStatus.RESOLVED, [WorkflowAction.CLOSE, WorkflowAction.REOPEN]],
  [IncidentStatus.CLOSED, [WorkflowAction.CLONE]],
  [IncidentStatus.REOPENED, [WorkflowAction.RESUBMIT]],
  [IncidentStatus.REJECTED, [WorkflowAction.REOPEN, WorkflowAction.CLONE]],
  [IncidentStatus.CANCELLED, [WorkflowAction.CLONE]],
  [
    IncidentStatus.UNRESOLVED_PROLONGED_WAIT,
    // L'annulation (= infirmation) passe par CANCELLATION_ALLOWED_STATUSES, comme
    // partout ailleurs : elle n'a pas sa place dans cette table.
    [
      WorkflowAction.REQUEST_CONFIRMATION,
      WorkflowAction.CONFIRM_RELEVANCE,
      WorkflowAction.REOPEN,
    ],
  ],
  [
    IncidentStatus.DRAFT,
    [
      WorkflowAction.DIRECTION_VALIDATE,
      WorkflowAction.DIRECTION_REJECT,
      WorkflowAction.SUBMIT_SOLUTION,
    ],
  ],
]);

// Annuler et rejeter sont deux portes disjointes : avant validation on rejette,
// apres on annule. Miroir de IncidentServiceImpl.CANCELLABLE_STATUSES.
const CANCELLATION_ALLOWED_STATUSES = new Set<string>([
  IncidentStatus.VALIDATED,
  IncidentStatus.TRANSFERRED,
  IncidentStatus.ASSIGNED,
  IncidentStatus.BLOCKED,
  IncidentStatus.UNRESOLVED_PROLONGED_WAIT,
]);

// Style visuel par action
const ACTION_TONE = new Map<
  WorkflowAction,
  "success" | "danger" | "routing" | "neutral" | "primary"
>([
  [WorkflowAction.VALIDATE, "primary"],
  [WorkflowAction.REJECT, "danger"],
  [WorkflowAction.TRANSFER, "routing"],
  [WorkflowAction.ASSIGN, "neutral"],
  [WorkflowAction.SELF_ASSIGN, "primary"],
  [WorkflowAction.START, "primary"],
  [WorkflowAction.SUBMIT_SOLUTION, "primary"],
  [WorkflowAction.DIRECTION_VALIDATE, "primary"],
  [WorkflowAction.DIRECTION_REJECT, "danger"],
  [WorkflowAction.BLOCK, "danger"],
  [WorkflowAction.RESUME, "primary"],
  [WorkflowAction.REQUEST_CONFIRMATION, "routing"],
  [WorkflowAction.CONFIRM_RELEVANCE, "primary"],
  [WorkflowAction.TREAT, "primary"],
  [WorkflowAction.RESOLVE, "primary"],
  [WorkflowAction.MARK_UNRESOLVED, "danger"],
  [WorkflowAction.CLOSE, "neutral"],
  [WorkflowAction.REOPEN, "routing"],
  [WorkflowAction.RESUBMIT, "primary"],
  [WorkflowAction.CLONE, "neutral"],
  [WorkflowAction.CANCEL, "danger"],
]);

const ACTION_ICON = new Map<WorkflowAction, ReactNode>([
  [WorkflowAction.VALIDATE, <SafetyCertificateOutlined aria-hidden="true" />],
  [WorkflowAction.REJECT, <CloseCircleOutlined aria-hidden="true" />],
  [WorkflowAction.TRANSFER, <SwapOutlined aria-hidden="true" />],
  [WorkflowAction.ASSIGN, <UserSwitchOutlined aria-hidden="true" />],
  [WorkflowAction.SELF_ASSIGN, <UserAddOutlined aria-hidden="true" />],
  [WorkflowAction.START, <PlayCircleOutlined aria-hidden="true" />],
  [WorkflowAction.SUBMIT_SOLUTION, <PlayCircleOutlined aria-hidden="true" />],
  [
    WorkflowAction.DIRECTION_VALIDATE,
    <CheckCircleOutlined aria-hidden="true" />,
  ],
  [WorkflowAction.DIRECTION_REJECT, <CloseCircleOutlined aria-hidden="true" />],
  [WorkflowAction.BLOCK, <PauseCircleOutlined aria-hidden="true" />],
  [WorkflowAction.RESUME, <PlayCircleOutlined aria-hidden="true" />],
  [
    WorkflowAction.REQUEST_CONFIRMATION,
    <QuestionCircleOutlined aria-hidden="true" />,
  ],
  [
    WorkflowAction.CONFIRM_RELEVANCE,
    <CheckCircleOutlined aria-hidden="true" />,
  ],
  [WorkflowAction.TREAT, <CheckCircleOutlined aria-hidden="true" />],
  [WorkflowAction.RESOLVE, <CheckCircleOutlined aria-hidden="true" />],
  [WorkflowAction.MARK_UNRESOLVED, <CloseCircleOutlined aria-hidden="true" />],
  [WorkflowAction.CLOSE, <CheckCircleOutlined aria-hidden="true" />],
  [WorkflowAction.REOPEN, <ReloadOutlined aria-hidden="true" />],
  [WorkflowAction.RESUBMIT, <CheckCircleOutlined aria-hidden="true" />],
  [WorkflowAction.CLONE, <CopyOutlined aria-hidden="true" />],
  [WorkflowAction.CANCEL, <CloseCircleOutlined aria-hidden="true" />],
]);

// Filtre les options disponibles pour workflow actions.
const filterOptionByLabel = (input: string, option?: { label?: unknown }) =>
  (option?.label as string)?.toLowerCase().includes(input.toLowerCase());

const SERVICE_TARGET_PREFIX = "service:";
const AGENCY_TARGET_PREFIX = "agency:";

// Definit les proprietes attendues par le composant WorkflowActions.
interface WorkflowActionsProps {
  incidentId: string;
  status?: string;
  currentAssigneeId?: string | null;
  existingClosureDescription?: string | null;
  currentServiceId?: string | null;
  defaultServiceId?: string | null;
  defaultUserId?: string | null;
  creatorId?: string | null;
  agencyId?: string | null;
  agencyName?: string | null;
  creatorServiceId?: string | null;
  currentAgencyTargetId?: string | null;
  treaterRoles?: IncidentActorRole[];
  resolverRoles?: IncidentActorRole[];
  closerRoles?: IncidentActorRole[];
  reopenerRoles?: IncidentActorRole[];
  requiresCauseAnalysis?: boolean;
  existingCause?: IncidentCause | null;
  existingCauseDetail?: string | null;
  isReopenExpired?: boolean;
  isMaxReopenReached?: boolean;
  reopenRejectorId?: string | null;
  /** Verdict de portée de validation calculé côté back (source de vérité du bouton Valider). */
  canValidate?: boolean;
  /** Verdict de portée d'annulation calculé côté back (source de vérité du bouton Annuler). */
  canCancel?: boolean;
  /** Verdicts de portée des deux étapes de la confirmation d'actualité (calculés côté back). */
  canRequestConfirmation?: boolean;
  canConfirmRelevance?: boolean;
  /** Qui est attendu pour confirmer l'actualité, portée résolue côté back. */
  relevanceResponderRole?: IncidentValidatorRole;
  relevanceResponderTarget?: string;
  chosenTargetServiceId?: string | null;
  requiresDirectionValidation?: boolean;
  directionRejectionReason?: string | null;
  proposedSolution?: string | null;
  incidentTitle?: string | null;
  incidentDescription?: string | null;
  currentTypeId?: string | null;
}

// Type les valeurs action key utilisees par l'interface.
type ActionKey = WorkflowAction;

// Definit les proprietes attendues par le composant ActionButton.
interface ActionButtonProps {
  actionKey: ActionKey;
  label: string;
  icon: ReactNode;
  tone: "success" | "danger" | "routing" | "neutral" | "primary";
  disabled: boolean;
  onOpen: (key: ActionKey) => void;
}

// Rend le composant ActionButton pour l'interface workflow actions.
const ActionButton = memo(
  ({ actionKey, label, icon, tone, disabled, onOpen }: ActionButtonProps) => {
    // Traite le clic utilisateur.
    const handleClick = useCallback(
      () => onOpen(actionKey),
      [onOpen, actionKey],
    );
    return (
      <Button
        type="text"
        icon={icon}
        onClick={handleClick}
        disabled={disabled}
        aria-label={label}
        className={styles.actionButton}
        data-tone={tone}
      >
        {label}
      </Button>
    );
  },
);

ActionButton.displayName = "ActionButton";

// Rend le composant WorkflowActions.
const WorkflowActions = memo(
  ({
    incidentId,
    status,
    currentAssigneeId,
    existingClosureDescription,
    currentServiceId,
    defaultServiceId,
    defaultUserId,
    creatorId,
    agencyId,
    agencyName,
    creatorServiceId,
    currentAgencyTargetId,
    treaterRoles,
    resolverRoles,
    closerRoles,
    reopenerRoles,
    requiresCauseAnalysis = false,
    existingCause,
    existingCauseDetail,
    isReopenExpired = false,
    isMaxReopenReached = false,
    reopenRejectorId,
    canValidate = false,
    canCancel = false,
    canRequestConfirmation = false,
    canConfirmRelevance = false,
    relevanceResponderRole,
    relevanceResponderTarget,
    chosenTargetServiceId,
    requiresDirectionValidation = false,
    directionRejectionReason,
    proposedSolution,
    incidentTitle,
    incidentDescription,
    currentTypeId,
  }: WorkflowActionsProps) => {
    const { t } = useTranslation();
    const { message } = App.useApp();
    const navigate = useNavigate();
    const { hasPermission, user: currentUser } = useAuth();
    const requiresTransferReason = hasPermission(
      PERMISSIONS.INCIDENT.TRANSFER_WITH_REASON,
    );
    const [openModal, setOpenModal] = useState<ActionKey | null>(null);
    const queryClient = useQueryClient();
    const [attachmentFiles, setAttachmentFiles] = useState<UploadFile[]>([]);
    const [pendingDeleteAttachmentIds, setPendingDeleteAttachmentIds] =
      useState<string[]>([]);
    const [form] = Form.useForm();
    const selectedTargetServiceId = Form.useWatch("targetServiceId", form);

    const { mutateAsync: uploadAttachment, isPending: isUploadingAttachment } =
      useUploadAttachment(incidentId);
    const { data: rawIncidentAttachments = [] } = useIncidentAttachments(
      openModal === WorkflowAction.SUBMIT_SOLUTION ? incidentId : undefined,
    );
    // Seules les pièces jointes de la solution (et non celles de déclaration ou de commentaires)
    // doivent figurer dans la gestion des pièces jointes existantes de la solution.
    const incidentAttachments = useMemo(
      () =>
        rawIncidentAttachments.filter(
          (att) => !att.commentId && att.category === "SOLUTION",
        ),
      [rawIncidentAttachments],
    );

    const togglePendingDeleteAttachment = useCallback((attId: string) => {
      setPendingDeleteAttachmentIds((prev) =>
        prev.includes(attId)
          ? prev.filter((id) => id !== attId)
          : [...prev, attId],
      );
    }, []);

    const { mutate: validate, isPending: isValidating } = useValidateIncident();
    const { mutate: reject, isPending: isRejecting } = useRejectIncident();
    const { mutate: transfer, isPending: isTransferring } =
      useTransferIncident();
    const { mutate: assign, isPending: isAssigning } = useAssignIncident();
    const { mutate: start, isPending: isStarting } = useStartIncident();
    const { mutate: block, isPending: isBlocking } = useBlockIncident();
    const { mutate: resume, isPending: isResuming } = useResumeIncident();
    const { mutate: requestConfirmation, isPending: isRequestingConfirmation } =
      useRequestConfirmation();
    const { mutate: confirmRelevance, isPending: isConfirmingRelevance } =
      useConfirmRelevance();
    const { mutate: treat, isPending: isTreating } = useTreatIncident();
    const { mutate: resolve, isPending: isResolving } = useResolveIncident();
    const { mutate: markUnresolved, isPending: isMarkingUnresolved } =
      useMarkUnresolvedIncident();
    const { mutate: close, isPending: isClosing } = useCloseIncident();
    const { mutate: reopen, isPending: isReopening } = useReopenIncident();
    const { mutate: clone, isPending: isCloning } = useCloneIncident();
    const { mutate: resubmit, isPending: isResubmitting } =
      useResubmitIncident();
    const { mutate: cancel, isPending: isCanceling } = useCancelIncident();
    const { mutate: submitSolution, isPending: isSubmittingSolution } =
      useSubmitSolution();
    const { mutate: directionValidate, isPending: isDirectionValidating } =
      useDirectionValidate();
    const { mutate: directionReject, isPending: isDirectionRejecting } =
      useDirectionReject();

    const needsServices =
      openModal === WorkflowAction.TRANSFER ||
      openModal === WorkflowAction.VALIDATE;
    const { data: services = [] } = useServices({ enabled: needsServices });
    const { data: incidentTypes = [] } = useIncidentTypes();

    // Destination de transfert choisie : sert a proposer les types du service cible.
    const selectedTransferTarget = Form.useWatch("transferTarget", form);
    const transferTargetServiceId = useMemo(() => {
      if (
        typeof selectedTransferTarget !== "string" ||
        !selectedTransferTarget.startsWith(SERVICE_TARGET_PREFIX)
      ) {
        return null;
      }
      return selectedTransferTarget.slice(SERVICE_TARGET_PREFIX.length);
    }, [selectedTransferTarget]);

    // Reclassification : propose les types d'incident du service cible (ou tous les types actifs par defaut).
    const reclassifyTypeOptions = useMemo(() => {
      if (!transferTargetServiceId) return [];
      const serviceSpecific = incidentTypes.filter(
        (type) =>
          type.isActive !== false &&
          type.defaultTargetService?.id === transferTargetServiceId,
      );
      const targetList =
        serviceSpecific.length > 0
          ? serviceSpecific
          : incidentTypes.filter((type) => type.isActive !== false);

      return targetList.map((type) => ({
        label: type.displayName,
        value: type.id,
      }));
    }, [incidentTypes, transferTargetServiceId]);

    // Pre-remplit le type avec le type courant s'il est present dans les options,
    // ou avec le premier type du service cible par defaut.
    useEffect(() => {
      if (openModal !== WorkflowAction.TRANSFER || !transferTargetServiceId)
        return;
      const keepsCurrentType = reclassifyTypeOptions.some(
        (option) => option.value === currentTypeId,
      );
      form.setFieldValue(
        "newTypeId",
        keepsCurrentType
          ? currentTypeId
          : (reclassifyTypeOptions[0]?.value ?? currentTypeId),
      );
    }, [
      openModal,
      transferTargetServiceId,
      reclassifyTypeOptions,
      currentTypeId,
      form,
    ]);
    const needsUsers =
      openModal === WorkflowAction.ASSIGN ||
      openModal === WorkflowAction.VALIDATE;
    const assignableServiceScope =
      openModal === WorkflowAction.VALIDATE
        ? selectedTargetServiceId
        : currentServiceId;

    const { data: users = [] } = useAssignableUsers(
      {
        serviceId: assignableServiceScope,
      },
      { enabled: needsUsers },
    );

    const serviceOptions = useMemo(
      () =>
        services
          .filter((s) => !currentServiceId || s.id !== currentServiceId)
          .map((s) => ({ label: s.name, value: s.id })),
      [services, currentServiceId],
    );

    const transferOptions = useMemo(() => {
      const groups = [
        {
          label: t("incidents.workflow.modals.transfer.services_group"),
          options: serviceOptions.map((service) => ({
            ...service,
            value: `${SERVICE_TARGET_PREFIX}${service.value}`,
          })),
        },
      ];
      if (agencyId && !creatorServiceId && currentAgencyTargetId !== agencyId) {
        groups.push({
          label: t("incidents.workflow.modals.transfer.agency_group"),
          options: [
            {
              label: agencyName ?? agencyId,
              value: `${AGENCY_TARGET_PREFIX}${agencyId}`,
            },
          ],
        });
      }
      return groups.filter((group) => group.options.length > 0);
    }, [
      agencyId,
      agencyName,
      creatorServiceId,
      currentAgencyTargetId,
      serviceOptions,
      t,
    ]);

    const excludedAssigneeIds = useMemo(() => {
      const ids = new Set<string>();
      if (currentUser?.id) ids.add(currentUser.id);
      if (currentAssigneeId) ids.add(currentAssigneeId);
      return ids;
    }, [currentUser, currentAssigneeId]);

    // Construit les options de selection affichables.
    const userOptions = useMemo(
      () =>
        users
          .filter((u) => !excludedAssigneeIds.has(u.id))
          .map((u) => ({
            label: `${u.firstName} ${u.lastName} (${u.username})`,
            value: u.id,
          })),
      [users, excludedAssigneeIds],
    );

    // Options de cause pour la modale de resolution (analyse de cause).
    const causeOptions = useMemo(
      () =>
        Object.values(IncidentCause).map((c) => ({
          value: c,
          label: t(`incidents.cause_values.${c}`, c),
        })),
      [t],
    );

    const requiresValidationRouting =
      !defaultServiceId && !defaultUserId && !currentAssigneeId;

    useEffect(() => {
      if (openModal === WorkflowAction.VALIDATE) {
        form.setFieldValue("targetUserId", undefined);
      }
    }, [form, openModal, selectedTargetServiceId]);

    const attachmentBatchRef = useRef(false);
    const [isAttachmentBatchPending, setAttachmentBatchPending] = useState(false);
    const isPending =
      isAttachmentBatchPending ||
      isValidating ||
      isRejecting ||
      isTransferring ||
      isAssigning ||
      isStarting ||
      isBlocking ||
      isResuming ||
      isRequestingConfirmation ||
      isConfirmingRelevance ||
      isTreating ||
      isResolving ||
      isMarkingUnresolved ||
      isClosing ||
      isReopening ||
      isCanceling ||
      isCloning ||
      isResubmitting ||
      isUploadingAttachment ||
      isSubmittingSolution ||
      isDirectionValidating ||
      isDirectionRejecting;

    const openAction = useCallback(
      (action: ActionKey) => {
        form.resetFields();
        setAttachmentFiles([]);
        if (action === WorkflowAction.CLOSE && existingClosureDescription) {
          form.setFieldValue("closureDescription", existingClosureDescription);
        }
        // Traitement : on prerempli la cause deja saisie (par le createur) pour que
        // l'agent traitant puisse la confirmer ou la modifier plutot que de repartir a vide.
        if (action === WorkflowAction.TREAT) {
          if (existingCause) form.setFieldValue("cause", existingCause);
          if (existingCauseDetail)
            form.setFieldValue("causeDetail", existingCauseDetail);
        }
        if (
          action === WorkflowAction.VALIDATE &&
          requiresValidationRouting &&
          chosenTargetServiceId
        ) {
          form.setFieldValue("targetServiceId", chosenTargetServiceId);
        }
        if (
          action === WorkflowAction.TRANSFER &&
          defaultServiceId &&
          defaultServiceId !== currentServiceId
        ) {
          form.setFieldValue(
            "transferTarget",
            `${SERVICE_TARGET_PREFIX}${defaultServiceId}`,
          );
        }
        setOpenModal(action);
        setPendingDeleteAttachmentIds([]);
      },
      [
        form,
        existingClosureDescription,
        defaultServiceId,
        currentServiceId,
        existingCause,
        existingCauseDetail,
        requiresValidationRouting,
        chosenTargetServiceId,
      ],
    );

    const closeModal = useCallback(() => {
      setOpenModal(null);
      setAttachmentFiles([]);
      setPendingDeleteAttachmentIds([]);
      form.resetFields();
    }, [form]);

    const runWithAttachments = useCallback(
      async (
        run: () => void,
        category:
          | "SOLUTION"
          | "TREATMENT"
          | "RESOLUTION"
          | "UNRESOLVED"
          | "CLOSURE"
          | "CANCELLATION"
          | string,
      ) => {
        if (attachmentBatchRef.current) return;
        attachmentBatchRef.current = true;
        setAttachmentBatchPending(true);
        try {
          if (pendingDeleteAttachmentIds.length > 0) {
            for (const attId of pendingDeleteAttachmentIds) {
              await documentApi.delete(attId);
              setPendingDeleteAttachmentIds((ids) => ids.filter((id) => id !== attId));
            }
            queryClient.invalidateQueries({
              queryKey: QUERY_KEYS.DOCUMENTS.BY_INCIDENT(incidentId),
            });
          }

          const files = attachmentFiles
            .map((item) => item.originFileObj as File | undefined)
            .filter((file): file is File => Boolean(file));
          for (const file of files) {
            await uploadAttachment({ file, category });
            setAttachmentFiles((items) => items.filter((item) => item.originFileObj !== file));
          }
          run();
        } catch (error) {
          message.error(getApiErrorMessage(error));
        } finally {
          attachmentBatchRef.current = false;
          setAttachmentBatchPending(false);
        }
      },
      [
        attachmentFiles,
        pendingDeleteAttachmentIds,
        uploadAttachment,
        queryClient,
        incidentId,
        message,
      ],
    );

    // Traite la validation du formulaire.
    const handleSubmit = useCallback(() => {
      form
        .validateFields()
        .then((values) => {
          const onSuccess = () => closeModal();

          switch (openModal) {
            case WorkflowAction.VALIDATE:
              validate(
                {
                  id: incidentId,
                  data: {
                    comment: values.comment,
                    targetServiceId: values.targetServiceId,
                    targetUserId: values.targetUserId,
                  },
                },
                { onSuccess },
              );
              break;
            case WorkflowAction.REJECT:
              reject(
                {
                  id: incidentId,
                  data: { reason: values.reason, comment: values.comment },
                },
                { onSuccess },
              );
              break;
            case WorkflowAction.TRANSFER: {
              const transferTarget = values.transferTarget as string;
              const targetsAgency =
                transferTarget.startsWith(AGENCY_TARGET_PREFIX);
              const targetId = transferTarget.slice(
                targetsAgency
                  ? AGENCY_TARGET_PREFIX.length
                  : SERVICE_TARGET_PREFIX.length,
              );
              transfer(
                {
                  id: incidentId,
                  data: {
                    ...(targetsAgency
                      ? { targetAgencyId: targetId }
                      : {
                          targetServiceId: targetId,
                          ...(values.newTypeId
                            ? { newTypeId: values.newTypeId as string }
                            : {}),
                        }),
                    reason: values.reason,
                    comment: values.comment,
                  },
                },
                {
                  onSuccess: () => {
                    closeModal();
                    incidentNavigation.navigateToIncidents(navigate, {
                      replace: true,
                    });
                  },
                },
              );
              break;
            }
            case WorkflowAction.ASSIGN:
              assign(
                {
                  id: incidentId,
                  data: {
                    assignedTo: values.assignedTo,
                    comment: values.comment,
                  },
                },
                { onSuccess },
              );
              break;
            case WorkflowAction.SELF_ASSIGN:
              if (currentUser?.id) {
                assign(
                  {
                    id: incidentId,
                    data: {
                      assignedTo: currentUser.id,
                      comment: values.comment,
                    },
                  },
                  { onSuccess },
                );
              }
              break;
            case WorkflowAction.START:
              start(
                {
                  id: incidentId,
                  data: {
                    comment: values.comment,
                    estimatedResolutionHours: values.estimatedResolutionHours,
                  },
                },
                { onSuccess },
              );
              break;
            case WorkflowAction.BLOCK:
              block(
                {
                  id: incidentId,
                  data: { reason: values.reason, comment: values.comment },
                },
                { onSuccess },
              );
              break;
            case WorkflowAction.RESUME:
              resume(
                { id: incidentId, data: { comment: values.comment } },
                { onSuccess },
              );
              break;
            case WorkflowAction.REQUEST_CONFIRMATION:
              requestConfirmation(
                { id: incidentId, data: { comment: values.comment } },
                { onSuccess },
              );
              break;
            case WorkflowAction.CONFIRM_RELEVANCE:
              confirmRelevance(
                { id: incidentId, data: { comment: values.comment } },
                { onSuccess },
              );
              break;
            case WorkflowAction.TREAT:
              runWithAttachments(
                () =>
                  treat(
                    {
                      id: incidentId,
                      data: {
                        treatmentDescription: values.treatmentDescription,
                        cause: values.cause,
                        causeDetail: values.causeDetail,
                      },
                    },
                    { onSuccess },
                  ),
                "TREATMENT",
              );
              break;
            case WorkflowAction.RESOLVE:
              runWithAttachments(
                () =>
                  resolve(
                    {
                      id: incidentId,
                      data: { resolutionNote: values.resolutionNote },
                    },
                    { onSuccess },
                  ),
                "RESOLUTION",
              );
              break;
            case WorkflowAction.MARK_UNRESOLVED:
              runWithAttachments(
                () =>
                  markUnresolved(
                    { id: incidentId, data: { reason: values.reason } },
                    { onSuccess },
                  ),
                // Justificatifs d'un refus de resolution : ce n'est pas le compte
                // rendu de resolution, ils ne doivent pas s'y melanger.
                "UNRESOLVED",
              );
              break;
            case WorkflowAction.CLOSE:
              runWithAttachments(
                () =>
                  close(
                    {
                      id: incidentId,
                      data: {
                        closureDescription: values.closureDescription,
                        comment: values.comment,
                      },
                    },
                    { onSuccess },
                  ),
                "CLOSURE",
              );
              break;
            case WorkflowAction.REOPEN:
              reopen(
                {
                  id: incidentId,
                  data: { reason: values.reason, comment: values.comment },
                },
                { onSuccess },
              );
              break;
            case WorkflowAction.RESUBMIT:
              resubmit(
                { id: incidentId, data: { comment: values.comment } },
                {
                  onSuccess: () => {
                    closeModal();
                    incidentNavigation.navigateToIncidents(navigate);
                  },
                },
              );
              break;
            case WorkflowAction.CLONE:
              clone(
                { id: incidentId },
                {
                  onSuccess: (response) => {
                    closeModal();
                    if (response && response.id) {
                      incidentNavigation.navigateToIncidentDetail(
                        navigate,
                        incidentPathIdentifier(response),
                      );
                    } else {
                      incidentNavigation.navigateToIncidents(navigate);
                    }
                  },
                },
              );
              break;
            case WorkflowAction.CANCEL:
              runWithAttachments(
                () =>
                  cancel(
                    { id: incidentId, data: { reason: values.reason } },
                    { onSuccess },
                  ),
                "CANCELLATION",
              );
              break;
            case WorkflowAction.SUBMIT_SOLUTION:
              runWithAttachments(
                () =>
                  submitSolution(
                    {
                      id: incidentId,
                      data: {
                        proposedSolution: values.proposedSolution,
                        estimatedResolutionHours:
                          values.estimatedResolutionHours,
                      },
                    },
                    { onSuccess },
                  ),
                "SOLUTION",
              );
              break;
            case WorkflowAction.DIRECTION_VALIDATE:
              directionValidate(
                { id: incidentId, data: { comment: values.comment } },
                { onSuccess },
              );
              break;
            case WorkflowAction.DIRECTION_REJECT:
              directionReject(
                {
                  id: incidentId,
                  data: { rejectionReason: values.rejectionReason },
                },
                { onSuccess },
              );
              break;
          }
        })
        .catch(() => {});
    }, [
      form,
      openModal,
      incidentId,
      currentUser,
      navigate,
      validate,
      reject,
      transfer,
      assign,
      start,
      block,
      resume,
      requestConfirmation,
      confirmRelevance,
      treat,
      resolve,
      markUnresolved,
      close,
      reopen,
      clone,
      resubmit,
      cancel,
      submitSolution,
      directionValidate,
      directionReject,
      closeModal,
      runWithAttachments,
    ]);

    // Nomme celui de qui la confirmation est attendue plutot que « l'entité source ».
    const relevanceResponderLabel = useMemo(() => {
      if (!relevanceResponderRole) {
        return t(
          "incidents.workflow.modals.request_confirmation.responder_fallback",
        );
      }
      return relevanceResponderTarget
        ? t(`incidents.validator_roles.${relevanceResponderRole}_named`, {
            name: relevanceResponderTarget,
          })
        : t(`incidents.validator_roles.${relevanceResponderRole}`);
    }, [relevanceResponderRole, relevanceResponderTarget, t]);

    // En attente prolongee, « Annuler » se dit « Plus d'actualite ».
    const isRelevanceAnswer =
      status === IncidentStatus.UNRESOLVED_PROLONGED_WAIT;
    const cancelKeyPrefix = isRelevanceAnswer
      ? "incidents.workflow.no_longer_relevant"
      : "incidents.workflow.cancel";

    const allowedByStatus: Set<WorkflowAction> = useMemo(() => {
      if (!status) return new Set<WorkflowAction>();
      return new Set(STATUS_ALLOWED_ACTIONS.get(status) ?? []);
    }, [status]);

    const allActions = useMemo(
      () => [
        {
          key: WorkflowAction.VALIDATE,
          permissions: [PERMISSIONS.INCIDENT.VALIDATE],
          label: t("incidents.workflow.validate"),
        },
        {
          key: WorkflowAction.REJECT,
          permissions: [PERMISSIONS.INCIDENT.REJECT],
          label: t("incidents.workflow.reject"),
        },
        {
          key: WorkflowAction.TRANSFER,
          permissions: [PERMISSIONS.INCIDENT.TRANSFER],
          label: t("incidents.workflow.transfer"),
        },
        {
          key: WorkflowAction.ASSIGN,
          permissions: [PERMISSIONS.INCIDENT.ASSIGN],
          label: t("incidents.workflow.assign"),
        },
        {
          key: WorkflowAction.SELF_ASSIGN,
          permissions: [
            PERMISSIONS.INCIDENT.TREAT,
            PERMISSIONS.INCIDENT.RESOLVE,
          ],
          label: t("incidents.workflow.self_assign"),
        },
        {
          key: WorkflowAction.START,
          permissions: [
            PERMISSIONS.INCIDENT.TREAT,
            PERMISSIONS.INCIDENT.RESOLVE,
          ],
          label: t("incidents.workflow.start"),
        },
        {
          key: WorkflowAction.BLOCK,
          permissions: [
            PERMISSIONS.INCIDENT.TREAT,
            PERMISSIONS.INCIDENT.RESOLVE,
          ],
          label: t("incidents.workflow.block"),
        },
        {
          key: WorkflowAction.RESUME,
          permissions: [
            PERMISSIONS.INCIDENT.TREAT,
            PERMISSIONS.INCIDENT.RESOLVE,
          ],
          label: t("incidents.workflow.resume"),
        },
        {
          key: WorkflowAction.REQUEST_CONFIRMATION,
          permissions: [PERMISSIONS.INCIDENT.TREAT],
          label: t("incidents.workflow.request_confirmation"),
        },
        {
          key: WorkflowAction.CONFIRM_RELEVANCE,
          permissions: [PERMISSIONS.INCIDENT.VALIDATE],
          label: t("incidents.workflow.confirm_relevance"),
        },
        {
          key: WorkflowAction.TREAT,
          permissions: [PERMISSIONS.INCIDENT.TREAT],
          label: t("incidents.workflow.treat"),
        },
        {
          key: WorkflowAction.RESOLVE,
          permissions: [PERMISSIONS.INCIDENT.RESOLVE],
          label: t("incidents.workflow.resolve"),
        },
        {
          key: WorkflowAction.MARK_UNRESOLVED,
          permissions: [PERMISSIONS.INCIDENT.RESOLVE],
          label: t("incidents.workflow.mark_unresolved"),
        },
        {
          key: WorkflowAction.CLOSE,
          permissions: [PERMISSIONS.INCIDENT.CLOSE],
          label: t("incidents.workflow.close"),
        },
        {
          key: WorkflowAction.REOPEN,
          permissions: [PERMISSIONS.INCIDENT.REOPEN],
          label: t("incidents.workflow.reopen"),
        },
        {
          key: WorkflowAction.RESUBMIT,
          permissions: [PERMISSIONS.INCIDENT.CREATE],
          label: t("incidents.workflow.resubmit"),
        },
        {
          key: WorkflowAction.CLONE,
          permissions: [PERMISSIONS.INCIDENT.CREATE],
          label: t("incidents.workflow.clone"),
        },
        {
          key: WorkflowAction.CANCEL,
          permissions: [PERMISSIONS.INCIDENT.CANCEL],
          label: t(cancelKeyPrefix),
        },
        {
          key: WorkflowAction.SUBMIT_SOLUTION,
          permissions: [
            PERMISSIONS.INCIDENT.TREAT,
            PERMISSIONS.INCIDENT.RESOLVE,
          ],
          // Premiere soumission : « Démarrer le traitement » (ouvre le modal de solution).
          // Apres un rejet de la Direction : « Demander une nouvelle validation ».
          label: directionRejectionReason
            ? t("incidents.workflow.request_new_validation")
            : t("incidents.workflow.start"),
        },
        {
          key: WorkflowAction.DIRECTION_VALIDATE,
          permissions: [PERMISSIONS.INCIDENT.VALIDATION_DIRECTION],
          label: t("incidents.workflow.direction_validate"),
        },
        {
          key: WorkflowAction.DIRECTION_REJECT,
          permissions: [PERMISSIONS.INCIDENT.VALIDATION_DIRECTION],
          label: t("incidents.workflow.direction_reject"),
        },
      ],
      [t, directionRejectionReason, cancelKeyPrefix],
    );

    const currentUserIsAssignee = Boolean(
      currentAssigneeId && currentAssigneeId === currentUser?.id,
    );

    // Seul le createur peut re-soumettre un incident rouvert (aligne sur
    // IncidentWorkflowGuard.assertCreator, sans contournement VIEW_ALL).
    const currentUserIsCreator = Boolean(
      creatorId && creatorId === currentUser?.id,
    );

    const currentUserIsRejector = Boolean(
      reopenRejectorId && reopenRejectorId === currentUser?.id,
    );

    const canManageProcessing = useMemo(() => {
      if (!currentUser || currentUser.isActive === false) return false;
      if (hasPermission(PERMISSIONS.INCIDENT.VIEW_ALL)) return true; // Les admins globaux peuvent tout gerer

      const targetService = currentServiceId || defaultServiceId;
      if (targetService) {
        // Doit etre dans le service cible ou manager de ce service
        return (
          currentUser.serviceId === targetService ||
          currentUser.managedServiceIds?.includes(targetService)
        );
      }
      // Si aucun service n'est cible, il faut etre dans la meme agence
      return agencyId ? currentUser.agencyId === agencyId : false;
    }, [
      currentUser,
      hasPermission,
      currentServiceId,
      defaultServiceId,
      agencyId,
    ]);

    // Garde-fou d'assignation : ne montrer les boutons ASSIGN / SELF_ASSIGN que
    // si une assignation est REELLEMENT possible. Le backend n'accepte qu'un
    // assigne du service traitant (ou qui le gere) doté de INCIDENT_TREAT/RESOLVE.
    // On récupère donc les assignables du service traitant pour ce même périmètre
    // et on masque le bouton quand aucune action n'aboutirait — y compris pour un
    // admin qui n'appartient pas au service désigné.
    const canAssignByStatus =
      allowedByStatus.has(WorkflowAction.ASSIGN) ||
      allowedByStatus.has(WorkflowAction.SELF_ASSIGN);
    const { data: serviceAssignableUsers = [] } = useAssignableUsers(
      { serviceId: currentServiceId ?? undefined },
      {
        enabled:
          Boolean(currentServiceId) && canManageProcessing && canAssignByStatus,
      },
    );

    // Existe-t-il un assigné valide AUTRE que soi / l'assigné courant ?
    const hasAssignableTarget = useMemo(
      () =>
        serviceAssignableUsers.some(
          (u) => u.id !== currentUser?.id && u.id !== currentAssigneeId,
        ),
      [serviceAssignableUsers, currentUser, currentAssigneeId],
    );

    // L'utilisateur courant peut-il se prendre l'incident ? Il doit traiter/résoudre
    // ET appartenir au service traitant (ou le gérer). Un admin hors du service ne
    // remplit pas ce critère : pas de bouton « Prendre en charge ».
    const currentUserCanSelfAssign = useMemo(() => {
      if (!currentUser?.id || !currentServiceId) return false;
      if (
        !hasAnyPermission(hasPermission, [
          PERMISSIONS.INCIDENT.TREAT,
          PERMISSIONS.INCIDENT.RESOLVE,
        ])
      ) {
        return false;
      }
      return (
        currentUser.serviceId === currentServiceId ||
        Boolean(currentUser.managedServiceIds?.includes(currentServiceId))
      );
    }, [currentUser, currentServiceId, hasPermission]);

    // Vrai si l'utilisateur endosse ce role sur l'incident (miroir de
    // IncidentWorkflowGuard.userHasActorRole).
    const userMatchesActorRole = useCallback(
      (role: IncidentActorRole): boolean => {
        if (!currentUser?.id) return false;
        switch (role) {
          case "CREATOR":
            return currentUser.id === creatorId;
          case "ASSIGNEE":
            return currentUserIsAssignee;
          case "CHEF_SERVICE": {
            const svc = currentServiceId || defaultServiceId;
            return svc
              ? currentUser.serviceId === svc ||
                  Boolean(currentUser.managedServiceIds?.includes(svc))
              : false;
          }
          case "SOURCE_AGENCY_MANAGER":
            return (
              currentUser.agencyId === agencyId &&
              hasPermission(PERMISSIONS.INCIDENT.VIEW_AGENCY)
            );
          default:
            return false;
        }
      },
      [
        currentUser,
        creatorId,
        currentUserIsAssignee,
        currentServiceId,
        defaultServiceId,
        agencyId,
        hasPermission,
      ],
    );

    // Autorise si vue globale (admin) ou si l'utilisateur endosse au moins un des
    // roles configures (defaut d'etape applique si la liste est vide).
    const matchesConfiguredRoles = useCallback(
      (
        roles: IncidentActorRole[] | undefined,
        defaults: IncidentActorRole[],
      ) => {
        if (!currentUser?.id) return false;
        if (hasPermission(PERMISSIONS.INCIDENT.VIEW_ALL)) return true;
        const effective = roles && roles.length > 0 ? roles : defaults;
        return effective.some(userMatchesActorRole);
      },
      [currentUser, hasPermission, userMatchesActorRole],
    );

    const canClose = useMemo(
      () => matchesConfiguredRoles(closerRoles, ["ASSIGNEE"]),
      [matchesConfiguredRoles, closerRoles],
    );

    // Portee acteur de la reouverture, alignee sur IncidentWorkflowGuard :
    const canReopen = useMemo(() => {
      if (!currentUser?.id) return false;
      if (status === IncidentStatus.REJECTED) {
        return currentUserIsCreator || currentUserIsRejector;
      }
      if (status === IncidentStatus.RESOLVED) {
        return matchesConfiguredRoles(reopenerRoles, ["SOURCE_AGENCY_MANAGER"]);
      }
      return true;
    }, [
      currentUser,
      status,
      currentUserIsCreator,
      currentUserIsRejector,
      matchesConfiguredRoles,
      reopenerRoles,
    ]);

    // Annulation : le verdict vient du back (IncidentWorkflowGuard.canCancel), seul a
    // connaitre le valideur attendu d'un incident encore en attente de validation.
    // Le recalculer ici reintroduirait la derive qu'on vient de corriger.

    const canResolve = useMemo(
      () => matchesConfiguredRoles(resolverRoles, ["SOURCE_AGENCY_MANAGER"]),
      [matchesConfiguredRoles, resolverRoles],
    );

    const canTreat = useMemo(
      () => matchesConfiguredRoles(treaterRoles, ["ASSIGNEE"]),
      [matchesConfiguredRoles, treaterRoles],
    );

    const visibleActions = useMemo(
      () =>
        allActions.filter((a) => {
          return (
            hasAnyPermission(hasPermission, a.permissions) &&
            (a.key === WorkflowAction.CANCEL
              ? CANCELLATION_ALLOWED_STATUSES.has(status ?? "")
              : allowedByStatus.has(a.key)) &&
            (a.key !== WorkflowAction.REOPEN ||
              (!isReopenExpired && !isMaxReopenReached && canReopen)) &&
            (a.key !== WorkflowAction.RESUBMIT || currentUserIsCreator) &&
            (a.key !== WorkflowAction.SELF_ASSIGN || !currentUserIsAssignee) &&
            (!(
              [
                WorkflowAction.ASSIGN,
                WorkflowAction.TRANSFER,
                WorkflowAction.SELF_ASSIGN,
              ] as WorkflowAction[]
            ).includes(a.key) ||
              canManageProcessing) &&
            ((a.key !== WorkflowAction.ASSIGN &&
              a.key !== WorkflowAction.SELF_ASSIGN) ||
              Boolean(currentServiceId)) &&
            (a.key !== WorkflowAction.ASSIGN || hasAssignableTarget) &&
            (a.key !== WorkflowAction.SELF_ASSIGN ||
              currentUserCanSelfAssign) &&
            (a.key !== WorkflowAction.CLOSE || canClose) &&
            ((a.key !== WorkflowAction.VALIDATE &&
              a.key !== WorkflowAction.REJECT) ||
              canValidate) &&
            ((a.key !== WorkflowAction.RESOLVE &&
              a.key !== WorkflowAction.MARK_UNRESOLVED) ||
              canResolve) &&
            (a.key !== WorkflowAction.TREAT || canTreat) &&
            (a.key !== WorkflowAction.START ||
              (!requiresDirectionValidation && currentUserIsAssignee)) &&
            (a.key !== WorkflowAction.SUBMIT_SOLUTION ||
              (requiresDirectionValidation &&
                currentUserIsAssignee &&
                (status !== IncidentStatus.DRAFT ||
                  Boolean(directionRejectionReason)))) &&
            ((a.key !== WorkflowAction.DIRECTION_VALIDATE &&
              a.key !== WorkflowAction.DIRECTION_REJECT) ||
              (hasPermission(PERMISSIONS.INCIDENT.VALIDATION_DIRECTION) &&
                status === IncidentStatus.DRAFT &&
                !directionRejectionReason)) &&
            (a.key !== WorkflowAction.CANCEL || canCancel) &&
            (a.key !== WorkflowAction.REQUEST_CONFIRMATION ||
              canRequestConfirmation) &&
            (a.key !== WorkflowAction.CONFIRM_RELEVANCE ||
              canConfirmRelevance) &&
            (!(
              [
                WorkflowAction.START,
                WorkflowAction.BLOCK,
                WorkflowAction.RESUME,
              ] as WorkflowAction[]
            ).includes(a.key) ||
              currentUserIsAssignee)
          );
        }),
      [
        allActions,
        hasPermission,
        allowedByStatus,
        isReopenExpired,
        isMaxReopenReached,
        canReopen,
        currentUserIsAssignee,
        currentUserIsCreator,
        canManageProcessing,
        canCancel,
        canRequestConfirmation,
        canConfirmRelevance,
        canClose,
        canValidate,
        canResolve,
        canTreat,
        requiresDirectionValidation,
        status,
        directionRejectionReason,
        currentServiceId,
        hasAssignableTarget,
        currentUserCanSelfAssign,
      ],
    );

    if (visibleActions.length === 0) return null;

    const modalTitles = new Map<ActionKey, string>([
      [WorkflowAction.VALIDATE, t("incidents.workflow.modals.validate.title")],
      [WorkflowAction.REJECT, t("incidents.workflow.modals.reject.title")],
      [WorkflowAction.TRANSFER, t("incidents.workflow.modals.transfer.title")],
      [WorkflowAction.ASSIGN, t("incidents.workflow.modals.assign.title")],
      [
        WorkflowAction.SELF_ASSIGN,
        t("incidents.workflow.modals.self_assign.title"),
      ],
      [WorkflowAction.START, t("incidents.workflow.modals.start.title")],
      [WorkflowAction.BLOCK, t("incidents.workflow.modals.block.title")],
      [WorkflowAction.RESUME, t("incidents.workflow.modals.resume.title")],
      [
        WorkflowAction.REQUEST_CONFIRMATION,
        t("incidents.workflow.modals.request_confirmation.title"),
      ],
      [
        WorkflowAction.CONFIRM_RELEVANCE,
        t("incidents.workflow.modals.confirm_relevance.title"),
      ],
      [WorkflowAction.TREAT, t("incidents.workflow.modals.treat.title")],
      [WorkflowAction.RESOLVE, t("incidents.workflow.modals.resolve.title")],
      [
        WorkflowAction.MARK_UNRESOLVED,
        t("incidents.workflow.modals.mark_unresolved.title"),
      ],
      [WorkflowAction.CLOSE, t("incidents.workflow.modals.close.title")],
      [WorkflowAction.REOPEN, t("incidents.workflow.modals.reopen.title")],
      [WorkflowAction.RESUBMIT, t("incidents.workflow.modals.resubmit.title")],
      [WorkflowAction.CLONE, t("incidents.workflow.modals.clone.title")],
      [
        WorkflowAction.CANCEL,
        t(
          isRelevanceAnswer
            ? "incidents.workflow.modals.no_longer_relevant.title"
            : "incidents.workflow.modals.cancel.title",
        ),
      ],
      [
        WorkflowAction.SUBMIT_SOLUTION,
        t("incidents.workflow.modals.submit_solution.title"),
      ],
      [
        WorkflowAction.DIRECTION_VALIDATE,
        t("incidents.workflow.modals.direction_validate.title"),
      ],
      [
        WorkflowAction.DIRECTION_REJECT,
        t("incidents.workflow.modals.direction_reject.title"),
      ],
    ]);

    const PRIMARY_ACTIONS = new Set<WorkflowAction>([
      WorkflowAction.VALIDATE,
      WorkflowAction.START,
      WorkflowAction.SUBMIT_SOLUTION,
      WorkflowAction.DIRECTION_VALIDATE,
      WorkflowAction.DIRECTION_REJECT,
      WorkflowAction.RESUME,
      WorkflowAction.CONFIRM_RELEVANCE,
      WorkflowAction.TREAT,
      WorkflowAction.RESOLVE,
      WorkflowAction.CLOSE,
    ]);

    const INLINE_ACTIONS_THRESHOLD = 2;
    const shouldInlineAllActions =
      visibleActions.length <= INLINE_ACTIONS_THRESHOLD;

    const primaryActions = shouldInlineAllActions
      ? visibleActions
      : visibleActions.filter((a) => PRIMARY_ACTIONS.has(a.key));
    const secondaryActions = shouldInlineAllActions
      ? []
      : visibleActions.filter((a) => !PRIMARY_ACTIONS.has(a.key));

    const dropdownItems: MenuProps["items"] = secondaryActions.map(
      (action) => ({
        key: action.key,
        label: action.label,
        icon: ACTION_ICON.get(action.key),
        onClick: () => openAction(action.key),
        danger: ACTION_TONE.get(action.key) === "danger",
        disabled: isPending,
      }),
    );

    // Joindre une PJ : disponible dans les modales de traitement / validation de resolution /
    // cloture / solution. L'acteur y a deja acces (visibilite du bouton gardee en amont) et
    // l'action metier est gardee cote backend.
    const canAttachInModal =
      openModal === WorkflowAction.TREAT ||
      openModal === WorkflowAction.RESOLVE ||
      openModal === WorkflowAction.MARK_UNRESOLVED ||
      openModal === WorkflowAction.CLOSE ||
      openModal === WorkflowAction.CANCEL ||
      openModal === WorkflowAction.SUBMIT_SOLUTION;

    // Champ de pieces jointes des modales de resolution et de fermeture.
    const attachmentUploadField = !canAttachInModal ? null : (
      <Form.Item label={t("incidents.workflow.common.attachments_label")}>
        <Upload
          multiple
          fileList={attachmentFiles}
          accept={ACCEPTED_ATTACHMENT_ACCEPT}
          beforeUpload={(file) => {
            if (!isAcceptedAttachment(file as unknown as File)) {
              message.error(t("incidents.attachments.invalid_type"));
              return Upload.LIST_IGNORE;
            }
            if (!isAttachmentSizeValid(file as unknown as File)) {
              message.error(
                t(file.size === 0 ? "incidents.attachments.empty_file" : "incidents.attachments.too_large", {
                  name: file.name,
                  limit: Math.round(MAX_ATTACHMENT_SIZE_BYTES / (1024 * 1024)),
                }),
              );
              return Upload.LIST_IGNORE;
            }
            return false;
          }}
          onChange={({ fileList }) => setAttachmentFiles(fileList)}
        >
          <Button
            icon={<UploadOutlined />}
            loading={isUploadingAttachment}
            disabled={isPending}
          >
            {t("incidents.workflow.common.attachments_add")}
          </Button>
        </Upload>
      </Form.Item>
    );

    return (
      <div className={styles.workflowActions}>
        <div className={styles.actionList}>
          {primaryActions.map((action) => (
            <ActionButton
              key={action.key}
              actionKey={action.key}
              label={action.label}
              icon={ACTION_ICON.get(action.key)}
              tone={ACTION_TONE.get(action.key) ?? "neutral"}
              disabled={isPending}
              onOpen={openAction}
            />
          ))}
          {secondaryActions.length > 0 && (
            <Dropdown
              menu={{ items: dropdownItems }}
              placement="bottomRight"
              disabled={isPending}
            >
              <Button type="default" icon={<MoreOutlined />}>
                {t("common.more_actions")}
              </Button>
            </Dropdown>
          )}
        </div>

        <Modal
          open={openModal !== null}
          title={openModal ? (modalTitles.get(openModal) ?? "") : ""}
          onClose={closeModal}
          onConfirm={handleSubmit}
          confirmText={
            isUploadingAttachment
              ? t("incidents.form.buttons.uploading")
              : openModal
                ? t(`incidents.workflow.modals.${openModal}.confirm`)
                : ""
          }
          cancelText={t("common.cancel")}
          confirmLoading={isPending}
          cancelDisabled={isPending}
          danger={
            openModal === WorkflowAction.REJECT ||
            openModal === WorkflowAction.CANCEL ||
            openModal === WorkflowAction.BLOCK ||
            openModal === WorkflowAction.MARK_UNRESOLVED ||
            openModal === WorkflowAction.DIRECTION_REJECT
          }
          destroyOnHidden
        >
          <Form form={form} layout="vertical">
            {openModal === WorkflowAction.VALIDATE && (
              <>
                {requiresValidationRouting && (
                  <Form.Item
                    name="targetServiceId"
                    label={t(
                      "incidents.workflow.modals.transfer.service_label",
                    )}
                    rules={[
                      {
                        required: true,
                        message: t(
                          "incidents.workflow.modals.transfer.service_required",
                        ),
                      },
                    ]}
                  >
                    <Select
                      placeholder={t(
                        "incidents.workflow.modals.transfer.service_placeholder",
                      )}
                      options={serviceOptions}
                      showSearch
                      allowClear
                      filterOption={filterOptionByLabel}
                    />
                  </Form.Item>
                )}
                <Form.Item
                  name="comment"
                  label={t("incidents.workflow.common.comment_label")}
                >
                  <TextArea
                    rows={3}
                    placeholder={t(
                      "incidents.workflow.modals.validate.comment_placeholder",
                    )}
                    spellCheck={true}
                  />
                </Form.Item>
              </>
            )}

            {openModal === WorkflowAction.REJECT && (
              <>
                <Form.Item
                  name="reason"
                  label={t("incidents.workflow.modals.reject.reason_label")}
                  rules={[
                    {
                      required: true,
                      message: t(
                        "incidents.workflow.modals.reject.reason_required",
                      ),
                    },
                  ]}
                >
                  <TextArea
                    rows={3}
                    placeholder={t(
                      "incidents.workflow.modals.reject.reason_placeholder",
                    )}
                    spellCheck={true}
                  />
                </Form.Item>
                <Form.Item
                  name="comment"
                  label={t("incidents.workflow.common.comment_label")}
                >
                  <TextArea
                    rows={2}
                    placeholder={t(
                      "incidents.workflow.common.comment_placeholder",
                    )}
                    spellCheck={true}
                  />
                </Form.Item>
              </>
            )}

            {openModal === WorkflowAction.CANCEL && (
              <>
                <Form.Item
                  name="reason"
                  label={t(
                    isRelevanceAnswer
                      ? "incidents.workflow.modals.no_longer_relevant.reason_label"
                      : "incidents.workflow.modals.cancel.reason_label",
                  )}
                  rules={[
                    {
                      required: true,
                      message: t(
                        "incidents.workflow.modals.cancel.reason_required",
                      ),
                    },
                  ]}
                >
                  <TextArea
                    rows={3}
                    placeholder={t(
                      isRelevanceAnswer
                        ? "incidents.workflow.modals.no_longer_relevant.reason_placeholder"
                        : "incidents.workflow.modals.cancel.reason_placeholder",
                    )}
                    spellCheck={true}
                  />
                </Form.Item>
                {attachmentUploadField}
              </>
            )}

            {openModal === WorkflowAction.TRANSFER && (
              <>
                <Form.Item
                  name="transferTarget"
                  label={t(
                    "incidents.workflow.modals.transfer.destination_label",
                  )}
                  rules={[
                    {
                      required: true,
                      message: t(
                        "incidents.workflow.modals.transfer.destination_required",
                      ),
                    },
                  ]}
                >
                  <Select
                    placeholder={t(
                      "incidents.workflow.modals.transfer.destination_placeholder",
                    )}
                    options={transferOptions}
                    showSearch
                    filterOption={filterOptionByLabel}
                  />
                </Form.Item>
                {transferTargetServiceId && (
                  <Form.Item
                    name="newTypeId"
                    label={t("incidents.workflow.modals.transfer.new_type")}
                    extra={t(
                      "incidents.workflow.modals.transfer.new_type_help",
                    )}
                  >
                    <Select
                      placeholder={t(
                        "incidents.workflow.modals.transfer.new_type_placeholder",
                      )}
                      options={reclassifyTypeOptions}
                      showSearch
                      allowClear
                      filterOption={filterOptionByLabel}
                    />
                  </Form.Item>
                )}
                {requiresTransferReason && (
                  <Form.Item
                    name="reason"
                    label={t("incidents.workflow.modals.transfer.reason_label")}
                    rules={[
                      {
                        required: true,
                        message: t(
                          "incidents.workflow.modals.transfer.reason_required",
                        ),
                      },
                    ]}
                  >
                    <TextArea
                      rows={3}
                      placeholder={t(
                        "incidents.workflow.modals.transfer.reason_placeholder",
                      )}
                      spellCheck={true}
                    />
                  </Form.Item>
                )}
                <Form.Item
                  name="comment"
                  label={t("incidents.workflow.common.comment_label")}
                >
                  <TextArea
                    rows={2}
                    placeholder={t(
                      "incidents.workflow.common.comment_placeholder",
                    )}
                    spellCheck={true}
                  />
                </Form.Item>
              </>
            )}

            {openModal === WorkflowAction.ASSIGN && (
              <>
                <Form.Item
                  name="assignedTo"
                  label={t("incidents.workflow.modals.assign.user_label")}
                  rules={[
                    {
                      required: true,
                      message: t(
                        "incidents.workflow.modals.assign.user_required",
                      ),
                    },
                  ]}
                >
                  <Select
                    placeholder={t(
                      "incidents.workflow.modals.assign.user_placeholder",
                    )}
                    options={userOptions}
                    showSearch
                    filterOption={filterOptionByLabel}
                  />
                </Form.Item>
                <Form.Item
                  name="comment"
                  label={t("incidents.workflow.common.comment_label")}
                >
                  <TextArea
                    rows={2}
                    placeholder={t(
                      "incidents.workflow.common.comment_placeholder",
                    )}
                    spellCheck={true}
                  />
                </Form.Item>
              </>
            )}

            {openModal === WorkflowAction.SELF_ASSIGN && (
              <Form.Item
                name="comment"
                label={t("incidents.workflow.common.comment_label")}
              >
                <TextArea
                  rows={3}
                  placeholder={t(
                    "incidents.workflow.modals.self_assign.comment_placeholder",
                  )}
                  spellCheck={true}
                />
              </Form.Item>
            )}

            {openModal === WorkflowAction.START && (
              <>
                <Form.Item
                  name="estimatedResolutionHours"
                  label={t("incidents.workflow.common.estimated_hours_label")}
                  extra={t("incidents.workflow.common.estimated_hours_help")}
                  rules={[{ type: "number", min: 1 }]}
                >
                  <InputNumber
                    min={1}
                    style={{ width: "100%" }}
                    placeholder={t(
                      "incidents.workflow.common.estimated_hours_placeholder",
                    )}
                  />
                </Form.Item>
                <Form.Item
                  name="comment"
                  label={t("incidents.workflow.common.comment_label")}
                >
                  <TextArea
                    rows={3}
                    placeholder={t(
                      "incidents.workflow.modals.start.comment_placeholder",
                    )}
                    spellCheck={true}
                  />
                </Form.Item>
              </>
            )}

            {openModal === WorkflowAction.BLOCK && (
              <>
                <Form.Item
                  name="reason"
                  label={t("incidents.workflow.modals.block.reason_label")}
                  rules={[
                    {
                      required: true,
                      message: t(
                        "incidents.workflow.modals.block.reason_required",
                      ),
                    },
                  ]}
                >
                  <TextArea
                    rows={3}
                    placeholder={t(
                      "incidents.workflow.modals.block.reason_placeholder",
                    )}
                    spellCheck={true}
                  />
                </Form.Item>
                <Form.Item
                  name="comment"
                  label={t("incidents.workflow.common.comment_label")}
                >
                  <TextArea
                    rows={2}
                    placeholder={t(
                      "incidents.workflow.common.comment_placeholder",
                    )}
                    spellCheck={true}
                  />
                </Form.Item>
              </>
            )}

            {(openModal === WorkflowAction.REQUEST_CONFIRMATION ||
              openModal === WorkflowAction.CONFIRM_RELEVANCE) && (
              <>
                <Alert
                  type="info"
                  showIcon
                  message={
                    openModal === WorkflowAction.REQUEST_CONFIRMATION
                      ? t(
                          "incidents.workflow.modals.request_confirmation.hint",
                          { responder: relevanceResponderLabel },
                        )
                      : t("incidents.workflow.modals.confirm_relevance.hint")
                  }
                  className={styles.alertBox}
                />
                <Form.Item
                  name="comment"
                  label={t("incidents.workflow.common.comment_label")}
                >
                  <TextArea
                    rows={3}
                    placeholder={t(
                      openModal === WorkflowAction.REQUEST_CONFIRMATION
                        ? "incidents.workflow.modals.request_confirmation.comment_placeholder"
                        : "incidents.workflow.modals.confirm_relevance.comment_placeholder",
                    )}
                    spellCheck={true}
                  />
                </Form.Item>
              </>
            )}

            {openModal === WorkflowAction.RESUME && (
              <Form.Item
                name="comment"
                label={t("incidents.workflow.common.comment_label")}
              >
                <TextArea
                  rows={3}
                  placeholder={t(
                    "incidents.workflow.modals.resume.comment_placeholder",
                  )}
                  spellCheck={true}
                />
              </Form.Item>
            )}

            {openModal === WorkflowAction.TREAT && (
              <>
                <Form.Item
                  name="treatmentDescription"
                  label={t("incidents.workflow.modals.treat.description_label")}
                  rules={[
                    {
                      required: true,
                      message: t(
                        "incidents.workflow.modals.treat.description_required",
                      ),
                    },
                  ]}
                >
                  <TextArea
                    rows={4}
                    placeholder={t(
                      "incidents.workflow.modals.treat.description_placeholder",
                    )}
                    spellCheck={true}
                  />
                </Form.Item>
                <Form.Item
                  name="cause"
                  label={t("incidents.form.labels.cause")}
                  rules={
                    requiresCauseAnalysis
                      ? [
                          {
                            required: true,
                            message: t(
                              "incidents.workflow.modals.treat.cause_required",
                            ),
                          },
                        ]
                      : []
                  }
                >
                  <Select
                    options={causeOptions}
                    placeholder={t("incidents.form.placeholders.cause")}
                    allowClear
                    showSearch
                    filterOption={filterOptionByLabel}
                  />
                </Form.Item>
                <Form.Item
                  name="causeDetail"
                  label={t("incidents.form.labels.cause_detail_other")}
                  rules={
                    requiresCauseAnalysis
                      ? [
                          {
                            required: true,
                            message: t(
                              "incidents.workflow.modals.treat.cause_detail_required",
                            ),
                          },
                          { max: 2000 },
                        ]
                      : [{ max: 2000 }]
                  }
                >
                  <TextArea
                    rows={3}
                    maxLength={2000}
                    showCount
                    placeholder={t(
                      "incidents.form.placeholders.cause_detail_other",
                    )}
                    spellCheck={true}
                  />
                </Form.Item>
                {attachmentUploadField}
              </>
            )}

            {openModal === WorkflowAction.RESOLVE && (
              <>
                <Form.Item
                  name="resolutionNote"
                  label={t("incidents.workflow.modals.resolve.note_label")}
                  rules={[
                    {
                      required: true,
                      message: t(
                        "incidents.workflow.modals.resolve.note_required",
                      ),
                    },
                  ]}
                >
                  <TextArea
                    rows={4}
                    placeholder={t(
                      "incidents.workflow.modals.resolve.note_placeholder",
                    )}
                    spellCheck={true}
                  />
                </Form.Item>
                {attachmentUploadField}
              </>
            )}

            {openModal === WorkflowAction.MARK_UNRESOLVED && (
              <Form.Item
                name="reason"
                label={t(
                  "incidents.workflow.modals.mark_unresolved.reason_label",
                )}
                rules={[
                  {
                    required: true,
                    message: t(
                      "incidents.workflow.modals.mark_unresolved.reason_required",
                    ),
                  },
                ]}
              >
                <TextArea
                  rows={4}
                  placeholder={t(
                    "incidents.workflow.modals.mark_unresolved.reason_placeholder",
                  )}
                  spellCheck={true}
                />
              </Form.Item>
            )}

            {openModal === WorkflowAction.REOPEN && (
              <>
                <Form.Item
                  name="reason"
                  label={t("incidents.workflow.modals.reopen.reason_label")}
                  rules={[
                    {
                      required: true,
                      message: t(
                        "incidents.workflow.modals.reopen.reason_required",
                      ),
                    },
                    { min: 3 },
                    { max: 500 },
                  ]}
                >
                  <TextArea
                    rows={3}
                    placeholder={t(
                      "incidents.workflow.modals.reopen.reason_placeholder",
                    )}
                    maxLength={500}
                    showCount
                    spellCheck={true}
                  />
                </Form.Item>
                <Form.Item
                  name="comment"
                  label={t("incidents.workflow.common.comment_label")}
                >
                  <TextArea
                    rows={2}
                    placeholder={t(
                      "incidents.workflow.modals.reopen.comment_placeholder",
                    )}
                    spellCheck={true}
                  />
                </Form.Item>
              </>
            )}

            {openModal === WorkflowAction.RESUBMIT && (
              <Form.Item
                name="comment"
                label={t("incidents.workflow.common.comment_label")}
              >
                <TextArea
                  rows={3}
                  placeholder={t(
                    "incidents.workflow.modals.resubmit.comment_placeholder",
                  )}
                  spellCheck={true}
                />
              </Form.Item>
            )}

            {openModal === WorkflowAction.CLONE && (
              <p>{t("incidents.workflow.modals.clone.message")}</p>
            )}

            {openModal === WorkflowAction.CLOSE && (
              <>
                <Form.Item
                  name="closureDescription"
                  label={t("incidents.workflow.modals.close.closure_label")}
                  rules={[
                    {
                      required: true,
                      message: t(
                        "incidents.workflow.modals.close.closure_required",
                      ),
                    },
                    { max: 5000 },
                  ]}
                >
                  <TextArea
                    rows={4}
                    placeholder={t(
                      "incidents.workflow.modals.close.closure_placeholder",
                    )}
                    maxLength={5000}
                    showCount
                    spellCheck={true}
                  />
                </Form.Item>
                <Form.Item
                  name="comment"
                  label={t("incidents.workflow.common.comment_label")}
                >
                  <TextArea
                    rows={2}
                    placeholder={t(
                      "incidents.workflow.modals.close.comment_placeholder",
                    )}
                    spellCheck={true}
                  />
                </Form.Item>
                {attachmentUploadField}
              </>
            )}

            {openModal === WorkflowAction.SUBMIT_SOLUTION && (
              <>
                <Alert
                  type="info"
                  showIcon={false}
                  className={styles.alertBox}
                  message={
                    <div className={styles.solutionSummary}>
                      <div className={styles.summaryLabel}>
                        {t(
                          "incidents.workflow.modals.submit_solution.incident_title_label",
                        )}
                      </div>
                      <div className={styles.summaryValue}>{incidentTitle}</div>
                      <div className={styles.summaryLabel}>
                        {t(
                          "incidents.workflow.modals.submit_solution.incident_description_label",
                        )}
                      </div>
                      <div className={styles.descriptionValue}>
                        {incidentDescription}
                      </div>
                    </div>
                  }
                />
                {directionRejectionReason && (
                  <Alert
                    type="warning"
                    showIcon
                    message={t(
                      "incidents.workflow.modals.submit_solution.previous_rejection_title",
                    )}
                    description={directionRejectionReason}
                    className={styles.alertBox}
                  />
                )}
                <Form.Item
                  name="proposedSolution"
                  label={t(
                    "incidents.workflow.modals.submit_solution.proposedSolutionDescriptionLabel",
                  )}
                  rules={[
                    {
                      required: true,
                      message: t(
                        "incidents.workflow.modals.submit_solution.proposedSolutionDescriptionRequired",
                      ),
                    },
                    { max: 15000 },
                  ]}
                  initialValue={proposedSolution ?? ""}
                >
                  <TextArea
                    rows={6}
                    maxLength={15000}
                    showCount
                    placeholder={t(
                      "incidents.workflow.modals.submit_solution.proposedSolutionDescriptionPlaceholder",
                    )}
                    spellCheck={true}
                  />
                </Form.Item>

                <Form.Item
                  name="estimatedResolutionHours"
                  label={t("incidents.workflow.common.estimated_hours_label")}
                  extra={t("incidents.workflow.common.estimated_hours_help")}
                  rules={[{ type: "number", min: 1 }]}
                >
                  <InputNumber
                    min={1}
                    style={{ width: "100%" }}
                    placeholder={t(
                      "incidents.workflow.common.estimated_hours_placeholder",
                    )}
                  />
                </Form.Item>

                {incidentAttachments.length > 0 && (
                  <div className={styles.existingAttachmentsSection}>
                    <Text strong className={styles.existingAttachmentsTitle}>
                      {t(
                        "incidents.workflow.modals.submit_solution.existing_attachments_title",
                      )}
                    </Text>
                    <div className={styles.existingAttachmentsList}>
                      {incidentAttachments.map((att) => {
                        const isMarked = pendingDeleteAttachmentIds.includes(
                          att.id,
                        );
                        return (
                          <div
                            key={att.id}
                            className={`${styles.existingAttachmentItem} ${
                              isMarked ? styles.markedForDeletion : ""
                            }`}
                          >
                            <div className={styles.attachmentInfo}>
                              <PaperClipOutlined />
                              <Text
                                ellipsis
                                className={styles.attachmentFilename}
                              >
                                {att.filename}
                              </Text>
                              {att.fileSize && (
                                <Text
                                  type="secondary"
                                  className={styles.attachmentSize}
                                >
                                  ({(att.fileSize / 1024).toFixed(1)} KB)
                                </Text>
                              )}
                            </div>
                            {isMarked ? (
                              <Button
                                type="text"
                                size="small"
                                icon={<UndoOutlined />}
                                onClick={() =>
                                  togglePendingDeleteAttachment(att.id)
                                }
                              >
                                {t("common.restore")}
                              </Button>
                            ) : (
                              <Button
                                type="text"
                                danger
                                size="small"
                                icon={<DeleteOutlined />}
                                onClick={() =>
                                  togglePendingDeleteAttachment(att.id)
                                }
                              />
                            )}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                )}

                {attachmentUploadField}
              </>
            )}

            {openModal === WorkflowAction.DIRECTION_VALIDATE && (
              <>
                <Alert
                  type="info"
                  showIcon
                  className={styles.alertBox}
                  message={t(
                    "incidents.workflow.modals.direction_validate.confirm_message",
                  )}
                />
                <Form.Item
                  name="comment"
                  label={t("incidents.workflow.common.comment_label")}
                >
                  <TextArea
                    rows={3}
                    placeholder={t(
                      "incidents.workflow.modals.direction_validate.comment_placeholder",
                    )}
                    spellCheck={true}
                  />
                </Form.Item>
              </>
            )}

            {openModal === WorkflowAction.DIRECTION_REJECT && (
              <>
                <Alert
                  type="warning"
                  showIcon
                  className={styles.alertBox}
                  message={t(
                    "incidents.workflow.modals.direction_reject.confirm_message",
                  )}
                />
                <Form.Item
                  name="rejectionReason"
                  label={t(
                    "incidents.workflow.modals.direction_reject.reason_label",
                  )}
                  rules={[
                    {
                      required: true,
                      message: t(
                        "incidents.workflow.modals.direction_reject.reason_required",
                      ),
                    },
                    { max: 10000 },
                  ]}
                >
                  <TextArea
                    rows={4}
                    maxLength={10000}
                    showCount
                    placeholder={t(
                      "incidents.workflow.modals.direction_reject.reason_placeholder",
                    )}
                    spellCheck={true}
                  />
                </Form.Item>
              </>
            )}
          </Form>
        </Modal>
      </div>
    );
  },
);

export default WorkflowActions;
