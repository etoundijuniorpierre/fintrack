// Fenetre modale : enveloppe le Modal d'Ant Design avec des boutons de confirmation et d'annulation.
import { memo } from "react";
import { Modal as AntModal } from "antd";
import type { ReactNode } from "react";
import Button from "../Button/Button";
import styles from "./Modal.module.scss";

// Centralise la logique d'interface liee a modal props.
export interface ModalProps {
  open: boolean;
  title: string;
  children: ReactNode;
  onClose: () => void;
  onConfirm?: () => void;
  confirmLoading?: boolean;
  confirmText?: string;
  cancelText?: string;
  danger?: boolean;
  width?: number | string;
  footer?: boolean;
  className?: string;
  destroyOnClose?: boolean;
  destroyOnHidden?: boolean;
  // Empeche l'annulation/fermeture pendant une operation en cours.
  cancelDisabled?: boolean;
  closable?: boolean;
  maskClosable?: boolean;
}

// Rend le composant Modal.
const Modal = memo(
  ({
    open,
    title,
    children,
    onClose,
    onConfirm,
    confirmLoading = false,
    confirmText = "Confirm",
    cancelText = "Cancel",
    danger = false,
    width = 520,
    footer = true,
    className,
    destroyOnClose = false,
    destroyOnHidden,
    cancelDisabled = false,
    closable = true,
    maskClosable = true,
  }: ModalProps) => {
    const shouldDestroyOnHidden = destroyOnHidden ?? destroyOnClose;

    // Construit les boutons du pied de page (annuler, et confirmer si un handler est fourni).
    const footerContent = footer
      ? [
          <Button
            key="cancel"
            variant="secondary"
            onClick={onClose}
            disabled={cancelDisabled}
          >
            {cancelText}
          </Button>,
          onConfirm && (
            <Button
              key="confirm"
              variant={danger ? "danger" : "primary"}
              loading={confirmLoading}
              onClick={onConfirm}
            >
              {confirmText}
            </Button>
          ),
        ].filter(Boolean)
      : null;

    return (
      <AntModal
        open={open}
        title={title}
        onCancel={onClose}
        footer={footerContent}
        width={width}
        className={className}
        destroyOnHidden={shouldDestroyOnHidden}
        closable={closable}
        maskClosable={maskClosable}
        classNames={{ footer: styles.footer }}
      >
        {children}
      </AntModal>
    );
  },
);

Modal.displayName = "Modal";

export default Modal;
