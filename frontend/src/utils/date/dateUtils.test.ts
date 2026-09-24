// Tests frontend : verifie le comportement de date utils.test.

import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import dayjs from "dayjs";
import {
  DATE_FORMATS,
  isPastDate,
  getToday,
  formatToApiDate,
  formatToApiDateTime,
  parseTime,
  getLocalizedDayName,
} from "./dateUtils";

// Definit les donnees de test mock date.
const MOCK_DATE = "2024-04-29";

describe("dateUtils", () => {
  describe("DATE_FORMATS", () => {
    it("should expose the correct API format string", () => {
      expect(DATE_FORMATS.API).toBe("YYYY-MM-DD");
    });

    it("should expose the correct API date-time format string", () => {
      expect(DATE_FORMATS.API_DATE_TIME).toBe("YYYY-MM-DDTHH:mm:ss");
    });

    it("should expose the correct DISPLAY format string", () => {
      expect(DATE_FORMATS.DISPLAY).toBe("DD/MM/YYYY");
    });

    it("should expose the correct FULL format string", () => {
      expect(DATE_FORMATS.FULL).toBe("DD/MM/YYYY HH:mm");
    });

    it("should expose the correct TIME format string", () => {
      expect(DATE_FORMATS.TIME).toBe("HH:mm");
    });
  });

  describe("isPastDate", () => {
    beforeEach(() => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date(`${MOCK_DATE}T12:00:00Z`));
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it("should return false when date is null", () => {
      expect(isPastDate(null)).toBe(false);
    });

    it("should return false when date is undefined", () => {
      expect(isPastDate(undefined)).toBe(false);
    });

    it("should return true when date is in the past", () => {
      const pastDate = dayjs(MOCK_DATE).subtract(1, "day");
      expect(isPastDate(pastDate)).toBe(true);
    });

    it("should return false when date is today", () => {
      expect(isPastDate(dayjs(MOCK_DATE))).toBe(false);
    });

    it("should return false when date is in the future", () => {
      const futureDate = dayjs(MOCK_DATE).add(1, "day");
      expect(isPastDate(futureDate)).toBe(false);
    });

    it("should return false when date is an invalid dayjs object", () => {
      const invalidDate = dayjs("not-a-date");
      expect(isPastDate(invalidDate)).toBe(false);
    });
  });

  describe("getToday", () => {
    beforeEach(() => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date(`${MOCK_DATE}T12:00:00Z`));
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it("should return a dayjs object set to the start of the current day", () => {
      const today = getToday();
      expect(today.hour()).toBe(0);
      expect(today.minute()).toBe(0);
      expect(today.second()).toBe(0);
      expect(today.millisecond()).toBe(0);
    });

    it("should return the correct current date", () => {
      const today = getToday();
      expect(today.format(DATE_FORMATS.API)).toBe(MOCK_DATE);
    });
  });

  describe("formatToApiDate", () => {
    it("should format a valid dayjs object to the API date string", () => {
      expect(formatToApiDate(dayjs(MOCK_DATE))).toBe(MOCK_DATE);
    });

    it("should return undefined when date is null", () => {
      expect(formatToApiDate(null)).toBeUndefined();
    });

    it("should return undefined when date is undefined", () => {
      expect(formatToApiDate(undefined)).toBeUndefined();
    });

    it("should return an invalid format string when date is invalid", () => {
      const result = formatToApiDate(dayjs("invalid"));
      expect(result).toBe("Invalid Date");
    });
  });

  describe("formatToApiDateTime", () => {
    it("should format a valid dayjs object to the API local date-time string", () => {
      expect(formatToApiDateTime(dayjs("2024-04-29T14:30:15"))).toBe(
        "2024-04-29T14:30:15",
      );
    });

    it("should apply the start-of-day boundary", () => {
      expect(
        formatToApiDateTime(dayjs("2024-04-29T14:30:15"), "startOfDay"),
      ).toBe("2024-04-29T00:00:00");
    });

    it("should apply the end-of-day boundary", () => {
      expect(
        formatToApiDateTime(dayjs("2024-04-29T14:30:15"), "endOfDay"),
      ).toBe("2024-04-29T23:59:59");
    });

    it("should return undefined when date is null or undefined", () => {
      expect(formatToApiDateTime(null)).toBeUndefined();
      expect(formatToApiDateTime(undefined)).toBeUndefined();
    });
  });

  describe("parseTime", () => {
    it("should return a valid dayjs object from a time string", () => {
      const result = parseTime("14:30");
      expect(result).not.toBeNull();
      expect(result?.format(DATE_FORMATS.TIME)).toBe("14:30");
    });

    it("should return null when time string is null or undefined", () => {
      expect(parseTime(null)).toBeNull();
      expect(parseTime(undefined)).toBeNull();
    });
  });

  describe("getLocalizedDayName", () => {
    it("should return a day name string", () => {
      const dayName = getLocalizedDayName(1);
      expect(typeof dayName).toBe("string");
      expect(dayName.length).toBeGreaterThan(0);
    });
  });
});
