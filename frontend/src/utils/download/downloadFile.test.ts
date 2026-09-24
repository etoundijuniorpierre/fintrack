// Tests frontend : valide le telechargement navigateur centralise.

import { afterEach, describe, expect, it, vi } from "vitest";
import { downloadFile } from "./downloadFile";

describe("downloadFile", () => {
  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  const installBrowserDownloadMocks = () => {
    const click = vi.fn();
    const remove = vi.fn();
    const link = {
      href: "",
      download: "",
      click,
      remove,
    } as unknown as HTMLAnchorElement;
    const createObjectURL = vi.fn(
      (_blob: Blob): string => "blob:http://localhost/test-uuid",
    );
    const revokeObjectURL = vi.fn();

    vi.stubGlobal("URL", {
      createObjectURL,
      revokeObjectURL,
    });

    const createElement = vi
      .spyOn(document, "createElement")
      .mockReturnValue(link);
    const appendChild = vi
      .spyOn(document.body, "appendChild")
      .mockReturnValue(link);

    return {
      appendChild,
      click,
      createElement,
      createObjectURL,
      link,
      remove,
      revokeObjectURL,
    };
  };

  it("should create a temporary link and clean it up after clicking", () => {
    vi.useFakeTimers();
    const {
      appendChild,
      click,
      createElement,
      createObjectURL,
      link,
      remove,
      revokeObjectURL,
    } = installBrowserDownloadMocks();
    const content = new Blob(["hello world"], { type: "text/plain" });

    downloadFile(content, "test.txt");

    expect(createObjectURL).toHaveBeenCalledWith(content);
    expect(createElement).toHaveBeenCalledWith("a");
    expect(link.href).toBe("blob:http://localhost/test-uuid");
    expect(link.download).toBe("test.txt");
    expect(appendChild).toHaveBeenCalledWith(link);
    expect(click).toHaveBeenCalledTimes(1);
    expect(remove).toHaveBeenCalledTimes(1);
    expect(revokeObjectURL).not.toHaveBeenCalled();

    vi.advanceTimersByTime(1000);

    expect(revokeObjectURL).toHaveBeenCalledWith(
      "blob:http://localhost/test-uuid",
    );
  });

  it("should convert a BlobPart to a Blob with the requested MIME type", () => {
    const { createObjectURL } = installBrowserDownloadMocks();

    downloadFile("csv-content-string", "data.csv", "text/csv");

    expect(createObjectURL).toHaveBeenCalledTimes(1);
    const blob = createObjectURL.mock.calls[0]?.[0];
    expect(blob).toBeInstanceOf(Blob);
    if (!(blob instanceof Blob)) {
      throw new Error("Downloaded content must be converted to a Blob.");
    }
    expect(blob.type).toBe("text/csv");
  });
});
