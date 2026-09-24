// Tests frontend : verifie l'impression securisee d'une image.

import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { printImage } from "./printImage";

describe("printImage", () => {
  let printSpy: ReturnType<typeof vi.fn>;
  let closeSpy: ReturnType<typeof vi.fn>;
  let fakeDoc: Document;
  let openSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    fakeDoc = document.implementation.createHTMLDocument("");
    printSpy = vi.fn();
    closeSpy = vi.fn();
    const fakeWindow = {
      document: fakeDoc,
      print: printSpy,
      close: closeSpy,
    } as unknown as Window;
    openSpy = vi
      .spyOn(window, "open")
      .mockReturnValue(fakeWindow as Window);
  });

  afterEach(() => {
    openSpy.mockRestore();
  });

  it("does nothing when the source is empty", () => {
    printImage("", "photo.png");
    expect(openSpy).not.toHaveBeenCalled();
  });

  it("opens a print window and renders the image", () => {
    printImage("blob:abc", "photo.png");
    expect(openSpy).toHaveBeenCalledWith("", "_blank");
    const img = fakeDoc.querySelector("img");
    expect(img).not.toBeNull();
    expect(img?.getAttribute("src")).toBe("blob:abc");
  });

  it("sets the document title from the filename without HTML injection", () => {
    // Un nom de fichier pieg�e ne doit pas creer de balise : title est une
    // affectation de propriete, pas du HTML interprete.
    printImage("blob:abc", "</title><script>alert(1)</script>.png");
    expect(fakeDoc.querySelector("script")).toBeNull();
    expect(fakeDoc.title).toContain("<script>");
  });

  it("prints and closes once the image has loaded", () => {
    printImage("blob:abc", "photo.png");
    const img = fakeDoc.querySelector("img") as HTMLImageElement;
    img.onload?.(new Event("load"));
    expect(printSpy).toHaveBeenCalledTimes(1);
    expect(closeSpy).toHaveBeenCalledTimes(1);
  });
});
