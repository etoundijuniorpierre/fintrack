// Barre d'actions confirmer/annuler centralisee : impose le rendu du design-system
// (annuler = secondary contoure, confirmer = primary marque, destructif = danger),
// pour une distinction nette et coherente sur tous les formulaires et modales.
import { memo, type ReactNode } from "react";
import { useTranslation } from "react-i18next";
import clsx from "clsx";
import Button from "../Button/Button";
import styles from "./FormActions.module.scss";

// Centralise la logique d'interface liee a form actions props.
export interface FormActionsProps {
  submitText: string;
  onSubmit?: () => void;
  onCancel?: () => void;
  cancelText?: string;
  submitIcon?: ReactNode;
  loading?: boolean;
  submitDisabled?: boolean;
  danger?: boolean;
  htmlType?: "submit" | "button";
  align?: "start" | "end";
  className?: string;
}

// Rend la paire annuler / confirmer alignee sur le design-system.
const FormActions = memo(
  ({
    submitText,
    onSubmit,
    onCancel,
    cancelText,
    submitIcon,
    loading = false,
    submitDisabled = false,
    danger = false,
    htmlType = "submit",
    align = "end",
    className,
  }: FormActionsProps) => {
    const { t } = useTranslation();
    return (
      <div
        className={clsx(
          styles.actions,
          align === "start" && styles.start,
          className,
        )}
      >
        {onCancel && (
          <Button variant="secondary" onClick={onCancel}>
            {cancelText ?? t("common.cancel")}
          </Button>
        )}
        <Button
          variant={danger ? "danger" : "primary"}
          htmlType={htmlType}
          icon={submitIcon}
          loading={loading}
          disabled={submitDisabled}
          onClick={onSubmit}
        >
          {submitText}
        </Button>
      </div>
    );
  },
);

FormActions.displayName = "FormActions";

export default FormActions;
