// Carte generique : enveloppe le Card d'Ant Design avec titre, pied de page et options de style.
import React, { memo } from "react";
import { Card as AntCard } from "antd";
import type { ReactNode } from "react";
import clsx from "clsx";
import styles from "./Card.module.scss";

// Centralise la logique d'interface liee a card props.
export interface CardProps {
  title?: ReactNode;
  extra?: ReactNode;
  footer?: ReactNode;
  noPadding?: boolean;
  children?: ReactNode;
  className?: string;
  style?: React.CSSProperties;
  loading?: boolean;
  hoverable?: boolean;
  onClick?: React.MouseEventHandler<HTMLDivElement>;
}

// Rend le composant Card.
const Card = memo(
  ({
    title,
    extra,
    footer,
    noPadding = false,
    children,
    className,
    style,
    loading = false,
    hoverable = false,
    onClick,
  }: CardProps) => {
    return (
      <AntCard
        title={title}
        extra={extra}
        loading={loading}
        hoverable={hoverable}
        className={clsx(styles.card, className)}
        style={style}
        onClick={onClick}
        styles={{
          body: noPadding ? { padding: 0 } : { padding: "var(--card-padding)" },
        }}
      >
        {children}
        {footer && <div className={styles.footer}>{footer}</div>}
      </AntCard>
    );
  },
);

Card.displayName = "Card";

export default Card;
