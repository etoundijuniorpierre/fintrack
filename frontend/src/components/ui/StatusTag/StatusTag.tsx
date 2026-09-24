// Etiquette de statut : affiche un Tag colore selon une correspondance statut/couleur.
import { memo } from "react";
import { Tag } from "antd";

// Centralise la logique d'interface liee a statut tag props.
export interface StatusTagProps {
  status: string;
  colorMap: Record<string, string>;
  label?: React.ReactNode;
  icon?: React.ReactNode;
  className?: string;
}

// Rend le composant StatusTag pour l'interface status tag.
const StatusTag = memo(
  ({ status, colorMap, label, icon, className }: StatusTagProps) => {
    const color = colorMap[status] ?? "default";
    const displayLabel = label ?? status;

    return (
      <Tag color={color} icon={icon} className={className}>
        {displayLabel}
      </Tag>
    );
  },
);

StatusTag.displayName = "StatusTag";

export default StatusTag;
