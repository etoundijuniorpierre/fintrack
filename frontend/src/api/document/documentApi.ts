// Client API pour le microservice de gestion documentaire.

import apiClient from "../client";
import { DOCUMENT_ENDPOINTS } from "./endpoints/endpoints";
import type { AttachmentResponse } from "./types";
import i18n from "../../i18n";
import {
  isAttachmentSizeValid,
  MAX_ATTACHMENT_SIZE_BYTES,
} from "../../utils/attachments/attachmentValidation";

const assertUploadSize = (file: File) => {
  if (!isAttachmentSizeValid(file)) {
    throw new Error(
      i18n.t(
        file.size === 0
          ? "incidents.attachments.empty_file"
          : "incidents.attachments.too_large",
        { name: file.name, limit: MAX_ATTACHMENT_SIZE_BYTES / (1024 * 1024) },
      ),
    );
  }
};

// Une reprise du meme fichier et de la meme cible conserve son identifiant.
const uploadIds = new WeakMap<File, Map<string, string>>();
const uploadIdFor = (file: File, scope: string): string => {
  let scopes = uploadIds.get(file);
  if (!scopes) {
    scopes = new Map();
    uploadIds.set(file, scopes);
  }
  let id = scopes.get(scope);
  if (!id) {
    id = crypto.randomUUID();
    scopes.set(scope, id);
  }
  return id;
};

// Prepare document api pour document API.
export const documentApi = {
  getByIncidentId: async (
    incidentId: string,
  ): Promise<AttachmentResponse[]> => {
    const { data } = await apiClient.get<AttachmentResponse[]>(
      DOCUMENT_ENDPOINTS.ATTACHMENTS.BY_INCIDENT(incidentId),
    );
    return data;
  },

  upload: async (
    file: File,
    incidentId: string,
    options?: { commentId?: string; category?: string },
  ): Promise<AttachmentResponse> => {
    assertUploadSize(file);
    const formData = new FormData();
    formData.append("file", file);
    formData.append("uploadId", uploadIdFor(file, JSON.stringify([incidentId, options?.commentId, options?.category])));
    // Ne pas fixer Content-Type ici : l'intercepteur de requete retire l'en-tete
    // par defaut pour les FormData afin que le navigateur ajoute la boundary
    // multipart correcte (sinon le backend renvoie 500 sur @RequestPart).
    let url = `${DOCUMENT_ENDPOINTS.ATTACHMENTS.UPLOAD}?incidentId=${encodeURIComponent(incidentId)}`;
    if (options?.commentId) {
      url += `&commentId=${encodeURIComponent(options.commentId)}`;
    }
    if (options?.category) {
      url += `&category=${encodeURIComponent(options.category)}`;
    }
    const { data } = await apiClient.post<AttachmentResponse>(url, formData);
    return data;
  },

  uploadAvatar: async (file: File): Promise<AttachmentResponse> => {
    assertUploadSize(file);
    const formData = new FormData();
    formData.append("file", file);
    formData.append("uploadId", uploadIdFor(file, "avatar"));
    const { data } = await apiClient.post<AttachmentResponse>(
      DOCUMENT_ENDPOINTS.ATTACHMENTS.UPLOAD_AVATAR,
      formData,
    );
    return data;
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(DOCUMENT_ENDPOINTS.ATTACHMENTS.BY_ID(id));
  },

  download: async (id: string): Promise<Blob> => {
    const { data } = await apiClient.get<Blob>(
      DOCUMENT_ENDPOINTS.ATTACHMENTS.DOWNLOAD(id),
      { responseType: "blob" },
    );
    return data;
  },

  getDownloadUrl: (id: string): string =>
    DOCUMENT_ENDPOINTS.ATTACHMENTS.DOWNLOAD(id),
};
