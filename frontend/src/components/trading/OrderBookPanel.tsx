'use client';

import { useEffect, useState, useRef } from 'react';
import { OrderBook, OrderBookLevel, WsMarketEvent } from '@/lib/types';
import { wsClient } from '@/lib/websocket';
import { formatCurrency, formatQty } from '@/lib/utils';
import { Wifi, WifiOff, RefreshCw } from 'lucide-react';
import { orderBookApi } from '@/lib/api';

interface Props {
  symbol: string;
  initialBook?: OrderBook;
}

const MAX_LEVELS = 12;

function calcMaxTotal(levels: OrderBookLevel[]): number {
  return Math.max(...levels.map((l) => parseFloat(l.quantity)), 1);
}

function LevelRow({
  level,
  side,
  maxTotal,
}: {
  level: OrderBookLevel;
  side: 'bid' | 'ask';
  maxTotal: number;
}) {
  const pct      = (parseFloat(level.quantity) / maxTotal) * 100;
  const isBid    = side === 'bid';
  const rowRef   = useRef<HTMLDivElement>(null);

  return (
    <div ref={rowRef} className="relative group hover:bg-surface-tertiary/50 transition-colors">
      {/* Depth bar */}
      <div
        className={`absolute inset-y-0 ${isBid ? 'right-0 depth-bar-bid' : 'left-0 depth-bar-ask'}`}
        style={{ width: `${pct}%` }}
      />
      <div className={`relative z-10 grid grid-cols-3 px-3 py-1.5 text-xs font-mono`}>
        {isBid ? (
          <>
            <span className="price-bid">{formatCurrency(level.price, 2)}</span>
            <span className="text-slate-300 text-center">{formatQty(level.quantity)}</span>
            <span className="text-slate-500 text-right">{level.orderCount}</span>
          </>
        ) : (
          <>
            <span className="text-slate-500 text-left">{level.orderCount}</span>
            <span className="text-slate-300 text-center">{formatQty(level.quantity)}</span>
            <span className="price-ask text-right">{formatCurrency(level.price, 2)}</span>
          </>
        )}
      </div>
    </div>
  );
}

export function OrderBookPanel({ symbol, initialBook }: Props) {
  const [book, setBook]           = useState<OrderBook | null>(initialBook ?? null);
  const [connected, setConnected] = useState(false);
  const [loading, setLoading]     = useState(!initialBook);

  const loadSnapshot = async () => {
    setLoading(true);
    try {
      const snapshot = await orderBookApi.get(symbol);
      setBook(snapshot);
    } catch {
      // keep stale book
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!initialBook) loadSnapshot();

    const sub = wsClient.subscribeMarket(symbol, (event: WsMarketEvent) => {
      if (event.type === 'ORDER_BOOK_UPDATE' && event.orderBook) {
        setBook(event.orderBook);
        setConnected(true);
      }
    });

    setConnected(wsClient.isConnected());

    return () => {
      sub?.unsubscribe();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [symbol]);

  const bids = (book?.bids ?? []).slice(0, MAX_LEVELS);
  const asks = (book?.asks ?? []).slice(0, MAX_LEVELS).reverse();

  const maxBidTotal = calcMaxTotal(bids);
  const maxAskTotal = calcMaxTotal(asks);

  const spread =
    bids[0] && asks[asks.length - 1]
      ? (parseFloat(asks[asks.length - 1].price) - parseFloat(bids[0].price)).toFixed(2)
      : '—';

  return (
    <div className="card flex flex-col h-full">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-surface-border">
        <div className="flex items-center gap-2">
          <h3 className="text-sm font-semibold text-slate-200">Order Book</h3>
          <span className="text-xs text-slate-500 font-mono">{symbol}</span>
        </div>
        <div className="flex items-center gap-2">
          <span
            className={`flex items-center gap-1 text-xs ${connected ? 'text-emerald-400' : 'text-slate-500'}`}
          >
            {connected ? <Wifi className="w-3 h-3" /> : <WifiOff className="w-3 h-3" />}
            {connected ? 'Live' : 'Snapshot'}
          </span>
          <button
            onClick={loadSnapshot}
            className="btn-ghost btn-sm !p-1"
            title="Refresh order book"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Column headers */}
      <div className="grid grid-cols-3 px-3 py-1.5 text-[10px] text-slate-500 uppercase tracking-wider border-b border-surface-border">
        <span>Price</span>
        <span className="text-center">Qty</span>
        <span className="text-right">Orders</span>
      </div>

      {/* Asks (sell side) */}
      <div className="flex flex-col-reverse overflow-y-auto no-scrollbar max-h-52">
        {asks.length === 0 ? (
          <p className="text-center text-slate-600 text-xs py-4">No asks</p>
        ) : (
          asks.map((level, i) => (
            <LevelRow key={`ask-${i}`} level={level} side="ask" maxTotal={maxAskTotal} />
          ))
        )}
      </div>

      {/* Spread */}
      <div className="flex items-center justify-between px-3 py-2 bg-surface-tertiary border-y border-surface-border text-xs font-mono">
        <span className="text-slate-500">Spread</span>
        <span className="text-slate-300 font-semibold">{spread}</span>
      </div>

      {/* Bids (buy side) */}
      <div className="overflow-y-auto no-scrollbar max-h-52">
        {bids.length === 0 ? (
          <p className="text-center text-slate-600 text-xs py-4">No bids</p>
        ) : (
          bids.map((level, i) => (
            <LevelRow key={`bid-${i}`} level={level} side="bid" maxTotal={maxBidTotal} />
          ))
        )}
      </div>
    </div>
  );
}
