import {
  cn,
  formatCurrency,
  formatQty,
  formatDateTime,
  statusBadgeClass,
  statusLabel,
  sideClass,
  isCancellable,
  newClientOrderId,
} from '@/lib/utils';
import type { OrderStatus } from '@/lib/types';

const ALL_STATUSES: OrderStatus[] = [
  'PENDING_VALIDATION',
  'ACCEPTED',
  'PARTIALLY_FILLED',
  'FILLED',
  'REJECTED',
  'CANCELLED',
];

describe('cn', () => {
  it('merges conflicting tailwind classes, last wins', () => {
    expect(cn('px-2', 'px-4')).toBe('px-4');
  });
  it('drops falsy values', () => {
    expect(cn('a', false && 'b', undefined, 'c')).toBe('a c');
  });
});

describe('formatCurrency', () => {
  it('formats with 2 decimals by default', () => {
    expect(formatCurrency(1234.5)).toBe('1,234.50');
  });
  it('accepts numeric strings', () => {
    expect(formatCurrency('10')).toBe('10.00');
  });
  it('respects custom decimals', () => {
    expect(formatCurrency(1.23456, 4)).toBe('1.2346');
  });
  it('returns an em dash for NaN input', () => {
    expect(formatCurrency('abc')).toBe('—');
  });
});

describe('formatQty', () => {
  it('strips trailing zeros', () => {
    expect(formatQty('1.50000000')).toBe('1.5');
  });
  it('keeps up to 8 decimals', () => {
    expect(formatQty(0.123456789)).toBe('0.12345679');
  });
  it('adds thousands separators', () => {
    expect(formatQty(1000000)).toBe('1,000,000');
  });
  it('returns an em dash for NaN input', () => {
    expect(formatQty('x')).toBe('—');
  });
});

describe('formatDateTime', () => {
  it('formats a valid ISO timestamp as dd MMM HH:mm:ss', () => {
    expect(formatDateTime('2024-03-05T14:07:09')).toBe('05 Mar 14:07:09');
  });
  it('falls back to the raw input for an invalid date', () => {
    expect(formatDateTime('not-a-date')).toBe('not-a-date');
  });
});

describe('status helpers', () => {
  it.each(ALL_STATUSES)('has a badge class and label for %s', (s) => {
    expect(statusBadgeClass(s)).toMatch(/^badge-/);
    expect(statusLabel(s)).not.toBe('');
  });

  it('maps ACCEPTED to Active', () => {
    expect(statusLabel('ACCEPTED')).toBe('Active');
  });

  it('falls back to pending badge for unknown status', () => {
    expect(statusBadgeClass('WAT' as OrderStatus)).toBe('badge-pending');
  });

  it('falls back to the raw status as label for unknown status', () => {
    expect(statusLabel('WAT' as OrderStatus)).toBe('WAT');
  });
});

describe('sideClass', () => {
  it('is green for BUY and red for SELL', () => {
    expect(sideClass('BUY')).toBe('text-emerald-400');
    expect(sideClass('SELL')).toBe('text-red-400');
  });
});

describe('isCancellable', () => {
  it.each([
    ['ACCEPTED', true],
    ['PARTIALLY_FILLED', true],
    ['PENDING_VALIDATION', false],
    ['FILLED', false],
    ['REJECTED', false],
    ['CANCELLED', false],
  ] as [OrderStatus, boolean][])('%s -> %s', (status, expected) => {
    expect(isCancellable(status)).toBe(expected);
  });
});

describe('newClientOrderId', () => {
  it('matches the CL-<timestamp>-<suffix> format', () => {
    expect(newClientOrderId()).toMatch(/^CL-\d+-[A-Z0-9]{1,5}$/);
  });
  it('produces unique ids', () => {
    const ids = new Set(Array.from({ length: 200 }, newClientOrderId));
    expect(ids.size).toBe(200);
  });
});
