// Indicateur de chargement : enveloppe le Spin d'Ant Design avec gestion de la taille et du centrage.
import { memo } from "react";
import { Spin } from "antd";
import styles from "./Spinner.module.scss";
import type { ComponentSize, AntdSpinSize } from "../types";

// Centralise la logique d'interface liee a spinner props.
export interface SpinnerProps {
  size?: ComponentSize;
  centered?: boolean;
  label?: string;
  className?: string;
}

// Correspondance entre les tailles du composant et celles attendues par Ant Design.
const sizeMap: Record<ComponentSize, AntdSpinSize> = {
  sm: "small",
  md: "default",
  lg: "large",
};

// Rend le composant Spinner pour l'interface spinner.
const Spinner = memo(
  ({ size = "md", centered = false, label, className }: SpinnerProps) => {
    const spin = (
      <Spin
        size={sizeMap[size]}
        aria-label={label ?? "Loading"}
        className={className}
      />
    );

    if (centered) {
      return (
        <div className={styles.centered} role="status">
          {spin}
        </div>
      );
    }

    return <span role="status">{spin}</span>;
  },
);

Spinner.displayName = "Spinner";

export default Spinner;
