import { AlertCircle, CheckCircle2, Info, XCircle } from 'lucide-react';
import { cn } from '@/lib/utils';

type AlertVariant = 'success' | 'error' | 'warning' | 'info';

const config: Record<AlertVariant, { icon: typeof Info; classes: string }> = {
  success: { icon: CheckCircle2, classes: 'bg-emerald-900/30 border-emerald-700/50 text-emerald-300' },
  error:   { icon: XCircle,      classes: 'bg-red-900/30 border-red-700/50 text-red-300' },
  warning: { icon: AlertCircle,  classes: 'bg-amber-900/30 border-amber-700/50 text-amber-300' },
  info:    { icon: Info,         classes: 'bg-brand-900/30 border-brand-700/50 text-brand-300' },
};

export function Alert({
  variant = 'info',
  message,
  className,
}: {
  variant?: AlertVariant;
  message: string;
  className?: string;
}) {
  const { icon: Icon, classes } = config[variant];
  return (
    <div className={cn('flex items-start gap-3 px-4 py-3 rounded-lg border text-sm', classes, className)}>
      <Icon className="w-4 h-4 mt-0.5 shrink-0" />
      <span>{message}</span>
    </div>
  );
}
