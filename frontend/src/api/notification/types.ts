// Centralise les contrats de notification echanges avec le backend.
export type NotificationType = "EMAIL" | "INTERNAL";

// Centralise la logique d'interface liee a notification statut.
export type NotificationStatus = "PENDING" | "SENT" | "FAILED" | "READ";

// Definit les constantes notification type colors.
export const NOTIFICATION_TYPE_COLORS: Record<NotificationType, string> = {
  EMAIL: "blue",
  INTERNAL: "purple",
};

// Definit les constantes notification status colors.
export const NOTIFICATION_STATUS_COLORS: Record<NotificationStatus, string> = {
  PENDING: "orange",
  SENT: "green",
  FAILED: "red",
  READ: "gray",
};

// Centralise la logique d'interface liee a incident resume response.
export interface IncidentSummaryResponse {
  id: string;
  title: string;
  status?: string;
  reference?: string;
}

// Decrit les derniers changements de presence connus pour un utilisateur.
export interface PresenceState {
  online: boolean;
  connectedAt?: string;
  disconnectedAt?: string;
}

// Centralise l'instantane de presence diffuse par REST et WebSocket.
export interface PresenceResponse {
  online: string[];
  states: Record<string, PresenceState>;
}

// Centralise la logique d'interface liee a notification enum response.
export interface NotificationEnumResponse {
  code: string;
  name: string;
  description: string;
}

// Centralise la logique d'interface liee a notification response.
export interface NotificationResponse {
  id: string;
  type: NotificationType;
  recipient: string;
  incidentId?: IncidentSummaryResponse;
  subject?: string;
  content?: string;
  subjectEn?: string;
  contentEn?: string;
  status: NotificationStatus;
  sentAt?: string;
  retryCount?: number;
  retryAt?: string;
  nextRetry?: string;
  createdAt: string;
  updatedAt: string;
  modifiedBy?: string;
  templateParams?: Record<string, unknown>;
}

// Centralise la logique d'interface liee a notification request.
export interface NotificationRequest {
  type: NotificationType;
  recipient: string;
  subject?: string;
  content?: string;
  incidentId?: string;
}

// Centralise la logique d'interface liee a notifications params.
export interface NotificationsParams {
  page?: number;
  size?: number;
  sort?: string;
}

// Centralise la logique d'interface liee a paged notification response.
export interface PagedNotificationResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
