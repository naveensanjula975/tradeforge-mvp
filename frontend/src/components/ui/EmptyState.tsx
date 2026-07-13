import { LucideIcon } from 'lucide-react';

interface EmptyStateProps {
  icon: LucideIcon;
  title: string;
  description?: string;
  action?: React.ReactNode;
}

export function EmptyState({ icon: Icon, title, description, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-16 gap-4 text-center">
      <div className="w-16 h-16 rounded-2xl bg-surface-tertiary border border-surface-border flex items-center justify-center">
        <Icon className="w-8 h-8 text-slate-500" />
      </div>
      <div>
        <p className="text-slate-300 font-semibold">{title}</p>
        {description && <p className="text-slate-500 text-sm mt-1">{description}</p>}
      </div>
      {action}
    </div>
  );
}
