// Declarations de types TypeScript du domaine.

import type { UserSummaryResponse } from "../incident/types";
import type { PagedResponse } from "../user/types";

// Centralise la logique d'interface liee a attachment incident resume.
export interface AttachmentIncidentSummary {
  id: string;
  title: string;
  status?: string;
}

// Centralise la logique d'interface liee a attachment response.
export interface AttachmentResponse {
  id: string;
  createdAt: string;
  updatedAt: string;
  incidentId?: string;
  /** Renseigné pour une pièce jointe de commentaire (sinon PJ d'incident). */
  commentId?: string;
  incident?: AttachmentIncidentSummary;
  category?: string;
  filename: string;
  storagePath: string;
  fileSize: number;
  mimeType?: string;
  uploadedById: string;
  uploadedBy?: UserSummaryResponse;
  uploadedAt: string;
}

// Centralise la logique d'interface liee a attachment metadata response.
export type AttachmentMetadataResponse = AttachmentResponse;

// Centralise la logique d'interface liee a attachment metadata page response.
export type AttachmentMetadataPageResponse =
  PagedResponse<AttachmentMetadataResponse>;

// Centralise la logique d'interface liee a attachment metadata request.
export interface AttachmentMetadataRequest {
  incidentId?: string;
  filename: string;
  storagePath?: string;
  fileSize: number;
  mimeType?: string;
  uploadedBy?: string;
  uploadedById?: string;
  uploadedAt?: string;
}
