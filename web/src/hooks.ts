import { useEffect, useState } from "react";
import { month, usePersonal } from "./data";
import { addDays, malaysiaDate } from "./core";
import type { PrayerSnapshot } from "./types";
export function useAsync<T>(fn: () => Promise<T>, deps: unknown[]) {
  const [result, set] = useState<{
    data: T | null;
    error: boolean;
    loading: boolean;
  }>({ data: null, error: false, loading: true });
  const [revision, reload] = useState(0);
  useEffect(() => {
    let live = true;
    set({ data: null, error: false, loading: true });
    fn()
      .then((data) => {
        if (live) set({ data, error: false, loading: false });
      })
      .catch(() => {
        if (live) set({ data: null, error: true, loading: false });
      });
    return () => {
      live = false;
    };
  }, [...deps, revision]);
  return { ...result, retry: () => reload((r) => r + 1) };
}
export function useTimetable(date = malaysiaDate(), span = 1) {
  const zone = usePersonal().preferences.zone_code,
    [revision, refresh] = useState(0);
  return {
    ...useAsync(async () => {
      const months = [
          ...new Set(
            Array.from({ length: span + 1 }, (_, i) =>
              addDays(date, i).slice(0, 7),
            ),
          ),
        ],
        result: PrayerSnapshot[] = [];
      for (const m of months)
        result.push(...(await month(zone, m, revision > 0)));
      return result;
    }, [zone, date, span, revision]),
    refresh: () => refresh((r) => r + 1),
  };
}
export function useNow() {
  const [now, set] = useState(new Date());
  useEffect(() => {
    const id = setInterval(() => {
      if (!document.hidden) set(new Date());
    }, 1000);
    const focus = () => set(new Date());
    document.addEventListener("visibilitychange", focus);
    return () => {
      clearInterval(id);
      document.removeEventListener("visibilitychange", focus);
    };
  }, []);
  return now;
}
