'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { adminApi, instrumentsApi } from '@/lib/api';
import { useAuth } from '@/context/AuthContext';
import { Spinner } from '@/components/ui/Spinner';
import { Alert } from '@/components/ui/Alert';
import { formatCurrency, formatQty } from '@/lib/utils';
import { Shield, Plus, ToggleLeft, ToggleRight, BarChart3, Database, Eye } from 'lucide-react';
import Link from 'next/link';

interface InstrumentFormInputs {
  symbol: string;
  name: string;
  tickSize: string;
  lotSize: string;
}

export default function AdminDashboardPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [selectedSymbol, setSelectedSymbol] = useState<string | null>(null);
  const [isAdding, setIsAdding] = useState(false);

  // Stats query
  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['admin-stats'],
    queryFn: adminApi.getStats,
    enabled: user?.role === 'ADMIN',
  });

  // Instruments query
  const { data: instruments, isLoading: instrumentsLoading } = useQuery({
    queryKey: ['admin-instruments'],
    queryFn: instrumentsApi.list,
    enabled: user?.role === 'ADMIN',
  });

  // Selected order book snapshot query
  const { data: orderBook, isLoading: bookLoading } = useQuery({
    queryKey: ['admin-orderbook', selectedSymbol],
    queryFn: () => (selectedSymbol ? adminApi.getOrderBook(selectedSymbol) : null),
    enabled: !!selectedSymbol && user?.role === 'ADMIN',
    refetchInterval: 5000, // Poll every 5 seconds for real-time monitoring
  });

  // Create instrument form
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<InstrumentFormInputs>({
    defaultValues: {
      symbol: '',
      name: '',
      tickSize: '0.01',
      lotSize: '1',
    },
  });

  // Create mutation
  const createMutation = useMutation({
    mutationFn: (data: InstrumentFormInputs) => adminApi.createInstrument(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-instruments'] });
      reset();
      setIsAdding(false);
    },
  });

  // Status toggle mutation
  const toggleMutation = useMutation({
    mutationFn: ({ symbol, status }: { symbol: string; status: 'ACTIVE' | 'INACTIVE' }) =>
      adminApi.updateInstrumentStatus(symbol, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-instruments'] });
    },
  });

  const onSubmit = (data: InstrumentFormInputs) => {
    createMutation.mutate(data);
  };

  const handleToggleStatus = (symbol: string, currentStatus: 'ACTIVE' | 'INACTIVE') => {
    const nextStatus = currentStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    toggleMutation.mutate({ symbol, status: nextStatus });
  };

  // Guard: Role check
  if (!user) {
    return (
      <div className="flex justify-center items-center py-20 min-h-screen bg-surface-primary">
        <Spinner size="lg" />
      </div>
    );
  }

  if (user.role !== 'ADMIN') {
    return (
      <div className="p-6 min-h-screen bg-surface-primary flex items-center justify-center">
        <div className="max-w-md w-full">
          <Alert type="error" title="Access Denied">
            You do not have administrative privileges to access this area.
            <div className="mt-4">
              <Link href="/dashboard" className="text-brand-400 hover:text-brand-300 font-bold">
                Return to Trader Dashboard &rarr;
              </Link>
            </div>
          </Alert>
        </div>
      </div>
    );
  }

  return (
    <div className="animate-fade-in min-h-screen bg-surface-primary p-6 space-y-6 pb-12">
      {/* Title */}
      <div className="flex items-center gap-3">
        <Shield className="w-8 h-8 text-brand-400" />
        <h1 className="text-3xl font-bold text-slate-100 font-display">Admin Dashboard</h1>
      </div>

      {/* Top Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="card p-6 border border-surface-border/60 bg-surface-secondary">
          <div className="flex justify-between items-center text-slate-400 text-sm font-semibold mb-2">
            <span>Total Orders placed</span>
            <Database className="w-4 h-4 text-brand-400" />
          </div>
          <p className="text-2xl font-bold text-slate-100 font-mono">
            {statsLoading ? '...' : stats?.totalOrders}
          </p>
        </div>

        <div className="card p-6 border border-surface-border/60 bg-surface-secondary">
          <div className="flex justify-between items-center text-slate-400 text-sm font-semibold mb-2">
            <span>Aggregated Match Volume</span>
            <BarChart3 className="w-4 h-4 text-emerald-400" />
          </div>
          <p className="text-2xl font-bold text-emerald-400 font-mono">
            {statsLoading ? '...' : formatQty(stats?.totalVolume ?? 0)}
          </p>
        </div>

        <div className="card p-6 border border-surface-border/60 bg-surface-secondary">
          <div className="flex justify-between items-center text-slate-400 text-sm font-semibold mb-2">
            <span>Active Users count</span>
            <Shield className="w-4 h-4 text-brand-300" />
          </div>
          <p className="text-2xl font-bold text-slate-100 font-mono">
            {statsLoading ? '...' : stats?.activeUsers}
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Instruments management */}
        <div className="lg:col-span-2 space-y-6">
          <div className="card p-6 border border-surface-border/60 bg-surface-secondary">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-lg font-bold text-slate-100 font-display">Manage Instruments</h2>
              <button
                onClick={() => setIsAdding(!isAdding)}
                className="btn btn-primary btn-sm flex items-center gap-1 bg-brand-500 hover:bg-brand-600 font-semibold px-3 py-1.5 rounded text-xs text-white"
              >
                <Plus className="w-3.5 h-3.5" />
                Add New
              </button>
            </div>

            {isAdding && (
              <div className="p-4 bg-surface-tertiary rounded-lg border border-surface-border/60 mb-4 animate-slide-in">
                <h3 className="text-sm font-bold text-slate-200 mb-3">Create Instrument</h3>
                <form onSubmit={handleSubmit(onSubmit)} className="grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
                  <div>
                    <label className="block text-xs font-semibold text-slate-400 mb-1.5">Symbol</label>
                    <input
                      type="text"
                      {...register('symbol', { required: true })}
                      placeholder="e.g. CAL"
                      className="input py-1.5 text-xs w-full"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-400 mb-1.5">Name</label>
                    <input
                      type="text"
                      {...register('name', { required: true })}
                      placeholder="e.g. Caltex PLC"
                      className="input py-1.5 text-xs w-full"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-400 mb-1.5">Tick Size</label>
                    <input
                      type="text"
                      {...register('tickSize', { required: true })}
                      placeholder="e.g. 0.10"
                      className="input py-1.5 text-xs w-full"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-400 mb-1.5">Lot Size</label>
                    <input
                      type="text"
                      {...register('lotSize', { required: true })}
                      placeholder="e.g. 10"
                      className="input py-1.5 text-xs w-full"
                    />
                  </div>
                  <div className="md:col-span-4 flex justify-end gap-2 pt-2">
                    <button
                      type="button"
                      onClick={() => setIsAdding(false)}
                      className="px-3 py-1.5 bg-surface-tertiary text-slate-400 hover:text-slate-200 rounded font-semibold text-xs border border-surface-border"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      disabled={isSubmitting}
                      className="px-3 py-1.5 bg-brand-500 hover:bg-brand-600 text-white rounded font-semibold text-xs disabled:opacity-50"
                    >
                      Save Instrument
                    </button>
                  </div>
                </form>
              </div>
            )}

            <div className="overflow-x-auto">
              {instrumentsLoading ? (
                <div className="flex justify-center py-6">
                  <Spinner size="sm" />
                </div>
              ) : (
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-surface-border text-xs text-slate-500 uppercase tracking-wider font-semibold">
                      <th className="pb-3">Symbol</th>
                      <th className="pb-3">Name</th>
                      <th className="pb-3 text-right">Tick</th>
                      <th className="pb-3 text-right">Lot</th>
                      <th className="pb-3 text-center">Status</th>
                      <th className="pb-3 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-surface-border/40 text-sm">
                    {instruments?.map((ins) => (
                      <tr key={ins.id} className="hover:bg-surface-tertiary/20 transition-colors">
                        <td className="py-3 font-bold text-slate-200">{ins.symbol}</td>
                        <td className="py-3 text-slate-300">{ins.name}</td>
                        <td className="py-3 text-right font-mono">{ins.tickSize}</td>
                        <td className="py-3 text-right font-mono">{ins.lotSize}</td>
                        <td className="py-3 text-center">
                          <button
                            onClick={() => handleToggleStatus(ins.symbol, ins.status)}
                            className={`p-1 rounded hover:bg-surface-tertiary transition-colors`}
                            title={`Toggle status to ${ins.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'}`}
                          >
                            {ins.status === 'ACTIVE' ? (
                              <ToggleRight className="w-6 h-6 text-emerald-400" />
                            ) : (
                              <ToggleLeft className="w-6 h-6 text-slate-500" />
                            )}
                          </button>
                        </td>
                        <td className="py-3 text-right">
                          <button
                            onClick={() => setSelectedSymbol(ins.symbol)}
                            className={`p-1.5 rounded transition-all text-xs font-semibold flex items-center gap-1 ml-auto ${
                              selectedSymbol === ins.symbol
                                ? 'bg-brand-500/20 text-brand-300 border border-brand-500/30'
                                : 'bg-surface-tertiary text-slate-400 hover:text-slate-200 border border-surface-border'
                            }`}
                          >
                            <Eye className="w-3.5 h-3.5" />
                            Monitor
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </div>

        {/* Order Book Monitor */}
        <div className="lg:col-span-1">
          <div className="card p-6 border border-surface-border/60 bg-surface-secondary h-[600px] flex flex-col">
            <h2 className="text-lg font-bold text-slate-100 mb-4 font-display flex items-center gap-2">
              <Eye className="w-4 h-4 text-slate-400" />
              Order Book Monitor
            </h2>

            {!selectedSymbol ? (
              <div className="flex-1 flex items-center justify-center text-center text-slate-500 text-sm">
                Select an instrument from the list to monitor its real-time depth.
              </div>
            ) : (
              <div className="flex-1 flex flex-col overflow-hidden">
                <div className="flex justify-between items-center mb-3">
                  <span className="text-sm font-bold text-brand-300 font-mono">{selectedSymbol}</span>
                  <span className="text-xs text-slate-500">Live polling (5s)</span>
                </div>

                {bookLoading && !orderBook ? (
                  <div className="flex-1 flex items-center justify-center">
                    <Spinner size="sm" />
                  </div>
                ) : (
                  <div className="flex-1 flex flex-col overflow-y-auto no-scrollbar font-mono text-xs divide-y divide-surface-border/40">
                    {/* Asks */}
                    <div className="space-y-1 py-3">
                      <p className="text-[10px] text-red-400 font-semibold uppercase tracking-wider mb-2">Asks (Sell Side)</p>
                      {(!orderBook?.asks || orderBook.asks.length === 0) ? (
                        <p className="text-slate-600 italic">No resting asks</p>
                      ) : (
                        orderBook.asks.slice(0, 10).map((l, i) => (
                          <div key={`ask-${i}`} className="flex justify-between">
                            <span className="text-red-400">{formatCurrency(l.price, 2)}</span>
                            <span className="text-slate-300">{formatQty(l.quantity)}</span>
                            <span className="text-slate-500">{l.orderCount} ord</span>
                          </div>
                        ))
                      )}
                    </div>

                    {/* Bids */}
                    <div className="space-y-1 py-3">
                      <p className="text-[10px] text-emerald-400 font-semibold uppercase tracking-wider mb-2">Bids (Buy Side)</p>
                      {(!orderBook?.bids || orderBook.bids.length === 0) ? (
                        <p className="text-slate-600 italic">No resting bids</p>
                      ) : (
                        orderBook.bids.slice(0, 10).map((l, i) => (
                          <div key={`bid-${i}`} className="flex justify-between">
                            <span className="text-emerald-400">{formatCurrency(l.price, 2)}</span>
                            <span className="text-slate-300">{formatQty(l.quantity)}</span>
                            <span className="text-slate-500">{l.orderCount} ord</span>
                          </div>
                        ))
                      )}
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
