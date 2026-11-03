type Env = { ASSETS: { fetch: (request: Request) => Promise<Response> } };

const allowedKeys = [
  "imsak",
  "fajr",
  "syuruk",
  "dhuha",
  "dhuhr",
  "asr",
  "maghrib",
  "isha",
];

const json = (data: unknown, status = 200) =>
  new Response(JSON.stringify(data), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
      "X-Content-Type-Options": "nosniff",
    },
  });

export async function route(request: Request, env: Env): Promise<Response> {
  const url = new URL(request.url);
  if (!url.pathname.startsWith("/api/")) return env.ASSETS.fetch(request);
  if (request.method !== "GET")
    return json({ error: "Method not allowed" }, 405);
  let upstream = "";
  let kind = "";
  if (url.pathname === "/api/catalog")
    return env.ASSETS.fetch(new Request(new URL("/data/catalog.json", url)));
  if (url.pathname === "/api/prayers") {
    const zone = url.searchParams.get("zone") ?? "",
      month = url.searchParams.get("month") ?? "";
    if (
      !/^[A-Z]{3}\d{2}$/.test(zone) ||
      !/^\d{4}-(0[1-9]|1[0-2])$/.test(month) ||
      +month.slice(0, 4) < 2000 ||
      +month.slice(0, 4) > 2100
    )
      return json({ error: "Invalid zone or month" }, 400);
    const [year, m] = month.split("-");
    upstream = `https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=month&zone=${zone}&year=${year}&month=${+m}`;
    kind = "prayers";
  } else {
    const match = /^\/api\/quran\/([1-9]\d{0,2})$/.exec(url.pathname);
    if (!match || +match[1] > 114)
      return json({ error: "Unknown API route" }, 404);
    upstream = `https://api.alquran.cloud/v1/surah/${match[1]}/editions/quran-uthmani,ms.basmeih,ar.alafasy,en.sahih`;
    kind = "quran";
  }

  const start = Date.now();
  try {
    const response = await fetch(upstream, {
      headers: {
        Accept: "application/json",
        "User-Agent": "WaktuSolatMalaysia/3.0",
      },
      signal: AbortSignal.timeout(20000),
    });
    if (!response.ok) throw Error("Provider unavailable");
    const text = await response.text();
    if (text.length > 6000000) throw Error("Provider response too large");
    const data = JSON.parse(text);
    if (kind === "prayers") {
      if (
        !Array.isArray(data.prayerTime) ||
        !data.prayerTime.length ||
        data.prayerTime.some((row: Record<string, string>) =>
          allowedKeys.some(
            (k) => !/^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/.test(row[k]),
          ),
        )
      )
        throw Error("Invalid provider timetable");
    } else if (!Array.isArray(data.data) || data.data.length !== 4)
      throw Error("Invalid Quran response");
    console.info(
      JSON.stringify({
        service: kind,
        status: 200,
        latencyMs: Date.now() - start,
      }),
    );
    return new Response(
      JSON.stringify({
        ...data,
        _meta: {
          retrievedAt: Date.now(),
          source: kind === "prayers" ? "JAKIM e-Solat" : "AlQuran Cloud",
        },
      }),
      {
        headers: {
          "Content-Type": "application/json; charset=utf-8",
          "Cache-Control":
            kind === "quran" ? "public,max-age=86400" : "public,max-age=3600",
          "X-Content-Type-Options": "nosniff",
        },
      },
    );
  } catch {
    console.warn(
      JSON.stringify({
        service: kind,
        status: 502,
        latencyMs: Date.now() - start,
      }),
    );
    return json(
      { error: "Provider unavailable. Saved content may still be used." },
      502,
    );
  }
}

export default { fetch: route };
