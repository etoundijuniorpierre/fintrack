import type { Criticality } from "../incident/enums/enums";
import type { ReportContentType } from "../reporting/types";

// Centralise les contrats de parametrage echanges avec le backend.
export type ReportType = "DAILY" | "WEEKLY" | "MONTHLY" | "CUSTOM";

// Centralise la logique d'interface liee a rapport format.
export type ReportFormat = "PDF" | "EXCEL" | "JSON";

// Centralise la logique d'interface liee a service resume response.
export interface ServiceSummaryResponse {
  id: string;
  name: string;
  description?: string;
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

// Portee de validation configuree par type d'incident.
export type IncidentValidatorScope =
  | "SOURCE_SERVICE_MANAGER"
  | "AGENCY_MANAGER"
  | "TARGET_SERVICE_MANAGER"
  | "ADMIN";

// Vocabulaire unifie des acteurs habilites pour les etapes configurables.
export type IncidentActorRole =
  | "SOURCE_AGENCY_MANAGER"
  | "CREATOR"
  | "ASSIGNEE"
  | "CHEF_SERVICE";

// Ordre d'affichage des roles dans les selecteurs de configuration.
export const INCIDENT_ACTOR_ROLES: IncidentActorRole[] = [
  "SOURCE_AGENCY_MANAGER",
  "CREATOR",
  "ASSIGNEE",
  "CHEF_SERVICE",
];

// Centralise la logique d'interface liee a incident type configuration response.
export interface IncidentTypeConfigResponse {
  requiresDirectionValidation?: boolean;
  directionValidators?: UserSummaryResponse[];
  id: string;
  name: string;
  displayName: string;
  description?: string;
  isActive: boolean;
  slaHours?: number;

  defaultTargetService?: ServiceSummaryResponse;

  defaultTargetUser?: UserSummaryResponse;
  requiresValidation: boolean;
  requiresCauseAnalysis: boolean;
  emailNotificationsEnabled: boolean;
  treaterRoles?: IncidentActorRole[];
  resolverRoles?: IncidentActorRole[];
  closerRoles?: IncidentActorRole[];
  reopenerRoles?: IncidentActorRole[];
  validatorScope: IncidentValidatorScope;
  defaultCriticality?: Criticality;
  createdAt: string;
  updatedAt: string;
  modifiedBy?: string;
}

// Centralise la logique d'interface liee a agence response.
export interface AgencyResponse {
  id: string;
  name: string;
  code: string;
  city?: string;
  address?: string;
  isActive: boolean;
  headOfAgency?: UserSummaryResponse;
  members: UserSummaryResponse[];
  createdAt: string;
  updatedAt: string;
}

// Centralise la logique d'interface liee a service response.
export interface ServiceResponse {
  id: string;
  name: string;
  description?: string;
  isActive: boolean;
  headOfService?: UserSummaryResponse;
  members: UserSummaryResponse[];
  createdAt: string;
  updatedAt: string;
}

// Centralise la logique d'interface liee a permission response.
export interface PermissionResponse {
  id: string;
  name: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

// Centralise la logique d'interface liee a role response.
export interface RoleResponse {
  id: string;
  name: string;
  displayName?: string;
  description?: string;
  isSystem: boolean;
  permissions: PermissionResponse[];
  createdAt: string;
  updatedAt: string;
}

// Centralise la logique d'interface liee a rapport planification response.
export interface ReportScheduleResponse {
  id: string;
  name: string;
  type: ReportType;
  contentType?: ReportContentType;
  format: ReportFormat;
  recipientEmails: string[];
  sendTime?: string;
  weekDay?: number;
  scope?: string;
  isActive: boolean;
  lastGeneratedAt?: string;
  createdBy?: UserSummaryResponse;
  createdAt: string;
  updatedAt: string;
}

// Centralise la logique d'interface liee a incident type configuration request.
export interface IncidentTypeConfigRequest {
  name: string;

  displayName: string;

  description?: string;
  isActive: boolean;
  slaHours?: number;

  defaultTargetServiceId?: string;

  defaultTargetUserId?: string;

  requiresValidation: boolean;
  requiresDirectionValidation?: boolean;
  directionValidatorIds?: string[];
  requiresCauseAnalysis: boolean;
  emailNotificationsEnabled: boolean;
  treaterRoles?: IncidentActorRole[];
  resolverRoles?: IncidentActorRole[];
  closerRoles?: IncidentActorRole[];
  reopenerRoles?: IncidentActorRole[];
  validatorScope: IncidentValidatorScope;
  defaultCriticality?: Criticality;
}

// Centralise la logique d'interface liee a agence request.
export interface AgencyRequest {
  name: string;
  city: string;
  address?: string;
  isActive?: boolean;
  headUserId?: string;
}

// Centralise la logique d'interface liee a service request.
export interface ServiceRequest {
  name: string;
  description?: string;
  isActive?: boolean;
  headUserId?: string;
}

// Centralise la logique d'interface liee a role request.
export interface RoleRequest {
  name: string;
  displayName?: string;
  description?: string;
  isSystem?: boolean;
  permissionIds: string[];
}

// Centralise la logique d'interface liee a rapport planification request.
export interface ReportScheduleRequest {
  name: string;
  type: ReportType;
  contentType?: ReportContentType;
  format: ReportFormat;
  recipientEmails?: string[];
  sendTime?: string;
  weekDay?: number;
  scope?: string;
}
