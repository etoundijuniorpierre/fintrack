// Expose les enumerations incident consommees par le frontend.
export const IncidentStatus = {
  DRAFT: "DRAFT",
  OPEN: "OPEN",
  PENDING_VALIDATION: "PENDING_VALIDATION",
  VALIDATED: "VALIDATED",
  TRANSFERRED: "TRANSFERRED",
  ASSIGNED: "ASSIGNED",
  IN_PROGRESS: "IN_PROGRESS",
  TREATED: "TREATED",
  BLOCKED: "BLOCKED",
  RESOLVED: "RESOLVED",
  CLOSED: "CLOSED",
  REOPENED: "REOPENED",
  REJECTED: "REJECTED",
  CANCELLED: "CANCELLED",
  UNRESOLVED_PROLONGED_WAIT: "UNRESOLVED_PROLONGED_WAIT",
} as const;

// Centralise la logique d'interface liee a incident statut.
export type IncidentStatus =
  (typeof IncidentStatus)[keyof typeof IncidentStatus];

// Rend le composant Criticality.
export const Criticality = {
  LOW: "LOW",
  MEDIUM: "MEDIUM",
  HIGH: "HIGH",
  CRITICAL: "CRITICAL",
} as const;

// Centralise la logique d'interface liee a criticality.
export type Criticality = (typeof Criticality)[keyof typeof Criticality];

// Rend le composant ActionType.
export const ActionType = {
  CREATION: "CREATION",
  VALIDATION: "VALIDATION",
  TRANSFER: "TRANSFER",
  STATUS_CHANGE: "STATUS_CHANGE",
  COMMENT: "COMMENT",
  UPDATE: "UPDATE",
  ASSIGNMENT: "ASSIGNMENT",
  ROUTING: "ROUTING",
  TYPE_CHANGE: "TYPE_CHANGE",
  CRITICALITY_CHANGE: "CRITICALITY_CHANGE",
  REOPENING: "REOPENING",
  RESOLUTION_REJECTED: "RESOLUTION_REJECTED",
} as const;

// Centralise la logique d'interface liee a action type.
export type ActionType = (typeof ActionType)[keyof typeof ActionType];

// Definit les constantes status colors.
export const STATUS_COLORS: Record<IncidentStatus, string> = {
  DRAFT: "purple",
  OPEN: "red",
  PENDING_VALIDATION: "gold",
  VALIDATED: "blue",
  TRANSFERRED: "blue",
  ASSIGNED: "geekblue",
  IN_PROGRESS: "gold",
  TREATED: "cyan",
  BLOCKED: "volcano",
  RESOLVED: "green",
  CLOSED: "green",
  REOPENED: "orange",
  REJECTED: "red",
  CANCELLED: "default",
  UNRESOLVED_PROLONGED_WAIT: "magenta",
};

// Definit les constantes criticality colors.
export const CRITICALITY_COLORS: Record<Criticality, string> = {
  LOW: "green",
  MEDIUM: "orange",
  HIGH: "red",
  CRITICAL: "red",
};

// Definit les constantes action type colors.
export const ACTION_TYPE_COLORS: Record<ActionType, string> = {
  CREATION: "green",
  VALIDATION: "cyan",
  TRANSFER: "geekblue",
  STATUS_CHANGE: "orange",
  COMMENT: "blue",
  UPDATE: "blue",
  ASSIGNMENT: "geekblue",
  ROUTING: "geekblue",
  TYPE_CHANGE: "purple",
  CRITICALITY_CHANGE: "purple",
  REOPENING: "red",
  RESOLUTION_REJECTED: "red",
};

// Valideur reellement attendu sur un incident, une fois la portee du type resolue.
// Forme runtime (et pas seulement type) pour que les libelles i18n construits a
// partir de ces valeurs soient verifiables.
export const IncidentValidatorRole = {
  SERVICE_MANAGER: "SERVICE_MANAGER",
  AGENCY_MANAGER: "AGENCY_MANAGER",
  ADMIN: "ADMIN",
} as const;

// Centralise la logique d'interface liee a incident validator role.
export type IncidentValidatorRole =
  (typeof IncidentValidatorRole)[keyof typeof IncidentValidatorRole];

// Roles dont le libelle se decline avec le nom de la structure porteuse.
export const VALIDATOR_ROLES_WITH_TARGET: IncidentValidatorRole[] = [
  IncidentValidatorRole.SERVICE_MANAGER,
  IncidentValidatorRole.AGENCY_MANAGER,
];

// Rend le composant IncidentCause.
export const IncidentCause = {
  HUMAN: "HUMAN",
  TECHNICAL: "TECHNICAL",
  ORGANIZATIONAL: "ORGANIZATIONAL",
  ENVIRONMENTAL: "ENVIRONMENTAL",
  EXTERNAL: "EXTERNAL",
  STRUCTURAL: "STRUCTURAL",
  OTHER: "OTHER",
} as const;

// Centralise la logique d'interface liee a incident cause.
export type IncidentCause = (typeof IncidentCause)[keyof typeof IncidentCause];

/** Causes nécessitant un sélecteur de personne côté frontend */
export const CAUSE_NEEDS_PERSON: ReadonlySet<IncidentCause> = new Set([
  IncidentCause.HUMAN,
  IncidentCause.EXTERNAL,
]);

// Rend le composant WorkflowAction.
export const WorkflowAction = {
  VALIDATE: "validate",
  REJECT: "reject",
  TRANSFER: "transfer",
  ASSIGN: "assign",
  SELF_ASSIGN: "self_assign",
  START: "start",
  SUBMIT_SOLUTION: "submit_solution",
  DIRECTION_VALIDATE: "direction_validate",
  DIRECTION_REJECT: "direction_reject",
  BLOCK: "block",
  RESUME: "resume",
  REQUEST_CONFIRMATION: "request_confirmation",
  CONFIRM_RELEVANCE: "confirm_relevance",
  TREAT: "treat",
  RESOLVE: "resolve",
  MARK_UNRESOLVED: "mark_unresolved",
  CLOSE: "close",
  REOPEN: "reopen",
  RESUBMIT: "resubmit",
  CLONE: "clone",
  CANCEL: "cancel",
} as const;

// Centralise la logique d'interface liee a workflow action.
export type WorkflowAction =
  (typeof WorkflowAction)[keyof typeof WorkflowAction];
