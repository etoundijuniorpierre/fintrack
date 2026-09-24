// Section pieces jointes d'un incident : liste, televersement et suppression des fichiers.
import {
  App,
  Divider,
  Empty,
  Image,
  List,
  Popconfirm,
  Skeleton,
  Space,
  Typography,
  Upload,
} from "antd";

import { useTranslation } from "react-i18next";
import { memo, useCallback, useMemo, useState } from "react";
import {
  useIncidentAttachments,
  useUploadAttachment,
  useDeleteAttachment,
} from "../../../../../hooks/document/useDocuments";
import { useAttachmentObjectUrl } from "../../../../../hooks/document/useAttachmentObjectUrl";
import { documentApi } from "../../../../../api/document/documentApi";
import type { AttachmentResponse } from "../../../../../api/document/types";
import { formatDate } from "../../../../../utils/formatters/formatters";
import { printImage } from "../../../../../utils/print/printImage";
import { PrinterOutlined } from "@ant-design/icons";
import { Button, IconOnlyButton } from "../../../../../components/ui";
import AttachmentPreviewModal from "../../../../../components/AttachmentPreview/AttachmentPreviewModal";
import { actionIcons, incidentIcons } from "../../../../../utils/icons/appIcons";
import type { UploadRequestOption } from "@rc-component/upload/lib/interface";
import { useAuth } from "../../../../../hooks/auth/useAuth";
import { downloadFile } from "../../../../../utils/download/downloadFile";
import {
  ACCEPTED_ATTACHMENT_ACCEPT,
  MAX_ATTACHMENT_SIZE_BYTES,
  isAcceptedAttachment,
  isAttachmentSizeValid,
} from "../../../../../utils/attachments/attachmentValidation";
import { isPreviewableDocument } from "../../../../../utils/attachments/documentPreview";
import styles from "./AttachmentsSection.module.scss";

// Statuts editables : le createur peut ajouter/retirer une PJ de declaration
// (creation + phases de modification), pas au-dela.
const ATTACHMENT_EDITABLE_STATUSES = new Set<string>([
  "OPEN",
  "PENDING_VALIDATION",
  "REOPENED",
]);

const { Text } = Typography;

// Formate une taille en octets de facon lisible (B, KB, MB).
const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};

// Indique si la pièce jointe est une image.
const isImageAttachment = (mimeType?: string): boolean =>
  Boolean(mimeType && mimeType.startsWith("image/"));


// Aperçu d'une image avec téléchargement et impression directes.
const AttachmentImage = memo(
  ({ attachment }: { attachment: AttachmentResponse }) => {
    const { t } = useTranslation();
    const { src, failed } = useAttachmentObjectUrl(attachment.id);

    const handleDownload = useCallback(async () => {
      try {
        const blob = await documentApi.download(attachment.id);
        downloadFile(blob, attachment.filename);
      } catch { /* handled silently */ }
    }, [attachment.id, attachment.filename]);

    const handlePrint = useCallback(() => {
      if (src) printImage(src, attachment.filename);
    }, [src, attachment.filename]);

    if (failed) return null;
    if (!src) {
      return (
        <Skeleton.Image active className={styles.attachmentImageSkeleton} />
      );
    }

    return (
      <Image
        src={src}
        alt={attachment.filename}
        className={styles.attachmentImage}
        preview={{
          toolbarRender: (_, { icons }) => (
            <Space size={12} className="ant-image-preview-operations">
              {icons.zoomInIcon}
              {icons.zoomOutIcon}
              {icons.rotateLeftIcon}
              {icons.rotateRightIcon}
              <span
                role="button"
                title={t("common.print")}
                onClick={handlePrint}
                className={styles.toolbarDownload}
                style={{ cursor: "pointer", display: "inline-flex", alignItems: "center" }}
              >
                <PrinterOutlined style={{ fontSize: 16 }} />
              </span>
              <span
                role="button"
                title={t("incidents.attachments.download")}
                onClick={handleDownload}
                className={styles.toolbarDownload}
              >
                {actionIcons.download}
              </span>
            </Space>
          ),
        }}
      />
    );
  },
);

AttachmentImage.displayName = "AttachmentImage";

// Definit les proprietes attendues par le composant AttachmentItem.
interface AttachmentItemProps {
  attachment: AttachmentResponse;
  onDelete: (id: string) => void;
  onDownload: (attachment: AttachmentResponse) => void;
  isDeleting: boolean;
  canDelete: boolean;
}

