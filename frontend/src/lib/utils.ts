import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { format, formatDistanceToNow } from 'date-fns';
import { OrderStatus, OrderSide } from './types';

/** Merge Tailwind classes safely. */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/** Format a number as currency (LKR). */
export function formatCurrency(value: string | number, decimals = 2): string {
  const num = typeof value === 'string' ? parseFloat(value) : value;
  if (isNaN(num)) return '—';
  return new Intl.NumberFormat('en-LK', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(num);
}

/** Format a quantity with up to 8 decimal places, stripping trailing zeros. */
export function formatQty(value: string | number): string {
  const num = typeof value === 'string' ? parseFloat(value) : value;
  if (isNaN(num)) return '—';
  return num.toLocaleString('en', { minimumFractionDigits: 0, maximumFractionDigits: 8 });
}

/** Format an ISO timestamp as a short date-time. */
export function formatDateTime(iso: string): string {
  try {
    return format(new Date(iso), 'dd MMM HH:mm:ss');
  } catch {
    return iso;
  }
}

/** Format a relative time (e.g. "2 minutes ago"). */
export function formatRelative(iso: string): string {
  try {
    return formatDistanceToNow(new Date(iso), { addSuffix: true });
  } catch {
    return iso;
  }
}

/** Map OrderStatus to a CSS badge class. */
export function statusBadgeClass(status: OrderStatus): string {
  const map: Record<OrderStatus, string> = {
    PENDING_VALIDATION: 'badge-pending',
    ACCEPTED:           'badge-accepted',
    PARTIALLY_FILLED:   'badge-partial',
    FILLED:             'badge-filled',
    REJECTED:           'badge-rejected',
    CANCELLED:          'badge-cancelled',
  };
  return map[status] ?? 'badge-pending';
}

/** Human-readable order status label. */
export function statusLabel(status: OrderStatus): string {
  const map: Record<OrderStatus, string> = {
    PENDING_VALIDATION: 'Pending',
    ACCEPTED:           'Active',
    PARTIALLY_FILLED:   'Partial',
    FILLED:             'Filled',
    REJECTED:           'Rejected',
    CANCELLED:          'Cancelled',
  };
  return map[status] ?? status;
}

/** Side colour class. */
export function sideClass(side: OrderSide): string {
  return side === 'BUY' ? 'text-emerald-400' : 'text-red-400';
}

/** Returns true if the order can still be cancelled. */
export function isCancellable(status: OrderStatus): boolean {
  return status === 'ACCEPTED' || status === 'PARTIALLY_FILLED';
}

/** Generate a unique client order ID. */
export function newClientOrderId(): string {
  return `CL-${Date.now()}-${Math.random().toString(36).slice(2, 7).toUpperCase()}`;
}
