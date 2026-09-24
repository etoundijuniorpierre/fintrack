// Utilitaire de validation des pieces jointes avant televersement.

// Types acceptes pour les pieces jointes : documents bureautiques + images courantes.
export const ACCEPTED_ATTACHMENT_EXTENSIONS = [
  ".pdf",
  ".doc",
  ".docx",
  ".xls",
  ".xlsx",
  ".csv",
  ".txt",
  ".rtf",
  ".odt",
  ".ods",
  ".png",
  ".jpg",
  ".jpeg",
  ".gif",
  ".bmp",
  ".webp",
  ".heic",
  ".zip",
];

const ACCEPTED_ATTACHMENT_MIME_EXACT = new Set([
  "application/pdf",
  "image/png", "image/jpeg", "image/gif", "image/bmp", "image/webp", "image/heic",
  "application/msword",
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  "application/vnd.ms-excel",
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  "application/vnd.oasis.opendocument.text",
  "application/vnd.oasis.opendocument.spreadsheet",
  "application/zip",
  "application/x-zip-compressed",
  "image/x-ms-bmp",
  "text/csv",
  "text/plain",
  "text/rtf",
  "application/rtf",
]);

// Valeur accept partagee par les composants d'upload.
export const ACCEPTED_ATTACHMENT_ACCEPT = ACCEPTED_ATTACHMENT_EXTENSIONS.join(",");

// Taille maximale d'une piece jointe : 150 Mo, alignee avec le backend.
export const MAX_ATTACHMENT_SIZE_BYTES = 150 * 1024 * 1024;

// Verifie si le format du fichier est autorise.
export const isAcceptedAttachment = (file: File): boolean => {
  const type = (file.type || "").toLowerCase();
  const name = file.name.toLowerCase();
  return ACCEPTED_ATTACHMENT_EXTENSIONS.some((ext) => name.endsWith(ext)) &&
    (!type || type === "application/octet-stream" || ACCEPTED_ATTACHMENT_MIME_EXACT.has(type));
};

// Refuse les fichiers vides et ceux depassant la taille maximale.
export const isAttachmentSizeValid = (file: File): boolean =>
  file.size > 0 && file.size <= MAX_ATTACHMENT_SIZE_BYTES;
