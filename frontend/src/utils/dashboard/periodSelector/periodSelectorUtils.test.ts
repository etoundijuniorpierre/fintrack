// Tests frontend : verifie le comportement de periode selecteur utils.test.

import dayjs from "dayjs";
import { PeriodType } from "../../../types/dashboard";
import { getImmediateFilter, validateCustomRange } from "./periodSelectorUtils";
import { describe, expect, it } from "vitest";

describe("periodSelectorUtils", () => {
  it("should return a filter object for immediate periods", () => {
    expect(getImmediateFilter(PeriodType.LAST_30_DAYS)).toEqual({
      period: PeriodType.LAST_30_DAYS,
    });
    expect(getImmediateFilter(PeriodType.TODAY)).toEqual({
      period: PeriodType.TODAY,
    });
  });

  it("should return null for CUSTOM period", () => {
    expect(getImmediateFilter(PeriodType.CUSTOM)).toBeNull();
  });

  it("should return null if either date is missing", () => {
    const date = dayjs("2023-01-01");
    expect(validateCustomRange(null, date)).toBeNull();
    expect(validateCustomRange(date, null)).toBeNull();
    expect(validateCustomRange(null, null)).toBeNull();
  });

  it("should return null if from date is after to date", () => {
    const from = dayjs("2023-01-02");
    const to = dayjs("2023-01-01");
    expect(validateCustomRange(from, to)).toBeNull();
  });

  it("should return a valid PeriodFilter if range is valid", () => {
    const from = dayjs("2023-01-01T10:00:00Z");
    const to = dayjs("2023-01-02T10:00:00Z");
    const result = validateCustomRange(from, to);

    expect(result).toEqual({
      period: PeriodType.CUSTOM,
      dateFrom: from.startOf("day").format("YYYY-MM-DDTHH:mm:ss.SSS"),
      dateTo: to.endOf("day").format("YYYY-MM-DDTHH:mm:ss.SSS"),
    });
  });

  it("should handle same date/time as valid", () => {
    const date = dayjs("2023-01-01T10:00:00Z");
    const result = validateCustomRange(date, date);

    expect(result).toEqual({
      period: PeriodType.CUSTOM,
      dateFrom: date.startOf("day").format("YYYY-MM-DDTHH:mm:ss.SSS"),
      dateTo: date.endOf("day").format("YYYY-MM-DDTHH:mm:ss.SSS"),
    });
  });
});
