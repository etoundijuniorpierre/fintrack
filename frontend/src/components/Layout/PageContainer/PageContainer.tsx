// Conteneur de page : applique la mise en page et les marges standard autour du contenu d'une page.
import { memo } from "react";
import type { ReactNode } from "react";
import clsx from "clsx";
import styles from "./PageContainer.module.scss";

// Centralise la logique d'interface liee a page container props.
export interface PageContainerProps {
  children: ReactNode;
  className?: string;
  style?: React.CSSProperties;
}

// Rend le composant PageContainer pour l'interface page container.
const PageContainer = memo(
  ({ children, className, style }: PageContainerProps) => {
    return (
      <div>
        <div
          className={clsx(styles.container, className)}
          style={style}
          data-testid="page-container"
        >
          {children}
        </div>
      </div>
    );
  },
);

PageContainer.displayName = "PageContainer";

export default PageContainer;
