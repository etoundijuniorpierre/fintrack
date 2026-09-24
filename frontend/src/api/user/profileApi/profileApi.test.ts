// Tests frontend : verifie le comportement de profil api.test.

import { describe, it, expect, vi, beforeEach, type Mock } from "vitest";
import apiClient from "../../client";
import { profileApi } from "./profileApi";
import { USER_SERVICE_ENDPOINTS } from "../endpoints/endpoints";
import { makeUser } from "../../../mocks";
import type { ProfileUpdateRequest } from "../types";

vi.mock("../../client", () => ({
  default: {
    get: vi.fn(),
    patch: vi.fn(),
  },
}));

const mockGet = apiClient.get as Mock;
const mockPatch = apiClient.patch as Mock;

describe("profileApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("getProfile", () => {
    it("calls GET /users/{id} and returns the user", async () => {
      const user = makeUser({ id: "user-1" });
      mockGet.mockResolvedValueOnce({ data: user });

      const result = await profileApi.getProfile("user-1");

      expect(mockGet).toHaveBeenCalledWith(
        `${USER_SERVICE_ENDPOINTS.USERS.BASE}/user-1`,
        expect.anything(),
      );
      expect(result).toEqual(user);
    });

    it("passes AbortSignal to the request", async () => {
      const controller = new AbortController();
      mockGet.mockResolvedValueOnce({ data: makeUser() });

      await profileApi.getProfile("user-1", controller.signal);

      expect(mockGet).toHaveBeenCalledWith(expect.any(String), {
        signal: controller.signal,
      });
    });
  });

  describe("updateProfile", () => {
    it("calls PATCH /users/{id}/profile with payload and returns updated user", async () => {
      const payload: ProfileUpdateRequest = {
        username: "newname",
        email: "new@finstar.com",
      };
      const updated = makeUser({ id: "user-1", username: "newname" });
      mockPatch.mockResolvedValueOnce({ data: updated });

      const result = await profileApi.updateProfile("user-1", payload);

      expect(mockPatch).toHaveBeenCalledWith(
        USER_SERVICE_ENDPOINTS.USERS.PROFILE("user-1"),
        payload,
      );
      expect(result.username).toBe("newname");
    });
  });

  it("should throw when the API call rejects", async () => {
    mockGet.mockRejectedValueOnce(new Error("Network error"));
    await expect(profileApi.getProfile("user-1")).rejects.toThrow(
      "Network error",
    );
  });
});
