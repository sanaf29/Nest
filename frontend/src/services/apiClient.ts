import axios from 'axios';
import { tokenStorage } from './tokenStorage';

/**
 * The single Axios instance. Requests go to /api on the same origin (the Vite dev server proxies
 * them to Spring Boot). The bearer token is attached automatically. A 401 on any authenticated
 * call means the session is over, so listeners (the auth context) are told to log out.
 */
export const apiClient = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

type UnauthorizedListener = () => void;
const unauthorizedListeners = new Set<UnauthorizedListener>();

export function onUnauthorized(listener: UnauthorizedListener): () => void {
  unauthorizedListeners.add(listener);
  return () => unauthorizedListeners.delete(listener);
}

apiClient.interceptors.request.use((config) => {
  const token = tokenStorage.read()?.accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const isAuthEndpoint = String(error.config?.url ?? '').startsWith('/auth/');
    if (error.response?.status === 401 && !isAuthEndpoint) {
      unauthorizedListeners.forEach((listener) => listener());
    }
    return Promise.reject(error);
  },
);
