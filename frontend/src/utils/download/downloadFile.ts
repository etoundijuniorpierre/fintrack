// Utilitaire de telechargement : centralise la creation temporaire de liens de fichiers.

// Declenche le telechargement navigateur pour un contenu Blob ou BlobPart.
export const downloadFile = (
  content: Blob | BlobPart,
  filename: string,
  mimeType?: string,
): void => {
  const blob =
    content instanceof Blob
      ? content
      : new Blob([content], mimeType ? { type: mimeType } : undefined);
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => window.URL.revokeObjectURL(url), 1000);
};
