import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import prayers from "../../shared/fixtures/prayers-wly01-2026-11.json" with { type: "json" };

test.beforeEach(async ({ page }) => {
  await page.route("**/api/prayers?*", (r) => r.fulfill({ json: prayers }));
});

test("calendar distinguishes provisional events and unavailable year data", async ({page}) => {
  await page.goto("/calendar");
  await expect(page.getByText("Rujukan tertakluk kepada pengisytiharan rasmi Malaysia.")).toBeVisible();
  await page.route("**/data/islamic_dates.json",r=>r.fulfill({json:{data:[]}}));
  await page.reload();
  await expect(page.getByText("Rujukan peristiwa bagi tahun ini belum tersedia.")).toBeVisible();
});
test("tablet reader adapts to actual space and large text without overflow", async ({
  page,
}) => {
  test.setTimeout(120000);
  for (const width of [320, 360, 600, 800, 840, 1280, 1920])
    for (const scale of [1, 1.3, 2]) {
      await page.setViewportSize({ width, height: 1000 });
      await page.goto("/collections/wirid");
      await page.evaluate(
        (scale) =>
          (document.documentElement.style.fontSize = scale * 100 + "%"),
        scale,
      );
      await page.waitForTimeout(50);
      const back = page.getByRole("button", {
        name: "Kembali ke senarai",
        exact: true,
      });
      if (await back.isVisible()) await back.click();
      await expect(
        page.locator(".reading-list .list-link").first(),
      ).toBeVisible();
      const wide = await page
        .locator(".collection-split")
        .getAttribute("data-split");
      if (width === 800 && scale <= 1.3) expect(wide).toBe("true");
      if (scale === 2) expect(wide).toBe("false");
      await page.locator(".reading-list .list-link").first().click();
      await expect(
        page.locator(".reading-card .transliteration"),
      ).toBeVisible();
      await expect(page.locator(".reading-card")).toContainText(
        "Terjemahan · Bahasa Melayu",
      );
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth,
        ),
      ).toBeTruthy();
      if (width === 800 || (scale === 2 && width === 320))
        await page.screenshot({
          path: `../app/build/verification/v31/web-collection-${width}-text${scale}.png`,
          fullPage: true,
        });
      if (wide === "false") {
        await page
          .getByRole("button", { name: "Kembali ke senarai", exact: true })
          .click();
        await expect(page.locator(".reading-list")).toBeVisible();
      }
    }
});
test("all 36 readings, Rumi, translation labels, saved filters and offline reading", async ({
  page,
  context,
}) => {
  await page.setViewportSize({ width: 1280, height: 1000 });
  let total = 0;
  for (const [kind, count] of [
    ["doa", 5],
    ["wirid", 14],
    ["niat", 5],
    ["hadith", 12],
  ] as const) {
    await page.goto("/collections/" + kind);
    await expect(page.locator(".reading-list .list-link")).toHaveCount(count);
    for (let i = 0; i < count; i++) {
      await page.locator(".reading-list .list-link").nth(i).click();
      for (const part of await page.locator(".reading-section .arabic").all())
        await expect(part).not.toBeEmpty();
      await expect(page.locator(".reading-card footer")).toContainText(
        "Rujukan",
      );
      total++;
    }
    await page.screenshot({
      path: `../app/build/verification/v31/web-${kind}.png`,
      fullPage: true,
    });
  }
  expect(total).toBe(36);
  await page.goto("/collections/wirid");
  await page.locator(".reading-list .list-link").first().click();
  await page
    .locator(".reading-card")
    .getByRole("button", { name: "Simpan", exact: true })
    .click();
  await page.getByRole("button", { name: "Semua bacaan", exact: true }).click();
  await expect(page.locator(".reading-list .list-link")).toHaveCount(1);
  await page.reload();
  await expect(page.locator(".collection-filter")).toHaveAttribute(
    "aria-pressed",
    "true",
  );
  await page.goto("/settings");
  await page.getByLabel("Bahasa", { exact: true }).selectOption("en");
  await page.goto("/collections/wirid");
  await expect(page.locator(".reading-card")).toContainText(
    "English translation unavailable",
  );
  await expect(page.locator(".reading-card")).toContainText(
    "Translation · Bahasa Melayu",
  );
  await page.evaluate(async () => {
    await navigator.serviceWorker.ready;
  });
  await page.reload();
  await expect
    .poll(() => page.evaluate(() => !!navigator.serviceWorker.controller))
    .toBe(true);
  await context.setOffline(true);
  await page.reload();
  await expect(page.locator(".reading-card .transliteration")).not.toBeEmpty();
  await context.setOffline(false);
});
test("collections and Kiblat remain accessible at 200 percent text", async ({
  page,
}) => {
  await page.setViewportSize({ width: 320, height: 900 });
  for (const route of ["/collections/wirid/0", "/qibla", "/tasbih"]) {
    await page.goto(route);
    await page.evaluate(
      () => (document.documentElement.style.fontSize = "200%"),
    );
    if (route.includes("collections"))
      await expect(page.locator(".reading-card")).toBeVisible();
    const result = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa", "wcag21aa"])
      .analyze();
    expect(
      result.violations.map((v) => ({
        id: v.id,
        nodes: v.nodes.map((n) => n.target),
      })),
      route,
    ).toEqual([]);
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBeTruthy();
  }
});
test("browser Kiblat never certifies unknown north, and provides an official reference", async ({
  page,
}) => {
  await page.addInitScript(() => {
    Object.defineProperty(navigator.geolocation, "getCurrentPosition", {
      value: (ok: PositionCallback) =>
        ok({
          coords: {
            latitude: 3.139,
            longitude: 101.6869,
            accuracy: 20,
            altitude: null,
            altitudeAccuracy: null,
            heading: null,
            speed: null,
          },
          timestamp: Date.now(),
        } as GeolocationPosition),
    });
  });
  await page.goto("/qibla");
  await page
    .getByRole("button", { name: "Tentukan arah saya", exact: true })
    .click();
  await page.evaluate(() =>
    window.dispatchEvent(
      new DeviceOrientationEvent("deviceorientationabsolute", {
        absolute: true,
        alpha: 67.5,
        beta: 0,
        gamma: 0,
      }),
    ),
  );
  await expect(page.locator(".qibla-confidence")).toHaveAttribute(
    "data-quality",
    "low_confidence",
  );
  await expect(page.getByRole("heading", { name: "292.5°" })).toBeVisible();
  await expect(page.locator(".qibla-confidence")).toContainText(
    "bukan kompas peranti yang disahkan",
  );
  await expect(
    page.getByRole("link", { name: /eFalak Selangor/ }),
  ).toHaveAttribute("href", "https://efalak.muftiselangor.gov.my");
  await page.screenshot({
    path: "../app/build/verification/v31/web-qibla.png",
    fullPage: true,
  });
});
test("reader scroll and selected item survive reload and pane resizing", async ({
  page,
}) => {
  await page.setViewportSize({ width: 1280, height: 800 });
  await page.goto("/collections/niat");
  await page.locator(".reading-list .list-link").first().click();
  await expect(page.locator(".reading-card")).toBeVisible();
  await page.waitForTimeout(100);
  await page.locator(".library-detail").evaluate((el) => (el.scrollTop = 200));
  await expect
    .poll(() =>
      page.evaluate(
        () =>
          JSON.parse(sessionStorage.getItem("wsm-reading-ui:niat") ?? "{}")
            .readerPositions?.["niat:0"] ?? 0,
      ),
    )
    .toBeGreaterThan(100);
  await page.reload();
  await expect(page.locator(".reading-card")).toBeVisible();
  await expect
    .poll(() => page.locator(".library-detail").evaluate((el) => el.scrollTop))
    .toBeGreaterThan(100);
  await page.setViewportSize({ width: 360, height: 900 });
  await expect(page.locator(".reading-card")).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Kembali ke senarai", exact: true }),
  ).toBeAttached();
  await page.setViewportSize({ width: 1280, height: 800 });
  await expect(page.locator(".reading-list")).toBeVisible();
  await expect(page.locator(".reading-list .current")).toContainText("Subuh");
});
