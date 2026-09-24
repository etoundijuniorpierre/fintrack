// Bouton generique : enveloppe le Button d'Ant Design avec gestion des variantes, tailles et chargement.
import React, { memo } from "react";
import { Button as AntButton } from "antd";
import { LoadingOutlined } from "@ant-design/icons";
import type { ButtonHTMLAttributes } from "react";
import clsx from "clsx";
import styles from "./Button.module.scss";

import type {
  ButtonVariant,
  ComponentSize,
  ButtonHtmlType,
  AntdSize,
} from "../types";

// Centralise la logique d'interface liee a button props.
export interface ButtonProps extends Omit<
  ButtonHTMLAttributes<HTMLButtonElement>,
  "type"
> {
  variant?: ButtonVariant;
  size?: ComponentSize;
  loading?: boolean;
  disabled?: boolean;
  icon?: React.ReactNode;
  href?: string;
  target?: string;
  rel?: string;
  block?: boolean;
  htmlType?: ButtonHtmlType;
  onClick?: React.MouseEventHandler<HTMLButtonElement>;
  children?: React.ReactNode;
  className?: string;
}

// Correspondance entre les tailles du composant et celles attendues par Ant Design.
const sizeMap: Record<ComponentSize, AntdSize> = {
  sm: "small",
  md: "middle",
  lg: "large",
};

// Rend le composant Button.
const Button = memo(
  ({
    variant = "primary",
    size = "md",
    loading = false,
    disabled = false,
    icon,
    htmlType = "button",
    onClick,
    children,
    className,
    style,
    ...rest
  }: ButtonProps) => {
    const isDisabled = disabled || loading;

    return (
      <AntButton
        size={sizeMap[size]}
        disabled={isDisabled}
        htmlType={htmlType}
        icon={
          loading ? (
            // Spinner decoratif (aria-hidden) en couleur du texte du bouton :
            // visible sur toutes les variantes sans alterer le libelle accessible.
            <LoadingOutlined aria-hidden spin style={{ color: "currentColor" }} />
          ) : (
            icon
          )
        }
        onClick={isDisabled ? undefined : onClick}
        className={clsx(
          styles.base,
          styles[variant],
          { [styles.disabled]: isDisabled },
          className,
        )}
        style={style}
        aria-disabled={isDisabled}
        {...(rest as Record<string, unknown>)}
      >
        {children}
      </AntButton>
    );
  },
);

Button.displayName = "Button";

export default Button;
