// Verifie la selection des formats pris en charge par la visionneuse locale.

import { describe, expect, it } from "vitest";
import {
  isExcelAttachment,
  isLegacyExcelAttachment,
  isPreviewableDocument,
  isWordAttachment,
} from "./documentPreview";

describe("documentPreview", () => {
  it.each(["a.pdf", "a.docx", "a.xlsx", "a.xls"])("does not trust %s over an HTML MIME type", (name) => {
    expect(isPreviewableDocument("text/html", name)).toBe(false);
  });
  it("does not preview SVG documents", () => {
    expect(isPreviewableDocument("image/svg+xml", "a.svg")).toBe(false);
  });
  it("should preview DOCX documents without treating legacy Word formats as previewable", () => {
    expect(isWordAttachment(undefined, "procedure.docx")).toBe(true);
    expect(isWordAttachment("application/msword", "procedure.doc")).toBe(false);
  });

  it("should preview XLSX workbooks by MIME type or extension", () => {
    expect(
      isExcelAttachment(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      ),
    ).toBe(true);
    expect(isExcelAttachment(undefined, "suivi.xlsx")).toBe(true);
  });

  it("should preview legacy XLS workbooks by MIME type or extension", () => {
    expect(isLegacyExcelAttachment("application/vnd.ms-excel")).toBe(true);
    expect(isLegacyExcelAttachment(undefined, "archive.xls")).toBe(true);
    // Le XLSX n'est pas un XLS : il se lit avec l'autre analyseur.
    expect(isLegacyExcelAttachment(undefined, "suivi.xlsx")).toBe(false);
    expect(
      isPreviewableDocument("application/vnd.ms-excel", "archive.xls"),
    ).toBe(true);
  });

  it("should keep unsupported office formats downloadable only", () => {
    expect(isPreviewableDocument("application/msword", "archive.doc")).toBe(
      false,
    );
  });
});
