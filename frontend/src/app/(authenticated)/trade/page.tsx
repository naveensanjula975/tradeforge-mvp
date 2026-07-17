'use client';

import { useSearchParams } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { ordersApi, instrumentsApi } from '@/lib/api';
import { PlaceOrderRequest, OrderSide, Order } from '@/lib/types';
import { OrderBookPanel } from '@/components/trading/OrderBookPanel';
import { Spinner } from '@/components/ui/Spinner';
import { Alert } from '@/components/ui/Alert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatCurrency, formatQty } from '@/lib/utils';
import { Send, ArrowLeft, Trash2, X } from 'lucide-react';
import Link from 'next/link';
import { wsClient } from '@/lib/websocket';

interface OrderFormInputs {
  clientOrderId: string;
  side: OrderSide;
  limitPrice: string;
  quantity: string;
}

export default function TradePage() {
  const searchParams = useSearchParams();
  const symbol = searchParams.get('symbol') || '';
  const queryClient = useQueryClient();
  const [submitted, setSubmitted] = useState(false);
  const [successOrder, setSuccessOrder] = useState<Order | null>(null);
  const [cancelOrderId, setCancelOrderId] = useState<string | null>(null);

  const { data: instrument, isLoading: instrumentLoading, isError: instrumentError } = useQuery({
    queryKey: ['instrument', symbol],
    queryFn: () => (symbol ? instrumentsApi.getBySymbol(symbol) : null),
    enabled: !!symbol,
  });

  const { data: ordersData } = useQuery({
    queryKey: ['orders', symbol],
    queryFn: () => ordersApi.myOrders(0, 50, undefined, symbol),
    enabled: !!symbol,
  });

  useEffect(() => {
    const sub = wsClient.subscribeUserOrders((event) => {
      queryClient.invalidateQueries({ queryKey: ['orders', symbol] });
    });

    return () => {
      sub?.unsubscribe();
    };
  }, [symbol, queryClient]);

  const { register, handleSubmit, watch, reset, setValue, formState: { errors, isSubmitting } } = useForm<OrderFormInputs>({
    defaultValues: {
      side: 'BUY',
      clientOrderId: `${symbol}-${Date.now()}`,
      limitPrice: '100.00',
      quantity: '10',
    },
  });

  const mutation = useMutation({
    mutationFn: async (data: OrderFormInputs) => {
      if (!instrument) throw new Error('Instrument not loaded');
      const request: PlaceOrderRequest = {
        clientOrderId: data.clientOrderId,
        symbol: instrument.symbol,
        side: data.side,
        type: 'LIMIT',
        limitPrice: data.limitPrice,
        quantity: data.quantity,
      };
      return ordersApi.place(request);
    },
    onSuccess: (order) => {
      setSuccessOrder(order);
      setSubmitted(true);
      queryClient.invalidateQueries({ queryKey: ['orders', symbol] });
      reset({
        side: watch('side'),
        clientOrderId: `${symbol}-${Date.now()}`,
        limitPrice: watch('limitPrice'),
        quantity: watch('quantity'),
      });
      setTimeout(() => setSubmitted(false), 5000);
    },
  });

  const cancelMutation = useMutation({
    mutationFn: (id: string) => ordersApi.cancel(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders', symbol] });
      setCancelOrderId(null);
    },
  });

  const onSubmit = async (data: OrderFormInputs) => {
    mutation.mutate(data);
  };

  const side = watch('side');
  const activeOrders = (ordersData?.content ?? []).filter(
    (o) => o.status === 'ACCEPTED' || o.status === 'PARTIALLY_FILLED'
  );

  const recentOrders = (ordersData?.content ?? []).filter(
    (o) => o.status !== 'ACCEPTED' && o.status !== 'PARTIALLY_FILLED'
  );

  return (
    <div className="animate-fade-in min-h-screen bg-surface-primary pb-12">
      {/* Header */}
      <div className="px-6 py-4 border-b border-surface-border bg-surface-secondary/40 backdrop-blur">
        <div className="flex items-center gap-4">
          <Link href="/dashboard" className="hover:text-brand-300 transition-colors">
            <ArrowLeft className="w-5 h-5 text-slate-400" />
          </Link>
          <h1 className="text-3xl font-bold text-slate-100 font-display">
            {symbol || 'Trade'}
          </h1>
          {instrument && (
            <div className="flex items-center gap-4 ml-auto">
              <span className="text-slate-400 text-sm bg-surface-tertiary px-3 py-1.5 rounded border border-surface-border">
                Tick Size: <span className="font-mono text-slate-200">{instrument.tickSize}</span>
              </span>
              <span className="text-slate-400 text-sm bg-surface-tertiary px-3 py-1.5 rounded border border-surface-border">
                Lot Size: <span className="font-mono text-slate-200">{instrument.lotSize}</span>
              </span>
            </div>
          )}
        </div>
      </div>

      {instrumentError && (
        <div className="p-6">
          <Alert type="error" title="Instrument Not Found">
            The symbol {symbol} does not exist or is inactive.
          </Alert>
        </div>
      )}

      {instrumentLoading && (
        <div className="flex justify-center py-20">
          <Spinner size="lg" />
        </div>
      )}

      {instrument && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 p-6">
          {/* Left Panel: Order entry form */}
          <div className="lg:col-span-1 space-y-6">
            <div className="card p-6 shadow-xl border border-surface-border/60 bg-surface-secondary">
              <h2 className="text-lg font-bold text-slate-100 mb-4 font-display">Place Order</h2>

              {submitted && successOrder && (
                <div className="mb-4">
                  <Alert type="success" title="Order Accepted">
                    Order {successOrder.clientOrderId} accepted with status {successOrder.status}
                  </Alert>
                </div>
              )}

              {mutation.isError && (
                <div className="mb-4">
                  <Alert type="error" title="Order Rejected">
                    {mutation.error instanceof Error ? mutation.error.message : 'Unknown error'}
                  </Alert>
                </div>
              )}

              <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                {/* Side Selection */}
                <div>
                  <label className="block text-sm font-semibold text-slate-400 mb-2">Side</label>
                  <div className="grid grid-cols-2 gap-2">
                    {(['BUY', 'SELL'] as const).map((s) => (
                      <button
                        key={s}
                        type="button"
                        onClick={() => setValue('side', s)}
                        className={`py-2.5 rounded font-bold border transition-all ${
                          side === s
                            ? s === 'BUY'
                              ? 'bg-emerald-500/20 border-emerald-500 text-emerald-400 shadow-lg shadow-emerald-500/15'
                              : 'bg-red-500/20 border-red-500 text-red-400 shadow-lg shadow-red-500/15'
                            : 'bg-surface-tertiary border-surface-border text-slate-400 hover:bg-surface-tertiary/75'
                        }`}
                      >
                        {s}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Client Order ID */}
                <div>
                  <label className="block text-sm font-semibold text-slate-400 mb-2">Client Order ID</label>
                  <input
                    type="text"
                    {...register('clientOrderId', { required: 'Client Order ID is required' })}
                    className={`input w-full ${errors.clientOrderId ? 'input-error' : ''}`}
                    placeholder="e.g., MY-ORD-001"
                  />
                  {errors.clientOrderId && (
                    <p className="text-red-400 text-xs mt-1">{errors.clientOrderId.message}</p>
                  )}
                </div>

                {/* Limit Price */}
                <div>
                  <label className="block text-sm font-semibold text-slate-400 mb-2">Limit Price</label>
                  <input
                    type="number"
                    step={instrument.tickSize}
                    {...register('limitPrice', {
                      required: 'Limit price is required',
                      validate: {
                        positive: (v) => parseFloat(v) > 0 || 'Price must be positive',
                        onTick: (v) => {
                          const tick = parseFloat(instrument.tickSize);
                          const val = parseFloat(v);
                          return Number((val / tick).toFixed(8)) % 1 === 0 || `Price must be a multiple of tick size (${tick})`;
                        }
                      }
                    })}
                    className={`input w-full ${errors.limitPrice ? 'input-error' : ''}`}
                    placeholder="100.00"
                  />
                  {errors.limitPrice && (
                    <p className="text-red-400 text-xs mt-1">{errors.limitPrice.message}</p>
                  )}
                </div>

                {/* Quantity */}
                <div>
                  <label className="block text-sm font-semibold text-slate-400 mb-2">Quantity</label>
                  <input
                    type="number"
                    step={instrument.lotSize}
                    {...register('quantity', {
                      required: 'Quantity is required',
                      validate: {
                        positive: (v) => parseFloat(v) > 0 || 'Quantity must be positive',
                        onLot: (v) => {
                          const lot = parseFloat(instrument.lotSize);
                          const val = parseFloat(v);
                          return Number((val / lot).toFixed(8)) % 1 === 0 || `Quantity must be a multiple of lot size (${lot})`;
                        }
                      }
                    })}
                    className={`input w-full ${errors.quantity ? 'input-error' : ''}`}
                    placeholder="10"
                  />
                  {errors.quantity && (
                    <p className="text-red-400 text-xs mt-1">{errors.quantity.message}</p>
                  )}
                </div>

                {/* Submit Button */}
                <button
                  type="submit"
                  disabled={isSubmitting || mutation.isPending}
                  className={`btn w-full flex items-center justify-center gap-2 py-3 rounded font-bold text-sm ${
                    side === 'BUY'
                      ? 'bg-emerald-500 hover:bg-emerald-600 text-white shadow-lg shadow-emerald-500/20'
                      : 'bg-red-500 hover:bg-red-600 text-white shadow-lg shadow-red-500/20'
                  } ${(isSubmitting || mutation.isPending) ? 'opacity-50 cursor-not-allowed' : ''}`}
                >
                  {isSubmitting || mutation.isPending ? (
                    <>
                      <Spinner size="sm" />
                      Submitting...
                    </>
                  ) : (
                    <>
                      <Send className="w-4 h-4" />
                      Place {side} Order
                    </>
                  )}
                </button>
              </form>
            </div>
          </div>

          {/* Right Panel: Order Book and My Active Orders */}
          <div className="lg:col-span-2 space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <OrderBookPanel symbol={symbol} />

              {/* My Active Orders */}
              <div className="card p-6 shadow-xl border border-surface-border/60 bg-surface-secondary flex flex-col h-[500px]">
                <h2 className="text-lg font-bold text-slate-100 mb-4 font-display">My Active Orders</h2>
                <div className="flex-1 overflow-y-auto no-scrollbar">
                  {activeOrders.length === 0 ? (
                    <p className="text-center text-slate-500 text-sm py-12">No active orders</p>
                  ) : (
                    <div className="space-y-3">
                      {activeOrders.map((order) => (
                        <div
                          key={order.id}
                          className="flex items-center justify-between p-3.5 bg-surface-tertiary rounded-lg border border-surface-border/60 hover:bg-surface-tertiary/85 transition-colors"
                        >
                          <div>
                            <div className="flex items-center gap-2">
                              <span className={`text-xs font-bold px-2 py-0.5 rounded ${
                                order.side === 'BUY' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/25' : 'bg-red-500/10 text-red-400 border border-red-500/25'
                              }`}>
                                {order.side}
                              </span>
                              <span className="font-mono text-slate-200 text-sm font-semibold">{formatCurrency(order.limitPrice, 2)}</span>
                            </div>
                            <div className="mt-1 text-slate-400 text-xs font-medium space-x-2">
                              <span>Qty: {formatQty(order.remainingQuantity)} / {formatQty(order.originalQuantity)}</span>
                              <span className="text-slate-500">•</span>
                              <span className="font-mono">{order.clientOrderId}</span>
                            </div>
                          </div>
                          <div className="flex items-center gap-2">
                            <StatusBadge status={order.status} />
                            <button
                              onClick={() => setCancelOrderId(order.id)}
                              className="p-2 hover:bg-red-500/15 text-slate-400 hover:text-red-400 rounded transition-colors"
                              title="Cancel order"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Order History / Recent Fills */}
            <div className="card p-6 shadow-xl border border-surface-border/60 bg-surface-secondary">
              <h2 className="text-lg font-bold text-slate-100 mb-4 font-display">Order History</h2>
              <div className="overflow-x-auto">
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
                    {recentOrders.length === 0 ? (
                      <tr>
                        <td colSpan={6} className="text-center text-slate-500 py-6">No historical orders</td>
                      </tr>
                    ) : (
                      recentOrders.slice(0, 10).map((order) => (
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
                            {new Date(order.createdAt).toLocaleTimeString()}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Confirmation Dialog Modal */}
      {cancelOrderId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm animate-fade-in">
          <div className="card p-6 max-w-sm w-full mx-4 shadow-2xl border border-surface-border bg-surface-secondary">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-bold text-slate-100 font-display">Cancel Order</h3>
              <button
                onClick={() => setCancelOrderId(null)}
                className="p-1 hover:bg-surface-tertiary text-slate-400 hover:text-slate-200 rounded"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
            <p className="text-sm text-slate-400 mb-6">
              Are you sure you want to cancel this order? This action cannot be undone.
            </p>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setCancelOrderId(null)}
                className="px-4 py-2 bg-surface-tertiary border border-surface-border text-slate-300 rounded font-semibold text-sm hover:bg-surface-tertiary/75"
              >
                Go Back
              </button>
              <button
                onClick={() => cancelMutation.mutate(cancelOrderId)}
                disabled={cancelMutation.isPending}
                className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded font-semibold text-sm shadow-lg shadow-red-500/20 flex items-center gap-2"
              >
                {cancelMutation.isPending ? (
                  <>
                    <Spinner size="sm" />
                    Cancelling...
                  </>
                ) : (
                  'Yes, Cancel'
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
