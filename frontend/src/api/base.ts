// Centralise les chemins de base des API consommees par le frontend.
export const SERVICE_BASES = {
  USER: "/api/v1/userService",
  INCIDENT: "/api/v1/incidentService",
  DOCUMENT: "/api/v1/documentService",
  REPORTING: "/api/v1/reportingService",
  AUDIT: "/api/v1/auditService",
  NOTIFICATION: "/api/v1/notificationService",
} as const;
