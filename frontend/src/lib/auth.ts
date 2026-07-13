'use client';

import Cookies from 'js-cookie';
import { AuthResponse } from './types';

const TOKEN_KEY = 'tf_token';
const USER_KEY  = 'tf_user';

export interface StoredUser {
  userId: string;
  email: string;
  role: 'TRADER' | 'ADMIN';
}

/** Save token and user info to cookies after successful auth. */
export function saveAuth(auth: AuthResponse): void {
  const expiresInDays = auth.expiresInMs / (1000 * 60 * 60 * 24);
  Cookies.set(TOKEN_KEY, auth.accessToken, {
    expires: expiresInDays,
    sameSite: 'strict',
    secure: process.env.NODE_ENV === 'production',
  });
  Cookies.set(
    USER_KEY,
    JSON.stringify({ userId: auth.userId, email: auth.email, role: auth.role }),
    { expires: expiresInDays, sameSite: 'strict', secure: process.env.NODE_ENV === 'production' }
  );
}

/** Clear all auth cookies (logout). */
export function clearAuth(): void {
  Cookies.remove(TOKEN_KEY);
  Cookies.remove(USER_KEY);
}

/** Get the stored JWT token. */
export function getToken(): string | undefined {
  return Cookies.get(TOKEN_KEY);
}

/** Get the currently authenticated user metadata. */
export function getStoredUser(): StoredUser | null {
  const raw = Cookies.get(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as StoredUser;
  } catch {
    return null;
  }
}

/** Returns true if a token cookie is present (does not validate expiry client-side). */
export function isAuthenticated(): boolean {
  return !!Cookies.get(TOKEN_KEY);
}
