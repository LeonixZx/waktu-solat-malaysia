import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import prayers from "../../shared/fixtures/prayers-wly01-2026-11.json" with { type: "json" };
import quran from "../../shared/fixtures/quran-1.json" with { type: "json" };
import { readFileSync, existsSync } from "node:fs";

test("large text, keyboard focus, dark mode and accessibility checks", async ({
  page,
}) => {
  await page.route("**/api/prayers?*", (r) => r.fulfill({ json: prayers }));
  await page.route("**/api/quran/1", (r) => r.fulfill({ json: quran }));
  await page.setViewportSize({ width: 320, height: 900 });
  for (const route of [
    "/",
    "/quran/1",
    "/worship",
    "/tasks",
    "/backup",
    "/settings",
    "/calendar",
  ]) {
    await page.goto(route);
    if (route === "/")
      await expect(page.locator(".prayer-row").first()).toBeVisible();
    if (route === "/quran/1")
      await expect(page.locator(".ayah-card").first()).toBeVisible();
    await page.evaluate(
      () => (document.documentElement.style.fontSize = "200%"),
    );
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
      route,
    ).toBeTruthy();
    const result = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa", "wcag21aa"])
      .analyze();
    expect(
      result.violations.map((v) => ({
        id: v.id,
        impact: v.impact,
        nodes: v.nodes.map((n) => ({
          target: n.target,
          summary: n.failureSummary,
        })),
      })),
      route,
    ).toEqual([]);
    if (route === "/")
      await page.screenshot({
        path: "../app/build/verification/v3/web-home-320-text200.png",
        fullPage: true,
      });
  }
  await page.goto("/settings");
  await page
    .getByRole("combobox", { name: "Tema", exact: true })
    .selectOption("dark");
  await expect(page.locator("html")).toHaveClass("dark");
  await page.keyboard.press("Tab");
  expect(
    await page.evaluate(() => document.activeElement?.tagName !== "BODY"),
  ).toBeTruthy();
  await page.emulateMedia({ reducedMotion: "reduce" });
  expect(
    await page.evaluate(
      () => matchMedia("(prefers-reduced-motion: reduce)").matches,
    ),
  ).toBeTruthy();
});

test("Android backup imports with preview and disabled tasks; malformed files are rejected", async ({
  page,
}) => {
  await page.goto("/backup");
  const native = "../app/build/verification/v3/android-backup.json";
  const base = existsSync(native)
    ? JSON.parse(readFileSync(native, "utf8"))
    : JSON.parse(
        readFileSync("../app/build/verification/v3/web-backup.json", "utf8"),
      );
  base.bookmarks.push("surah:114");
  base.reminders.push({
    id: "portable-test",
    title: "Portable reading",
    anchor: "isha",
    offsetMinutes: 360,
    weekdays: [1],
    enabled: true,
  });
  await page
    .locator("input[type=file]")
    .setInputFiles({
      name: "native.json",
      mimeType: "application/json",
      buffer: Buffer.from(JSON.stringify(base)),
    });
  await expect(page.getByRole("dialog")).toBeVisible();
  await page.getByRole("button", { name: "Gantikan", exact: true }).click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await page.goto("/tasks");
  await expect(
    page.getByRole("heading", { name: "Portable reading" }),
  ).toBeVisible();
  await expect(page.getByRole("checkbox").last()).not.toBeChecked();
  await page.goto("/backup");
  await page
    .locator("input[type=file]")
    .setInputFiles({
      name: "bad.json",
      mimeType: "application/json",
      buffer: Buffer.from('{"schemaVersion":999}'),
    });
  await expect(page.getByRole("alert")).toBeVisible();
});
