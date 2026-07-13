'use client';

import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { instrumentsApi } from '@/lib/api';
import { Instrument } from '@/lib/types';
import { Spinner } from '@/components/ui/Spinner';
import { EmptyState } from '@/components/ui/EmptyState';
import { BarChart3, TrendingUp, Activity } from 'lucide-react';
import Link from 'next/link';
import { cn } from '@/lib/utils';

function InstrumentCard({ instrument }: { instrument: Instrument }) {
  const isActive = instrument.status === 'ACTIVE';

  return (
    <Link
      href={`/trade?symbol=${instrument.symbol}`}
      className={cn(
        'card p-5 hover:border-brand-700/50 transition-all duration-200 hover:shadow-glow-brand group cursor-pointer',
        !isActive && 'opacity-60 cursor-not-allowed pointer-events-none'
      )}
    >
      <div className="flex items-start justify-between mb-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-xl font-bold font-mono text-slate-100 group-hover:text-brand-300 transition-colors">
              {instrument.symbol}
            </span>
            <span className={cn('badge', isActive ? 'badge-active' : 'badge-inactive')}>
              {instrument.status}
            </span>
          </div>
          <p className="text-slate-500 text-xs mt-0.5">{instrument.name}</p>
        </div>
        <div className="w-10 h-10 rounded-xl bg-brand-600/10 border border-brand-700/30 flex items-center justify-center group-hover:bg-brand-600/20 transition-colors">
          <Activity className="w-5 h-5 text-brand-400" />
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3 mt-3">
        <div className="bg-surface-tertiary rounded-lg p-2.5">
          <p className="text-[10px] text-slate-500 uppercase tracking-wider mb-1">Tick Size</p>
          <p className="text-sm font-mono font-semibold text-slate-200">{instrument.tickSize}</p>
        </div>
        <div className="bg-surface-tertiary rounded-lg p-2.5">
          <p className="text-[10px] text-slate-500 uppercase tracking-wider mb-1">Lot Size</p>
          <p className="text-sm font-mono font-semibold text-slate-200">{instrument.lotSize}</p>
        </div>
      </div>

      {isActive && (
        <div className="mt-4 flex items-center gap-2 text-xs text-brand-400 font-medium">
          <TrendingUp className="w-3.5 h-3.5" />
          Click to trade
        </div>
      )}
    </Link>
  );
}

export default function DashboardPage() {
  const { data: instruments, isLoading, isError } = useQuery({
    queryKey: ['instruments'],
    queryFn:  instrumentsApi.list,
    refetchInterval: 30_000,
  });

  return (
    <div className="animate-fade-in">
      {/* Header */}
      <div className="mb-8">
        <div className="flex items-center gap-3 mb-2">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-brand-500 to-purple-600 flex items-center justify-center shadow-glow-brand">
            <BarChart3 className="w-5 h-5 text-white" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-slate-100">Market Overview</h1>
            <p className="text-slate-500 text-sm">Select an instrument to start trading</p>
          </div>
        </div>
      </div>

      {/* Ticker bar */}
      <div className="bg-surface-secondary border border-surface-border rounded-xl px-4 py-2 mb-8 overflow-hidden">
        <div className="flex items-center gap-6 animate-ticker whitespace-nowrap">
          {instruments?.map((i) => (
            <span key={i.symbol} className="text-sm font-mono flex items-center gap-2">
              <span className="text-slate-400">{i.symbol}</span>
              <span className={i.status === 'ACTIVE' ? 'text-emerald-400' : 'text-slate-500'}>
                {i.status}
              </span>
            </span>
          ))}
        </div>
      </div>

      {/* Content */}
      {isLoading && (
        <div className="flex justify-center py-20">
          <Spinner size="lg" />
        </div>
      )}

      {isError && (
        <div className="card p-8 text-center">
          <p className="text-red-400">Failed to load instruments. Make sure the backend is running.</p>
        </div>
      )}

      {instruments && instruments.length === 0 && (
        <EmptyState
          icon={Activity}
          title="No instruments available"
          description="Contact your administrator to add trading instruments."
        />
      )}

      {instruments && instruments.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {instruments.map((instrument) => (
            <InstrumentCard key={instrument.id} instrument={instrument} />
          ))}
        </div>
      )}
    </div>
  );
}
