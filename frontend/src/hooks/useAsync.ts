import { useCallback, useEffect, useRef, useState, type DependencyList } from 'react';
import { toApiError, type ApiError } from '../utils/errors';

export interface AsyncState<T> {
  data: T | undefined;
  loading: boolean;
  error: ApiError | null;
  reload: () => void;
}

/**
 * Loads data when the dependencies change and exposes loading/error state. Responses that
 * arrive after a newer request started are ignored, so fast month switching never shows stale data.
 */
export function useAsync<T>(load: () => Promise<T>, deps: DependencyList): AsyncState<T> {
  const [data, setData] = useState<T>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<ApiError | null>(null);
  const [version, setVersion] = useState(0);
  const latest = useRef(0);

  // The caller lists what `load` depends on, the same contract as useEffect.
  const stableLoad = useCallback(load, deps);

  useEffect(() => {
    const request = ++latest.current;
    setLoading(true);
    setError(null);
    stableLoad()
      .then((result) => {
        if (request === latest.current) setData(result);
      })
      .catch((e: unknown) => {
        if (request === latest.current) setError(toApiError(e));
      })
      .finally(() => {
        if (request === latest.current) setLoading(false);
      });
  }, [stableLoad, version]);

  const reload = useCallback(() => setVersion((v) => v + 1), []);
  return { data, loading, error, reload };
}
