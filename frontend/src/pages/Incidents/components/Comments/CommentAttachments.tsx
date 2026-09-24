// Affiche les pieces jointes d'un commentaire (image en apercu ou lien).
import { memo, useCallback, useState } from "react";
import { App, Space, Typography, Image } from "antd";
import { PrinterOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import type { AttachmentResponse } from "../../../../api/document/types";
import { documentApi } from "../../../../api/document/documentApi";
import { downloadFile } from "../../../../utils/download/downloadFile";
import { printImage } from "../../../../utils/print/printImage";
import { useAttachmentObjectUrl } from "../../../../hooks/document/useAttachmentObjectUrl";
import { actionIcons, incidentIcons } from "../../../../utils/icons/appIcons";
import { IconOnlyButton } from "../../../../components/ui";
import AttachmentPreviewModal from "../../../../components/AttachmentPreview/AttachmentPreviewModal";
import { isPreviewableDocument } from "../../../../utils/attachments/documentPreview";
import styles from "./CommentAttachments.module.scss";

const { Text } = Typography;

const isImage = (mimeType?: string): boolean =>
  Boolean(mimeType && mimeType.startsWith("image/"));

// Aperçu image d'une pièce jointe de commentaire (blob authentifié -> object URL).
const CommentImage = memo(
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

    if (failed || !src) return null;

    return (
      <Image
        src={src}
        alt={attachment.filename}
        className={styles.image}
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

CommentImage.displayName = "CommentImage";

// Definit les proprietes attendues par le composant CommentAttachments.
interface CommentAttachmentsProps {
  attachments: AttachmentResponse[];
}

// Rend la liste des pièces jointes d'un commentaire.
const CommentAttachments = memo(({ attachments }: CommentAttachmentsProps) => {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const [previewAttachment, setPreviewAttachment] =
    useState<AttachmentResponse | null>(null);

  const download = useCallback(
    async (attachment: AttachmentResponse) => {
      try {
        const blob = await documentApi.download(attachment.id);
        downloadFile(blob, attachment.filename);
      } catch {
        message.error(
          t(
            "incidents.attachments.download_error"
          ),
        );
      }
    },
    [message, t],
  );

  // Charge une copie pour afficher le contenu sans téléchargement.
  const openPreview = useCallback((attachment: AttachmentResponse) => {
    if (isPreviewableDocument(attachment.mimeType, attachment.filename)) {
      setPreviewAttachment(attachment);
    }
  }, []);

  // Ferme l'aperçu et libère l'URL temporaire.
  const closePreview = useCallback(() => setPreviewAttachment(null), []);

  if (!attachments.length) return null;

  return (
    <div className={styles.attachments}>
      {attachments.map((attachment) =>
        isImage(attachment.mimeType) ? (
          <CommentImage
            key={attachment.id}
            attachment={attachment}
          />
        ) : (
          <Space key={attachment.id} size={4}>
            <Text
              className={styles.fileLink}
              role="button"
              tabIndex={0}
              title={
                isPreviewableDocument(attachment.mimeType, attachment.filename)
                  ? t("incidents.attachments.preview")
                  : t("incidents.attachments.download")
              }
              onClick={() =>
                isPreviewableDocument(attachment.mimeType, attachment.filename)
                  ? openPreview(attachment)
                  : download(attachment)
              }
              onKeyDown={(event) => {
                if (event.key === "Enter" || event.key === " ") {
                  event.preventDefault();
                  if (
                    isPreviewableDocument(attachment.mimeType, attachment.filename)
                  ) {
                    openPreview(attachment);
                  } else {
                    download(attachment);
                  }
                }
              }}
            >
              <Space size={4}>
                {incidentIcons.attachment}
                {attachment.filename}
              </Space>
            </Text>
            <IconOnlyButton
              variant="text"
              size="sm"
              icon={actionIcons.download}
              onClick={() => download(attachment)}
              label={t("incidents.attachments.download")}
            />
          </Space>
        ),
      )}
      <AttachmentPreviewModal
        attachment={previewAttachment}
        open={Boolean(previewAttachment)}
        onClose={closePreview}
      />
    </div>
  );
});

CommentAttachments.displayName = "CommentAttachments";

export default CommentAttachments;
