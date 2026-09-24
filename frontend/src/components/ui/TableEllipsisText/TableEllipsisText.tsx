// Texte de cellule : limite l'affichage et conserve la valeur complete dans un tooltip.
import { memo } from "react";
import { Typography } from "antd";

const { Text } = Typography;

// Definit les proprietes du texte tronque utilise dans les tableaux.
export interface TableEllipsisTextProps {
  value?: string | null;
  maxWidth?: number;
  fallback?: string;
  strong?: boolean;
}

// Affiche une seule ligne tronquee sans perdre l'acces au contenu complet.
const TableEllipsisText = memo(
  ({
    value,
    maxWidth = 280,
    fallback = "-",
    strong = false,
  }: TableEllipsisTextProps) => {
    const displayValue = value?.trim() || fallback;

    return (
      <Text
        strong={strong}
        ellipsis={value?.trim() ? { tooltip: displayValue } : false}
        style={{ display: "inline-block", maxWidth, width: "100%" }}
      >
        {displayValue}
      </Text>
    );
  },
);

TableEllipsisText.displayName = "TableEllipsisText";

export default TableEllipsisText;
