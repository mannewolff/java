import { useEffect, useState } from 'react';

/**
 * Liefert die aktuelle Viewport-Breite und aktualisiert bei Resize. Fallback für SSR/jsdom
 * ohne `window` ist 0 — Aufrufer behandeln das als "noch zu klein".
 */
export default function useViewportWidth(): number {
  const [width, setWidth] = useState<number>(
    typeof window === 'undefined' ? 0 : window.innerWidth,
  );

  useEffect(() => {
    if (typeof window === 'undefined') return undefined;
    const handler = (): void => setWidth(window.innerWidth);
    window.addEventListener('resize', handler);
    return () => window.removeEventListener('resize', handler);
  }, []);

  return width;
}
