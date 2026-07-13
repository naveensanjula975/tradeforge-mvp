'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  BarChart3, BookOpen, History, LayoutDashboard,
  LogOut, TrendingUp, User, Wifi, WifiOff, Zap,
} from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { cn } from '@/lib/utils';
import { useState, useEffect } from 'react';
import { wsClient } from '@/lib/websocket';

const navLinks = [
  { href: '/dashboard',  label: 'Market',    icon: LayoutDashboard },
  { href: '/trade',      label: 'Trade',     icon: TrendingUp },
  { href: '/orders',     label: 'My Orders', icon: BookOpen },
  { href: '/trades',     label: 'Trades',    icon: History },
  { href: '/portfolio',  label: 'Portfolio', icon: BarChart3 },
];

export default function Navbar() {
  const pathname             = usePathname();
  const { user, logout, isLoggedIn } = useAuth();
  const [wsConnected, setWsConnected] = useState(false);

  useEffect(() => {
    if (!isLoggedIn) return;
    const checkInterval = setInterval(() => {
      setWsConnected(wsClient.isConnected());
    }, 2000);
    return () => clearInterval(checkInterval);
  }, [isLoggedIn]);

  if (!isLoggedIn) return null;

  return (
    <nav className="sticky top-0 z-50 border-b border-surface-border bg-surface-secondary/90 backdrop-blur-xl">
      <div className="max-w-screen-2xl mx-auto px-4 flex items-center justify-between h-14">

        {/* ── Brand ──────────────────────────────────────────────────── */}
        <Link href="/dashboard" className="flex items-center gap-2 group">
          <div className="w-7 h-7 rounded-lg bg-gradient-to-br from-brand-500 to-purple-600 flex items-center justify-center shadow-glow-brand group-hover:shadow-lg transition-shadow">
            <Zap className="w-4 h-4 text-white" />
          </div>
          <span className="font-bold text-lg text-gradient tracking-tight">TradeForge</span>
        </Link>

        {/* ── Nav Links ───────────────────────────────────────────────── */}
        <div className="hidden md:flex items-center gap-1">
          {navLinks.map(({ href, label, icon: Icon }) => {
            const active = pathname.startsWith(href);
            return (
              <Link
                key={href}
                href={href}
                className={cn(
                  'flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium transition-all duration-200',
                  active
                    ? 'bg-brand-600/20 text-brand-300 border border-brand-700/50'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-surface-tertiary'
                )}
              >
                <Icon className="w-4 h-4" />
                {label}
              </Link>
            );
          })}
        </div>

        {/* ── Right side ──────────────────────────────────────────────── */}
        <div className="flex items-center gap-3">
          {/* WS Connection indicator */}
          <div
            className={cn(
              'flex items-center gap-1.5 text-xs font-medium px-2 py-1 rounded-full border',
              wsConnected
                ? 'text-emerald-400 border-emerald-700/50 bg-emerald-900/20'
                : 'text-slate-500 border-slate-700 bg-surface-tertiary'
            )}
            title={wsConnected ? 'Real-time connected' : 'Connecting…'}
          >
            {wsConnected ? <Wifi className="w-3 h-3" /> : <WifiOff className="w-3 h-3" />}
            <span className="hidden sm:inline">{wsConnected ? 'Live' : 'Offline'}</span>
          </div>

          {/* User info */}
          <div className="flex items-center gap-2 text-sm">
            <div className="w-7 h-7 rounded-full bg-gradient-to-br from-brand-500 to-purple-600 flex items-center justify-center">
              <User className="w-3.5 h-3.5 text-white" />
            </div>
            <div className="hidden sm:block">
              <p className="text-slate-200 text-xs font-medium leading-tight">{user?.email}</p>
              <p className="text-slate-500 text-[10px]">{user?.role}</p>
            </div>
          </div>

          <button
            onClick={logout}
            className="btn-ghost btn-sm"
            title="Logout"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* ── Mobile nav ───────────────────────────────────────────────── */}
      <div className="md:hidden flex gap-1 px-4 pb-2 overflow-x-auto no-scrollbar">
        {navLinks.map(({ href, label, icon: Icon }) => {
          const active = pathname.startsWith(href);
          return (
            <Link
              key={href}
              href={href}
              className={cn(
                'flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap',
                active ? 'bg-brand-600/20 text-brand-300' : 'text-slate-400'
              )}
            >
              <Icon className="w-3.5 h-3.5" />
              {label}
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
