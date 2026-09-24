// Badge de criticite : affiche une pastille coloree selon la variante (low, medium, high, critical).
import { memo } from "react";
import clsx from "clsx";
import styles from "./Badge.module.scss";

import type { BadgeVariant } from "../types";

// Centralise la logique d'interface liee a badge props.
export interface BadgeProps {
  variant: BadgeVariant;
  label: string;
  className?: string;
}

// Rend le composant Badge pour l'interface badge.
const Badge = memo(({ variant, label, className }: BadgeProps) => {
  return (
    <span
      className={clsx(styles.badge, styles[variant], className)}
      aria-label={`Criticality: ${label}`}
    >
      {label}
    </span>
  );
});

Badge.displayName = "Badge";

export default Badge;
