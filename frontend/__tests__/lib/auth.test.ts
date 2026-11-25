import Cookies from 'js-cookie';
import {
  saveAuth,
  clearAuth,
  getToken,
  getStoredUser,
  isAuthenticated,
} from '@/lib/auth';
import type { AuthResponse } from '@/lib/types';

const auth = {
  accessToken: 'jwt-token',
  userId: 'u-1',
  email: 'trader@example.com',
  role: 'TRADER',
  expiresInMs: 24 * 60 * 60 * 1000,
} as AuthResponse;

beforeEach(() => {
  clearAuth();
});

describe('auth cookies', () => {
  it('is unauthenticated initially', () => {
    expect(isAuthenticated()).toBe(false);
    expect(getToken()).toBeUndefined();
    expect(getStoredUser()).toBeNull();
  });

  it('saveAuth stores token and user', () => {
    saveAuth(auth);
    expect(isAuthenticated()).toBe(true);
    expect(getToken()).toBe('jwt-token');
    expect(getStoredUser()).toEqual({
      userId: 'u-1',
      email: 'trader@example.com',
      role: 'TRADER',
    });
  });

  it('clearAuth removes both cookies', () => {
    saveAuth(auth);
    clearAuth();
    expect(isAuthenticated()).toBe(false);
    expect(getStoredUser()).toBeNull();
  });

  it('getStoredUser returns null for corrupt JSON', () => {
    Cookies.set('tf_user', '{not json');
    expect(getStoredUser()).toBeNull();
  });
});
