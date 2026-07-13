// ─── Auth ─────────────────────────────────────────────────────────────────────
export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresInMs: number;
  userId: string;
  email: string;
  role: 'TRADER' | 'ADMIN';
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

// ─── Instrument ───────────────────────────────────────────────────────────────
export type InstrumentStatus = 'ACTIVE' | 'INACTIVE';

export interface Instrument {
  id: string;
  symbol: string;
  name: string;
  status: InstrumentStatus;
  tickSize: string;
  lotSize: string;
  createdAt: string;
  updatedAt: string;
}

// ─── Order ────────────────────────────────────────────────────────────────────
export type OrderSide = 'BUY' | 'SELL';
export type OrderType = 'LIMIT';
export type OrderStatus =
  | 'PENDING_VALIDATION'
  | 'ACCEPTED'
  | 'PARTIALLY_FILLED'
  | 'FILLED'
  | 'REJECTED'
  | 'CANCELLED';

export interface Order {
  id: string;
  clientOrderId: string;
  userId: string;
  instrumentId: string;
  side: OrderSide;
  type: OrderType;
  limitPrice: string;
  originalQuantity: string;
  remainingQuantity: string;
  filledQuantity: string;
  status: OrderStatus;
  sequenceNumber: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface PlaceOrderRequest {
  clientOrderId: string;
  symbol: string;
  side: OrderSide;
  type: OrderType;
  limitPrice: string;
  quantity: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// ─── Order Book ───────────────────────────────────────────────────────────────
export interface OrderBookLevel {
  price: string;
  quantity: string;
  orderCount: number;
}

export interface OrderBook {
  symbol: string;
  bids: OrderBookLevel[];
  asks: OrderBookLevel[];
  timestamp: string;
}

// ─── Trade ────────────────────────────────────────────────────────────────────
export interface Trade {
  id: string;
  symbol: string;
  side: OrderSide;
  executionPrice: string;
  executionQuantity: string;
  executedAt: string;
}

// ─── Portfolio ────────────────────────────────────────────────────────────────
export interface Account {
  id: string;
  cashBalance: string;
  reservedCash: string;
  availableCash: string;
}

export interface Position {
  id: string;
  instrumentId: string;
  symbol: string;
  quantity: string;
  reservedQuantity: string;
  availableQuantity: string;
  averagePrice: string;
}

export interface Portfolio {
  account: Account;
  positions: Position[];
}

// ─── API Error ────────────────────────────────────────────────────────────────
export interface ApiError {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  correlationId: string;
  fieldErrors?: { field: string; message: string }[];
}

// ─── WebSocket Events ─────────────────────────────────────────────────────────
export type WsEventType =
  | 'ORDER_ACCEPTED'
  | 'ORDER_REJECTED'
  | 'ORDER_PARTIALLY_FILLED'
  | 'ORDER_FILLED'
  | 'ORDER_CANCELLED'
  | 'ORDER_BOOK_UPDATE'
  | 'TRADE_EXECUTED';

export interface WsOrderEvent {
  type: WsEventType;
  order: Order;
  trade?: Trade;
  timestamp: string;
}

export interface WsMarketEvent {
  type: 'ORDER_BOOK_UPDATE' | 'TRADE_EXECUTED';
  symbol: string;
  orderBook?: OrderBook;
  trade?: Trade;
  timestamp: string;
}
