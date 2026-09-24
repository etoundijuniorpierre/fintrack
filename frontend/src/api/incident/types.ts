// Types des requetes et reponses du microservice incident.
import type { PagedResponse } from "../user/types";

export type { PagedResponse };

import {
  IncidentStatus,
  Criticality,
  IncidentCause,
  ActionType,
  IncidentValidatorRole,
  STATUS_COLORS,
  CRITICALITY_COLORS,
  ACTION_TYPE_COLORS,
  CAUSE_NEEDS_PERSON,
} from "./enums/enums";

export {
  IncidentStatus,
  Criticality,
  IncidentCause,
  ActionType,
  IncidentValidatorRole,
  STATUS_COLORS,
  CRITICALITY_COLORS,
  ACTION_TYPE_COLORS,
  CAUSE_NEEDS_PERSON,
};

// Centralise la logique d'interface liee a enum response.
export interface EnumResponse {
  code: string;
  name: string;
  description: string;
}

// Modele le resume utilisateur retourne par l'API.
export interface UserSummaryResponse {
  id: string;
  username: string;
  firstName: string;
  lastName: string;
  email?: string;
}

// Centralise la logique d'interface liee a agence resume response.
export interface AgencySummaryResponse {
  id: string;
  name: string;
  code: string;
}

// Centralise la logique d'interface liee a service resume response.
export interface ServiceSummaryResponse {
  id: string;
  name: string;
  description?: string;
}

// Vocabulaire unifie des acteurs habilites pour les etapes configurables du workflow
// (traitement, resolution, cloture, reouverture).
export type IncidentActorRole =
  | "SOURCE_AGENCY_MANAGER"
  | "CREATOR"
  | "ASSIGNEE"
  | "CHEF_SERVICE";

// Portee de validation configuree par type d'incident.
export type IncidentValidatorScope =
  | "SOURCE_SERVICE_MANAGER"
  | "AGENCY_MANAGER"
  | "TARGET_SERVICE_MANAGER"
  | "ADMIN";

// Centralise la logique d'interface liee a incident type configuration response.
export interface IncidentTypeConfigResponse {
  treaterRoles?: IncidentActorRole[];
  resolverRoles?: IncidentActorRole[];
  closerRoles?: IncidentActorRole[];
  reopenerRoles?: IncidentActorRole[];
  id: string;
  name: string;
  displayName: string;
  description?: string;
  isActive: boolean;
  slaHours?: number;
  requiresValidation: boolean;
  requiresCauseAnalysis: boolean;
  emailNotificationsEnabled: boolean;
  validatorScope: IncidentValidatorScope;
  /** Service cible imposé par défaut pour ce type d'incident. */
  defaultTargetService?: ServiceSummaryResponse;
  /** Utilisateur cible imposé par défaut (type ne concernant pas un service). */
  defaultTargetUser?: UserSummaryResponse;
  /** Criticité pré-remplie lors de la création d'un incident de ce type. */
  defaultCriticality?: Criticality;
  requiresDirectionValidation?: boolean;
  directionValidators?: UserSummaryResponse[];
}

// Centralise la logique d'interface liee a incident resume response.
export interface IncidentSummaryResponse {
  id: string;
  createdAt: string;
  updatedAt: string;
  /** Code lisible et immuable (FT-I-2026-0001). */
  reference?: string;
  title: string;
  type: IncidentTypeConfigResponse;
  criticality: Criticality;
  status: IncidentStatus;
  dueDate?: string;
  createdBy: UserSummaryResponse;
  assignedTo?: UserSummaryResponse;
  agency: AgencySummaryResponse;
  creatorServiceId?: string;
  transferredToService?: ServiceSummaryResponse;
}

// Centralise la logique d'interface liee a incident comment response.
export interface IncidentCommentResponse {
  id: string;
  createdAt: string;
  updatedAt: string;
  author: UserSummaryResponse;
  content: string;
  isInternal: boolean;
  parentCommentId?: string;
  parentAuthor?: UserSummaryResponse;
}

// Centralise la logique d'interface liee a incident history response.
export interface IncidentHistoryResponse {
  id: string;
  createdAt: string;
  updatedAt: string;
  user: UserSummaryResponse;
  action: ActionType;
  oldValue?: string;
  newValue?: string;
  comment?: string;
  incidentId?: string;
  incidentTitle?: string;
}

