// Controles a icone seule : un libelle et un bouton n'affichant qu'une icone, avec une infobulle.
import { memo } from "react";
import type { ReactNode } from "react";
import { Tooltip } from "antd";
import clsx from "clsx";
import Button from "../Button/Button";
import type { ButtonProps } from "../Button/Button";
import styles from "./IconOnlyControl.module.scss";

// Centralise la logique d'interface liee a icon only label props.
export interface IconOnlyLabelProps {
  label: string;
  icon: ReactNode;
  className?: string;
}

// Rend le composant IconOnlyLabel pour l'interface icon only control.
export const IconOnlyLabel = memo(
  ({ label, icon, className }: IconOnlyLabelProps) => (
    <Tooltip title={label}>
      <span className={clsx(styles.label, className)} aria-label={label}>
        {icon}
      </span>
    </Tooltip>
  ),
);

IconOnlyLabel.displayName = "IconOnlyLabel";

// Centralise la logique d'interface liee a icon only button props.
export interface IconOnlyButtonProps extends Omit<ButtonProps, "children"> {
  label: string;
  tooltip?: string;
  iconClassName?: string;
}

// Rend le composant IconOnlyButton.
export const IconOnlyButton = memo(
  ({
    label,
    tooltip = label,
    className,
    icon,
    iconClassName,
    ...buttonProps
  }: IconOnlyButtonProps) => (
    <Tooltip title={tooltip}>
      <Button
        {...buttonProps}
        icon={
          iconClassName ? <span className={iconClassName}>{icon}</span> : icon
        }
        className={clsx(styles.button, className)}
        aria-label={label}
      />
    </Tooltip>
  ),
);

IconOnlyButton.displayName = "IconOnlyButton";
