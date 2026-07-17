'use client';

import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { accountApi, ordersApi } from '@/lib/api';
import { Spinner } from '@/components/ui/Spinner';
import { Alert } from '@/components/ui/Alert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatCurrency, formatQty, formatDateTime } from '@/lib/utils';
import { Wallet, Briefcase, History, Search } from 'lucide-react';
import Link from 'next/link';

export default function PortfolioPage() {
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [symbolFilter, setSymbolFilter] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const size = 10;

  // Query Statement
  const { data: statement, isLoading: statementLoading, isError: statementError } = useQuery({
    queryKey: ['statement'],
    queryFn: accountApi.statement,
  });

  // Query Positions
  const { data: positions, isLoading: positionsLoading } = useQuery({
    queryKey: ['positions'],
    queryFn: accountApi.positions,
  });

  // Query Order History (paginated)
  const { data: ordersData, isLoading: ordersLoading } = useQuery({
    queryKey: ['portfolio-orders', statusFilter, symbolFilter, page],
    queryFn: () => ordersApi.myOrders(page, size, statusFilter || undefined, symbolFilter || undefined),
  });

  const orders = ordersData?.content ?? [];
  const totalPages = ordersData?.totalPages ?? 0;

  const isLloading = statementLoading || positionsLoading;

  if (isLloading) {
    return (
      <div className="flex justify-center items-center py-20 min-h-screen bg-surface-primary">
        <Spinner size="lg" />
      </div>
    );
  }

  if (statementError || !statement) {
    return (
      <div className="p-6 min-h-screen bg-surface-primary">
        <Alert type="error" title="Error Loading Portfolio">
          Could not retrieve account details. Please try again.
        </Alert>
      </div>
    );
  }

  return (
    <div className="animate-fade-in min-h-screen bg-surface-primary p-6 space-y-6">
      {/* Page Title */}
      <div className="flex items-center gap-3">
        <Briefcase className="w-8 h-8 text-brand-400" />
        <h1 className="text-3xl font-bold text-slate-100 font-display">Portfolio</h1>
      </div>

      {/* Top Cards (Cash, Buying Power, Position Value, Total Portfolio Value) */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="card p-6 border border-surface-border/60 bg-surface-secondary">
          <div className="flex justify-between items-center text-slate-400 text-sm font-semibold mb-2">
            <span>Cash Balance</span>
            <Wallet className="w-4 h-4 text-brand-400" />
          </div>
          <p className="text-2xl font-bold text-slate-100 font-mono">
            {formatCurrency(statement.balance, 2)}
          </p>
        </div>

        <div className="card p-6 border border-surface-border/60 bg-surface-secondary">
          <div className="flex justify-between items-center text-slate-400 text-sm font-semibold mb-2">
            <span>Buying Power</span>
            <Wallet className="w-4 h-4 text-emerald-400" />
          </div>
          <p className="text-2xl font-bold text-emerald-400 font-mono">
            {formatCurrency(statement.buyingPower, 2)}
          </p>
        </div>

        <div className="card p-6 border border-surface-border/60 bg-surface-secondary">
          <div className="flex justify-between items-center text-slate-400 text-sm font-semibold mb-2">
            <span>Position Value</span>
            <Briefcase className="w-4 h-4 text-brand-400" />
          </div>
          <p className="text-2xl font-bold text-slate-100 font-mono">
            {formatCurrency(statement.totalPositionValue, 2)}
          </p>
        </div>

        <div className="card p-6 border border-surface-border/60 bg-surface-secondary ring-1 ring-brand-500/25">
          <div className="flex justify-between items-center text-slate-400 text-sm font-semibold mb-2">
            <span>Total Value</span>
            <Briefcase className="w-4 h-4 text-brand-300 animate-pulse" />
          </div>
          <p className="text-2xl font-bold text-brand-300 font-mono">
            {formatCurrency(statement.totalPortfolioValue, 2)}
          </p>
        </div>
      </div>

      {/* Positions Table */}
      <div className="card p-6 border border-surface-border/60 bg-surface-secondary">
        <h2 className="text-lg font-bold text-slate-100 mb-4 font-display flex items-center gap-2">
          <Briefcase className="w-4 h-4 text-slate-400" />
          My Holdings
        </h2>
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-surface-border text-xs text-slate-500 uppercase tracking-wider font-semibold">
                <th className="pb-3">Symbol</th>
                <th className="pb-3 text-right">Quantity</th>
                <th className="pb-3 text-right">Reserved</th>
                <th className="pb-3 text-right">Avg Price</th>
                <th className="pb-3 text-right">Current Value</th>
                <th className="pb-3 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-surface-border/40 text-sm">
              {(!positions || positions.length === 0) ? (
                <tr>
                  <td colSpan={6} className="text-center text-slate-500 py-8">No positions held</td>
                </tr>
              ) : (
                positions.map((pos) => {
                  const qty = parseFloat(pos.quantity);
                  const reserved = parseFloat(pos.reservedQuantity);
                  if (qty === 0 && reserved === 0) return null; // Hide empty positions
                  
                  const avgPrice = parseFloat(pos.averagePrice);
                  const value = qty * avgPrice;

                  return (
                    <tr key={pos.id} className="hover:bg-surface-tertiary/20 transition-colors">
                      <td className="py-4 font-bold text-slate-200">{pos.symbol}</td>
                      <td className="py-4 text-right font-mono text-slate-300">{formatQty(pos.quantity)}</td>
                      <td className="py-4 text-right font-mono text-slate-500">{formatQty(pos.reservedQuantity)}</td>
                      <td className="py-4 text-right font-mono text-slate-300">{formatCurrency(pos.averagePrice, 2)}</td>
                      <td className="py-4 text-right font-mono text-slate-200 font-semibold">{formatCurrency(value, 2)}</td>
                      <td className="py-4 text-right">
                        <Link
                          href={`/trade?symbol=${pos.symbol}`}
                          className="text-brand-400 hover:text-brand-300 font-semibold transition-colors"
                        >
                          Trade
                        </Link>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Order History Table with Filter */}
      <div className="card p-6 border border-surface-border/60 bg-surface-secondary space-y-4">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <h2 className="text-lg font-bold text-slate-100 font-display flex items-center gap-2">
            <History className="w-4 h-4 text-slate-400" />
            Order History
          </h2>

          {/* Filters */}
          <div className="flex flex-wrap items-center gap-3">
            <div className="relative">
              <Search className="absolute left-3 top-2.5 w-4 h-4 text-slate-500" />
              <input
                type="text"
                placeholder="Search Symbol..."
                value={symbolFilter}
                onChange={(e) => {
                  setSymbolFilter(e.target.value.toUpperCase());
                  setPage(0);
                }}
                className="input pl-9 py-2 text-sm w-40"
              />
            </div>

            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
              className="select py-2 text-sm bg-surface-tertiary text-slate-300 border-surface-border rounded cursor-pointer"
            >
              <option value="">All Statuses</option>
              <option value="ACCEPTED">Active</option>
              <option value="FILLED">Filled</option>
              <option value="CANCELLED">Cancelled</option>
              <option value="REJECTED">Rejected</option>
            </select>
          </div>
        </div>

        <div className="overflow-x-auto">
          {ordersLoading ? (
            <div className="flex justify-center py-6">
              <Spinner size="sm" />
            </div>
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-surface-border text-xs text-slate-500 uppercase tracking-wider font-semibold">
                  <th className="pb-3">Client Order ID</th>
                  <th className="pb-3 text-center">Side</th>
                  <th className="pb-3 text-right">Price</th>
                  <th className="pb-3 text-right">Filled / Original</th>
                  <th className="pb-3 text-center">Status</th>
                  <th className="pb-3 text-right">Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-surface-border/40 text-sm">
                {orders.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="text-center text-slate-500 py-6">No matching orders found</td>
                  </tr>
                ) : (
                  orders.map((order) => (
                    <tr key={order.id} className="hover:bg-surface-tertiary/20 transition-colors">
                      <td className="py-3 font-mono text-slate-300 text-xs">{order.clientOrderId}</td>
                      <td className="py-3 text-center">
                        <span className={`text-xs font-bold px-2 py-0.5 rounded ${
                          order.side === 'BUY' ? 'bg-emerald-500/10 text-emerald-400' : 'bg-red-500/10 text-red-400'
                        }`}>
                          {order.side}
                        </span>
                      </td>
                      <td className="py-3 text-right font-mono text-slate-200">{formatCurrency(order.limitPrice, 2)}</td>
                      <td className="py-3 text-right font-mono text-slate-300">
                        {formatQty(order.filledQuantity)} / {formatQty(order.originalQuantity)}
                      </td>
                      <td className="py-3 text-center">
                        <StatusBadge status={order.status} />
                      </td>
                      <td className="py-3 text-right text-xs text-slate-400 font-mono">
                        {formatDateTime(order.createdAt)}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          )}
        </div>

        {/* Pagination controls */}
        {totalPages > 1 && (
          <div className="flex justify-between items-center pt-4 border-t border-surface-border/60">
            <span className="text-xs text-slate-500">
              Page {page + 1} of {totalPages}
            </span>
            <div className="flex gap-2">
              <button
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="px-3 py-1.5 bg-surface-tertiary border border-surface-border text-slate-300 rounded font-semibold text-xs disabled:opacity-50 hover:bg-surface-tertiary/75"
              >
                Previous
              </button>
              <button
                disabled={page + 1 >= totalPages}
                onClick={() => setPage((p) => p + 1)}
                className="px-3 py-1.5 bg-surface-tertiary border border-surface-border text-slate-300 rounded font-semibold text-xs disabled:opacity-50 hover:bg-surface-tertiary/75"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
