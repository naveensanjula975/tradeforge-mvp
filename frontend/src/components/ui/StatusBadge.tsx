import { OrderStatus } from '@/lib/types';
import { statusBadgeClass, statusLabel } from '@/lib/utils';

export function StatusBadge({ status }: { status: OrderStatus }) {
  return (
    <span className={statusBadgeClass(status)}>
      {statusLabel(status)}
    </span>
  );
}