// Rend le composant AttachmentItem pour l'interface attachments section.
const AttachmentItem = memo(
  ({
    attachment,
    onDelete,
    onDownload,
    isDeleting,
    canDelete,
  }: AttachmentItemProps) => {
    const { t } = useTranslation();
    const [previewOpen, setPreviewOpen] = useState(false);

    // Traite la suppression.
    const handleDelete = useCallback(
      () => onDelete(attachment.id),
      [onDelete, attachment.id],
    );
    // Traite le telechargement (clic sur le nom ou l'apercu de la piece jointe).
    const handleDownload = useCallback(
      () => onDownload(attachment),
      [attachment, onDownload],
    );

    // Charge une copie authentifiée de la pièce jointe pour consultation.
    const handlePreview = useCallback(() => {
      if (isPreviewableDocument(attachment.mimeType, attachment.filename)) {
        setPreviewOpen(true);
      }
    }, [attachment.mimeType, attachment.filename]);

    // Ferme l'aperçu et libère l'URL temporaire.
    const uploadedBy = attachment.uploadedBy
      ? `${attachment.uploadedBy.firstName} ${attachment.uploadedBy.lastName}`
      : null;

    const isImage = isImageAttachment(attachment.mimeType);

    return (
      <List.Item className={styles.attachmentItem}>
        <div className={styles.attachmentContent}>
          <div className={styles.attachmentHeader}>
            {/* Le clic sur la zone (icône + nom + infos) ouvre directement l'aperçu si le fichier est prévisualisable, ou le télécharge sinon. */}
            <div
              className={styles.attachmentClickable}
              role="button"
              tabIndex={0}
              title={
                isPreviewableDocument(attachment.mimeType, attachment.filename)
                  ? t("incidents.attachments.preview")
                  : t("incidents.attachments.download")
              }
              onClick={
                isPreviewableDocument(attachment.mimeType, attachment.filename)
                  ? handlePreview
                  : handleDownload
              }
              onKeyDown={(event) => {
                if (event.key === "Enter" || event.key === " ") {
                  event.preventDefault();
                  if (isPreviewableDocument(attachment.mimeType, attachment.filename)) {
                    handlePreview();
                  } else {
                    handleDownload();
                  }
                }
              }}
            >
              <List.Item.Meta
                avatar={
                  <span className={styles.attachmentIcon}>
                    {incidentIcons.attachment}
                  </span>
                }
                title={
                  <Text className={styles.attachmentName}>
                    {attachment.filename}
                  </Text>
                }
                description={
                  <Space size={8} className={styles.attachmentInfo}>
                    <span>{formatFileSize(attachment.fileSize)}</span>
                    {uploadedBy && (
                      <span>
                        · {t("incidents.attachments.uploaded_by")} {uploadedBy}
                      </span>
                    )}
                    <span>· {formatDate(attachment.uploadedAt)}</span>
                  </Space>
                }
              />
            </div>
            <Space size={4}>
              <IconOnlyButton
                variant="text"
                size="sm"
                icon={actionIcons.download}
                onClick={handleDownload}
                label={t("incidents.attachments.download")}
              />
              {canDelete && (
                <Popconfirm
                  title={t("incidents.attachments.delete_confirm")}
                  description={t("incidents.attachments.delete_ask")}
                  onConfirm={handleDelete}
                  okType="danger"
                >
                  <IconOnlyButton
                    variant="text"
                    size="sm"
                    icon={actionIcons.delete}
                    loading={isDeleting}
                    label={t("incidents.attachments.delete")}
                  />
                </Popconfirm>
              )}
            </Space>
          </div>
          {isImage && (
            <AttachmentImage attachment={attachment} />
          )}
        </div>
        <AttachmentPreviewModal
          attachment={attachment}
          open={previewOpen}
          onClose={() => setPreviewOpen(false)}
        />
      </List.Item>
    );
  },
);

AttachmentItem.displayName = "AttachmentItem";

// Definit les proprietes attendues par le composant AttachmentsSection.
interface AttachmentsSectionProps {
  incidentId: string;
  /** Statut courant de l'incident — pilote les permissions d'ajout/suppression. */
  incidentStatus?: string;
  /** Identifiant du créateur de l'incident — seul autorisé à modifier les PJ. */
  creatorId?: string | null;
  /** Notifie le formulaire qu'une pièce jointe a bougé : c'est une modification
   *  de l'incident au même titre qu'un champ, même si elle est déjà enregistrée. */
  onAttachmentsChange?: () => void;
}

const DECLARATION_CATEGORIES = new Set(["INCIDENT", "INCIDENT_ATTACHMENT"]);

