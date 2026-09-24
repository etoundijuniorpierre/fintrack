// Champ de formulaire : enveloppe Form.Item d'Ant Design et gere aussi un mode lecture seule.
import { memo } from "react";
import { Form } from "antd";
import type { FormItemProps } from "antd";
import type { ReactNode } from "react";
import type { Rule } from "antd/es/form";
import clsx from "clsx";
import styles from "./FormField.module.scss";

// Centralise la logique d'interface liee a champ de formulaire props.
export interface FormFieldProps extends Omit<FormItemProps, "children"> {
  name: string | (string | number)[];
  label?: string;
  required?: boolean;
  rules?: Rule[];
  children: ReactNode;
  errorMessage?: string;
  help?: string;
  className?: string;
  initialValue?: unknown;
  fullWidth?: boolean;
  tooltip?: string;
  /**
   * Si vrai, le champ est rendu comme un bloc libelle/valeur en lecture seule
   * au lieu d'un Form.Item editable. Aligne visuellement le mode consultation
   * et le mode edition sans dupliquer le balisage.
   */
  readOnly?: boolean;
  /**
   * Valeur affichee en mode lecture seule. Revient a `emptyText` si nulle.
   * Ignoree lorsque `readOnly` est faux.
   */
  displayValue?: ReactNode;
  /** Texte de remplacement affiche en lecture seule quand `displayValue` est vide. */
  emptyText?: string;
}

// Indique si une valeur est consideree comme vide (null, undefined ou chaine vide).
const isEmptyValue = (value: ReactNode): boolean =>
  value === null || value === undefined || value === "";

// Rend le composant FormField.
const FormField = memo(
  ({
    name,
    label,
    required = false,
    rules = [],
    children,
    errorMessage,
    help,
    className,
    initialValue,
    fullWidth = true,
    tooltip,
    readOnly = false,
    displayValue,
    emptyText = "—",
    ...restProps
  }: FormFieldProps) => {
    if (readOnly) {
      const empty = isEmptyValue(displayValue);

      return (
        <div className={clsx(styles.readOnlyField, className)}>
          {label && <span className={styles.readOnlyLabel}>{label}</span>}
          <span
            className={clsx(styles.readOnlyValue, {
              [styles.readOnlyEmpty]: empty,
            })}
          >
            {empty ? emptyText : displayValue}
          </span>
        </div>
      );
    }

    // Evite d'ajouter une regle "requise" en double si elle est deja presente dans les regles.
    const hasRequiredRule = rules.some(
      (rule) => typeof rule === "object" && "required" in rule && rule.required,
    );

    const combinedRules: Rule[] = [
      ...(required && !hasRequiredRule ? [{ required: true }] : []),
      ...rules,
    ];

    return (
      <Form.Item
        name={name}
        label={label}
        required={required || hasRequiredRule}
        rules={combinedRules}
        validateStatus={errorMessage ? "error" : undefined}
        help={errorMessage ?? help}
        className={clsx(className, { [styles.fullWidth]: fullWidth })}
        initialValue={initialValue}
        tooltip={tooltip}
        {...restProps}
      >
        {children}
      </Form.Item>
    );
  },
);

FormField.displayName = "FormField";

export default FormField;
