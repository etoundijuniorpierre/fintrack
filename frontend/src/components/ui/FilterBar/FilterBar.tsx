// Barre de filtres : regroupe les controles de filtrage, un compteur de filtres actifs et des actions.
import { memo } from "react";
import { Badge } from "antd";
import type { ReactNode } from "react";
import styles from "./FilterBar.module.scss";
import clsx from "clsx";

// Centralise la logique d'interface liee a filter bar props.
export interface FilterBarProps {
  children: ReactNode;
  activeCount?: number;
  className?: string;
  actions?: ReactNode[];
  extra?: ReactNode;
}

// Rend le composant FilterBar pour l'interface filter bar.
const FilterBar = memo(
  ({ children, activeCount, className, actions, extra }: FilterBarProps) => {
    const hasActions = (actions && actions.length > 0) || extra;
    return (
      <div>
        <div
          className={clsx(styles.container, className)}
          role="search"
          aria-label="Filters"
        >
          {children}
          {activeCount !== undefined && activeCount > 0 && (
            <Badge
              count={activeCount}
              className={styles.badge}
              aria-label={`${activeCount} active filter${activeCount > 1 ? "s" : ""}`}
            />
          )}
        </div>
        {hasActions && (
          <div className={styles.actions} data-testid="page-header-actions">
            {actions}
            {extra}
          </div>
        )}
      </div>
    );
  },
);

FilterBar.displayName = "FilterBar";

export default FilterBar;
