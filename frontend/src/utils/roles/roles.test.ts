// Tests frontend : verifie le comportement de roles.test.

import { describe, expect, it } from "vitest";
import {
  getRoleTranslationKey,
  hasRoleName,
  normalizeRoleName,
  ROLE_NAMES,
} from "./roles";

describe("roles utilities", () => {
  it("normalizes role names consistently for comparisons and translations", () => {
    expect(normalizeRoleName("ROLE_ADMIN")).toBe("ADMIN");
    expect(normalizeRoleName(" chef_agence ")).toBe("CHEF_AGENCE");
    expect(getRoleTranslationKey("ROLE_SUPER_ADMIN")).toBe("SUPER_ADMIN");
  });

  it("matches roles without depending on case or ROLE_ prefix", () => {
    const roles = [{ name: "ROLE_CHEF_AGENCE" }, { name: "agent" }];

    expect(hasRoleName(roles, ROLE_NAMES.AGENCY_MANAGER)).toBe(true);
    expect(hasRoleName(roles, "ROLE_AGENT")).toBe(true);
    expect(hasRoleName(roles, ROLE_NAMES.ADMIN)).toBe(false);
  });
});
