// Tests frontend : verifie le comportement de validators.test.

import { describe, it, expect } from "vitest";
import {
  isValidEmail,
  isBlank,
  isValidPhone,
} from "./validators";

describe("validators", () => {
  describe("isValidEmail", () => {
    it("should return true when the email is valid", () => {
      expect(isValidEmail("test@example.com")).toBe(true);
      expect(isValidEmail("user.name+tag@domain.co.uk")).toBe(true);
    });

    it("should return false when the email has no domain", () => {
      expect(isValidEmail("test@")).toBe(false);
    });

    it("should return false when the email has no local part", () => {
      expect(isValidEmail("@domain.com")).toBe(false);
    });

    it("should return false when the email has no @ symbol", () => {
      expect(isValidEmail("invalid-email")).toBe(false);
    });

    it("should return false when the email is an empty string", () => {
      expect(isValidEmail("")).toBe(false);
    });
  });

  describe("isBlank", () => {
    it("should return true when the string is empty", () => {
      expect(isBlank("")).toBe(true);
    });

    it("should return true when the string contains only whitespace", () => {
      expect(isBlank("   ")).toBe(true);
    });

    it("should return true when the value is null", () => {
      expect(isBlank(null)).toBe(true);
    });

    it("should return true when the value is undefined", () => {
      expect(isBlank(undefined)).toBe(true);
    });

    it("should return false when the string has non-whitespace content", () => {
      expect(isBlank("hello")).toBe(false);
    });

    it("should return false when the string has non-whitespace content surrounded by spaces", () => {
      expect(isBlank(" hello ")).toBe(false);
    });
  });

  describe("isValidPhone", () => {
    it("should return true when the phone number contains only digits", () => {
      expect(isValidPhone("1234567890")).toBe(true);
    });

    it("should return true when the phone number has international format", () => {
      expect(isValidPhone("+1 234 567 890")).toBe(true);
    });

    it("should return true when the phone number has parentheses and dashes", () => {
      expect(isValidPhone("(123) 456-7890")).toBe(true);
    });

    it("should return false when the phone number contains letters", () => {
      expect(isValidPhone("abc")).toBe(false);
      expect(isValidPhone("123abc")).toBe(false);
    });
  });
});
