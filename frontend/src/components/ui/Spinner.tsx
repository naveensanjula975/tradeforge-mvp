import { cn } from '@/lib/utils';

interface SpinnerProps {
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

const sizeMap = {
  sm: 'w-4 h-4 border-2',
  md: 'w-8 h-8 border-2',
  lg: 'w-12 h-12 border-3',
};

export function Spinner({ size = 'md', className }: SpinnerProps) {
  return (
    <div
      className={cn(
        'rounded-full border-surface-border border-t-brand-500 animate-spin',
        sizeMap[size],
        className
      )}
    />
  );
}

export function PageLoader() {
  return (
    <div className="flex flex-col items-center justify-center min-h-[40vh] gap-4">
      <Spinner size="lg" />
      <p className="text-slate-500 text-sm animate-pulse">Loading…</p>
    </div>
  );
}
