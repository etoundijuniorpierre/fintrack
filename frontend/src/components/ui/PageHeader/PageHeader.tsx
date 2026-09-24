// En-tete de page : affiche un titre, un sous-titre, un bouton de retour et des actions optionnelles.
import { Children, memo } from "react";
import type { ReactNode } from "react";
import clsx from "clsx";
import { navigationIcons } from "../../../utils/icons/appIcons";
import styles from "./PageHeader.module.scss";

// Centralise la logique d'interface liee a page header props.
export interface PageHeaderProps {
  title: string;
  subtitle?: string;
  onBack?: () => void;
  backLabel?: string;
  className?: string;
  titleTag?: ReactNode;
  actions?: ReactNode | ReactNode[];
  children?: ReactNode;
}

// Rend le composant PageHeader pour l'interface page en-tete.
const PageHeader = memo(
  ({
    title,
    subtitle,
    onBack,
    backLabel,
    className,
    titleTag,
    actions,
    children,
  }: PageHeaderProps) => {
    const actionItems = actions ? Children.toArray(actions) : [];

    return (
      <div className={clsx(styles.container, className)}>
        <div className={styles.titleGroup}>
          <div className={styles.titleRow}>
            {onBack && (
              <button
                type="button"
                className={styles.backButton}
                onClick={onBack}
                aria-label={backLabel ?? "Return"}
              >
                {navigationIcons.back}
              </button>
            )}
            <h1 className={styles.title}>{title}</h1>
            {titleTag && <div className={styles.titleTag}>{titleTag}</div>}
          </div>
          {subtitle && <p className={styles.subtitle}>{subtitle}</p>}
          {children}
        </div>

        {actionItems.length > 0 && (
          <div className={styles.actions} data-testid="page-header-actions">
            {actionItems}
          </div>
        )}
      </div>
    );
  },
);

PageHeader.displayName = "PageHeader";

export default PageHeader;
