import { apiClient } from './api-client';
import {
  AuthResponse, LoginRequest, RegisterRequest,
  Instrument, Order, PlaceOrderRequest, PagedResponse,
  OrderBook, Trade, Portfolio,
} from './types';

// ─── Auth ─────────────────────────────────────────────────────────────────────
export const authApi = {
  register: (data: RegisterRequest) =>
    apiClient.post<AuthResponse>('/api/v1/auth/register', data).then((r) => r.data),

  login: (data: LoginRequest) =>
    apiClient.post<AuthResponse>('/api/v1/auth/login', data).then((r) => r.data),
};

// ─── Instruments ──────────────────────────────────────────────────────────────
export const instrumentsApi = {
  list: () =>
    apiClient.get<Instrument[]>('/api/v1/instruments').then((r) => r.data),

  getBySymbol: (symbol: string) =>
    apiClient.get<Instrument>(`/api/v1/instruments/${symbol}`).then((r) => r.data),
};

// ─── Orders ───────────────────────────────────────────────────────────────────
export const ordersApi = {
  place: (data: PlaceOrderRequest) =>
    apiClient.post<Order>('/api/v1/orders', data).then((r) => r.data),

  myOrders: (page = 0, size = 20, status?: string, symbol?: string) => {
    const params: Record<string, string | number> = { page, size };
    if (status) params.status = status;
    if (symbol) params.symbol = symbol;
    return apiClient
      .get<PagedResponse<Order>>('/api/v1/orders/me', { params })
      .then((r) => r.data);
  },

  getById: (id: string) =>
    apiClient.get<Order>(`/api/v1/orders/${id}`).then((r) => r.data),

  cancel: (id: string) =>
    apiClient.delete<Order>(`/api/v1/orders/${id}`).then((r) => r.data),
};

// ─── Order Book ───────────────────────────────────────────────────────────────
export const orderBookApi = {
  get: (symbol: string) =>
    apiClient.get<OrderBook>(`/api/v1/market/${symbol}/orderbook`).then((r) => r.data),
};

// ─── Trades ───────────────────────────────────────────────────────────────────
export const tradesApi = {
  myTrades: (page = 0, size = 20) =>
    apiClient
      .get<PagedResponse<Trade>>('/api/v1/trades/me', { params: { page, size } })
      .then((r) => r.data),

  publicTrades: (symbol: string, page = 0, size = 20) =>
    apiClient
      .get<PagedResponse<Trade>>(`/api/v1/market/${symbol}/trades`, { params: { page, size } })
      .then((r) => r.data),
};

// ─── Portfolio ────────────────────────────────────────────────────────────────
export const portfolioApi = {
  get: () =>
    apiClient.get<Portfolio>('/api/v1/portfolio/me').then((r) => r.data),
};
