// Etat vide : affiche un message, une icone et une action optionnelle lorsqu'aucune donnee n'est disponible.
import { memo } from "react";
import type { ReactNode } from "react";
import clsx from "clsx";
import styles from "./EmptyState.module.scss";

// Centralise la logique d'interface liee a empty state props.
export interface EmptyStateProps {
  icon?: ReactNode;
  title: string;
  description?: string;
  action?: ReactNode;
  className?: string;
}

// Rend le composant EmptyState pour l'interface empty state.
const EmptyState = memo(
  ({ icon, title, description, action, className }: EmptyStateProps) => {
    return (
      <div
        className={clsx(styles.container, className)}
        role="status"
        aria-label={title}
      >
        {icon && (
          <div className={styles.icon} aria-hidden="true">
            {icon}
          </div>
        )}
        <p className={styles.title}>{title}</p>
        {description && <p className={styles.description}>{description}</p>}
        {action && <div>{action}</div>}
      </div>
    );
  },
);

EmptyState.displayName = "EmptyState";

export default EmptyState;
