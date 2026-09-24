// Definition des constantes d'endpoints d'API.

import { SERVICE_BASES } from "../../base";

const BASE = SERVICE_BASES.DOCUMENT;

// Expose la constante DOCUMENT_ENDPOINTS utilisee par endpoints.
export const DOCUMENT_ENDPOINTS = {
  ATTACHMENTS: {
    BASE: `${BASE}/attachments`,
    ALL: `${BASE}/attachments/all`,
    BY_ID: (id: string) => `${BASE}/attachments/${id}`,
    BY_INCIDENT: (incidentId: string) =>
      `${BASE}/attachments/incident/${incidentId}`,
    BY_UPLOADER: (uploadedById: string) =>
      `${BASE}/attachments/uploaded-by/${uploadedById}`,
    UPLOAD: `${BASE}/attachments/upload`,
    UPLOAD_AVATAR: `${BASE}/attachments/avatar`,
    DOWNLOAD: (id: string) => `${BASE}/attachments/${id}/download`,
  },
} as const;
