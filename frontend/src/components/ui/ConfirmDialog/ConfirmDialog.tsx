// Boite de dialogue de confirmation basee sur Popconfirm d'Ant Design.
import { memo } from "react";
import { Popconfirm } from "antd";
import type { ReactNode } from "react";

// Centralise la logique d'interface liee a confirm dialog props.
export interface ConfirmDialogProps {
  title: string;
  description?: string;
  onConfirm: () => void;
  onCancel?: () => void;
  danger?: boolean;
  children: ReactNode;
  confirmText?: string;
  cancelText?: string;
  confirmLoading?: boolean;
  className?: string;
}

// Rend le composant ConfirmDialog.
const ConfirmDialog = memo(
  ({
    title,
    description,
    onConfirm,
    onCancel,
    danger = false,
    children,
    confirmText = "Confirm",
    cancelText = "Cancel",
    confirmLoading = false,
    className,
  }: ConfirmDialogProps) => {
    return (
      <Popconfirm
        title={title}
        description={description}
        onConfirm={onConfirm}
        onCancel={onCancel}
        okText={confirmText}
        cancelText={cancelText}
        okButtonProps={{
          danger,
          loading: confirmLoading,
        }}
        className={className}
      >
        {children}
      </Popconfirm>
    );
  },
);

ConfirmDialog.displayName = "ConfirmDialog";

export default ConfirmDialog;