const isDeclarationCategory = (category?: string) =>
  !category || DECLARATION_CATEGORIES.has(category);

// Rend le composant AttachmentsSection pour l'interface attachments section.
const AttachmentsSection = memo(
  ({
    incidentId,
    incidentStatus,
    creatorId,
    onAttachmentsChange,
  }: AttachmentsSectionProps) => {
    const { t } = useTranslation();
    const { message } = App.useApp();
    const { user: currentUser } = useAuth();

    const { data: attachments = [], isLoading } =
      useIncidentAttachments(incidentId);
    const incidentAttachments = useMemo(
      () =>
        attachments.filter(
          (attachment) =>
            !attachment.commentId && isDeclarationCategory(attachment.category),
        ),
      [attachments],
    );
    const { mutate: uploadAttachment, isPending: isUploading } =
      useUploadAttachment(incidentId);
    const { mutate: deleteAttachment, isPending: isDeleting } =
      useDeleteAttachment(incidentId);

    // Modification reservee au createur, statut editable.
    const isCreator = Boolean(
      currentUser?.id && creatorId && currentUser.id === creatorId,
    );
    const isStatusEditable = Boolean(
      incidentStatus && ATTACHMENT_EDITABLE_STATUSES.has(incidentStatus),
    );
    const canModifyAttachments = isCreator && isStatusEditable;

    // Televerse le fichier selectionne en tant que piece jointe.
    const handleUpload = useCallback(
      (options: UploadRequestOption) => {
        const file = options.file as File;
        if (!isAcceptedAttachment(file)) {
          message.error(
            t("incidents.attachments.invalid_type"),
          );
          return;
        }
        if (!isAttachmentSizeValid(file)) {
          message.error(
            t(file.size === 0 ? "incidents.attachments.empty_file" : "incidents.attachments.too_large", {
              name: file.name,
              limit: Math.round(MAX_ATTACHMENT_SIZE_BYTES / (1024 * 1024)),
            }),
          );
          return;
        }
        uploadAttachment({ file }, { onSuccess: onAttachmentsChange });
      },
      [uploadAttachment, onAttachmentsChange, message, t],
    );

    // Traite la suppression.
    const handleDelete = useCallback(
      (attachmentId: string) => {
        deleteAttachment(attachmentId, { onSuccess: onAttachmentsChange });
      },
      [deleteAttachment, onAttachmentsChange],
    );

    // Traite le telechargement.
    const handleDownload = useCallback(
      async (attachment: AttachmentResponse) => {
        try {
          const blob = await documentApi.download(attachment.id);
          downloadFile(blob, attachment.filename);
        } catch {
          message.error(
            t("incidents.attachments.download_error"),
          );
        }
      },
      [message, t],
    );

    const renderItem = useCallback(
      (attachment: AttachmentResponse) => (
        <AttachmentItem
          key={attachment.id}
          attachment={attachment}
          onDelete={handleDelete}
          onDownload={handleDownload}
          isDeleting={isDeleting}
          canDelete={canModifyAttachments}
        />
      ),
      [handleDelete, handleDownload, isDeleting, canModifyAttachments],
    );

    return (
      <div className={styles.attachmentsSection}>
        <Divider titlePlacement="left" className={styles.sectionDivider}>
          <Space size={8}>
            {incidentIcons.attachment}
            <Text strong>{t("incidents.attachments.title")}</Text>
          </Space>
        </Divider>

        <List
          loading={isLoading}
          dataSource={incidentAttachments}
          renderItem={renderItem}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={t("incidents.attachments.no_attachments")}
              />
            ),
          }}
        />

        {canModifyAttachments ? (
          <div className={styles.uploadArea}>
            <Upload
              customRequest={handleUpload}
              showUploadList={false}
              multiple={false}
              disabled={isUploading}
              accept={ACCEPTED_ATTACHMENT_ACCEPT}
            >
              <Button
                variant="secondary"
                icon={actionIcons.upload}
                loading={isUploading}
                className={styles.uploadButton}
              >
                {t("incidents.attachments.add_button")}
              </Button>
            </Upload>
            <Text type="secondary" className={styles.uploadHint}>
              {t("incidents.attachments.accepted_hint")}
            </Text>
          </div>
        ) : (
          <Text type="secondary" className={styles.uploadHint}>
            {!isStatusEditable
              ? t("incidents.attachments.locked_status")
              : t("incidents.attachments.locked_role")}
          </Text>
        )}
      </div>
    );
  },
);

AttachmentsSection.displayName = "AttachmentsSection";

export default AttachmentsSection;
