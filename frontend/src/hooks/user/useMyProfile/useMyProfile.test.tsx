// Tests frontend : verifie le comportement de use mon profile.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import React from "react";
import { useMyProfile, useUpdateProfile } from "./useMyProfile";
import * as profileApiModule from "../../../api/user/profileApi/profileApi";
import { makeUser } from "../../../mocks";

vi.mock("../../../api/user/profileApi/profileApi", () => ({
  profileApi: {
    getProfile: vi.fn(),
    updateProfile: vi.fn(),
  },
}));

const mockProfileApi = vi.mocked(profileApiModule.profileApi);

// Couvre les comportements du module teste.
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe("useMyProfile", () => {
  beforeEach(() => vi.clearAllMocks());

  it("should return profile data when the API call succeeds", async () => {
    const user = makeUser({ id: "user-1" });
    mockProfileApi.getProfile.mockResolvedValue(user);

    const { result } = renderHook(() => useMyProfile("user-1"), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(user);
    expect(mockProfileApi.getProfile).toHaveBeenCalledWith(
      "user-1",
      expect.anything(),
    );
  });

  it("should not fetch when id is empty", () => {
    renderHook(() => useMyProfile(""), { wrapper: createWrapper() });
    expect(mockProfileApi.getProfile).not.toHaveBeenCalled();
  });

  it("should expose isError when the API call rejects", async () => {
    mockProfileApi.getProfile.mockRejectedValue(new Error("Network error"));

    const { result } = renderHook(() => useMyProfile("user-1"), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.data).toBeUndefined();
  });
});

describe("useUpdateProfile", () => {
  beforeEach(() => vi.clearAllMocks());

  it("should call updateProfile with the correct args when mutate is called", async () => {
    const updated = makeUser({ id: "user-1", username: "newname" });
    mockProfileApi.updateProfile.mockResolvedValue(updated);

    const { result } = renderHook(() => useUpdateProfile(), {
      wrapper: createWrapper(),
    });

    result.current.mutate({ id: "user-1", data: { username: "newname" } });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(mockProfileApi.updateProfile).toHaveBeenCalledWith("user-1", {
      username: "newname",
    });
  });

  it("should expose isError when the mutation rejects", async () => {
    mockProfileApi.updateProfile.mockRejectedValue(new Error("Update failed"));

    const { result } = renderHook(() => useUpdateProfile(), {
      wrapper: createWrapper(),
    });

    result.current.mutate({ id: "user-1", data: { username: "x" } });

    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});
