// Carte de section : enveloppe le composant Card avec un titre et une barre d'outils optionnels.
import React, { memo } from "react";
import type { ReactNode } from "react";
import Card from "../../ui/Card/Card";

// Centralise la logique d'interface liee a section card props.
export interface SectionCardProps {
  title?: string;
  toolbar?: ReactNode;
  children?: ReactNode;
  className?: string;
  style?: React.CSSProperties;
  noPadding?: boolean;
  loading?: boolean;
}

// Rend le composant SectionCard.
const SectionCard = memo(
  ({
    title,
    toolbar,
    children,
    className,
    style,
    noPadding = false,
    loading = false,
  }: SectionCardProps) => {
    return (
      <Card
        title={title}
        extra={toolbar}
        className={className}
        style={style}
        noPadding={noPadding}
        loading={loading}
      >
        {children}
      </Card>
    );
  },
);

SectionCard.displayName = "SectionCard";

export default SectionCard;
