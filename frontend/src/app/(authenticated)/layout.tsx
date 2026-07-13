'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Navbar from '@/components/layout/Navbar';
import { useAuth } from '@/context/AuthContext';
import { wsClient } from '@/lib/websocket';
import { PageLoader } from '@/components/ui/Spinner';

export default function AuthenticatedLayout({ children }: { children: React.ReactNode }) {
  const { isLoggedIn, user } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoggedIn) {
      router.push('/login');
    }
  }, [isLoggedIn, router]);

  useEffect(() => {
    if (isLoggedIn && !wsClient.isConnected()) {
      wsClient.connect();
    }
    return () => {
      // WS stays alive across page navigations inside this layout
    };
  }, [isLoggedIn]);

  if (!isLoggedIn || !user) return <PageLoader />;

  return (
    <div className="min-h-screen bg-surface">
      <Navbar />
      <main className="max-w-screen-2xl mx-auto px-4 py-6">
        {children}
      </main>
    </div>
  );
}
