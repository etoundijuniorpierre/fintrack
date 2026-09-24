// Tests frontend : verifie le comportement de pagination config.test.

import { describe, it, expect } from "vitest";
import {
  DEFAULT_PAGE_SIZE,
  paginationConfig,
  getPaginationConfig,
} from "./paginationConfig";

describe("paginationConfig", () => {
  it("should have defaultPageSize of 10 by default", () => {
    expect(DEFAULT_PAGE_SIZE).toBe(10);
    expect(paginationConfig.defaultPageSize).toBe(DEFAULT_PAGE_SIZE);
  });

  it("should place pagination at bottomCenter by default", () => {
    expect(paginationConfig.placement).toEqual(["bottomCenter"]);
  });

  it("should show size changer by default", () => {
    expect(paginationConfig.showSizeChanger).toBe(true);
  });

  it('should format showTotal as "start-end / total" when range is within total', () => {
    const showTotal = paginationConfig.showTotal!;
    expect(showTotal(100, [1, 10])).toBe("1-10 / 100");
    expect(showTotal(50, [11, 20])).toBe("11-20 / 50");
  });

  it("should format showTotal correctly when on the last page with partial results", () => {
    const showTotal = paginationConfig.showTotal!;
    expect(showTotal(25, [21, 25])).toBe("21-25 / 25");
  });
});

describe("getPaginationConfig", () => {
  it("should return the default config when no overrides are provided", () => {
    const config = getPaginationConfig();
    expect(config.defaultPageSize).toBe(DEFAULT_PAGE_SIZE);
    expect(config.pageSize).toBeUndefined();
    expect(config.showSizeChanger).toBe(true);
    expect(config.placement).toEqual(["bottomCenter"]);
  });

  it("should override pageSize when a custom pageSize is provided", () => {
    const config = getPaginationConfig({ pageSize: 20 });
    expect(config.pageSize).toBe(20);
    expect(config.defaultPageSize).toBeUndefined();
  });

  it("should override showSizeChanger when false is provided", () => {
    const config = getPaginationConfig({ showSizeChanger: false });
    expect(config.showSizeChanger).toBe(false);
  });

  it("should preserve default placement when only pageSize is overridden", () => {
    const config = getPaginationConfig({ pageSize: 20 });
    expect(config.placement).toEqual(["bottomCenter"]);
  });

  it("should merge additional properties when hideOnSinglePage is provided", () => {
    const config = getPaginationConfig({ hideOnSinglePage: true });
    expect(config.hideOnSinglePage).toBe(true);
    expect(config.defaultPageSize).toBe(DEFAULT_PAGE_SIZE);
  });

  it("should not mutate the base paginationConfig when overrides are applied", () => {
    getPaginationConfig({ pageSize: 50 });
    expect(paginationConfig.defaultPageSize).toBe(DEFAULT_PAGE_SIZE);
    expect(paginationConfig.pageSize).toBeUndefined();
  });
});
