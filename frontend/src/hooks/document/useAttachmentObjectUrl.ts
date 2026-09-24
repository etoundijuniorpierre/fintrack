// Hook : object URL d'une piece jointe image (blob authentifie), revoque au demontage.

import { useEffect, useState } from "react";
import { documentApi } from "../../api/document/documentApi";

// Retourne l'URL objet de l'image et un indicateur d'échec de chargement.
export const useAttachmentObjectUrl = (attachmentId: string) => {
  const [src, setSrc] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let objectUrl: string | null = null;
    let active = true;

    documentApi
      .download(attachmentId)
      .then((blob) => {
        if (!active) return;
        objectUrl = URL.createObjectURL(blob);
        setSrc(objectUrl);
      })
      .catch(() => {
        if (active) setFailed(true);
      });

    return () => {
      active = false;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [attachmentId]);

  return { src, failed };
};
