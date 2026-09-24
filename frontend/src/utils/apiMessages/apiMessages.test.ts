// Tests frontend : verifie le comportement de API messages.test.

import { describe, it, expect, vi } from "vitest";
import {
  getApiErrorMessage,
  getApiSuccessMessage,
  isApiError,
  getApiErrorCode,
  getApiErrorStatus,
  getFailedLoginAttempts,
  getMaxFailedAttempts,
} from "./apiMessages";

vi.mock("axios", async () => {
  const actual = await vi.importActual<typeof import("axios")>("axios");
  const mockIsAxiosError = vi.fn((err: unknown) => {
    return typeof err === "object" && err !== null && "response" in err;
  });
  return {
    ...actual,
    default: {
      ...(actual.default as object),
      isAxiosError: mockIsAxiosError,
    },
  };
});

vi.mock("../../i18n", () => ({
  default: {
    t: (key: string) =>
      key === "common.unknown_error" ? "Une erreur est survenue" : key,
  },
}));

const API_ERROR = {
  response: {
    data: {
      message: "Invalid login credentials.",
      code: "INVALID_CREDENTIALS",
    },
    status: 401,
  },
};

const API_ERROR_WITH_DETAILS = {
  response: {
    data: {
      message: "Too many failed attempts.",
      code: "ACCOUNT_LOCKED",
      details: { failed_login_attempts: 5, max_failed_attempts: 10 },
    },
    status: 403,
  },
};

// Definit les donnees de test pour une erreur standard.
const REGULAR_ERROR = new Error("Something went wrong");

describe("apiMessages", () => {
  describe("getApiErrorMessage", () => {
    it("should return the API response message when the error is an API error", () => {
      expect(getApiErrorMessage(API_ERROR)).toBe("Invalid login credentials.");
    });

    it("should return the error message when the error is a plain Error instance", () => {
      expect(getApiErrorMessage(REGULAR_ERROR)).toBe("Something went wrong");
    });

    it("should return the default message when the error is a string", () => {
      expect(getApiErrorMessage("string error")).toBe(
        "Une erreur est survenue",
      );
    });

    it("should return the default message when the error is null", () => {
      expect(getApiErrorMessage(null)).toBe("Une erreur est survenue");
    });

    it("should return the default message when the error is undefined", () => {
      expect(getApiErrorMessage(undefined)).toBe("Une erreur est survenue");
    });

    it("should return the default message when the error is an empty object", () => {
      expect(getApiErrorMessage({})).toBe("Une erreur est survenue");
    });
  });

  describe("getApiSuccessMessage", () => {
    it("should return the message when the response contains a message field", () => {
      const response = {
        message: "User created successfully.",
        data: { id: "123" },
      };
      expect(getApiSuccessMessage(response)).toBe("User created successfully.");
    });

    it("should return null when the response does not contain a message field", () => {
      expect(getApiSuccessMessage({ data: { id: "123" } })).toBeNull();
    });

    it("should return null when the response is null", () => {
      expect(getApiSuccessMessage(null)).toBeNull();
    });

    it("should return null when the response is undefined", () => {
      expect(getApiSuccessMessage(undefined)).toBeNull();
    });

    it("should return null when the message field is an empty string", () => {
      expect(getApiSuccessMessage({ message: "" })).toBeNull();
    });
  });

  describe("isApiError", () => {
    it("should return true when the error is an API error with a response message", () => {
      expect(isApiError(API_ERROR)).toBe(true);
    });

    it("should return false when the error is a plain Error instance", () => {
      expect(isApiError(REGULAR_ERROR)).toBe(false);
    });

    it("should return false when the error is a string", () => {
      expect(isApiError("string error")).toBe(false);
    });

    it("should return false when the error is null", () => {
      expect(isApiError(null)).toBe(false);
    });
  });

  describe("getApiErrorCode", () => {
    it("should return the error code when the error is an API error", () => {
      expect(getApiErrorCode(API_ERROR)).toBe("INVALID_CREDENTIALS");
    });

    it("should return null when the error is a plain Error instance", () => {
      expect(getApiErrorCode(REGULAR_ERROR)).toBeNull();
    });

    it("should return null when the error is null", () => {
      expect(getApiErrorCode(null)).toBeNull();
    });

    it("should return null when the error is undefined", () => {
      expect(getApiErrorCode(undefined)).toBeNull();
    });
  });

  describe("getApiErrorStatus", () => {
    it("should return the HTTP status code when the error is an API error", () => {
      expect(getApiErrorStatus(API_ERROR)).toBe(401);
    });

    it("should return null when the error is a plain Error instance", () => {
      expect(getApiErrorStatus(REGULAR_ERROR)).toBeNull();
    });

    it("should return null when the error is null", () => {
      expect(getApiErrorStatus(null)).toBeNull();
    });

    it("should return null when the error is undefined", () => {
      expect(getApiErrorStatus(undefined)).toBeNull();
    });
  });

  describe("getFailedLoginAttempts", () => {
    it("should return the failed login attempts count when present in error details", () => {
      expect(getFailedLoginAttempts(API_ERROR_WITH_DETAILS)).toBe(5);
    });

    it("should return null when the error has no details field", () => {
      expect(getFailedLoginAttempts(API_ERROR)).toBeNull();
    });

    it("should return null when the error is a plain Error instance", () => {
      expect(getFailedLoginAttempts(REGULAR_ERROR)).toBeNull();
    });

    it("should return null when the error is null", () => {
      expect(getFailedLoginAttempts(null)).toBeNull();
    });

    it("should return null when failed_login_attempts is not a number", () => {
      const errorWithStringAttempts = {
        response: {
          data: {
            message: "Error",
            code: "ERR",
            details: { failed_login_attempts: "not-a-number" },
          },
        },
      };
      expect(getFailedLoginAttempts(errorWithStringAttempts)).toBeNull();
    });

    it("should return null when failed_login_attempts is missing", () => {
      const error = {
        response: {
          data: {
            details: {},
          },
        },
      };
      expect(getFailedLoginAttempts(error)).toBeNull();
    });
  });

  describe("getMaxFailedAttempts", () => {
    it("should return max attempts when present and valid", () => {
      const error = {
        response: {
          data: {
            message: "Error",
            details: { max_failed_attempts: 10 },
          },
        },
        isAxiosError: true,
      };
      expect(getMaxFailedAttempts(error)).toBe(10);
    });

    it("should return null when max_failed_attempts is not a number", () => {
      const error = {
        response: {
          data: {
            message: "Error",
            details: { max_failed_attempts: "not-a-number" },
          },
        },
        isAxiosError: true,
      };
      expect(getMaxFailedAttempts(error)).toBeNull();
    });

    it("should return null when details object is missing", () => {
      const error = {
        response: {
          data: {
            message: "Error",
          },
        },
        isAxiosError: true,
      };
      expect(getMaxFailedAttempts(error)).toBeNull();
    });
  });
});
