import { useEffect, useState } from 'react';

const QUERY = '(prefers-reduced-motion: reduce)';

/** True when the user asked the OS to minimise motion; charts then render without animation. */
export function usePrefersReducedMotion(): boolean {
  const supported = typeof window !== 'undefined' && typeof window.matchMedia === 'function';
  const [reduced, setReduced] = useState(() => supported && window.matchMedia(QUERY).matches);
  useEffect(() => {
    if (!supported) return;
    const media = window.matchMedia(QUERY);
    const onChange = () => setReduced(media.matches);
    media.addEventListener('change', onChange);
    return () => media.removeEventListener('change', onChange);
  }, [supported]);
  return reduced;
}
