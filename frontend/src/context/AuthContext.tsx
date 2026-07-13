'use client';

import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { StoredUser, getStoredUser, clearAuth, isAuthenticated } from '@/lib/auth';
import { wsClient } from '@/lib/websocket';

interface AuthContextValue {
  user: StoredUser | null;
  isLoggedIn: boolean;
  logout: () => void;
  refreshUser: () => void;
}

const AuthContext = createContext<AuthContextValue>({
  user: null,
  isLoggedIn: false,
  logout: () => {},
  refreshUser: () => {},
});

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<StoredUser | null>(null);

  const refreshUser = useCallback(() => {
    if (isAuthenticated()) {
      setUser(getStoredUser());
    } else {
      setUser(null);
    }
  }, []);

  useEffect(() => {
    refreshUser();
  }, [refreshUser]);

  const logout = useCallback(() => {
    clearAuth();
    wsClient.disconnect();
    setUser(null);
    window.location.href = '/login';
  }, []);

  return (
    <AuthContext.Provider value={{ user, isLoggedIn: !!user, logout, refreshUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
