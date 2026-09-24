// Mise en page en deux colonnes : une zone principale et une barre laterale a largeur configurable.
import React, { memo } from "react";
import type { ReactNode } from "react";
import styles from "./SplitLayout.module.scss";

// Centralise la logique d'interface liee a split layout props.
export interface SplitLayoutProps {
  main: ReactNode;
  sidebar: ReactNode;
  sidebarWidth?: string;
  className?: string;
  style?: React.CSSProperties;
}

// Rend le composant SplitLayout.
const SplitLayout = memo(
  ({
    main,
    sidebar,
    sidebarWidth = "320px",
    className,
    style,
  }: SplitLayoutProps) => {
    return (
      <div
        className={`${styles.container} ${className ?? ""}`}
        style={
          {
            "--split-sidebar-width": sidebarWidth,
            ...style,
          } as React.CSSProperties
        }
        data-testid="split-layout"
      >
        <div className={styles.main} data-testid="split-layout-main">
          {main}
        </div>
        <div className={styles.sidebar} data-testid="split-layout-sidebar">
          {sidebar}
        </div>
      </div>
    );
  },
);

SplitLayout.displayName = "SplitLayout";

export default SplitLayout;
