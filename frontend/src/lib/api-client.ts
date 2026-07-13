import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import Cookies from 'js-cookie';
import { ApiError } from './types';

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export const apiClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

// ─── Request interceptor — attach JWT ─────────────────────────────────────────
apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = Cookies.get('tf_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ─── Response interceptor — handle 401 ───────────────────────────────────────
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiError>) => {
    if (error.response?.status === 401) {
      Cookies.remove('tf_token');
      Cookies.remove('tf_user');
      if (typeof window !== 'undefined') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// ─── Helper to extract API error message ─────────────────────────────────────
export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const apiErr = error.response?.data as ApiError | undefined;
    if (apiErr?.fieldErrors?.length) {
      return apiErr.fieldErrors.map((fe) => `${fe.field}: ${fe.message}`).join('; ');
    }
    return apiErr?.message || error.message || 'An error occurred.';
  }
  if (error instanceof Error) return error.message;
  return 'An unexpected error occurred.';
}
