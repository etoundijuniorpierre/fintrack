// Affiche une piece jointe authentifiee sans l'envoyer vers un service externe.

import { Alert, Empty, Image, Modal, Space, Spin, Tabs, Typography } from "antd";
import { PrinterOutlined } from "@ant-design/icons";
import { useEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import type { AttachmentResponse } from "../../api/document/types";
import { documentApi } from "../../api/document/documentApi";
import { downloadFile } from "../../utils/download/downloadFile";
import {
  isExcelAttachment,
  isImageAttachment,
  isLegacyExcelAttachment,
  isPdfAttachment,
  isSpreadsheetAttachment,
  isWordAttachment,
} from "../../utils/attachments/documentPreview";
import { actionIcons } from "../../utils/icons/appIcons";
import { Button } from "../ui";
import styles from "./AttachmentPreviewModal.module.scss";

const MAX_EXCEL_ROWS = 300;
const MAX_EXCEL_COLUMNS = 40;

interface AttachmentPreviewModalProps {
  attachment: AttachmentResponse | null;
  open: boolean;
  onClose: () => void;
}

interface ExcelSheetPreview {
  name: string;
  rows: string[][];
  truncated: boolean;
}

interface RawSheet {
  name: string;
  data: unknown[][];
}

interface LoadedPreview {
  attachmentId: string;
  blob: Blob | null;
  objectUrl: string | null;
  failed: boolean;
}

// Normalise une valeur de cellule pour une lecture stable dans l'interface.
const formatCellValue = (value: unknown, locale: string): string => {
  if (value === null || value === undefined) return "";
  if (value instanceof Date) return value.toLocaleString(locale);
  return String(value);
};

// Rend un document DOCX dans une zone isolee de la modale.
const DocxViewer = ({ blob }: { blob: Blob }) => {
  const { t } = useTranslation();
  const containerRef = useRef<HTMLDivElement>(null);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let active = true;
    const container = containerRef.current;
    if (!container) return undefined;

    container.replaceChildren();
    import("docx-preview")
      .then(({ renderAsync }) => {
        if (!active) return undefined;
        return renderAsync(blob, container, container, {
          breakPages: true,
          inWrapper: true,
          ignoreHeight: false,
          ignoreWidth: false,
        });
      })
      .catch(() => {
        if (active) setFailed(true);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
      container.replaceChildren();
    };
  }, [blob]);

  return (
    <div className={styles.documentViewport}>
      {loading && <Spin className={styles.centeredState} />}
      {failed && (
        <Alert
          type="error"
          showIcon
          message={t("incidents.attachments.viewer.docx_error")}
        />
      )}
      <div
        ref={containerRef}
        className={styles.docxContainer}
        aria-label={t("incidents.attachments.viewer.docx_content")}
      />
    </div>
  );
};

// Deux generations de classeurs, deux analyseurs : le XLSX est du XML compresse,
// le XLS un binaire BIFF que le lecteur XLSX ne sait pas ouvrir. Seule la lecture
// differe — l'affichage en feuilles reste le meme.
const readWorkbookSheets = async (
  blob: Blob,
  legacy: boolean,
): Promise<RawSheet[]> => {
  if (!legacy) {
    const { default: readXlsxFile } = await import("read-excel-file/browser");
    const sheets = await readXlsxFile(blob);
    return sheets.map(({ sheet, data }) => ({ name: sheet, data }));
  }

  const { read, utils } = await import("xlsx");
  const workbook = read(await blob.arrayBuffer(), {
    type: "array",
    cellDates: true,
  });
  return workbook.SheetNames.map((name) => ({
    name,
    data: utils.sheet_to_json<unknown[]>(workbook.Sheets[name], {
      header: 1,
      blankrows: false,
      defval: null,
    }),
  }));
};

