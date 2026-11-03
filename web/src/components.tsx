import { useEffect, useRef, useState, useId } from "react";
import type { ReactNode } from "react";
import { useVirtualizer } from "@tanstack/react-virtual";
import {
  Bookmark,
  BookmarkCheck,
  LoaderCircle,
  Play,
  ExternalLink,
  X,
} from "lucide-react";
import { usePersonal, saveToggle } from "./data";
import { t } from "./i18n";
export function Page({
  title,
  subtitle,
  children,
  wide = false,
}: {
  title: string;
  subtitle?: string;
  children: ReactNode;
  wide?: boolean;
}) {
  return (
    <section className={"page " + (wide ? "wide" : "")}>
      <header className="page-heading">
        <h1>{title}</h1>
        {subtitle && <p>{subtitle}</p>}
      </header>
      {children}
    </section>
  );
}
export function ErrorState({
  message,
  retry,
}: {
  message?: string;
  retry?: () => void;
}) {
  const l = usePersonal().preferences.language;
  return (
    <div role="status" className="notice">
      <p>
        {message ??
          t(
            l,
            "Kandungan belum tersedia. Semak sambungan atau gunakan bacaan yang disimpan.",
            "Content is unavailable. Check your connection or use saved readings.",
          )}
      </p>
      {retry && (
        <button className="secondary" onClick={retry}>
          {t(l, "Cuba semula")}
        </button>
      )}
    </div>
  );
}
export function Loading() {
  const l = usePersonal().preferences.language;
  return (
    <div className="loading" role="status">
      <LoaderCircle className="spin" size={20} />
      {t(l, "Memuatkan…", "Loading…")}
    </div>
  );
}
export function Save({ id }: { id: string }) {
  const state = usePersonal(),
    on = state.bookmarks.includes(id);
  return (
    <button
      className={"icon-button " + (on ? "selected" : "")}
      onClick={() => saveToggle(id)}
      aria-label={t(
        state.preferences.language,
        id.startsWith("surah:")
          ? on
            ? "Surah disimpan"
            : "Simpan surah"
          : on
            ? "Disimpan"
            : "Simpan",
      )}
      aria-pressed={on}
    >
      {on ? <BookmarkCheck size={20} /> : <Bookmark size={20} />}
    </button>
  );
}
export function Banner({
  image,
  title,
  subtitle,
  onClick,
  dark = false,
}: {
  image: string;
  title: string;
  subtitle: string;
  onClick?: () => void;
  dark?: boolean;
}) {
  const inner = (
    <>
      <img src={"/assets/" + image + ".webp"} alt="" loading="lazy" />
      <span className="banner-wash" />
      <span className="banner-copy">
        <strong>{title}</strong>
        <span>{subtitle}</span>
      </span>
    </>
  );
  return onClick ? (
    <button
      className={"art-banner " + (dark ? "dark-art" : "")}
      onClick={onClick}
    >
      {inner}
    </button>
  ) : (
    <div className={"art-banner " + (dark ? "dark-art" : "")}>{inner}</div>
  );
}
export function Modal({
  title,
  children,
  onClose,
}: {
  title: string;
  children: ReactNode;
  onClose: () => void;
}) {
  const ref = useRef<HTMLDialogElement>(null),
    l = usePersonal().preferences.language,
    titleId = useId();
  useEffect(() => {
    const dialog = ref.current;
    dialog?.showModal();
    return () => dialog?.close();
  }, []);
  return (
    <dialog
      aria-labelledby={titleId}
      ref={ref}
      onCancel={onClose}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <header className="dialog-heading">
        <h2 id={titleId}>{title}</h2>
        <button
          className="icon-button"
          aria-label={t(l, "Tutup")}
          onClick={onClose}
        >
          <X />
        </button>
      </header>
      {children}
    </dialog>
  );
}
export function VirtualList<T>({
  items,
  render,
  estimate = 92,
  onVisible,
  initialIndex = 0,
}: {
  items: T[];
  render: (item: T, index: number) => ReactNode;
  estimate?: number;
  onVisible?: (index: number) => void;
  initialIndex?: number;
}) {
  const ref = useRef<HTMLDivElement>(null),
    v = useVirtualizer({
      count: items.length,
      getScrollElement: () => ref.current,
      estimateSize: () => estimate,
      overscan: 4,
      initialOffset: initialIndex * estimate,
    });
  const entries = v.getVirtualItems();
  useEffect(() => {
    if (initialIndex > 0 && initialIndex < items.length)
      v.scrollToIndex(initialIndex, { align: "start" });
  }, [initialIndex, items.length]);
  useEffect(() => {
    if (entries[0]) onVisible?.(entries[0].index);
  }, [entries[0]?.index]);
  return (
    <div ref={ref} className="virtual-scroll" tabIndex={0}>
      <div
        style={{
          height: v.getTotalSize(),
          position: "relative",
          width: "100%",
        }}
      >
        {entries.map((row) => (
          <div
            key={row.key}
            data-index={row.index}
            ref={v.measureElement}
            style={{
              position: "absolute",
              top: 0,
              left: 0,
              width: "100%",
              transform: `translateY(${row.start}px)`,
            }}
          >
            {render(items[row.index], row.index)}
          </div>
        ))}
      </div>
    </div>
  );
}
type Player = { destroy: () => void; pauseVideo: () => void };
declare global {
  interface Window {
    onYouTubeIframeAPIReady?: () => void;
    YT?: {
      Player: new (
        el: HTMLElement,
        options: {
          videoId: string;
          playerVars: Record<string, string | number>;
          events: { onError: (event: { data: number }) => void };
        },
      ) => Player;
    };
  }
}
let youtubeReady: Promise<void> | undefined;
function loadYT() {
  if (window.YT) return Promise.resolve();
  if (!youtubeReady)
    youtubeReady = new Promise((resolve, reject) => {
      window.onYouTubeIframeAPIReady = resolve;
      const script = document.createElement("script");
      script.src = "https://www.youtube.com/iframe_api";
      script.onerror = () => {
        youtubeReady = undefined;
        reject(Error("Player unavailable"));
      };
      document.head.append(script);
    });
  return youtubeReady;
}
export function YoutubePlayer({
  videoId,
  url,
  title,
}: {
  videoId: string;
  url: string;
  title: string;
}) {
  const [started, start] = useState(false),
    [error, setError] = useState(""),
    host = useRef<HTMLDivElement>(null),
    l = usePersonal().preferences.language;
  useEffect(() => {
    if (!started) return;
    let alive = true,
      player: Player | undefined;
    const visibility = () => {
      if (document.hidden) player?.pauseVideo();
    };
    document.addEventListener("visibilitychange", visibility);
    void loadYT()
      .then(() => {
        if (!alive || !host.current || !window.YT) return;
        player = new window.YT.Player(host.current, {
          videoId,
          playerVars: { playsinline: 1, autoplay: 0, origin: location.origin },
          events: {
            onError: (event) => {
              if (alive)
                setError(
                  t(l, "Pemain tidak tersedia", "Player unavailable") +
                    ` (${event.data})`,
                );
            },
          },
        });
      })
      .catch(() =>
        setError(t(l, "Pemain tidak tersedia", "Player unavailable")),
      );
    return () => {
      alive = false;
      document.removeEventListener("visibilitychange", visibility);
      player?.destroy();
    };
  }, [started, videoId, l]);
  return (
    <div className="video-player">
      {!started ? (
        <button
          className="video-poster"
          onClick={() => start(true)}
          aria-label={t(l, "Tonton") + " " + title}
        >
          <img
            src={`https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`}
            alt=""
            loading="lazy"
          />
          <span>
            <Play />
            {t(l, "Tonton")}
          </span>
        </button>
      ) : (
        <div className="youtube-frame">
          <div ref={host} />
        </div>
      )}
      {error && <p role="status">{error}</p>}
      <a
        className="button secondary"
        href={url}
        target="_blank"
        rel="noopener noreferrer"
      >
        <ExternalLink size={18} />
        {t(l, "Buka sumber")}
      </a>
    </div>
  );
}
