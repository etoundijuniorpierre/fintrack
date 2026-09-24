// Detecte les formats de pieces jointes que FinTrack sait afficher localement.

// Indique si le fichier est une image affichee nativement par le navigateur.
export const isImageAttachment = (mimeType?: string): boolean =>
  ["image/png", "image/jpeg", "image/gif", "image/bmp", "image/webp"].includes((mimeType || "").toLowerCase());

// Indique si le fichier est un PDF affiche nativement par le navigateur.
export const isPdfAttachment = (
  mimeType?: string,
  filename?: string,
): boolean => {
  const type = (mimeType || "").toLowerCase();
  const name = (filename || "").toLowerCase();
  return type === "application/pdf" || (!type && name.endsWith(".pdf"));
};

// Indique si le fichier est un DOCX pris en charge par la visionneuse.
export const isWordAttachment = (
  mimeType?: string,
  filename?: string,
): boolean => {
  const type = (mimeType || "").toLowerCase();
  const name = (filename || "").toLowerCase();
  return (
    type ===
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
    (!type && name.endsWith(".docx"))
  );
};

// Indique si le fichier est un XLSX pris en charge par la visionneuse.
export const isExcelAttachment = (
  mimeType?: string,
  filename?: string,
): boolean => {
  const type = (mimeType || "").toLowerCase();
  const name = (filename || "").toLowerCase();
  return (
    type ===
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ||
    (!type && name.endsWith(".xlsx"))
  );
};

// Indique si le fichier est un XLS historique. Format binaire (BIFF) et non XML :
// il se lit avec un autre analyseur que le XLSX, d'ou la distinction.
export const isLegacyExcelAttachment = (
  mimeType?: string,
  filename?: string,
): boolean => {
  const type = (mimeType || "").toLowerCase();
  const name = (filename || "").toLowerCase();
  return type === "application/vnd.ms-excel" || (!type && name.endsWith(".xls"));
};

// Indique si le fichier est un classeur, quelle que soit sa generation.
export const isSpreadsheetAttachment = (
  mimeType?: string,
  filename?: string,
): boolean =>
  isExcelAttachment(mimeType, filename) ||
  isLegacyExcelAttachment(mimeType, filename);

// Indique si FinTrack peut consulter le fichier sans service externe.
export const isPreviewableDocument = (
  mimeType?: string,
  filename?: string,
): boolean =>
  isImageAttachment(mimeType) ||
  isPdfAttachment(mimeType, filename) ||
  isWordAttachment(mimeType, filename) ||
  isSpreadsheetAttachment(mimeType, filename);
