'use client';

import { Client, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getToken } from './auth';
import { WsMarketEvent, WsOrderEvent } from './types';

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || 'http://localhost:8080/ws';

type MarketHandler = (event: WsMarketEvent) => void;
type OrderHandler  = (event: WsOrderEvent)  => void;

class TradeForgeWebSocket {
  private client: Client | null = null;
  private subscriptions: StompSubscription[] = [];
  private connected = false;
  private reconnectAttempts = 0;
  private maxReconnect = 5;

  /** Connect to the STOMP broker. */
  connect(onConnected?: () => void, onDisconnected?: () => void): void {
    if (this.client?.active) return;

    this.client = new Client({
      webSocketFactory: () => new SockJS(WS_URL) as unknown as WebSocket,
      connectHeaders: {
        Authorization: `Bearer ${getToken() ?? ''}`,
      },
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      reconnectDelay: 3000,

      onConnect: () => {
        this.connected = true;
        this.reconnectAttempts = 0;
        console.info('[WS] Connected to TradeForge broker');
        onConnected?.();
      },

      onDisconnect: () => {
        this.connected = false;
        console.warn('[WS] Disconnected');
        onDisconnected?.();
      },

      onStompError: (frame) => {
        console.error('[WS] STOMP error:', frame.headers['message']);
      },

      onWebSocketClose: () => {
        this.connected = false;
        this.reconnectAttempts++;
        if (this.reconnectAttempts >= this.maxReconnect) {
          console.error('[WS] Max reconnect attempts reached');
        }
      },
    });

    this.client.activate();
  }

  /** Disconnect cleanly. */
  disconnect(): void {
    this.subscriptions.forEach((s) => { try { s.unsubscribe(); } catch {} });
    this.subscriptions = [];
    this.client?.deactivate();
    this.client = null;
    this.connected = false;
  }

  /** Subscribe to public market updates for a symbol. */
  subscribeMarket(symbol: string, handler: MarketHandler): StompSubscription | null {
    if (!this.client?.active) return null;
    const destination = `/topic/market/${symbol}`;
    const sub = this.client.subscribe(destination, (msg) => {
      try {
        handler(JSON.parse(msg.body) as WsMarketEvent);
      } catch (e) {
        console.error('[WS] Failed to parse market event', e);
      }
    });
    this.subscriptions.push(sub);
    return sub;
  }

  /** Subscribe to private user order events. */
  subscribeUserOrders(handler: OrderHandler): StompSubscription | null {
    if (!this.client?.active) return null;
    const destination = '/user/queue/orders';
    const sub = this.client.subscribe(destination, (msg) => {
      try {
        handler(JSON.parse(msg.body) as WsOrderEvent);
      } catch (e) {
        console.error('[WS] Failed to parse order event', e);
      }
    });
    this.subscriptions.push(sub);
    return sub;
  }

  isConnected(): boolean {
    return this.connected;
  }
}

// Singleton instance
export const wsClient = new TradeForgeWebSocket();
