// Tests frontend : verifie le comportement de attachmentValidation.

import { describe, it, expect } from "vitest";
import {
  isAcceptedAttachment,
  isAttachmentSizeValid,
  ACCEPTED_ATTACHMENT_EXTENSIONS,
  MAX_ATTACHMENT_SIZE_BYTES,
} from "./attachmentValidation";

const createFile = (name: string, type: string, size = 1024): File => {
  const file = new File(["x"], name, { type });
  Object.defineProperty(file, "size", { value: size });
  return file;
};

describe("isAcceptedAttachment", () => {
  it("accepts the Windows ZIP MIME alias", () => {
    expect(isAcceptedAttachment(createFile("a.zip", "application/x-zip-compressed"))).toBe(true);
  });
  it.each([["a.pdf", "text/html"], ["a.svg", "image/svg+xml"], ["a.exe", "image/png"]])(
    "rejects active content and misleading extensions: %s", (name, type) => {
      expect(isAcceptedAttachment(createFile(name, type))).toBe(false);
    },
  );
  it("should accept a PDF file by MIME type", () => {
    expect(isAcceptedAttachment(createFile("doc.pdf", "application/pdf"))).toBe(true);
  });

  it("should accept an image by MIME prefix", () => {
    expect(isAcceptedAttachment(createFile("photo.png", "image/png"))).toBe(true);
  });

  it("should accept a DOCX file by exact MIME type", () => {
    const mime = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    expect(isAcceptedAttachment(createFile("doc.docx", mime))).toBe(true);
  });

  it("should accept a CSV file by exact MIME type", () => {
    expect(isAcceptedAttachment(createFile("data.csv", "text/csv"))).toBe(true);
  });

  it("should accept a file by extension when MIME is empty", () => {
    expect(isAcceptedAttachment(createFile("archive.zip", ""))).toBe(true);
  });

  it("should reject a file with unknown MIME and extension", () => {
    expect(isAcceptedAttachment(createFile("malware.exe", "application/x-msdownload"))).toBe(false);
  });

  it("should reject a file with unknown extension and no MIME", () => {
    expect(isAcceptedAttachment(createFile("script.sh", ""))).toBe(false);
  });
});

describe("isAttachmentSizeValid", () => {
  it.each(ACCEPTED_ATTACHMENT_EXTENSIONS)("rejects an empty %s file", (extension) => {
    expect(isAttachmentSizeValid(new File([], `empty${extension}`))).toBe(false);
  });

  it("accepts a nonempty one-byte file", () => {
    expect(isAttachmentSizeValid(new File(["x"], "tiny.txt"))).toBe(true);
  });
  it("should define the attachment limit as 150 MB", () => {
    expect(MAX_ATTACHMENT_SIZE_BYTES).toBe(150 * 1024 * 1024);
  });

  it("should accept a file within size limit", () => {
    expect(isAttachmentSizeValid(createFile("small.pdf", "application/pdf", 1024))).toBe(true);
  });

  it("should accept a file at exactly the size limit", () => {
    expect(isAttachmentSizeValid(createFile("exact.pdf", "application/pdf", MAX_ATTACHMENT_SIZE_BYTES))).toBe(true);
  });

  it("should reject a file exceeding the size limit", () => {
    expect(isAttachmentSizeValid(createFile("large.pdf", "application/pdf", MAX_ATTACHMENT_SIZE_BYTES + 1))).toBe(false);
  });
});

describe("ACCEPTED_ATTACHMENT_EXTENSIONS", () => {
  it("should contain common document extensions", () => {
    expect(ACCEPTED_ATTACHMENT_EXTENSIONS).toContain(".pdf");
    expect(ACCEPTED_ATTACHMENT_EXTENSIONS).toContain(".docx");
    expect(ACCEPTED_ATTACHMENT_EXTENSIONS).toContain(".xlsx");
  });

  it("should contain common image extensions", () => {
    expect(ACCEPTED_ATTACHMENT_EXTENSIONS).toContain(".png");
    expect(ACCEPTED_ATTACHMENT_EXTENSIONS).toContain(".jpg");
    expect(ACCEPTED_ATTACHMENT_EXTENSIONS).toContain(".jpeg");
  });
});
