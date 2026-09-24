import React, { memo } from "react";
import styles from "./SectionTitle.module.scss";
import clsx from "clsx";
import { designTokens } from "../../../theme/tokens";

export interface SectionTitleProps {
  title: React.ReactNode;
  size?: "small" | "default" | "large";
  className?: string;
  style?: React.CSSProperties;
  color?: string; // Permet d'écraser la couleur du décorateur si besoin (ex: rouge pour erreur)
}

/**
 * Composant de titre de section avec décorateur visuel unifié.
 * Remplace les anciennes pilules corail.
 */
const SectionTitle = memo(
  ({
    title,
    size = "default",
    className,
    style,
    color = designTokens.colorPrimary,
  }: SectionTitleProps) => {
    return (
      <div
        className={clsx(styles.titleWrapper, styles[size], className)}
        style={style}
      >
        <div
          className={styles.decorator}
          style={{ backgroundColor: color }}
          aria-hidden="true"
        />
        <h3 className={styles.title}>{title}</h3>
      </div>
    );
  },
);

SectionTitle.displayName = "SectionTitle";

export default SectionTitle;