// Rend les feuilles d'un classeur sous forme de tableaux consultables.
const ExcelViewer = ({ blob, legacy }: { blob: Blob; legacy: boolean }) => {
  const { t, i18n } = useTranslation();
  const [sheets, setSheets] = useState<ExcelSheetPreview[]>([]);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let active = true;
    readWorkbookSheets(blob, legacy)
      .then((workbookSheets) => {
        if (!active) return;
        const locale = i18n.resolvedLanguage || i18n.language || "fr";
        setSheets(
          workbookSheets.map(({ name, data }) => {
            const truncated =
              data.length > MAX_EXCEL_ROWS ||
              data.some((row) => row.length > MAX_EXCEL_COLUMNS);
            return {
              name,
              rows: data
                .slice(0, MAX_EXCEL_ROWS)
                .map((row) =>
                  row
                    .slice(0, MAX_EXCEL_COLUMNS)
                    .map((cell) => formatCellValue(cell, locale)),
                ),
              truncated,
            };
          }),
        );
      })
      .catch(() => {
        if (active) setFailed(true);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [blob, legacy, i18n.language, i18n.resolvedLanguage]);

  const items = useMemo(
    () =>
      sheets.map((sheet) => ({
        key: sheet.name,
        label: sheet.name,
        children: (
          <div className={styles.excelSheet}>
            {sheet.truncated && (
              <Alert
                type="info"
                showIcon
                message={t("incidents.attachments.viewer.xlsx_truncated", {
                  rows: MAX_EXCEL_ROWS,
                  columns: MAX_EXCEL_COLUMNS,
                })}
              />
            )}
            {sheet.rows.length ? (
              <div className={styles.excelTableViewport}>
                <table className={styles.excelTable}>
                  <tbody>
                    {sheet.rows.map((row, rowIndex) => (
                      <tr key={`${sheet.name}-${rowIndex}`}>
                        <th scope="row" className={styles.rowNumber}>
                          {rowIndex + 1}
                        </th>
                        {row.map((cell, columnIndex) => (
                          <td key={`${rowIndex}-${columnIndex}`}>{cell}</td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <Empty description={t("incidents.attachments.viewer.empty_sheet")} />
            )}
          </div>
        ),
      })),
    [sheets, t],
  );

  if (loading) return <Spin className={styles.centeredState} />;
  if (failed) {
    return (
      <Alert
        type="error"
        showIcon
        message={t("incidents.attachments.viewer.xlsx_error")}
      />
    );
  }
  return items.length ? <Tabs items={items} /> : <Empty />;
};

// Orchestre le chargement et l'affichage du format de document selectionne.
const AttachmentPreviewModal = ({
  attachment,
  open,
  onClose,
}: AttachmentPreviewModalProps) => {
  const { t } = useTranslation();
  const [loadedPreview, setLoadedPreview] = useState<LoadedPreview | null>(null);

  useEffect(() => {
    if (!open || !attachment) return undefined;

    let active = true;
    let localObjectUrl: string | null = null;

    documentApi
      .download(attachment.id)
      .then((receivedBlob) => {
        if (!active) return;
        const type = receivedBlob.type.toLowerCase();
        const expected = (attachment.mimeType || "").toLowerCase();
        if (["text/html", "application/xhtml+xml", "image/svg+xml"].includes(type) ||
            ["text/html", "application/xhtml+xml", "image/svg+xml"].includes(expected) ||
            (type && expected && type !== expected) || receivedBlob.size === 0) {
          throw new Error("Type de piece jointe incoherent ou dangereux");
        }
        const normalizedBlob =
          receivedBlob.type || !attachment.mimeType
            ? receivedBlob
            : new Blob([receivedBlob], { type: attachment.mimeType });
        localObjectUrl = URL.createObjectURL(normalizedBlob);
        setLoadedPreview({
          attachmentId: attachment.id,
          blob: normalizedBlob,
          objectUrl: localObjectUrl,
          failed: false,
        });
      })
      .catch(() => {
        if (active) {
          setLoadedPreview({
            attachmentId: attachment.id,
            blob: null,
            objectUrl: null,
            failed: true,
          });
        }
      });

    return () => {
      active = false;
      if (localObjectUrl) URL.revokeObjectURL(localObjectUrl);
    };
  }, [attachment, open]);

  const currentPreview =
    attachment && loadedPreview?.attachmentId === attachment.id
      ? loadedPreview
      : null;
  const blob = currentPreview?.blob ?? null;
  const objectUrl = currentPreview?.objectUrl ?? null;
  const loading = Boolean(open && attachment && !currentPreview);
  const failed = currentPreview?.failed ?? false;

  const handleClose = () => {
    setLoadedPreview(null);
    onClose();
  };

  const handleDownload = () => {
    if (!attachment || !blob) return;
    downloadFile(blob, attachment.filename, attachment.mimeType);
  };

  const handlePrint = () => {
    if (!objectUrl) return;
    const printWindow = window.open(objectUrl, "_blank");
    if (!printWindow) return;
    printWindow.addEventListener("load", () => printWindow.print(), {
      once: true,
    });
  };

  const canPrint = Boolean(
    attachment &&
      (isPdfAttachment(attachment.mimeType, attachment.filename) ||
        isImageAttachment(attachment.mimeType)),
  );

  return (
    <Modal
      open={open}
      title={attachment?.filename}
      className={styles.previewModal}
      width={1100}
      footer={
        <Space>
          {canPrint && (
            <Button
              variant="secondary"
              icon={<PrinterOutlined />}
              onClick={handlePrint}
              disabled={!objectUrl}
            >
              {t("common.print")}
            </Button>
          )}
          <Button
            variant="primary"
            icon={actionIcons.download}
            onClick={handleDownload}
            disabled={!blob}
          >
            {t("incidents.attachments.download")}
          </Button>
        </Space>
      }
      onCancel={handleClose}
      destroyOnHidden
    >
      {loading ? (
        <Spin className={styles.centeredState} />
      ) : failed || !attachment || !blob || !objectUrl ? (
        <Alert
          type="error"
          showIcon
          message={t("incidents.attachments.preview_error")}
        />
      ) : isImageAttachment(attachment.mimeType) ? (
        <Image
          src={objectUrl}
          alt={attachment.filename}
          preview={{ visible: false }}
          className={styles.previewImage}
        />
      ) : isPdfAttachment(attachment.mimeType, attachment.filename) ? (
        <iframe
          sandbox=""
          referrerPolicy="no-referrer"
          src={objectUrl}
          title={attachment.filename}
          className={styles.previewIframe}
        />
      ) : isWordAttachment(attachment.mimeType, attachment.filename) ? (
        <DocxViewer key={objectUrl} blob={blob} />
      ) : isSpreadsheetAttachment(attachment.mimeType, attachment.filename) ? (
        <ExcelViewer
          key={objectUrl}
          blob={blob}
          // Le XLSX prime : certains navigateurs annoncent un .xlsx en vnd.ms-excel.
          legacy={
            !isExcelAttachment(attachment.mimeType, attachment.filename) &&
            isLegacyExcelAttachment(attachment.mimeType, attachment.filename)
          }
        />
      ) : (
        <Typography.Text>
          {t("incidents.attachments.viewer.unsupported")}
        </Typography.Text>
      )}
    </Modal>
  );
};

export default AttachmentPreviewModal;
