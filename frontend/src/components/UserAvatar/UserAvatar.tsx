// Composant React : porte l'interface de utilisateur avatar.

import { Avatar } from "antd";
import { useEffect, useState } from "react";
import { documentApi } from "../../api/document/documentApi";
import { headerIcons } from "../../utils/icons/appIcons";

// Definit les proprietes attendues par le composant UserAvatar.
interface UserAvatarProps {
  avatarDocumentId?: string | null;
  size?: number | "large" | "small" | "default";
  className?: string;
}

// Rend le composant UserAvatar pour l'interface utilisateur avatar.
const UserAvatar = ({
  avatarDocumentId,
  size = "default",
  className,
}: UserAvatarProps) => {
  const [avatarData, setAvatarData] = useState<{
    id?: string | null;
    url?: string;
  }>({
    id: avatarDocumentId,
    url: undefined,
  });

  useEffect(() => {
    let objectUrl: string | undefined;
    let cancelled = false;

    if (!avatarDocumentId) {
      return undefined;
    }

    documentApi
      .download(avatarDocumentId)
      .then((blob) => {
        if (cancelled) return;
        objectUrl = window.URL.createObjectURL(blob);
        setAvatarData({ id: avatarDocumentId, url: objectUrl });
      })
      .catch(() => {
        if (!cancelled) {
          setAvatarData({ id: avatarDocumentId, url: undefined });
        }
      });

    return () => {
      cancelled = true;
      if (objectUrl) {
        window.URL.revokeObjectURL(objectUrl);
      }
    };
  }, [avatarDocumentId]);

  const displaySrc =
    avatarData.id === avatarDocumentId ? avatarData.url : undefined;

  return (
    <Avatar
      src={displaySrc}
      icon={!displaySrc ? headerIcons.user : undefined}
      size={size}
      className={className}
    />
  );
};

export default UserAvatar;
