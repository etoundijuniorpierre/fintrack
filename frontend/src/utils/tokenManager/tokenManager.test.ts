// Tests frontend : verifie le comportement de token manager.test.

import { describe, it, expect, beforeEach } from "vitest";
import { tokenManager } from "./tokenManager";

// Definit les donnees de test test token.
const TEST_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";

describe("tokenManager", () => {
  beforeEach(() => {
    tokenManager.removeToken();
  });

  describe("setToken / getToken", () => {
    it("should not persist the token to localStorage", () => {
      tokenManager.setToken(TEST_TOKEN);
      expect(localStorage.getItem("fintrack_token")).toBeNull();
    });

    it("should return the stored token when getToken is called", () => {
      tokenManager.setToken(TEST_TOKEN);
      expect(tokenManager.getToken()).toBe(TEST_TOKEN);
    });

    it("should return null when no token has been stored", () => {
      expect(tokenManager.getToken()).toBeNull();
    });
  });

  describe("removeToken", () => {
    it("should cause getToken to return null after removal", () => {
      tokenManager.setToken(TEST_TOKEN);
      tokenManager.removeToken();
      expect(tokenManager.getToken()).toBeNull();
    });
  });

  describe("hasToken", () => {
    it("should return false when no token is stored", () => {
      expect(tokenManager.hasToken()).toBe(false);
    });

    it("should return true when a token is stored", () => {
      tokenManager.setToken(TEST_TOKEN);
      expect(tokenManager.hasToken()).toBe(true);
    });

    it("should return false after the token is removed", () => {
      tokenManager.setToken(TEST_TOKEN);
      tokenManager.removeToken();
      expect(tokenManager.hasToken()).toBe(false);
    });
  });
});
