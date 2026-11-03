import { test, expect } from "@playwright/test";
import type { Page } from "@playwright/test";
import prayers from "../../shared/fixtures/prayers-wly01-2026-11.json" with { type: "json" };
import quran from "../../shared/fixtures/quran-1.json" with { type: "json" };
async function fixture(page: Page) {
  await page.clock.install({ time: new Date("2026-11-02T10:00:00+08:00") });
  await page.route("**/api/prayers?*", (route) =>
    route.fulfill({ json: prayers }),
  );
  await page.route("**/api/quran/1", (route) => route.fulfill({ json: quran }));
}
test("one primary navigation adapts to width and short windows, with reachable settings", async ({
  page,
}) => {
  await fixture(page);
  for (const [width, height] of [
    [320, 800],
    [360, 800],
    [600, 900],
    [840, 900],
    [1280, 900],
    [1920, 1000],
    [840, 400],
  ]) {
    await page.setViewportSize({ width, height });
    await page.goto("/");
    const menu = page.getByRole("navigation", { name: "Menu utama" });
    await expect(menu).toHaveCount(1);
    await expect(menu.getByRole("link")).toHaveCount(5);
    await expect(
      page.getByRole("button", { name: /Buka menu|Open menu/ }),
    ).toHaveCount(0);
    await menu.getByRole("link", { name: "Al-Quran", exact: true }).click();
    await page
      .getByRole("link", { name: "Muat turun", exact: true })
      .filter({ visible: true })
      .click();
    await expect(page).toHaveURL(/downloads$/);
    await expect(
      menu.getByRole("link", { name: "Al-Quran", exact: true }),
    ).toHaveAttribute("aria-current", "page");
    await page
      .getByRole("link", { name: "Pilihan", exact: true })
      .filter({ visible: true })
      .click();
    await expect(page.getByLabel("Bahasa", { exact: true })).toBeVisible();
    await expect(menu.locator('[aria-current="page"]')).toHaveCount(0);
    await expect(
      page
        .getByRole("link", { name: "Pilihan", exact: true })
        .filter({ visible: true }),
    ).toHaveAttribute("aria-current", "page");
    await page.screenshot({
      path: `../app/build/verification/v3/nav-settings-${width}x${height}.png`,
    });
    await page.goto("/");
    if ([360, 840, 1280].includes(width))
      await page.screenshot({
        path: `../app/build/verification/v3/nav-home-${width}x${height}.png`,
        fullPage: true,
      });
  }
});
test("Saved only highlights saved content; Settings and backup have their own active state", async ({
  page,
}) => {
  await fixture(page);
  for (const width of [360, 800, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto("/saved");
    const menu = page.getByRole("navigation", { name: "Menu utama" });
    const saved = menu.getByRole("link", { name: "Simpanan", exact: true });
    const settings = page
      .getByRole("link", { name: "Pilihan", exact: true })
      .filter({ visible: true });
    await expect(saved).toHaveAttribute("aria-current", "page");
    await settings.click();
    await expect(menu.locator('[aria-current="page"]')).toHaveCount(0);
    await expect(saved).not.toHaveClass(/current/);
    await expect(settings).toHaveAttribute("aria-current", "page");
    await page.reload();
    await expect(menu.locator('[aria-current="page"]')).toHaveCount(0);
    await page
      .getByRole("link", { name: "Sandaran & pemulihan", exact: true })
      .click();
    await expect(menu.locator('[aria-current="page"]')).toHaveCount(0);
    await expect(settings).toHaveAttribute("aria-current", "page");
    await saved.click();
    await expect(saved).toHaveAttribute("aria-current", "page");
    await expect(settings).not.toHaveAttribute("aria-current", "page");
  }
});
test("responsive home and feature routes have no horizontal overflow", async ({
  page,
}) => {
  await fixture(page);
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(e.message));
  for (const width of [320, 360, 600, 840, 1280, 1920]) {
    await page.setViewportSize({ width, height: 1000 });
    await page.goto("/");
    await expect(page.locator(".prayer-row").first()).toBeVisible();
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBeTruthy();
    if (width === 360 || width === 1280)
      await page.screenshot({
        path: `../app/build/verification/v3/web-home-${width}.png`,
        fullPage: true,
      });
  }
  for (const route of [
    "/worship",
    "/explore",
    "/calendar",
    "/settings",
    "/tasks",
    "/downloads",
    "/saved",
    "/backup",
    "/guide",
  ]) {
    await page.setViewportSize({ width: 360, height: 800 });
    await page.goto(route);
    await expect(page.locator("h1")).toBeVisible();
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBeTruthy();
  }
  expect(errors).toEqual([]);
});
test("Quran search, virtual reader and bookmark persist", async ({ page }) => {
  await fixture(page);
  await page.setViewportSize({ width: 360, height: 800 });
  await page.goto("/quran");
  await page.getByRole("textbox").fill("Al-Faatiha");
  await page.getByRole("button", { name: /01 Al-Faatiha/ }).click();
  await expect(page.getByRole("heading", { name: "Al-Faatiha" })).toBeVisible();
  await expect(page.locator(".ayah-card").first()).toContainText(
    "Dengan nama Allah",
  );
  await page.getByRole("button", { name: "Simpan surah", exact: true }).click();
  await page.reload();
  await expect(
    page.getByRole("button", { name: "Surah disimpan", exact: true }).first(),
  ).toHaveAttribute("aria-pressed", "true");
  await page.screenshot({
    path: "../app/build/verification/v3/web-reader.png",
    fullPage: true,
  });
  await page.goto("/saved");
  await expect(page.getByRole("link", { name: /Al-Faatiha/ })).toBeVisible();
});
test("Malay and English UI, tasbih, reminders and backup export", async ({
  page,
}) => {
  await fixture(page);
  await page.goto("/settings");
  await page.getByLabel("Bahasa", { exact: true }).selectOption("en");
  await expect(
    page.getByRole("heading", { name: "Your preferences" }),
  ).toBeVisible();
  await page.goto("/tasbih");
  const count = page.getByRole("button", { name: "Increase count" });
  await count.click();
  await count.click();
  await expect(count).toContainText("2");
  await page.reload();
  await expect(count).toContainText("2");
  await page.goto("/tasks");
  await page.getByRole("button", { name: "Add a reminder" }).click();
  await page.getByLabel("Title", { exact: true }).fill("Reading after Isha");
  await page.getByLabel("Prayer times", { exact: true }).selectOption("isha");
  await page.getByLabel("minutes", { exact: true }).fill("360");
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Reading after Isha" }),
  ).toBeVisible();
  await page.goto("/backup");
  const file = page.waitForEvent("download");
  await page.getByRole("button", { name: "Export backup" }).click();
  const download = await file;
  await download.saveAs("../app/build/verification/v3/web-backup.json");
  const path = await download.path();
  expect(path).toBeTruthy();
});
test("PWA has valid installation metadata and survives offline reload", async ({
  page,
  context,
}) => {
  await fixture(page);
  await page.goto("/");
  await expect(page.locator(".prayer-row").first()).toBeVisible();
  await page.goto("/quran/1");
  await expect(page.locator(".ayah-card").first()).toBeVisible();
  await page.evaluate(async () => {
    await navigator.serviceWorker.ready;
  });
  const manifest = await page.request.get("/manifest.webmanifest");
  expect((await manifest.json()).display).toBe("standalone");
  await page.reload();
  await expect
    .poll(() => page.evaluate(() => !!navigator.serviceWorker.controller))
    .toBe(true);
  await context.setOffline(true);
  await page.reload();
  await expect(page.locator(".ayah-card").first()).toContainText(
    "Dengan nama Allah",
  );
  await page.goto("/collections/wirid");
  await expect(page.locator(".reading-list .list-link").first()).toBeVisible();
  await context.setOffline(false);
});
test("all collections and video catalog have working actions", async ({
  page,
}) => {
  await fixture(page);
  await page.setViewportSize({ width: 1280, height: 900 });
  for (const kind of ["doa", "hadith", "wirid", "niat"]) {
    await page.goto("/collections/" + kind);
    await page.locator(".reading-list .list-link").first().click();
    await expect(page.locator(".reading-card .arabic").first()).toBeVisible();
    for (const part of await page.locator(".reading-card .meaning").all())
      await expect(part).not.toBeEmpty();
  }
  await page.goto("/explore");
  await page
    .getByRole("button", { name: "Tonton", exact: true })
    .first()
    .click();
  await expect(page.getByRole("dialog")).toBeVisible();
  await expect(page.locator(".video-poster")).toBeVisible();
  await page.getByRole("button", { name: "Tutup", exact: true }).click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
});
