'use client';

import { useSearchParams } from 'next/navigation';
import { useQuery, useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { ordersApi, instrumentsApi } from '@/lib/api';
import { PlaceOrderRequest, OrderSide, Instrument, Order } from '@/lib/types';
import { OrderBookPanel } from '@/components/trading/OrderBookPanel';
import { Spinner } from '@/components/ui/Spinner';
import { Alert } from '@/components/ui/Alert';
import { Send, ArrowLeft } from 'lucide-react';
import Link from 'next/link';

interface OrderFormInputs {
  clientOrderId: string;
  side: OrderSide;
  limitPrice: string;
  quantity: string;
}

export default function TradePage() {
  const searchParams = useSearchParams();
  const symbol = searchParams.get('symbol') || '';
  const [submitted, setSubmitted] = useState(false);
  const [successOrder, setSuccessOrder] = useState<Order | null>(null);

  const { data: instrument, isLoading: instrumentLoading, isError: instrumentError } = useQuery({
    queryKey: ['instrument', symbol],
    queryFn: () => (symbol ? instrumentsApi.getBySymbol(symbol) : null),
    enabled: !!symbol,
  });

  const { register, handleSubmit, watch, reset, formState: { errors, isSubmitting } } = useForm<OrderFormInputs>({
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
      reset();
      setSubmitted(true);
      setTimeout(() => setSubmitted(false), 5000);
    },
  });

  const onSubmit = async (data: OrderFormInputs) => {
    mutation.mutate(data);
  };

  const side = watch('side');

  return (
    <div className="animate-fade-in min-h-screen bg-surface-primary">
      {/* Header */}
      <div className="px-6 py-4 border-b border-surface-border">
        <div className="flex items-center gap-4">
          <Link href="/dashboard" className="hover:text-brand-300 transition-colors">
            <ArrowLeft className="w-5 h-5 text-slate-400" />
          </Link>
          <h1 className="text-3xl font-bold text-slate-100">
            {symbol || 'Trade'}
          </h1>
          {instrument && (
            <span className="ml-auto text-slate-500 text-sm">
              Tick: {instrument.tickSize} | Lot: {instrument.lotSize}
            </span>
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
          {/* Left: Order Entry Form */}
          <div className="lg:col-span-1">
            <div className="card p-6">
              <h2 className="text-lg font-semibold text-slate-100 mb-4">Place Order</h2>

              {submitted && successOrder && (
                <Alert type="success" title="Order Accepted">
                  Order {successOrder.clientOrderId} accepted with status {successOrder.status}
                </Alert>
              )}

              {mutation.isError && (
                <Alert type="error" title="Order Rejected">
                  {mutation.error instanceof Error ? mutation.error.message : 'Unknown error'}
                </Alert>
              )}

              <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                {/* Side */}
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">Side</label>
                  <div className="flex gap-2">
                    {(['BUY', 'SELL'] as const).map((s) => (
                      <label key={s} className="flex items-center gap-2 cursor-pointer">
                        <input
                          type="radio"
                          value={s}
                          {...register('side')}
                          className="w-4 h-4"
                        />
                        <span className={`font-medium ${s === 'BUY' ? 'text-emerald-400' : 'text-red-400'}`}>
                          {s}
                        </span>
                      </label>
                    ))}
                  </div>
                </div>

                {/* Client Order ID */}
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">Client Order ID</label>
                  <input
                    type="text"
                    {...register('clientOrderId', { required: 'Required' })}
                    className={`input w-full ${errors.clientOrderId ? 'input-error' : ''}`}
                    placeholder="e.g., MY-ORD-001"
                  />
                  {errors.clientOrderId && (
                    <p className="text-red-400 text-xs mt-1">{errors.clientOrderId.message}</p>
                  )}
                </div>

                {/* Limit Price */}
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">Limit Price</label>
                  <input
                    type="number"
                    step={instrument.tickSize}
                    {...register('limitPrice', { required: 'Required' })}
                    className={`input w-full ${errors.limitPrice ? 'input-error' : ''}`}
                    placeholder="100.00"
                  />
                  {errors.limitPrice && (
                    <p className="text-red-400 text-xs mt-1">{errors.limitPrice.message}</p>
                  )}
                </div>

                {/* Quantity */}
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-2">Quantity</label>
                  <input
                    type="number"
                    step={instrument.lotSize}
                    {...register('quantity', { required: 'Required' })}
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
                  className={`btn w-full flex items-center justify-center gap-2 ${
                    side === 'BUY'
                      ? 'btn-primary'
                      : 'btn-secondary'
                  } ${isSubmitting || mutation.isPending ? 'opacity-50 cursor-not-allowed' : ''}`}
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

          {/* Right: Order Book */}
          <div className="lg:col-span-2">
            <OrderBookPanel symbol={symbol} />
          </div>
        </div>
      )}
    </div>
  );
}
