import { useReaderLayout } from "./useReaderLayout";
import { sectionTranslation, sectionLanguage } from "./readings";
import { qiblaQuality, smoothHeading, angleDelta } from "./qibla";
import type { QiblaState } from "./qibla";
import { useEffect, useRef, useState } from "react";
import type { FormEvent, ReactNode } from "react";
import {
  Routes,
  Route,
  Link,
  useLocation,
  useNavigate,
  useParams,
} from "react-router-dom";
import {
  Home as HomeIcon,
  BookOpen,
  Compass,
  Bookmark,
  X,
  Settings,
  CalendarDays,
  Clock,
  Bell,
  Download,
  ArrowLeft,
  ArrowRight,
  ExternalLink,
  Play,
  Pause,
  Share2,
  RotateCcw,
  Check,
  Plus,
  Trash2,
  Edit3,
  Layers,
  Search,
  Sun,
  Moon,
  CloudSun,
  Sunrise,
  Sunset,
  Globe,
  HardDrive,
  Info,
} from "lucide-react";
import { useRegisterSW } from "virtual:pwa-register/react";
import type {
  PrayerSnapshot,
  Surah,
  Verse,
  Reading,
  CatalogEntry,
  PrayerRelativeReminder,
  BackupBundle,
} from "./types";
import { keys, obligatory } from "./types";
import {
  usePersonal,
  update,
  staticData,
  surahs,
  verses,
  readings,
  catalog,
  db,
  deleteText,
  month,
  queueAdd,
  runDownloads,
  pauseDownloads,
  watchDownloads,
  downloadRunning,
} from "./data";
import {
  names,
  zoneTime,
  malaysiaDate,
  addDays,
  nextPrayer,
  nextTask,
  qibla,
  calendarExport,
  validateBackup,
  mergeBackup,
} from "./core";
import { useAsync, useTimetable, useNow } from "./hooks";
import { t } from "./i18n";
import {
  Page,
  Loading,
  ErrorState,
  Save,
  Banner,
  Modal,
  VirtualList,
  YoutubePlayer,
} from "./components";
const hubs = [
  { to: "/", ms: "Utama", Icon: HomeIcon },
  { to: "/quran", ms: "Al-Quran", Icon: BookOpen },
  { to: "/worship", ms: "Ibadah", Icon: Layers },
  { to: "/explore", ms: "Terokai", Icon: Compass },
  { to: "/saved", ms: "Simpanan", Icon: Bookmark },
];
function activeHub(path: string) {
  if (settingsActive(path)) return null;
  if (path.startsWith("/quran") || path.startsWith("/downloads"))
    return "/quran";
  if (
    [
      "/worship",
      "/qibla",
      "/tasbih",
      "/collections",
      "/tasks",
      "/calendar",
    ].some((p) => path.startsWith(p))
  )
    return "/worship";
  if (path.startsWith("/explore")) return "/explore";
  if (path.startsWith("/saved")) return "/saved";
  return "/";
}
function settingsActive(path: string) {
  return ["/settings", "/backup"].some(
    (p) => path === p || path.startsWith(p + "/"),
  );
}
function TopNavigation() {
  const l = usePersonal().preferences.language,
    location = useLocation();
  return (
    <>
      <div className="brand">
        <img src="/assets/mark.svg" alt="" />
        <div>
          <strong>
            Waktu Solat
            <br />
            Malaysia
          </strong>
          <small>
            {t(l, "Ruang untuk yang bermakna.", "Time for what matters.")}
          </small>
        </div>
      </div>
      <nav aria-label={t(l, "Menu utama", "Main menu")}>
        {hubs.map(({ to, ms, Icon }) => (
          <Link
            key={to}
            to={to}
            aria-current={
              activeHub(location.pathname) === to ? "page" : undefined
            }
            className={
              activeHub(location.pathname) === to
                ? "nav-item current"
                : "nav-item"
            }
          >
            <Icon size={22} />
            <span>{t(l, ms)}</span>
          </Link>
        ))}
      </nav>
      <Link
        to="/settings"
        aria-label={t(l, "Pilihan")}
        title={t(l, "Pilihan")}
        aria-current={settingsActive(location.pathname) ? "page" : undefined}
        className={
          "nav-item nav-settings" +
          (settingsActive(location.pathname) ? " current" : "")
        }
      >
        <Settings size={22} />
        <span>{t(l, "Pilihan")}</span>
      </Link>
    </>
  );
}
export default function App() {
  const personal = usePersonal(),
    l = personal.preferences.language,
    [install, setInstall] = useState<null | { prompt: () => Promise<void> }>(
      null,
    ),
    location = useLocation();
  const {
    needRefresh: [needRefresh],
    updateServiceWorker,
  } = useRegisterSW();
  const [activeDownloads, setActiveDownloads] = useState(downloadRunning());
  useEffect(
    () => watchDownloads(() => setActiveDownloads(downloadRunning())),
    [],
  );
  useEffect(() => {
    document.documentElement.lang = l;
    document.documentElement.dataset.theme = personal.preferences.theme;
    const mode = personal.preferences.theme;
    const media = matchMedia("(prefers-color-scheme:dark)");
    const apply = () =>
      document.documentElement.classList.toggle(
        "dark",
        mode === "dark" || (mode === "system" && media.matches),
      );
    apply();
    media.addEventListener("change", apply);
    return () => media.removeEventListener("change", apply);
  }, [l, personal.preferences.theme]);
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [location.pathname]);
  useEffect(() => {
    const cb = (event: Event) => {
      event.preventDefault();
      setInstall(event as unknown as { prompt: () => Promise<void> });
    };
    window.addEventListener("beforeinstallprompt", cb);
    return () => window.removeEventListener("beforeinstallprompt", cb);
  }, []);
  return (
    <div className="app-shell">
      <a className="skip" href="#content">
        {t(l, "Ke kandungan", "Skip to content")}
      </a>
      <aside className="sidebar">
        <TopNavigation />
      </aside>
      <div className="app-main">
        <header className="topbar">
          <Link to="/" className="top-logo">
            <img src="/assets/mark.svg" alt="" />
            <span>Waktu Solat Malaysia</span>
          </Link>
          <div className="top-actions">
            {install && (
              <button
                className="secondary install"
                onClick={() => void install.prompt()}
              >
                <Download size={18} />
                {t(l, "Pasang", "Install")}
              </button>
            )}
            <Link
              to="/settings"
              className={
                "icon-button phone-settings" +
                (settingsActive(location.pathname) ? " selected" : "")
              }
              aria-current={
                settingsActive(location.pathname) ? "page" : undefined
              }
              aria-label={t(l, "Pilihan")}
            >
              <Settings size={21} />
            </Link>
          </div>
        </header>
        <main id="content">
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/schedule" element={<Schedule />} />
            <Route path="/quran" element={<Quran />} />
            <Route path="/quran/:id" element={<QuranReader />} />
            <Route path="/worship" element={<Worship />} />
            <Route path="/collections/:kind" element={<Collections />} />
            <Route path="/collections/:kind/:index" element={<Collections />} />
            <Route path="/tasbih" element={<Tasbih />} />
            <Route path="/qibla" element={<Qibla />} />
            <Route path="/calendar" element={<Calendar />} />
            <Route path="/tasks" element={<Tasks />} />
            <Route path="/explore" element={<Explore />} />
            <Route path="/saved" element={<Saved />} />
            <Route path="/downloads" element={<Downloads />} />
            <Route path="/settings" element={<Preferences />} />
            <Route path="/backup" element={<Backup />} />
            <Route path="/guide" element={<Guide />} />
            <Route
              path="*"
              element={
                <Page title="404">
                  <Link to="/">Home</Link>
                </Page>
              }
            />
          </Routes>
        </main>
        <nav
          className="bottom-nav"
          aria-label={t(l, "Menu utama", "Main menu")}
        >
          {hubs.map(({ to, ms, Icon }) => (
            <Link
              className={activeHub(location.pathname) === to ? "current" : ""}
              key={to}
              to={to}
              aria-current={
                activeHub(location.pathname) === to ? "page" : undefined
              }
            >
              <Icon size={23} />
              <span>{t(l, ms)}</span>
            </Link>
          ))}
        </nav>
      </div>
      {needRefresh && (
        <div className="update-banner" role="status">
          <span>
            {t(
              l,
              "Kemas kini sedia. Simpan bacaan sebelum memuat semula.",
              "An update is ready. Save your reading before reloading.",
            )}
          </span>
          <button
            disabled={activeDownloads}
            onClick={() => {
              if (!downloadRunning()) void updateServiceWorker(true);
            }}
          >
            {t(l, "Muat semula", "Reload")}
          </button>
        </div>
      )}
    </div>
  );
}
function useLanguage() {
  return usePersonal().preferences.language;
}
function formatTime(value: string) {
  const p = usePersonal().preferences;
  return p.twelve_hour
    ? new Intl.DateTimeFormat("en", {
        timeZone: zoneTime,
        hour: "numeric",
        minute: "2-digit",
        hour12: true,
      }).format(new Date("2026-01-01T" + value.slice(0, 5) + ":00+08:00"))
    : value.slice(0, 5);
}
function Time({ value }: { value: string }) {
  return <>{formatTime(value)}</>;
}
function Countdown({ days }: { days: PrayerSnapshot[] }) {
  const now = useNow(),
    personal = usePersonal(),
    l = personal.preferences.language,
    next = nextPrayer(days, now),
    seconds = next ? Math.max(0, Math.floor((+next.time - +now) / 1000)) : 0;
  return (
    <div className="next-card">
      <small>{t(l, "SOLAT SETERUSNYA")}</small>
      <div>
        <strong>{next ? names[l][next.index] : "—"}</strong>
        <strong>
          {next ? <Time value={next.day.times[next.index]} /> : ""}
        </strong>
      </div>
      <span>
        {next
          ? `${String(Math.floor(seconds / 3600)).padStart(2, "0")}:${String(Math.floor(seconds / 60) % 60).padStart(2, "0")}:${String(seconds % 60).padStart(2, "0")}`
          : t(l, "Jadual seterusnya belum tersedia")}
      </span>
    </div>
  );
}
function LiveClock() {
  const now = useNow(),
    p = usePersonal().preferences;
  return (
    <span className="live-clock">
      {new Intl.DateTimeFormat(p.language, {
        timeZone: zoneTime,
        hour: "2-digit",
        minute: "2-digit",
        hour12: p.twelve_hour,
      }).format(now)}
    </span>
  );
}
function Home() {
  const p = usePersonal(),
    l = p.preferences.language,
    today = malaysiaDate(),
    data = useTimetable(today, 1),
    navigate = useNavigate(),
    day = data.data?.find((d) => d.date === today),
    next = nextPrayer(data.data ?? []);
  return (
    <Page
      title={t(l, "Assalamualaikum")}
      subtitle={t(
        l,
        "Hari yang lebih teratur.",
        "A little more room in your day.",
      )}
      wide
    >
      <Link className="zone-pill" to="/settings">
        <Compass size={17} />
        {p.preferences.zone_name}
        <span>{p.preferences.zone_code}</span>
      </Link>
      <div className="dashboard">
        <div className="dashboard-primary">
          <section className="home-hero">
            <img src="/assets/hero_mosque.webp" alt="" />
            <div className="hero-wash" />
            <div className="hero-content">
              <LiveClock />
              <p>{day?.hijri ? `${day.hijri} H` : "MALAYSIA · UTC+8"}</p>
              <p>
                {new Intl.DateTimeFormat(l, {
                  timeZone: zoneTime,
                  dateStyle: "full",
                }).format(new Date())}
              </p>
              <Countdown days={data.data ?? []} />
            </div>
          </section>
          <div className="section-title">
            <h2>{t(l, "Ruang ibadah anda")}</h2>
          </div>
          <div className="shortcut-grid">
            {[
              { to: "/qibla", label: "Kiblat", Icon: Compass },
              { to: "/tasbih", label: "Tasbih", Icon: RotateCcw },
              { to: "/collections/doa", label: "Doa", Icon: BookOpen },
              { to: "/tasks", label: "Peringatan peribadi", Icon: Bell },
              { to: "/calendar", label: "Kalendar", Icon: CalendarDays },
              { to: "/downloads", label: "Muat turun", Icon: Download },
            ].map(({ to, label, Icon }, i) => (
              <Link key={to} to={to}>
                <span className={"shortcut-icon tone-" + i}>
                  <Icon />
                </span>
                <strong>{t(l, label)}</strong>
              </Link>
            ))}
          </div>
          <Link to="/guide" className="text-link">
            <Info size={18} />
            {t(l, "Panduan ringkas")}
          </Link>
        </div>
        <div className="dashboard-secondary">
          <div className="section-title">
            <h2>{t(l, "Waktu Solat")}</h2>
            <Link to="/schedule">{t(l, "Lihat semua")}</Link>
          </div>
          {data.loading ? (
            <Loading />
          ) : data.error ? (
            <ErrorState retry={data.refresh} />
          ) : (
            <div className="prayer-list">
              {obligatory.map((index, i) => {
                const Icon = [Sunrise, Sun, CloudSun, Sunset, Moon][i];
                return (
                  <Link
                    to="/schedule"
                    key={index}
                    className={
                      next?.index === index && next.day.date === today
                        ? "prayer-row active"
                        : "prayer-row"
                    }
                  >
                    <Icon size={24} />
                    <span>{names[l][index]}</span>
                    <strong>
                      {day ? <Time value={day.times[index]} /> : "—"}
                    </strong>
                    <Bell size={16} />
                  </Link>
                );
              })}
            </div>
          )}
          <Banner
            image="banner_quran"
            title={t(l, "Sambung bacaan")}
            subtitle={`${p.reading.name} · ${t(l, "Ayat", "Ayah")} ${p.reading.verse}`}
            onClick={() =>
              navigate(
                "/quran/" + p.reading.surah + "?verse=" + p.reading.verse,
              )
            }
          />
          <Banner
            image="banner_hikmah"
            title={t(l, "Ruang hikmah")}
            subtitle={t(
              l,
              "Satu bacaan untuk direnungkan.",
              "A reading to reflect on.",
            )}
            onClick={() => navigate("/collections/hadith")}
          />
        </div>
      </div>
      <div className="data-footer">
        <span>
          JAKIM e-Solat ·{" "}
          {day?.freshness === "offline"
            ? t(l, "Luar talian", "Offline")
            : day?.freshness === "cached"
              ? t(l, "Jadual tersimpan", "Saved timetable")
              : t(l, "Jadual rasmi", "Official timetable")}
        </span>
        <button className="text-button" onClick={data.refresh}>
          {t(l, "Segarkan jadual")}
        </button>
      </div>
    </Page>
  );
}
function downloadFile(name: string, text: string, type = "application/json") {
  const url = URL.createObjectURL(new Blob([text], { type })),
    link = document.createElement("a");
  link.href = url;
  link.download = name;
  link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
function Schedule() {
  const p = usePersonal(),
    l = p.preferences.language,
    [date, setDate] = useState(
      new URLSearchParams(location.search).get("date") ?? malaysiaDate(),
    ),
    [mode, setMode] = useState("day"),
    data = useTimetable(
      mode === "month" ? date.slice(0, 7) + "-01" : date,
      mode === "month"
        ? new Date(
            Date.UTC(+date.slice(0, 4), +date.slice(5, 7), 0),
          ).getUTCDate() - 1
        : mode === "week"
          ? 6
          : 0,
    ),
    days = (data.data ?? [])
      .filter((d) =>
        mode === "month"
          ? d.date.startsWith(date.slice(0, 7))
          : d.date >= date && d.date <= addDays(date, mode === "week" ? 6 : 0),
      )
      .sort((a, b) => a.date.localeCompare(b.date));
  function move(n: number) {
    if (mode === "month") {
      const d = new Date(date + "T12:00:00+08:00");
      d.setUTCMonth(d.getUTCMonth() + n, 1);
      setDate(d.toISOString().slice(0, 10));
    } else setDate(addDays(date, n * (mode === "week" ? 7 : 1)));
  }
  return (
    <Page
      title={t(l, "Jadual solat")}
      subtitle={`${p.preferences.zone_name} · ${p.preferences.zone_code}`}
    >
      <div className="segmented">
        {[
          ["day", "Harian"],
          ["week", "Mingguan"],
          ["month", "Bulanan"],
        ].map(([v, label]) => (
          <button key={v} aria-pressed={mode === v} onClick={() => setMode(v)}>
            {t(l, label)}
          </button>
        ))}
      </div>
      <div className="date-controls">
        <button
          className="icon-button"
          onClick={() => move(-1)}
          aria-label={t(l, "Sebelumnya")}
        >
          <ArrowLeft />
        </button>
        <input
          type="date"
          aria-label={t(l, "Pilih tarikh", "Choose date")}
          value={date}
          onChange={(e) => setDate(e.target.value || malaysiaDate())}
        />
        <button
          className="icon-button"
          onClick={() => move(1)}
          aria-label={t(l, "Seterusnya")}
        >
          <ArrowRight />
        </button>
        <button className="secondary" onClick={() => setDate(malaysiaDate())}>
          {t(l, "Hari ini")}
        </button>
      </div>
      {data.loading ? (
        <Loading />
      ) : data.error ? (
        <ErrorState retry={data.refresh} />
      ) : mode === "day" ? (
        <div className="prayer-list">
          {keys.map((key, i) => (
            <div key={key} className="prayer-row">
              <span>{names[l][i]}</span>
              <strong>
                {days[0] ? <Time value={days[0].times[i]} /> : "—"}
              </strong>
              <Link
                to="/tasks"
                className="icon-button"
                aria-label={t(l, "Peringatan peribadi")}
              >
                <Bell size={18} />
              </Link>
            </div>
          ))}
        </div>
      ) : (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>{t(l, "Tarikh", "Date")}</th>
                {obligatory.map((i) => (
                  <th key={i}>{names[l][i]}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {days.map((d) => (
                <tr key={d.date}>
                  <th>
                    {d.date.slice(8)} · {d.hijri} H
                  </th>
                  {obligatory.map((i) => (
                    <td key={i}>
                      <Time value={d.times[i]} />
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <div className="actions">
        <button className="secondary" onClick={data.refresh}>
          {t(l, "Segarkan jadual")}
        </button>
        <button
          className="secondary"
          disabled={!days.length}
          onClick={() =>
            downloadFile(
              "Prayer-times-" + date + ".ics",
              calendarExport(days, l),
              "text/calendar",
            )
          }
        >
          <CalendarDays size={18} />
          {t(l, "Eksport kalendar", "Export calendar")}
        </button>
      </div>
      <p className="help">
        {t(
          l,
          "Peringatan dan azan berjadual tersedia dalam aplikasi Android. Versi web menawarkan jadual langsung dan eksport kalendar.",
          "Scheduled alerts and adhan are available in the Android app. Web provides a live timetable and calendar export.",
        )}
      </p>
    </Page>
  );
}
function Quran() {
  const { ref, twoPane } = useReaderLayout();
  const p = usePersonal(),
    l = p.preferences.language,
    meta = useAsync(surahs, []),
    [query, setQuery] = useState(""),
    [saved, setSaved] = useState(false),
    [selected, setSelected] = useState<number | null>(null),
    navigate = useNavigate();
  const filtered = (meta.data ?? []).filter(
    (s) =>
      (!saved || p.bookmarks.includes("surah:" + s.number)) &&
      (s.englishName + " " + s.name + " " + s.number)
        .toLowerCase()
        .includes(query.toLowerCase()),
  );
  return (
    <Page
      title={t(l, "Al-Quran")}
      subtitle={t(l, "Baca, dengar dan sambung dari ayat terakhir.")}
      wide
    >
      <div ref={ref} className="library-split" data-split={twoPane}>
        <div className="library-index">
          <div className="library-actions">
            <button
              className="secondary"
              onClick={() =>
                navigate(`/quran/${p.reading.surah}?verse=${p.reading.verse}`)
              }
            >
              <BookOpen size={18} />
              {t(l, "Sambung bacaan")} · {p.reading.name}
            </button>
            <Link to="/downloads" className="button secondary">
              <Download size={18} />
              {t(l, "Muat turun")}
            </Link>
          </div>
          <div className="segmented">
            <button aria-pressed={!saved} onClick={() => setSaved(false)}>
              {t(l, "Semua surah")}
            </button>
            <button aria-pressed={saved} onClick={() => setSaved(true)}>
              {t(l, "Tersimpan")}
            </button>
          </div>
          <label className="search">
            <Search size={18} />
            <input
              aria-label={t(l, "Cari nama atau nombor surah")}
              placeholder={t(l, "Cari nama atau nombor surah")}
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
          </label>
          {meta.loading ? (
            <Loading />
          ) : meta.error ? (
            <ErrorState retry={meta.retry} />
          ) : filtered.length ? (
            <VirtualList
              items={filtered}
              render={(s) => (
                <button
                  className="surah-row"
                  onClick={() => {
                    if (twoPane) setSelected(s.number);
                    else navigate("/quran/" + s.number);
                  }}
                >
                  <span className="number-badge">
                    {String(s.number).padStart(2, "0")}
                  </span>
                  <span>
                    <strong>{s.englishName}</strong>
                    <small>
                      {s.numberOfAyahs} {t(l, "ayat", "ayahs")} ·{" "}
                      {s.revelationType === "Meccan" ? "Makkiyah" : "Madaniyah"}
                    </small>
                  </span>
                  <span className="arabic surah-name" lang="ar" dir="rtl">
                    {s.name}
                  </span>
                </button>
              )}
            />
          ) : (
            <div className="notice">
              {t(l, "Tiada surah sepadan.", "No matching surahs.")}
            </div>
          )}
          {saved &&
            p.bookmarks
              .filter((id) => id.startsWith("ayah:"))
              .map((id) => {
                const [, n, v] = id.split(":");
                return (
                  <Link
                    className="list-link"
                    key={id}
                    to={`/quran/${n}?verse=${v}`}
                  >
                    <Bookmark size={18} />
                    {
                      meta.data?.find((s) => s.number === +n)?.englishName
                    } · {t(l, "Ayat", "Ayah")} {v}
                  </Link>
                );
              })}
        </div>
        <div className="library-detail">
          {selected ? (
            <Reader number={selected} />
          ) : (
            <div className="reading-empty">
              <BookOpen size={48} />
              <h2>{t(l, "Pilih surah", "Choose a surah")}</h2>
              <p>
                {t(
                  l,
                  "Bacaan anda akan dibuka di sini.",
                  "Your reading opens here.",
                )}
              </p>
            </div>
          )}
        </div>
      </div>
    </Page>
  );
}
function QuranReader() {
  const number = Number(useParams().id);
  return (
    <Page title="" wide>
      <Reader
        number={number}
        initial={Number(new URLSearchParams(location.search).get("verse") ?? 1)}
      />
    </Page>
  );
}
function Reader({ number, initial = 1 }: { number: number; initial?: number }) {
  const p = usePersonal(),
    l = p.preferences.language,
    language = p.preferences.quran_language ?? l,
    data = useAsync(() => verses(number, language), [number, language]),
    meta = useAsync(surahs, []),
    name =
      meta.data?.find((s) => s.number === number)?.englishName ?? "Al-Quran",
    audio = useRef<HTMLAudioElement>(null),
    [playing, setPlaying] = useState<number | null>(null),
    [error, setError] = useState(false);
  useEffect(
    () => () => {
      audio.current?.pause();
    },
    [number],
  );
  function reading(v: number) {
    if (v < 1) return;
    void update((s) =>
      s.reading.surah === number && s.reading.verse === v
        ? s
        : { ...s, reading: { surah: number, verse: v, name } },
    );
  }
  function play(verse: Verse) {
    if (!audio.current) return;
    setError(false);
    setPlaying(verse.number);
    audio.current.src = verse.audio;
    void audio.current.play().catch(() => setError(true));
  }
  return (
    <section className="reader">
      <header className="reader-heading">
        <div>
          <h1>{name}</h1>
          <p>
            {data.data?.length ?? "…"} {t(l, "ayat", "ayahs")} ·{" "}
            {language === "en"
              ? "Sahih International"
              : "Abdullah Muhammad Basmeih"}
          </p>
        </div>
        <Save id={"surah:" + number} />
      </header>
      {data.loading ? (
        <Loading />
      ) : data.error ? (
        <ErrorState retry={data.retry} />
      ) : (
        <VirtualList
          items={data.data ?? []}
          initialIndex={Math.max(0, initial - 1)}
          estimate={300}
          onVisible={(index) => reading(index + 1)}
          render={(verse) => (
            <article className="ayah-card">
              <header>
                <strong>
                  {t(l, "Ayat", "Ayah")} {verse.number}
                </strong>
                <div className="actions">
                  <Save id={`ayah:${number}:${verse.number}`} />
                  <button
                    className="icon-button"
                    aria-label={t(l, "Dengar") + " " + verse.number}
                    onClick={() => play(verse)}
                  >
                    <Play size={20} />
                  </button>
                  <button
                    className="icon-button"
                    aria-label={t(l, "Kongsi") + " " + verse.number}
                    onClick={() =>
                      void share(
                        `${name} ${verse.number}\n${verse.arabic}\n\n${verse.meaning}`,
                      )
                    }
                  >
                    <Share2 size={19} />
                  </button>
                </div>
              </header>
              <p
                className="arabic ayah"
                lang="ar"
                dir="rtl"
                style={{ fontSize: p.preferences.arabic_size / 16 + "rem" }}
              >
                {verse.arabic}
              </p>
              {p.preferences.show_translation && (
                <p className="meaning">{verse.meaning}</p>
              )}
            </article>
          )}
        />
      )}
      <div className="audio-bar">
        <span>
          {error
            ? t(
                l,
                "Audio tidak tersedia. Cuba lagi.",
                "Audio unavailable. Please retry.",
              )
            : playing
              ? name + " · " + playing
              : "Mishary Rashid Alafasy"}
        </span>
        <audio
          ref={audio}
          controls
          preload="none"
          onError={() => setError(true)}
          onEnded={() => setPlaying(null)}
        />
        <button
          className="icon-button"
          aria-label={t(l, "Henti")}
          onClick={() => {
            audio.current?.pause();
            setPlaying(null);
          }}
        >
          <Pause size={18} />
        </button>
      </div>
    </section>
  );
}
async function share(text: string) {
  if (navigator.share) await navigator.share({ text }).catch(() => {});
  else await navigator.clipboard.writeText(text).catch(() => {});
}
function Worship() {
  const l = useLanguage();
  const groups = [
    {
      ms: "Amalan",
      en: "Practice",
      items: [
        {
          to: "/tasbih",
          title: "Tasbih digital",
          sub: t(l, "Amalan hari ini"),
          Icon: RotateCcw,
        },
        {
          to: "/tasks",
          title: "Peringatan peribadi",
          sub: t(l, "Sebelum atau selepas solat", "Before or after prayers"),
          Icon: Bell,
        },
      ],
    },
    {
      ms: "Panduan",
      en: "Guidance",
      items: [
        {
          to: "/qibla",
          title: "Kiblat",
          sub: t(
            l,
            "Arah dan keyakinan sensor",
            "Direction and sensor confidence",
          ),
          Icon: Compass,
        },
        {
          to: "/calendar",
          title: "Kalendar Islam",
          sub: t(
            l,
            "Tarikh, jadual dan eksport",
            "Dates, schedules and export",
          ),
          Icon: CalendarDays,
        },
      ],
    },
  ];
  return (
    <Page
      title={t(l, "Ibadah")}
      subtitle={t(
        l,
        "Amalan dan bacaan untuk rutin anda.",
        "Reading and tools for your daily practice.",
      )}
      wide
    >
      <div className="worship-hub">
        <h2>{t(l, "Bacaan", "Reading")}</h2>
        <div className="worship-readings">
          {[
            ["doa", "Doa"],
            ["wirid", "Wirid"],
            ["niat", "Niat"],
            ["hadith", "Hadis"],
          ].map(([kind, title]) => (
            <Link to={"/collections/" + kind} key={kind}>
              <BookOpen size={28} />
              <strong>{t(l, title)}</strong>
            </Link>
          ))}
        </div>
        {groups.map((group) => (
          <section key={group.ms}>
            <h2>{t(l, group.ms, group.en)}</h2>
            <div className="feature-links">
              {group.items.map(({ to, title, sub, Icon }) => (
                <Link key={to} to={to}>
                  <Icon />
                  <span>
                    <strong>{t(l, title)}</strong>
                    <small>{sub}</small>
                  </span>
                  <ArrowRight size={18} />
                </Link>
              ))}
            </div>
          </section>
        ))}
        <section>
          <h2>{t(l, "Skrin utama", "Home screen")}</h2>
          <div className="notice">
            <strong>{t(l, "Widget waktu solat", "Prayer widgets")}</strong>
            <p>
              {t(
                l,
                "Tiga widget tersedia dalam aplikasi Android. Web boleh dipasang untuk akses pantas.",
                "Three widgets are available in the Android app. Install the web app for quick access.",
              )}
            </p>
            <Link to="/guide">
              {t(l, "Panduan widget", "Widget setup guide")}
            </Link>
          </div>
        </section>
      </div>
    </Page>
  );
}
function Collections() {
  const { kind = "doa", index } = useParams();
  return <CollectionLibrary key={kind} kind={kind} index={index} />;
}
type CollectionMemory = {
  query: string;
  saved: boolean;
  selected: string | null;
  listScroll: number;
  readerScroll: number;
  open: boolean;
};
function CollectionLibrary({ kind, index }: { kind: string; index?: string }) {
  const p = usePersonal(),
    l = p.preferences.language,
    navigate = useNavigate();
  const { ref, twoPane } = useReaderLayout();
  const key = "wsm-reading-ui:" + kind;
  const [memory] = useState<CollectionMemory>(() => {
    try {
      return {
        query: "",
        saved: false,
        selected: null,
        listScroll: 0,
        readerScroll: 0,
        open: false,
        ...JSON.parse(sessionStorage.getItem(key) ?? "{}"),
      };
    } catch {
      return {
        query: "",
        saved: false,
        selected: null,
        listScroll: 0,
        readerScroll: 0,
        open: false,
      };
    }
  });
  const [query, setQuery] = useState(memory.query),
    [saved, setSaved] = useState(memory.saved),
    [selected, setSelected] = useState<string | null>(memory.selected),
    [open, setOpen] = useState(index !== undefined || memory.open),
    listRef = useRef<HTMLDivElement>(null),
    readerRef = useRef<HTMLDivElement>(null),
    data = useAsync(() => readings(kind, l), [kind, l]);
  const restoringScroll = useRef(true),
    previousIndex = useRef(index);
  const all = data.data ?? [];
  const filtered = all.filter(
    (r) =>
      (!saved || p.bookmarks.includes(r.id)) &&
      (
        r.title +
        " " +
        r.meaning +
        " " +
        r.arabic +
        " " +
        r.sections.map((s) => s.transliteration).join(" ")
      )
        .toLowerCase()
        .includes(query.toLowerCase().trim()),
  );
  const selectedReading = all.find((r) => r.id === selected);
  const reading = twoPane
    ? (filtered.find((r) => r.id === selected) ?? filtered[0])
    : selectedReading;
  const showingReader = !twoPane && open;
  useEffect(() => {
    if (index !== undefined && all.length) {
      const n = Number(index);
      setSelected(Number.isInteger(n) ? (all[n]?.id ?? null) : null);
      setOpen(true);
    } else if (index === undefined && previousIndex.current !== undefined)
      setOpen(false);
    previousIndex.current = index;
  }, [index, all.length]);
  useEffect(() => {
    if (data.loading) return;
    restoringScroll.current = true;
    let second = 0;
    const frame = requestAnimationFrame(() => {
      const savedState = JSON.parse(sessionStorage.getItem(key) ?? "{}");
      if (listRef.current)
        listRef.current.scrollTop = savedState.listScroll ?? memory.listScroll;
      const position = reading
        ? (savedState.readerPositions?.[reading.id] ??
          (memory.selected === reading.id ? memory.readerScroll : 0))
        : 0;
      if (twoPane && readerRef.current) readerRef.current.scrollTop = position;
      else if (open && readerRef.current)
        window.scrollTo(
          0,
          window.scrollY +
            readerRef.current.getBoundingClientRect().top +
            position,
        );
      second = requestAnimationFrame(() => {
        restoringScroll.current = false;
      });
    });
    return () => {
      cancelAnimationFrame(frame);
      cancelAnimationFrame(second);
    };
  }, [data.loading, twoPane, reading?.id, open]);
  useEffect(() => {
    const previous = JSON.parse(sessionStorage.getItem(key) ?? "{}");
    sessionStorage.setItem(
      key,
      JSON.stringify({
        ...previous,
        query,
        saved,
        selected: reading?.id ?? selected,
        open,
      }),
    );
  }, [query, saved, selected, open, reading?.id, key]);
  function rememberScroll() {
    if (restoringScroll.current) return;
    const value = JSON.parse(sessionStorage.getItem(key) ?? "{}");
    const position = twoPane
      ? (readerRef.current?.scrollTop ?? 0)
      : Math.max(0, -(readerRef.current?.getBoundingClientRect().top ?? 0));
    sessionStorage.setItem(
      key,
      JSON.stringify({
        ...value,
        listScroll: listRef.current?.scrollTop ?? value.listScroll ?? 0,
        readerPositions: reading
          ? { ...value.readerPositions, [reading.id]: position }
          : value.readerPositions,
      }),
    );
  }
  useEffect(() => {
    if (!twoPane && open) {
      window.addEventListener("scroll", rememberScroll, { passive: true });
      return () => window.removeEventListener("scroll", rememberScroll);
    }
  }, [twoPane, open, reading?.id]);
  function choose(r: Reading) {
    rememberScroll();
    if (r.id !== reading?.id || !open) restoringScroll.current = true;
    setSelected(r.id);
    setOpen(true);
    if (!twoPane) navigate(`/collections/${kind}/${all.indexOf(r)}`);
  }
  const titles: Record<string, string> = {
    doa: "Doa",
    wirid: "Wirid",
    niat: "Niat",
    hadith: "Hadis",
  };
  return (
    <Page
      title={t(l, titles[kind] ?? "Koleksi")}
      subtitle={t(
        l,
        "Arab, Rumi dan terjemahan bersumber.",
        "Arabic, supplied transliteration and labelled translations.",
      )}
      wide
    >
      <div ref={ref} className="collections-shell">
        <nav
          className="collection-tabs segmented"
          aria-label={t(l, "Kategori bacaan", "Reading category")}
        >
          {["doa", "wirid", "niat", "hadith"].map((k) => (
            <Link
              key={k}
              to={"/collections/" + k}
              aria-current={k === kind ? "page" : undefined}
              className={k === kind ? "selected" : ""}
            >
              {t(l, titles[k])}
            </Link>
          ))}
        </nav>
        <div
          className="library-split collection-split"
          data-split={twoPane}
          data-reading={showingReader}
        >
          <div className="library-index">
            <label className="search">
              <Search size={18} />
              <input
                aria-label={t(l, "Cari dalam koleksi")}
                placeholder={t(l, "Cari dalam koleksi")}
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
            </label>
            <button
              className="secondary collection-filter"
              aria-pressed={saved}
              onClick={() => setSaved(!saved)}
            >
              <Bookmark size={18} />
              {t(
                l,
                saved ? "Tersimpan sahaja · aktif" : "Semua bacaan",
                saved ? "Saved only · active" : "All readings",
              )}
            </button>
            <p className="help" role="status">
              {data.loading
                ? t(l, "Memuat bacaan…", "Loading readings…")
                : filtered.length + " " + t(l, "bacaan", "readings")}
            </p>
            {data.loading ? (
              <Loading />
            ) : data.error ? (
              <ErrorState retry={data.retry} />
            ) : !filtered.length ? (
              <div className="notice">
                {t(
                  l,
                  saved
                    ? "Tiada bacaan disimpan yang sepadan."
                    : "Tiada bacaan sepadan.",
                  saved
                    ? "No matching saved readings."
                    : "No matching readings.",
                )}
              </div>
            ) : (
              <div
                ref={listRef}
                className="reading-list"
                onScroll={rememberScroll}
              >
                {filtered.map((r) => (
                  <button
                    key={r.id}
                    className={
                      "list-link" + (reading?.id === r.id ? " current" : "")
                    }
                    aria-current={reading?.id === r.id ? "true" : undefined}
                    onClick={() => choose(r)}
                  >
                    <BookOpen size={20} />
                    <span>
                      <strong>{r.title}</strong>
                      <small>{r.reference}</small>
                    </span>
                    <ArrowRight size={18} />
                  </button>
                ))}
              </div>
            )}
          </div>
          <div
            ref={readerRef}
            className="library-detail"
            onScroll={rememberScroll}
          >
            {showingReader && (
              <button
                className="secondary reader-back"
                onClick={() => {
                  setOpen(false);
                  navigate("/collections/" + kind, { replace: true });
                }}
              >
                <ArrowLeft size={18} />
                {t(l, "Kembali ke senarai", "Back to list")}
              </button>
            )}
            {reading ? (
              <ReadingCard reading={reading} />
            ) : data.loading ? (
              <Loading />
            ) : (
              <div className="reading-empty">
                <BookOpen size={40} />
                <h2>
                  {t(
                    l,
                    showingReader
                      ? "Bacaan tidak ditemui"
                      : "Tiada bacaan dipilih",
                    showingReader ? "Reading not found" : "No reading selected",
                  )}
                </h2>
              </div>
            )}
          </div>
        </div>
      </div>
    </Page>
  );
}
function ReadingCard({ reading }: { reading: Reading }) {
  const p = usePersonal(),
    l = p.preferences.language;
  const shareText = [
    reading.title,
    ...reading.sections.map((s) =>
      [
        s.title,
        s.arabic,
        s.transliteration ? "Rumi\n" + s.transliteration : "",
        sectionLanguage(s, l) + "\n" + sectionTranslation(s, l),
      ]
        .filter(Boolean)
        .join("\n\n"),
    ),
    reading.reference,
  ].join("\n\n");
  return (
    <article className="reading-card">
      <header className="reader-heading">
        <h2>{reading.title}</h2>
        <div className="actions">
          <Save id={reading.id} />
          <button
            className="icon-button"
            aria-label={t(l, "Kongsi")}
            onClick={() => void share(shareText)}
          >
            <Share2 size={20} />
          </button>
        </div>
      </header>
      {reading.sections.map((s) => (
        <section className="reading-section" key={s.id}>
          {s.title && <h3>{s.title}</h3>}
          <p className="reading-label">{t(l, "Arab", "Arabic")}</p>
          <p
            className="arabic"
            lang="ar"
            dir="rtl"
            style={{ fontSize: p.preferences.arabic_size / 16 + "rem" }}
          >
            {s.arabic}
          </p>
          {s.transliteration && (
            <>
              <p className="reading-label">Rumi</p>
              <p className="transliteration" lang="ms-Latn">
                {s.transliteration}
              </p>
            </>
          )}
          {l === "en" && !s.englishTranslation && (
            <p className="help">
              English translation unavailable. The supplied translation is
              labelled below.
            </p>
          )}
          <p className="reading-label">
            {t(l, "Terjemahan", "Translation")} · {sectionLanguage(s, l)}
          </p>
          <p
            className="meaning"
            lang={s.englishTranslation && l === "en" ? "en" : s.localLanguage}
          >
            {sectionTranslation(s, l) ||
              t(l, "Terjemahan belum tersedia.", "Translation unavailable.")}
          </p>
        </section>
      ))}
      <footer>
        {t(l, "Rujukan", "Reference")} · {reading.reference}
      </footer>
    </article>
  );
}
function Tasbih() {
  const p = usePersonal(),
    l = p.preferences.language,
    count = Number(p.tracker.tasbih_count ?? 0),
    goal = Math.max(
      1,
      Math.min(10000, Number(p.tracker.tasbih_goal ?? 33) || 33),
    ),
    date = malaysiaDate();
  const [goalDraft, setGoalDraft] = useState(String(goal));
  const invalidGoal =
    !/^\d+$/.test(goalDraft) ||
    Number(goalDraft) < 1 ||
    Number(goalDraft) > 10000;
  function change(n: number) {
    void update((s) => ({
      ...s,
      tracker: { ...s.tracker, tasbih_count: Math.max(0, n) },
    }));
    if (n > 0 && n % goal === 0) navigator.vibrate?.(60);
  }
  return (
    <Page
      title={t(l, "Tasbih digital")}
      subtitle={t(
        l,
        "Satu ketikan, satu zikir. Teruskan mengikut rentak anda.",
      )}
    >
      <p className="arabic tasbih-phrase" lang="ar" dir="rtl">
        سُبْحَانَ اللَّهِ
      </p>
      <button
        className="tasbih-dial"
        aria-label={t(l, "Tambah kiraan", "Increase count")}
        style={
          {
            "--progress": `${((count % goal) / goal) * 360}deg`,
          } as React.CSSProperties
        }
        onClick={() => change(count + 1)}
      >
        <strong>{count}</strong>
        <span>
          {t(l, "Sasaran", "Goal")} {goal}
        </span>
      </button>
      <p className="center muted">
        {Math.floor(count / goal)} {t(l, "pusingan lengkap", "complete rounds")}
      </p>
      <div className="actions center">
        {[33, 99, 100].map((n) => (
          <button
            className="secondary"
            key={n}
            aria-pressed={goal === n}
            onClick={() => {
              setGoalDraft(String(n));
              void update((s) => ({
                ...s,
                tracker: { ...s.tracker, tasbih_goal: n },
              }));
            }}
          >
            {n}
          </button>
        ))}
        <label className="custom-goal">
          {t(l, "Sasaran", "Goal")}
          <input
            type="number"
            min="1"
            max="10000"
            value={goalDraft}
            aria-invalid={invalidGoal}
            aria-describedby={invalidGoal ? "tasbih-goal-error" : undefined}
            onChange={(e) => {
              setGoalDraft(e.target.value);
              const n = +e.target.value;
              if (Number.isInteger(n) && n >= 1 && n <= 10000)
                void update((s) => ({
                  ...s,
                  tracker: { ...s.tracker, tasbih_goal: n },
                }));
            }}
          />
        </label>
      </div>
      {invalidGoal && (
        <p id="tasbih-goal-error" role="status" className="help">
          {t(l, "Masukkan sasaran 1–10000.", "Enter a goal from 1–10000.")}
        </p>
      )}
      <div className="actions center">
        <button className="secondary" onClick={() => change(count - 1)}>
          {t(l, "Undur satu")}
        </button>
        <button
          className="secondary"
          onClick={() => {
            if (confirm(t(l, "Mulakan kiraan baharu?"))) change(0);
          }}
        >
          <RotateCcw size={18} />
          {t(l, "Mula semula")}
        </button>
      </div>
      <h2>{t(l, "Amalan hari ini")}</h2>
      <div className="card checklist">
        {obligatory.map((i) => {
          const key = "prayed_" + date + "_" + names.ms[i];
          return (
            <label key={key}>
              <input
                type="checkbox"
                checked={Boolean(p.tracker[key])}
                onChange={(e) =>
                  void update((s) => ({
                    ...s,
                    tracker: { ...s.tracker, [key]: e.target.checked },
                  }))
                }
              />
              {names[l][i]}
            </label>
          );
        })}
        <label>
          <input
            type="checkbox"
            checked={Boolean(p.tracker["fasted_" + date])}
            onChange={(e) =>
              void update((s) => ({
                ...s,
                tracker: { ...s.tracker, ["fasted_" + date]: e.target.checked },
              }))
            }
          />
          {t(l, "Saya berpuasa hari ini")}
        </label>
      </div>
      <p className="help">{t(l, "Catatan amalan kekal pada peranti anda.")}</p>
    </Page>
  );
}

function Qibla() {
  const l = useLanguage(),
    now = +useNow();
  const [state, setState] = useState<QiblaState>({
    bearing: null,
    heading: null,
    locationAt: 0,
    headingAt: 0,
    source: "unavailable",
    trueNorth: false,
    tilt: null,
    uncertainty: null,
    available: typeof DeviceOrientationEvent !== "undefined",
  });
  const [accuracy, setAccuracy] = useState<number | null>(null),
    [status, setStatus] = useState("");
  useEffect(() => {
    function orientation(event: DeviceOrientationEvent) {
      const e = event as DeviceOrientationEvent & {
        webkitCompassHeading?: number;
        webkitCompassAccuracy?: number;
      };
      let heading: number | null = null,
        source = "unavailable";
      if (typeof e.webkitCompassHeading === "number") {
        heading = e.webkitCompassHeading;
        source = "WebKit compass (magnetic)";
      } else if (e.absolute && e.alpha !== null) {
        heading = (360 - e.alpha) % 360;
        source = "absolute browser orientation (north reference unknown)";
      }
      if (heading === null) return;
      const at = Date.now(),
        incoming = heading;
      setState((s) => ({
        ...s,
        heading: smoothHeading(s.heading, incoming, at - s.headingAt),
        headingAt: at,
        source,
        available: true,
        tilt:
          e.beta === null || e.gamma === null
            ? null
            : Math.max(Math.abs(e.beta), Math.abs(e.gamma)),
        uncertainty:
          typeof e.webkitCompassAccuracy === "number" &&
          e.webkitCompassAccuracy >= 0
            ? e.webkitCompassAccuracy
            : null,
        trueNorth: false,
      }));
    }
    window.addEventListener(
      "deviceorientationabsolute",
      orientation as EventListener,
    );
    window.addEventListener("deviceorientation", orientation);
    return () => {
      window.removeEventListener(
        "deviceorientationabsolute",
        orientation as EventListener,
      );
      window.removeEventListener("deviceorientation", orientation);
    };
  }, []);
  async function locate() {
    setStatus(t(l, "Sedang mendapatkan kedudukan anda…"));
    if (!navigator.geolocation) {
      setStatus(t(l, "Lokasi tidak tersedia.", "Location is unavailable."));
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const bearing = qibla(pos.coords.latitude, pos.coords.longitude);
        if (!Number.isFinite(bearing)) {
          setStatus(t(l, "Lokasi tidak sah.", "Invalid location."));
          return;
        }
        setState((s) => ({ ...s, bearing, locationAt: pos.timestamp }));
        setAccuracy(pos.coords.accuracy);
        setStatus(t(l, "Diukur dari utara benar", "Measured from true north"));
      },
      () =>
        setStatus(
          t(
            l,
            "Lokasi belum ditemui. Semak kebenaran dan cuba lagi.",
            "Location was not found. Check permission and retry.",
          ),
        ),
      { timeout: 20000, maximumAge: 120000, enableHighAccuracy: true },
    );
    if (typeof DeviceOrientationEvent !== "undefined") {
      const api = DeviceOrientationEvent as typeof DeviceOrientationEvent & {
        requestPermission?: () => Promise<string>;
      };
      if (api.requestPermission) {
        const result = await api.requestPermission().catch(() => "denied");
        if (result !== "granted") setState((s) => ({ ...s, available: false }));
      }
    }
  }
  const quality = qiblaQuality(state, now),
    trusted = quality === "ready";
  const delta =
    trusted && state.bearing !== null && state.heading !== null
      ? angleDelta(state.bearing, state.heading)
      : null;
  return (
    <Page
      title={t(l, "Arah Kiblat")}
      subtitle={t(
        l,
        "Arah yang dikira dan keyakinan sensor ditunjukkan berasingan.",
        "Calculated bearing and sensor confidence are shown separately.",
      )}
    >
      <div className="qibla-layout">
        <div>
          <div
            className="compass"
            data-confidence={quality}
            aria-label={t(
              l,
              "Rajah arah dari utara benar",
              "True-north bearing diagram",
            )}
          >
            <div
              className="compass-rose"
              style={{
                transform: `rotate(${trusted ? -(state.heading ?? 0) : 0}deg)`,
              }}
            >
              <span className="north">N</span>
              <span className="east">E</span>
              <span className="south">S</span>
              <span className="west">W</span>
            </div>
            {state.bearing !== null && (
              <div
                className="compass-needle"
                style={{
                  transform: `rotate(${state.bearing - (trusted ? (state.heading ?? 0) : 0)}deg)`,
                }}
              />
            )}
          </div>
          <h2 className="center">
            {state.bearing === null ? "—" : state.bearing.toFixed(1) + "°"}
          </h2>
          <p className="center muted">
            {t(l, "Diukur dari utara benar", "Measured from true north")}
          </p>
        </div>
        <div>
          <p role="status">
            {status ||
              t(
                l,
                "Benarkan lokasi untuk mengira arah Kiblat.",
                "Allow location to calculate Qibla direction.",
              )}
          </p>
          {accuracy !== null && (
            <p className="muted">
              ±{Math.round(accuracy)} m ·{" "}
              {Math.max(0, Math.floor((now - state.locationAt) / 1000))} s
            </p>
          )}
          <button onClick={() => void locate()}>
            <Compass size={18} />
            {t(l, "Tentukan arah saya")}
          </button>
          <div
            className="notice qibla-confidence"
            role="status"
            data-quality={quality}
          >
            <strong>
              {quality === "stale"
                ? t(
                    l,
                    "Bacaan sudah lama; segarkan lokasi.",
                    "Reading is stale; refresh location.",
                  )
                : delta !== null
                  ? Math.abs(delta) <= 5
                    ? t(
                        l,
                        "Hampir sejajar · anggaran ±5°",
                        "Approximately aligned · ±5° band",
                      )
                    : `${t(l, "Putar", "Turn")} ${Math.round(Math.abs(delta))}° ${delta < 0 ? t(l, "ke kiri", "left") : t(l, "ke kanan", "right")}`
                  : t(
                      l,
                      "Rajah utara di atas, bukan kompas peranti yang disahkan.",
                      "North-up diagram, not a verified device compass.",
                    )}
            </strong>
            <p>
              {t(
                l,
                "Pelayar tidak mengesahkan utara benar atau ketepatan sensor. Arah peranti tidak dianggap boleh dipercayai.",
                "The browser does not certify true north or sensor accuracy. Device alignment is not treated as reliable.",
              )}
            </p>
            <small>
              {t(l, "Sensor", "Sensor")}: {state.source}
            </small>
          </div>
        </div>
      </div>
      <div className="notice">
        <h2>{t(l, "Sebelum menyemak arah", "Before checking direction")}</h2>
        <p>
          {t(
            l,
            "Letakkan peranti rata, tanggalkan penutup magnet/papan kekunci dan jauhkan daripada logam. Putar perlahan untuk menentukur. Bandingkan dengan tanda Kiblat yang telah disahkan.",
            "Lay the device flat, remove magnetic covers/keyboards and move away from metal. Rotate slowly to calibrate. Compare with an independently verified Qibla marker.",
          )}
        </p>
        <a
          href="https://efalak.muftiselangor.gov.my"
          target="_blank"
          rel="noopener noreferrer"
          className="button secondary"
        >
          <ExternalLink size={18} />
          {t(
            l,
            "Rujukan rasmi · eFalak Selangor",
            "Official reference · eFalak Selangor",
          )}
        </a>
      </div>
    </Page>
  );
}
function Calendar() {
  const l = useLanguage(),
    [monthValue, setMonth] = useState(malaysiaDate().slice(0, 7)),
    data = useTimetable(
      monthValue + "-01",
      new Date(
        Date.UTC(+monthValue.slice(0, 4), +monthValue.slice(5), 0),
      ).getUTCDate() - 1,
    ),
    events = useAsync(
      () =>
        staticData<{
          data: {
            year: string;
            dates: { title: string; title_english?: string; date: string; hijri_date: string }[];
          }[];
        }>("islamic_dates.json"),
      [],
    ),
    first = new Date(monthValue + "-01T12:00:00+08:00"),
    count = new Date(
      Date.UTC(+monthValue.slice(0, 4), +monthValue.slice(5), 0),
    ).getUTCDate(),
    offset = (first.getUTCDay() + 6) % 7,
    navigate = useNavigate();
  function move(n: number) {
    const d = new Date(first);
    d.setUTCMonth(d.getUTCMonth() + n);
    setMonth(d.toISOString().slice(0, 7));
  }
  return (
    <Page
      title={t(l, "Kalendar Islam")}
      subtitle={t(l, "Rancang hari dan semak tarikh penting dalam setahun.")}
    >
      <div className="date-controls">
        <button
          className="icon-button"
          aria-label={t(l, "Sebelumnya")}
          onClick={() => move(-1)}
        >
          <ArrowLeft />
        </button>
        <h2>
          {new Intl.DateTimeFormat(l, {
            timeZone: zoneTime,
            month: "long",
            year: "numeric",
          }).format(first)}
        </h2>
        <button
          className="icon-button"
          aria-label={t(l, "Seterusnya")}
          onClick={() => move(1)}
        >
          <ArrowRight />
        </button>
      </div>
      <div className="calendar-grid card">
        {(l === "en"
          ? ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
          : ["Isn", "Sel", "Rab", "Kha", "Jum", "Sab", "Ahd"]
        ).map((d) => (
          <strong className="weekday" key={d}>
            {d}
          </strong>
        ))}
        {Array.from({ length: offset }, (_, i) => (
          <span key={"offset" + i} />
        ))}
        {Array.from({ length: count }, (_, i) => {
          const date = monthValue + "-" + String(i + 1).padStart(2, "0"),
            saved = data.data?.find((d) => d.date === date);
          const approximate = !saved?.hijri;
          let hijri = saved?.hijri?.split("-")[2];
          if (!hijri)
            try {
              hijri = new Intl.DateTimeFormat("en-u-ca-islamic-umalqura", {
                day: "numeric",
                timeZone: zoneTime,
              }).format(new Date(date + "T12:00:00+08:00"));
            } catch {}
          return (
            <button
              key={date}
              className={date === malaysiaDate() ? "today" : ""}
              onClick={() => navigate("/schedule?date=" + date)}
              aria-label={date + " · " + hijri + " H"}
            >
              <span>{i + 1}</span>
              <small>
                {hijri
                  ? (approximate ? "~" : "") + Number.parseInt(hijri) + " H"
                  : ""}
              </small>
            </button>
          );
        })}
      </div>
      <p className="help">
        {data.data?.filter((d) => d.date.startsWith(monthValue) && d.hijri)
          .length === count
          ? t(
              l,
              "Padanan Hijri daripada jadual e-Solat bagi zon pilihan anda.",
              "Hijri dates paired with the selected zone’s e-Solat timetable.",
            )
          : t(
              l,
              "~ menandakan anggaran Hijri; tarikh lain dipadankan dengan e-Solat.",
              "~ marks estimated Hijri dates; other dates are paired with e-Solat.",
            )}
      </p>
      <button
        className="secondary"
        disabled={!data.data?.length}
        onClick={() =>
          downloadFile(
            "Prayer-calendar-" + monthValue + ".ics",
            calendarExport(
              (data.data ?? []).filter((d) => d.date.startsWith(monthValue)),
              l,
            ),
            "text/calendar",
          )
        }
      >
        <CalendarDays size={18} />
        {t(l, "Eksport kalendar", "Export calendar")}
      </button>
      <h2>
        {t(l, "Tarikh penting")} · {monthValue.slice(0, 4)}
      </h2>
      <p className="help">
        {t(
          l,
          "Rujukan tertakluk kepada pengisytiharan rasmi Malaysia.",
          "Reference dates remain subject to Malaysian official announcements.",
        )}
      </p>
      {events.loading && <p role="status">{t(l, "Memuat rujukan…", "Loading references…")}</p>}
      {events.error && <p role="status">{t(l, "Rujukan peristiwa belum dapat dibaca.", "Event references could not be loaded.")}</p>}
      {events.data && !events.data.data.find(y => String(y.year) === monthValue.slice(0, 4))?.dates.length &&
        <p role="status">{t(l, "Rujukan peristiwa bagi tahun ini belum tersedia.", "Event references for this year are unavailable.")}</p>}
      {events.data?.data
        .find((y) => String(y.year) === monthValue.slice(0, 4))
        ?.dates.map((e, i) => (
          <div className="event-row" key={i}>
            <CalendarDays />
            <span>
              <strong>{l === "en" ? e.title_english || e.title : e.title}</strong>
              <small>
                {e.date} · {e.hijri_date}
              </small>
            </span>
          </div>
        ))}
    </Page>
  );
}
function Tasks() {
  const p = usePersonal(),
    l = p.preferences.language,
    [editing, setEditing] = useState<PrayerRelativeReminder | null | undefined>(
      undefined,
    ),
    data = useTimetable(addDays(malaysiaDate(), -1), 10);
  return (
    <Page
      title={t(l, "Peringatan peribadi")}
      subtitle={t(
        l,
        "Rancang amalan sebelum atau selepas solat.",
        "Plan a practice before or after a prayer.",
      )}
    >
      <div className="notice">
        {t(
          l,
          "Versi web menyimpan rancangan anda. Hantar melalui sandaran ke Android untuk pemberitahuan berjadual.",
          "Web stores your plans. Transfer a backup to Android for scheduled notifications.",
        )}
      </div>
      <button onClick={() => setEditing(null)}>
        <Plus size={18} />
        {t(l, "Tambah peringatan")}
      </button>
      {!p.reminders.length && (
        <p className="empty">{t(l, "Tiada peringatan lagi.")}</p>
      )}
      <div className="task-list">
        {p.reminders.map((task) => {
          const next = nextTask(task, data.data ?? []);
          return (
            <article className="card task-card" key={task.id}>
              <header>
                <h2>{task.title}</h2>
                <label className="switch">
                  <input
                    type="checkbox"
                    aria-label={task.title}
                    checked={task.enabled}
                    onChange={(e) =>
                      void update((s) => ({
                        ...s,
                        reminders: s.reminders.map((t) =>
                          t.id === task.id
                            ? { ...t, enabled: e.target.checked }
                            : t,
                        ),
                      }))
                    }
                  />
                  <span />
                </label>
              </header>
              <p>
                {Math.abs(task.offsetMinutes)} {t(l, "minit")}{" "}
                {t(l, task.offsetMinutes < 0 ? "Sebelum" : "Selepas")}{" "}
                {names[l][keys.indexOf(task.anchor)]}
              </p>
              <p className="weekday-summary">
                {[...task.weekdays]
                  .sort()
                  .map(
                    (n) =>
                      (l === "en"
                        ? ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
                        : ["Isn", "Sel", "Rab", "Kha", "Jum", "Sab", "Ahd"])[
                        n - 1
                      ],
                  )
                  .join(" · ")}
              </p>
              <small>
                {next
                  ? new Intl.DateTimeFormat(l, {
                      timeZone: zoneTime,
                      dateStyle: "medium",
                      timeStyle: "short",
                    }).format(next.time)
                  : t(
                      l,
                      "Menunggu jadual atau tidak aktif.",
                      "Waiting for a schedule, or disabled.",
                    )}
              </small>
              <div className="actions">
                <button
                  className="icon-button"
                  aria-label={t(l, "Sunting", "Edit") + " " + task.title}
                  onClick={() => setEditing(task)}
                >
                  <Edit3 size={19} />
                </button>
                <button
                  className="icon-button"
                  aria-label={t(l, "Padam") + " " + task.title}
                  onClick={() => {
                    if (confirm(t(l, "Padam") + " " + task.title + "?"))
                      void update((s) => ({
                        ...s,
                        reminders: s.reminders.filter((t) => t.id !== task.id),
                      }));
                  }}
                >
                  <Trash2 size={19} />
                </button>
              </div>
            </article>
          );
        })}
      </div>
      {editing !== undefined && (
        <TaskEditor
          task={editing}
          days={data.data ?? []}
          close={() => setEditing(undefined)}
        />
      )}
    </Page>
  );
}
function TaskEditor({
  task,
  days,
  close,
}: {
  task: PrayerRelativeReminder | null;
  days: PrayerSnapshot[];
  close: () => void;
}) {
  const l = useLanguage(),
    [title, setTitle] = useState(task?.title ?? ""),
    [anchor, setAnchor] = useState(task?.anchor ?? "fajr"),
    [offset, setOffset] = useState(Math.abs(task?.offsetMinutes ?? 15)),
    [direction, setDirection] = useState(
      (task?.offsetMinutes ?? 15) < 0 ? -1 : 1,
    ),
    [weekdays, setDays] = useState(task?.weekdays ?? [1, 2, 3, 4, 5, 6, 7]);
  const value: PrayerRelativeReminder = {
      id: task?.id ?? crypto.randomUUID(),
      title,
      anchor,
      offsetMinutes: offset * direction,
      weekdays,
      enabled: task?.enabled ?? true,
    },
    next = nextTask(value, days);
  function save(e: FormEvent) {
    e.preventDefault();
    if (
      !title.trim() ||
      offset < 0 ||
      offset > 360 ||
      !Number.isInteger(offset) ||
      !weekdays.length
    )
      return;
    void update((s) => ({
      ...s,
      reminders: [
        ...s.reminders.filter((t) => t.id !== value.id),
        { ...value, title: title.trim() },
      ],
    }));
    close();
  }
  return (
    <Modal title={t(l, "Peringatan peribadi")} onClose={close}>
      <form className="form" onSubmit={save}>
        <label>
          {t(l, "Tajuk")}
          <input
            required
            maxLength={80}
            value={title}
            onChange={(e) => setTitle(e.target.value)}
          />
        </label>
        <label>
          {t(l, "Waktu Solat")}
          <select
            aria-label={t(l, "Waktu Solat")}
            value={anchor}
            onChange={(e) => setAnchor(e.target.value as typeof anchor)}
          >
            {keys.map((key, i) => (
              <option key={key} value={key}>
                {names[l][i]}
              </option>
            ))}
          </select>
        </label>
        <div className="form-row">
          <label>
            {t(l, "minit")}
            <input
              type="number"
              required
              min={0}
              max={360}
              value={offset}
              onChange={(e) => setOffset(+e.target.value)}
            />
          </label>
          <label>
            {t(l, "Rujukan", "Relative to")}
            <select
              value={direction}
              onChange={(e) => setDirection(+e.target.value)}
            >
              <option value={-1}>{t(l, "Sebelum")}</option>
              <option value={1}>{t(l, "Selepas")}</option>
            </select>
          </label>
        </div>
        <fieldset>
          <legend>{t(l, "Pilih hari rujukan solat.")}</legend>
          <div className="weekday-buttons">
            {[1, 2, 3, 4, 5, 6, 7].map((day) => (
              <button
                type="button"
                key={day}
                aria-pressed={weekdays.includes(day)}
                className="secondary"
                onClick={() =>
                  setDays(
                    weekdays.includes(day)
                      ? weekdays.filter((d) => d !== day)
                      : [...weekdays, day],
                  )
                }
              >
                {
                  (l === "en"
                    ? ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
                    : ["Isn", "Sel", "Rab", "Kha", "Jum", "Sab", "Ahd"])[
                    day - 1
                  ]
                }
              </button>
            ))}
          </div>
        </fieldset>
        <div className="notice">
          {next
            ? new Intl.DateTimeFormat(l, {
                timeZone: zoneTime,
                dateStyle: "full",
                timeStyle: "short",
              }).format(next.time)
            : t(
                l,
                "Tiada waktu tersedia. Muat jadual zon terlebih dahulu.",
                "No next time available. Download the zone schedule first.",
              )}
        </div>
        <p className="help">
          {t(
            l,
            "Hari rujukan ialah hari waktu solat, walaupun peringatan jatuh pada hari berikutnya.",
            "The reference day is the prayer’s weekday, even when the reminder falls on the following day.",
          )}
        </p>
        <button type="submit" disabled={!weekdays.length}>
          {t(l, "Simpan")}
        </button>
      </form>
    </Modal>
  );
}
function Explore() {
  const l = useLanguage(),
    data = useAsync(catalog, []),
    [filter, setFilter] = useState("all"),
    [selected, setSelected] = useState<CatalogEntry | null>(null);
  return (
    <Page
      title={t(l, "Terokai")}
      subtitle={t(
        l,
        "Belajar daripada sumber yang jelas.",
        "Learn from clearly attributed sources.",
      )}
      wide
    >
      <Banner
        image="explore_v3"
        title={t(l, "Ruang untuk ilmu", "Make room for learning")}
        subtitle="TV JAKIM · Mufti WP · Yaqeen Institute"
      />
      <div className="segmented">
        {[
          ["all", "Semua"],
          ["video", "Video"],
          ["article", "Artikel"],
        ].map(([value, label]) => (
          <button
            key={value}
            aria-pressed={filter === value}
            onClick={() => setFilter(value)}
          >
            {t(l, label)}
          </button>
        ))}
      </div>
      {data.loading ? (
        <Loading />
      ) : data.error ? (
        <ErrorState retry={data.retry} />
      ) : (
        <div className="catalog-grid">
          {data.data
            ?.filter(
              (e) =>
                filter === "all" ||
                (filter === "video" && e.type !== "article") ||
                e.type === filter,
            )
            .map((entry) => (
              <article className="catalog-card" key={entry.id}>
                {entry.thumbnail && (
                  <img
                    className="catalog-thumb"
                    src={entry.thumbnail}
                    alt=""
                    loading="lazy"
                  />
                )}
                <div className="catalog-body">
                  <small>
                    {entry.provider} ·{" "}
                    {entry.language === "en" ? "English" : "Bahasa Melayu"}
                  </small>
                  <h2>{entry.title}</h2>
                  <p>{entry.summary}</p>
                  <footer>
                    <div>
                      {entry.youtubeId ? (
                        <button
                          className="secondary"
                          onClick={() => setSelected(entry)}
                        >
                          <Play size={18} />
                          {t(l, "Tonton")}
                        </button>
                      ) : (
                        <a
                          className="button secondary"
                          href={entry.url}
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          <ExternalLink size={18} />
                          {t(
                            l,
                            entry.type === "article"
                              ? "Buka sumber"
                              : "Saluran rasmi",
                          )}
                        </a>
                      )}
                    </div>
                    <Save id={"catalog:" + entry.id} />
                  </footer>
                </div>
              </article>
            ))}
        </div>
      )}
      {selected && (
        <Modal title={selected.title} onClose={() => setSelected(null)}>
          <p>
            {selected.provider} ·{" "}
            {selected.language === "en" ? "English" : "Bahasa Melayu"}
          </p>
          <YoutubePlayer
            videoId={selected.youtubeId}
            url={selected.url}
            title={selected.title}
          />
        </Modal>
      )}
    </Page>
  );
}
function Saved() {
  const p = usePersonal(),
    l = p.preferences.language,
    content = useAsync(async () => {
      const result: { id: string; title: string; sub: string; url: string }[] =
        [];
      const metadata = await surahs(),
        entries = await catalog();
      for (const id of p.bookmarks) {
        if (id.startsWith("surah:") || id.startsWith("ayah:")) {
          const [, n, v] = id.split(":");
          result.push({
            id,
            title:
              (metadata.find((s) => s.number === +n)?.englishName ??
                "Al-Quran") + (v ? " · " + t(l, "Ayat", "Ayah") + " " + v : ""),
            sub: t(l, "Al-Quran"),
            url: "/quran/" + n + (v ? "?verse=" + v : ""),
          });
        } else if (id.startsWith("catalog:")) {
          const e = entries.find((e) => id === "catalog:" + e.id);
          if (e)
            result.push({ id, title: e.title, sub: e.provider, url: e.url });
        } else {
          const [kind, index] = id.split(":");
          try {
            const reading = (await readings(kind, l))[+index];
            if (reading)
              result.push({
                id,
                title: reading.title,
                sub: reading.reference,
                url: "/collections/" + kind + "/" + index,
              });
          } catch {}
        }
      }
      return result;
    }, [p.bookmarks.join(","), l]);
  return (
    <Page
      title={t(l, "Simpanan")}
      subtitle={t(
        l,
        "Bacaan, video dan artikel pilihan anda.",
        "Your saved readings, videos and articles.",
      )}
    >
      <Link to="/backup" className="button secondary">
        <HardDrive size={18} />
        {t(l, "Sandaran & pemulihan")}
      </Link>
      {content.loading ? (
        <Loading />
      ) : !content.data?.length ? (
        <div className="empty">
          <Bookmark size={40} />
          <p>{t(l, "Belum ada simpanan.")}</p>
        </div>
      ) : (
        content.data.map((item) => (
          <div className="saved-row" key={item.id}>
            {item.url.startsWith("https://") ? (
              <a href={item.url} target="_blank" rel="noopener noreferrer">
                <strong>{item.title}</strong>
                <small>{item.sub}</small>
              </a>
            ) : (
              <Link to={item.url}>
                <strong>{item.title}</strong>
                <small>{item.sub}</small>
              </Link>
            )}
            <Save id={item.id} />
          </div>
        ))
      )}
    </Page>
  );
}
function Downloads() {
  const l = useLanguage(),
    meta = useAsync(surahs, []),
    [revision, setRevision] = useState(0),
    [number, setNumber] = useState(1),
    [error, setError] = useState(""),
    [active, setActive] = useState(downloadRunning());
  useEffect(
    () =>
      watchDownloads(() => {
        setRevision((n) => n + 1);
        setActive(downloadRunning());
      }),
    [],
  );
  const data = useAsync(async () => {
    const keys = await db.keys(),
      stored = keys.filter((k) => String(k).startsWith("quran:"));
    let bytes = 0;
    for (const key of stored)
      bytes += new Blob([JSON.stringify(await db.get(String(key)))]).size;
    return {
      stored,
      bytes,
      queue: (await db.get<number[]>("downloadQueue")) ?? [],
    };
  }, [revision]);
  async function run(numbers: number[]) {
    try {
      await queueAdd(numbers);
      setRevision((n) => n + 1);
      await runDownloads();
    } catch {
      setError(
        t(
          l,
          "Muat turun dijeda. Sambung semula apabila internet pulih.",
          "Downloads paused. Resume when your connection returns.",
        ),
      );
    } finally {
      setRevision((n) => n + 1);
      setActive(false);
    }
  }
  return (
    <Page
      title={t(l, "Muat turun")}
      subtitle={t(
        l,
        "Teks Arab, Melayu dan Inggeris. Audio distrim sahaja.",
        "Arabic, Malay and English text. Audio is streamed only.",
      )}
    >
      <div className="storage-stat">
        <HardDrive />
        <strong>{data.data?.stored.length ?? 0} / 114</strong>
        <span>{((data.data?.bytes ?? 0) / 1048576).toFixed(1)} MB</span>
      </div>
      <p>
        {data.data?.queue.length ?? 0} {t(l, "dalam giliran", "queued")}
      </p>
      {error && (
        <p role="status" className="notice">
          {error}
        </p>
      )}
      <p className="help">
        {t(
          l,
          "Muat turun besar menggunakan data. Biarkan aplikasi terbuka; giliran boleh disambung selepas dibuka semula.",
          "Full downloads use data. Keep the app open; the queue can resume after reopening.",
        )}
      </p>
      <div className="actions">
        <button
          disabled={active}
          onClick={() => void run(Array.from({ length: 114 }, (_, i) => i + 1))}
        >
          <Download size={18} />
          {t(l, "Semua teks Al-Quran")}
        </button>
        <button
          className="secondary"
          onClick={() => {
            pauseDownloads();
            setActive(false);
          }}
        >
          <Pause size={18} />
          {t(l, "Jeda", "Pause")}
        </button>
        <button
          className="secondary"
          disabled={active}
          onClick={() => void run([])}
        >
          <Play size={18} />
          {t(l, "Sambung", "Resume")}
        </button>
      </div>
      <div className="form card">
        <label>
          {t(l, "Pilih surah", "Choose a surah")}
          <select value={number} onChange={(e) => setNumber(+e.target.value)}>
            {meta.data?.map((s) => (
              <option key={s.number} value={s.number}>
                {s.number} · {s.englishName}
              </option>
            ))}
          </select>
        </label>
        <div className="actions">
          <button
            className="secondary"
            disabled={active}
            onClick={() => void run([number])}
          >
            {t(l, "Muat turun teks")}
          </button>
          <button
            className="secondary"
            disabled={active}
            onClick={() => {
              if (
                confirm(
                  t(
                    l,
                    "Padam teks tersimpan? Penanda dikekalkan.",
                    "Delete cached text? Bookmarks are preserved.",
                  ),
                )
              )
                void deleteText(number).then(() => setRevision((n) => n + 1));
            }}
          >
            <Trash2 size={18} />
            {t(l, "Padam")}
          </button>
        </div>
      </div>
    </Page>
  );
}
function Preferences() {
  const p = usePersonal(),
    l = p.preferences.language,
    zones = useAsync(
      () =>
        staticData<{
          zones: { code: string; state: string; districts: string }[];
        }>("prayer_zones.json"),
      [],
    );
  function pref<K extends keyof typeof p.preferences>(
    key: K,
    value: (typeof p.preferences)[K],
  ) {
    void update((s) => ({
      ...s,
      preferences: { ...s.preferences, [key]: value },
    }));
  }
  return (
    <Page
      title={t(l, "Pilihan anda")}
      subtitle={t(l, "Sesuaikan paparan, bacaan dan peringatan.")}
    >
      <div className="form card">
        <label>
          {t(l, "Bahasa")}
          <select
            aria-label={t(l, "Bahasa")}
            value={l}
            onChange={(e) => pref("language", e.target.value as typeof l)}
          >
            <option value="ms">Bahasa Melayu</option>
            <option value="en">English</option>
          </select>
        </label>
        <label>
          {t(l, "Tema")}
          <select
            value={p.preferences.theme}
            onChange={(e) =>
              pref("theme", e.target.value as typeof p.preferences.theme)
            }
          >
            <option value="system">{t(l, "Ikut sistem")}</option>
            <option value="light">{t(l, "Cerah")}</option>
            <option value="dark">{t(l, "Gelap")}</option>
          </select>
        </label>
        <label>
          {t(l, "Zon solat")}
          <select
            value={p.preferences.zone_code}
            onChange={(e) => {
              const z = zones.data?.zones.find(
                (z) => z.code === e.target.value,
              );
              if (z)
                void update((s) => ({
                  ...s,
                  preferences: {
                    ...s.preferences,
                    zone_code: z.code,
                    zone_state: z.state,
                    zone_name: z.districts,
                  },
                }));
            }}
          >
            {zones.data?.zones.map((z) => (
              <option key={z.code} value={z.code}>
                {z.state} · {z.code} · {z.districts}
              </option>
            ))}
          </select>
        </label>
        <label className="check-line">
          <input
            type="checkbox"
            checked={p.preferences.twelve_hour}
            onChange={(e) => pref("twelve_hour", e.target.checked)}
          />
          {t(l, "Format masa 12 jam")}
        </label>
        <label className="check-line">
          <input
            type="checkbox"
            checked={p.preferences.show_translation}
            onChange={(e) => pref("show_translation", e.target.checked)}
          />
          {t(l, "Tunjukkan terjemahan", "Show translation")}
        </label>
        <label>
          {t(l, "Terjemahan Al-Quran", "Quran translation")}
          <select
            value={p.preferences.quran_language ?? l}
            onChange={(e) => pref("quran_language", e.target.value as typeof l)}
          >
            <option value="ms">Melayu · Basmeih</option>
            <option value="en">English · Sahih International</option>
          </select>
        </label>
        <label>
          {t(l, "Saiz tulisan Arab")} · {p.preferences.arabic_size}
          <input
            type="range"
            min={24}
            max={44}
            value={p.preferences.arabic_size}
            onChange={(e) => pref("arabic_size", +e.target.value)}
          />
        </label>
      </div>
      <Link to="/backup" className="list-link">
        <HardDrive />
        {t(l, "Sandaran & pemulihan")}
      </Link>
      <Link to="/guide" className="list-link">
        <Info />
        {t(l, "Panduan ringkas")}
      </Link>
      <Link to="/downloads" className="list-link">
        <Download />
        {t(l, "Muat turun")}
      </Link>
      <h2>{t(l, "Sumber & penghargaan")}</h2>
      <p className="help">
        JAKIM e-Solat · AlQuran Cloud / Islamic Network · Sahih al-Bukhari /
        Hadith API.{" "}
        {t(
          l,
          "Hadis tersedia dalam Bahasa Indonesia dan Inggeris. Tarikh penting ialah rujukan tertakluk kepada pengisytiharan rasmi.",
          "Hadith translations are available in Bahasa Indonesia and English. Important dates are references subject to official announcements.",
        )}
      </p>
      <p className="help">
        WSM Sans (Plus Jakarta Sans derivative) · Amiri · SIL OFL.
      </p>
      <div className="actions">
        <a href="/licenses/jakarta-ofl.txt">WSM Sans licence</a>
        <a href="/licenses/amiri-ofl.txt">Amiri licence</a>
        <a href="/licenses/hadith-api.txt">Hadith API licence</a>
      </div>
      <p className="help">
        {t(
          l,
          "Tiada akaun atau analitik. Data peribadi kekal pada pelayar ini. Lokasi digunakan untuk Kiblat; video YouTube dimuatkan apabila dipilih.",
          "No account or analytics. Personal data stays in this browser. Location is used for Qibla; YouTube players load only when selected.",
        )}
      </p>
      <small>Waktu Solat Malaysia · 3.0.0</small>
    </Page>
  );
}
function Backup() {
  const p = usePersonal(),
    l = p.preferences.language,
    [incoming, setIncoming] = useState<BackupBundle | null>(null),
    [error, setError] = useState("");
  async function read(file: File) {
    try {
      if (file.size > 5000000) throw Error("Too large");
      setIncoming(validateBackup(JSON.parse(await file.text())));
      setError("");
    } catch {
      setError(t(l, "Fail sandaran tidak sah.", "Invalid backup file."));
    }
  }
  async function apply(merge: boolean) {
    if (!incoming) return;
    await db.put("backup-before-import", {
      ...p,
      schemaVersion: 1,
      exportedAt: new Date().toISOString(),
    });
    await update((current) => mergeBackup(current, incoming, merge));
    setIncoming(null);
  }
  return (
    <Page
      title={t(l, "Sandaran & pemulihan")}
      subtitle={t(
        l,
        "Pindah data tempatan antara Android dan web.",
        "Move local data between Android and web.",
      )}
    >
      <div className="notice">
        {t(
          l,
          "Fail mengandungi simpanan dan rekod amalan anda. Simpan di tempat yang dipercayai. Kebenaran sistem dan widget tidak disertakan.",
          "The file contains your saved items and practice records. Store it privately. System permissions and widget instances are excluded.",
        )}
      </div>
      <button
        onClick={() =>
          downloadFile(
            "WaktuSolatMalaysia-" + malaysiaDate() + ".json",
            JSON.stringify(
              { ...p, schemaVersion: 1, exportedAt: new Date().toISOString() },
              null,
              2,
            ),
          )
        }
      >
        <HardDrive size={18} />
        {t(l, "Eksport sandaran")}
      </button>
      <label className="file-input">
        {t(l, "Import sandaran")}
        <input
          type="file"
          accept="application/json,.json"
          onChange={(e) => {
            const f = e.target.files?.[0];
            if (f) void read(f);
            e.target.value = "";
          }}
        />
      </label>
      {error && <p role="alert">{error}</p>}
      {incoming && (
        <Modal
          title={t(l, "Import sandaran")}
          onClose={() => setIncoming(null)}
        >
          <p>
            {incoming.bookmarks.length} {t(l, "simpanan", "saved items")} ·{" "}
            {incoming.reminders.length} {t(l, "peringatan", "reminders")}
          </p>
          <p>
            {t(
              l,
              "Peringatan yang diimport kekal mati sehingga diaktifkan. Gabungkan mengekalkan pilihan sedia ada; Gantikan menggunakan data fail.",
              "Imported reminders stay disabled until enabled. Merge keeps current preferences; Replace uses the file’s data.",
            )}
          </p>
          <div className="actions">
            <button className="secondary" onClick={() => void apply(true)}>
              {t(l, "Gabungkan")}
            </button>
            <button onClick={() => void apply(false)}>
              {t(l, "Gantikan")}
            </button>
          </div>
        </Modal>
      )}
    </Page>
  );
}
function Guide() {
  const l = useLanguage();
  return (
    <Page
      title={t(l, "Panduan ringkas")}
      subtitle={t(l, "Ruang untuk yang bermakna.", "Time for what matters.")}
    >
      <Banner
        image="onboarding_v3"
        title="Waktu Solat Malaysia"
        subtitle={t(
          l,
          "Waktu, bacaan dan rutin harian.",
          "Prayer times, reading and daily routines.",
        )}
      />
      <div className="feature-links">
        {[
          { to: "/settings", title: "Pilih zon solat", Icon: Compass },
          { to: "/schedule", title: "Jadual solat", Icon: Clock },
          { to: "/downloads", title: "Muat turun teks", Icon: Download },
          { to: "/backup", title: "Sandaran & pemulihan", Icon: HardDrive },
        ].map(({ to, title, Icon }) => (
          <Link key={to} to={to}>
            <Icon />
            <strong>{t(l, title)}</strong>
            <ArrowRight size={18} />
          </Link>
        ))}
      </div>
      <h2>{t(l, "Pasang pada Windows", "Install on Windows")}</h2>
      <p>
        {t(
          l,
          "Buka dengan Edge atau Chrome dan pilih Pasang aplikasi pada bar alamat atau menu pelayar. Aplikasi yang dipasang boleh dibuka dari menu Start.",
          "Open in Edge or Chrome and select Install app in the address bar or browser menu. The installed app can be opened from Start.",
        )}
      </p>
      <h2>{t(l, "Widget Android", "Android widgets")}</h2>
      <p>
        {t(
          l,
          "Dalam aplikasi Android, buka Ibadah → Widget. Pilih Solat seterusnya 2×2, Harian 4×2 atau Penuh 4×4. Widget menyokong zon dan tema tersendiri.",
          "In the Android app, open Worship → Widgets. Choose Next prayer 2×2, Daily 4×2 or Full 4×4. Widgets support independent zones and themes.",
        )}
      </p>
      <p className="help">
        {t(
          l,
          "Web tidak memainkan azan apabila ditutup. Gunakan aplikasi Android untuk peringatan berjadual.",
          "Web does not play adhan when closed. Use the Android app for scheduled reminders.",
        )}
      </p>
    </Page>
  );
}
