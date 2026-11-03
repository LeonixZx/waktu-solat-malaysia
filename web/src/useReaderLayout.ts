import { useEffect, useRef, useState } from "react";

/** Width is the actual pane container, after navigation and outer page margins. */
export function useReaderLayout() {
  const ref = useRef<HTMLDivElement>(null);
  const [twoPane, setTwoPane] = useState(false);
  useEffect(() => {
    const measure = () => {
      if (!ref.current) return;
      const font =
        parseFloat(getComputedStyle(document.documentElement).fontSize) / 16;
      setTwoPane(
        ref.current.clientWidth >= 640 && innerHeight >= 480 && font <= 1.3,
      );
    };
    const observer = new ResizeObserver(measure);
    if (ref.current) observer.observe(ref.current);
    observer.observe(document.documentElement);
    window.addEventListener("resize", measure);
    measure();
    return () => {
      observer.disconnect();
      window.removeEventListener("resize", measure);
    };
  }, []);
  return { ref, twoPane };
}
