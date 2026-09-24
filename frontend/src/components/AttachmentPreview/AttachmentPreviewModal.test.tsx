// Verifie le chargement authentifie, le rendu et le telechargement des apercus.

import { fireEvent, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { AttachmentResponse } from "../../api/document/types";
import { documentApi } from "../../api/document/documentApi";
import { downloadFile } from "../../utils/download/downloadFile";
import { renderWithProviders } from "../../test-utils/renderWithProviders";
import AttachmentPreviewModal from "./AttachmentPreviewModal";

const { mockReadXlsxFile, mockRenderAsync, mockRead, mockSheetToJson } =
  vi.hoisted(() => ({
    mockReadXlsxFile: vi.fn(),
    mockRenderAsync: vi.fn(),
    mockRead: vi.fn(),
    mockSheetToJson: vi.fn(),
  }));

vi.mock("../../api/document/documentApi", () => ({
  documentApi: { download: vi.fn() },
}));

vi.mock("../../utils/download/downloadFile", () => ({
  downloadFile: vi.fn(),
}));

vi.mock("docx-preview", () => ({ renderAsync: mockRenderAsync }));
vi.mock("read-excel-file/browser", () => ({ default: mockReadXlsxFile }));
vi.mock("xlsx", () => ({
  read: mockRead,
  utils: { sheet_to_json: mockSheetToJson },
}));

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: { language: "en", resolvedLanguage: "en" },
  }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

const createAttachment = (
  filename: string,
  mimeType: string,
): AttachmentResponse =>
  ({
    id: "attachment-1",
    filename,
    mimeType,
    fileSize: 128,
  }) as AttachmentResponse;

describe("AttachmentPreviewModal", () => {
  it.each(["text/html", "image/svg+xml"])("rejects a PDF response served as %s", async (type) => {
    vi.mocked(documentApi.download).mockResolvedValueOnce(new Blob(["<html>bad</html>"], { type }));
    renderWithProviders(<AttachmentPreviewModal attachment={createAttachment("proof.pdf", "application/pdf")} open onClose={vi.fn()} />);
    expect(await screen.findByText("incidents.attachments.preview_error")).toBeInTheDocument();
    expect(URL.createObjectURL).not.toHaveBeenCalled();
    expect(screen.queryByTitle("proof.pdf")).not.toBeInTheDocument();
  });
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal("URL", {
      createObjectURL: vi.fn(() => "blob:http://localhost/attachment"),
      revokeObjectURL: vi.fn(),
    });
  });

  it("should display a PDF and download the already loaded blob", async () => {
    const blob = new Blob(["%PDF"], { type: "application/pdf" });
    vi.mocked(documentApi.download).mockResolvedValueOnce(blob);
    const attachment = createAttachment("proof.pdf", "application/pdf");

    renderWithProviders(
      <AttachmentPreviewModal
        attachment={attachment}
        open
        onClose={vi.fn()}
      />,
    );

    expect(await screen.findByTitle("proof.pdf")).toHaveAttribute(
      "src",
      "blob:http://localhost/attachment",
    );
    expect(screen.getByTitle("proof.pdf")).toHaveAttribute("sandbox", "");
    expect(screen.getByTitle("proof.pdf")).toHaveAttribute("referrerpolicy", "no-referrer");
    fireEvent.click(
      screen.getByRole("button", {
        name: "incidents.attachments.download",
      }),
    );

    expect(downloadFile).toHaveBeenCalledWith(
      blob,
      "proof.pdf",
      "application/pdf",
    );
  });

  it("should render a DOCX with the local Word renderer", async () => {
    const blob = new Blob(["docx"], {
      type: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    });
    vi.mocked(documentApi.download).mockResolvedValueOnce(blob);
    mockRenderAsync.mockResolvedValueOnce(undefined);

    renderWithProviders(
      <AttachmentPreviewModal
        attachment={createAttachment("procedure.docx", blob.type)}
        open
        onClose={vi.fn()}
      />,
    );

    await waitFor(() => expect(mockRenderAsync).toHaveBeenCalledTimes(1));
    expect(
      screen.getByLabelText("incidents.attachments.viewer.docx_content"),
    ).toBeInTheDocument();
  });

  it("should read a legacy XLS workbook with the binary reader", async () => {
    const blob = new Blob(["xls"], { type: "application/vnd.ms-excel" });
    vi.mocked(documentApi.download).mockResolvedValueOnce(blob);
    mockRead.mockReturnValueOnce({
      SheetNames: ["Archive"],
      Sheets: { Archive: {} },
    });
    mockSheetToJson.mockReturnValueOnce([
      ["Reference", "Status"],
      ["INC-9", "Closed"],
    ]);

    renderWithProviders(
      <AttachmentPreviewModal
        attachment={createAttachment("archive.xls", blob.type)}
        open
        onClose={vi.fn()}
      />,
    );

    expect(await screen.findByText("Archive")).toBeInTheDocument();
    expect(screen.getByText("INC-9")).toBeInTheDocument();
    // Le lecteur XLSX ne sait pas ouvrir un binaire BIFF : il ne doit pas etre sollicite.
    expect(mockReadXlsxFile).not.toHaveBeenCalled();
  });

  it("should expose the sheets and cells of an XLSX workbook", async () => {
    const blob = new Blob(["xlsx"], {
      type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    });
    vi.mocked(documentApi.download).mockResolvedValueOnce(blob);
    mockReadXlsxFile.mockResolvedValueOnce([
      { sheet: "Incidents", data: [["Reference", "Status"], ["INC-1", "Open"]] },
    ]);

    renderWithProviders(
      <AttachmentPreviewModal
        attachment={createAttachment("incidents.xlsx", blob.type)}
        open
        onClose={vi.fn()}
      />,
    );

    expect(await screen.findByText("Incidents")).toBeInTheDocument();
    expect(screen.getByText("INC-1")).toBeInTheDocument();
    expect(screen.getByText("Open")).toBeInTheDocument();
  });
});
