// Tests frontend : verifie le comportement de endpoints.test.

import { describe, expect, it } from "vitest";
import { DOCUMENT_ENDPOINTS } from "./endpoints";

describe("DOCUMENT_ENDPOINTS", () => {
  it("builds attachment metadata endpoints", () => {
    expect(DOCUMENT_ENDPOINTS.ATTACHMENTS.BASE).toBe(
      "/api/v1/documentService/attachments",
    );
    expect(DOCUMENT_ENDPOINTS.ATTACHMENTS.ALL).toBe(
      "/api/v1/documentService/attachments/all",
    );
    expect(DOCUMENT_ENDPOINTS.ATTACHMENTS.BY_ID("att-1")).toBe(
      "/api/v1/documentService/attachments/att-1",
    );
    expect(DOCUMENT_ENDPOINTS.ATTACHMENTS.BY_INCIDENT("inc-1")).toBe(
      "/api/v1/documentService/attachments/incident/inc-1",
    );
    expect(DOCUMENT_ENDPOINTS.ATTACHMENTS.BY_UPLOADER("user-1")).toBe(
      "/api/v1/documentService/attachments/uploaded-by/user-1",
    );
    expect(DOCUMENT_ENDPOINTS.ATTACHMENTS.UPLOAD).toBe(
      "/api/v1/documentService/attachments/upload",
    );
    expect(DOCUMENT_ENDPOINTS.ATTACHMENTS.UPLOAD_AVATAR).toBe(
      "/api/v1/documentService/attachments/avatar",
    );
    expect(DOCUMENT_ENDPOINTS.ATTACHMENTS.DOWNLOAD("att-1")).toBe(
      "/api/v1/documentService/attachments/att-1/download",
    );
  });
});
