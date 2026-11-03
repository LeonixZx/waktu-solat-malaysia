import "fake-indexeddb/auto";
import { it, expect, vi } from "vitest";
import { db, queueAdd, runDownloads, deleteText, verses } from "./data";
import quran from "../../shared/fixtures/quran-1.json";
it("persists a selected-surah queue and drains it from validated stored text", async () => {
  await db.put("quran:1", quran);
  await queueAdd([1]);
  await runDownloads();
  expect(await db.get("downloadQueue")).toEqual([]);
  expect(await verses(1, "en")).toHaveLength(7);
  await Promise.all([queueAdd([2]), queueAdd([3])]);
  expect(await db.get("downloadQueue")).toEqual([2, 3]);
  await db.put("downloadQueue", []);
});
it("deleting text invalidates an in-flight response instead of recreating the cache", async () => {
  await deleteText(1);
  let resolve!: (value: Response) => void;
  const response = new Promise<Response>((done) => (resolve = done));
  const fetcher = vi.spyOn(globalThis, "fetch").mockReturnValue(response);
  try {
    const pending = verses(1, "en");
    await vi.waitFor(() => expect(fetcher).toHaveBeenCalled());
    await deleteText(1);
    resolve(new Response(JSON.stringify(quran), { status: 200 }));
    await expect(pending).rejects.toThrow("deleted");
    expect(await db.get("quran:1")).toBeUndefined();
  } finally {
    fetcher.mockRestore();
  }
});