// Centralise la logique d'interface liee a incident response.
export interface IncidentResponse extends IncidentSummaryResponse {
  isReopenExpired: boolean;
  isMaxReopenReached: boolean;
  canValidate?: boolean;
  /** Verdict de portee d'annulation calcule par le back (detail uniquement). */
  canCancel?: boolean;
  /** Attente prolongée : demande d'actualité en cours (absent si aucune n'est attendue). */
  confirmationRequestedAt?: string;
  /** Verdicts de portée des deux étapes de la confirmation d'actualité (détail uniquement). */
  canRequestConfirmation?: boolean;
  canConfirmRelevance?: boolean;
  /** Qui est attendu pour confirmer l'actualité ; présent en attente prolongée. */
  relevanceResponderRole?: IncidentValidatorRole;
  relevanceResponderTarget?: string;
  /** Valideur attendu, portee du type resolue ; present tant que l'incident attend sa validation. */
  expectedValidatorRole?: IncidentValidatorRole;
  /** Nom du service ou de l'agence portant le role attendu ; absent pour ADMIN. */
  expectedValidatorTarget?: string;
  description: string;
  incidentDate?: string;
  observationDate?: string;
  validatedAt?: string;
  transferredAt?: string;
  resolvedAt?: string;
  closedAt?: string;
  blockedAt?: string;
  unblockedAt?: string;
  reopenedAt?: string;
  validatedBy?: UserSummaryResponse;
  assignedTo?: UserSummaryResponse;
  blockedBy?: UserSummaryResponse;
  reopenedBy?: UserSummaryResponse;
  transferredToService?: ServiceSummaryResponse;
  transferredToAgency?: AgencySummaryResponse;
  transferReason?: string;
  rejectReason?: string;
  cancelReason?: string;
  blockedReason?: string;
  reopenReason?: string;
  reopenCount?: number;
  history: IncidentHistoryResponse[];
  comments: IncidentCommentResponse[];
  cause?: IncidentCause;
  causeDetail?: string;
  treatmentDescription?: string;
  resolutionDescription?: string;
  closureDescription?: string;
  proposedSolution?: string;
  directionRejectionReason?: string;
  estimatedResolutionHours?: number;
}

// Centralise la logique d'interface liee a incident request.
export interface IncidentRequest {
  title: string;
  description: string;
  typeId: string;
  criticality: Criticality;
  incidentDate?: string;
  observationDate?: string;
  /** Service cible choisi (ignoré si le type impose un service par défaut). */
  targetServiceId?: string;
  cause?: IncidentCause;
  causeDetail?: string;
  assignToSelf?: boolean;
  /**
   * Agence cible. Réservé aux profils globaux (SUPER_ADMIN / ADMIN sans
   * agence propre). Ignoré pour les autres rôles : l'agence est imposée
   * côté backend en fonction du créateur.
   */
  agencyId?: string;
}

// Centralise la logique d'interface liee a incident update request.
export interface IncidentUpdateRequest {
  title: string;
  description: string;
  typeId: string;
  criticality: Criticality;
  incidentDate?: string;
  observationDate?: string;
  cause?: IncidentCause;
  causeDetail?: string;
}

// Centralise la logique d'interface liee a validation request.
export interface ValidateRequest {
  comment?: string;
  targetServiceId?: string;
  targetUserId?: string;
}

// Centralise la logique d'interface liee a reject request.
export interface RejectRequest {
  reason: string;
  comment?: string;
}

// Centralise la logique d'interface liee a transfer request.
export type TransferRequest = {
  reason?: string;
  comment?: string;
  newTypeId?: string;
} & (
  | { targetServiceId: string; targetAgencyId?: never }
  | { targetServiceId?: never; targetAgencyId: string }
);

// Centralise la logique d'interface liee a assign request.
export interface AssignRequest {
  assignedTo: string;
  comment?: string;
}

// Centralise la logique d'interface liee a start request.
export interface StartRequest {
  comment?: string;
  // Temps de résolution estimé (en heures) saisi à la prise en charge ; pilote le SLA.
  estimatedResolutionHours?: number;
}

// Proposition de solution soumise pour validation préalable de la Direction.
export interface SolutionProposalRequest {
  proposedSolution: string;
  estimatedResolutionHours?: number;
}

// Centralise la logique d'interface liee a block request.
export interface BlockRequest {
  reason: string;
  comment?: string;
}

// Centralise la logique d'interface liee a resume request.
export interface ResumeRequest {
  comment?: string;
}

// Attente prolongée : demander l'actualité d'un incident comme y répondre. L'infirmation
// passe par l'annulation, qui exige un motif.
export interface RelevanceRequest {
  comment?: string;
}

// Le traitant marque un incident comme traite (IN_PROGRESS -> TREATED).
export interface TreatRequest {
  treatmentDescription: string;
  cause?: IncidentCause;
  causeDetail?: string;
}

// Le valideur confirme la resolution d'un incident traite (TREATED -> RESOLVED).
export interface ResolveRequest {
  resolutionNote: string;
}

// Le valideur juge un incident traite comme non resolu (TREATED -> IN_PROGRESS).
export interface UnresolvedRequest {
  reason: string;
}

// Centralise la logique d'interface liee a close request.
export interface CloseRequest {
  closureDescription: string;
  comment?: string;
}

// Centralise la logique d'interface liee a reopen request.
export interface ReopenRequest {
  /** Motif obligatoire de la réouverture (3 à 500 caractères). */
  reason: string;
  comment?: string;
}

// Centralise la logique d'interface liee a comment request.
export interface CommentRequest {
  content: string;
  isInternal: boolean;
  parentCommentId?: string;
}
