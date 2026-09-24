// Tests frontend : verifie le comportement de utilisateur avatar.test.

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { waitFor } from "@testing-library/react";
import { renderWithProviders } from "../../test-utils/renderWithProviders";
import { documentApi } from "../../api/document/documentApi";
import UserAvatar from "./UserAvatar";

vi.mock("../../api/document/documentApi", () => ({
  documentApi: {
    download: vi.fn(),
  },
}));

describe("UserAvatar", () => {
  const createObjectURL = vi.fn(() => "blob:avatar-url");
  const revokeObjectURL = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    Object.defineProperty(window.URL, "createObjectURL", {
      configurable: true,
      value: createObjectURL,
    });
    Object.defineProperty(window.URL, "revokeObjectURL", {
      configurable: true,
      value: revokeObjectURL,
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("does not download anything when no avatar document id is provided", () => {
    renderWithProviders(<UserAvatar />);

    expect(documentApi.download).not.toHaveBeenCalled();
  });

  it("downloads the avatar blob and revokes the object URL on unmount", async () => {
    const blob = new Blob(["avatar"], { type: "image/png" });
    vi.mocked(documentApi.download).mockResolvedValueOnce(blob);

    const { unmount } = renderWithProviders(
      <UserAvatar avatarDocumentId="avatar-1" />,
    );

    await waitFor(() => {
      expect(documentApi.download).toHaveBeenCalledWith("avatar-1");
      expect(createObjectURL).toHaveBeenCalledWith(blob);
    });

    unmount();

    expect(revokeObjectURL).toHaveBeenCalledWith("blob:avatar-url");
  });

  it("falls back silently when avatar download fails", async () => {
    vi.mocked(documentApi.download).mockRejectedValueOnce(
      new Error("not found"),
    );

    renderWithProviders(<UserAvatar avatarDocumentId="missing-avatar" />);

    await waitFor(() => {
      expect(documentApi.download).toHaveBeenCalledWith("missing-avatar");
    });
    expect(createObjectURL).not.toHaveBeenCalled();
  });
});
