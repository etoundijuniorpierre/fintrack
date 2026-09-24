// Tests frontend : verifient la langue transmise aux API par le client partage.

import { beforeEach, describe, expect, it, vi } from "vitest";
import { resolveApiLanguage } from "./client";

const { i18nMock } = vi.hoisted(() => ({
  i18nMock: {
    resolvedLanguage: "fr",
    language: "fr",
  },
}));

vi.mock("../i18n", () => ({ default: i18nMock }));

// Valide la normalisation de langue utilisee par l'intercepteur Axios.
describe("resolveApiLanguage", () => {
  // Restaure la langue par defaut avant chaque scenario.
  beforeEach(() => {
    i18nMock.resolvedLanguage = "fr";
    i18nMock.language = "fr";
  });

  // Transmet le francais lorsque l'interface est en francais.
  it("should return French for a French interface", () => {
    expect(resolveApiLanguage()).toBe("fr");
  });

  // Transmet l'anglais y compris pour une variante regionale.
  it("should return English for an English regional locale", () => {
    i18nMock.resolvedLanguage = "en-US";

    expect(resolveApiLanguage()).toBe("en");
  });
});
