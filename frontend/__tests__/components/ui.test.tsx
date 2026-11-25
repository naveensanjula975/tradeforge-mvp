import { render, screen } from '@testing-library/react';
import { Inbox } from 'lucide-react';
import { Alert } from '@/components/ui/Alert';
import { EmptyState } from '@/components/ui/EmptyState';
import { Spinner, PageLoader } from '@/components/ui/Spinner';
import { StatusBadge } from '@/components/ui/StatusBadge';

describe('Alert', () => {
  it('renders the message', () => {
    render(<Alert message="Order rejected" variant="error" />);
    expect(screen.getByText('Order rejected')).toBeInTheDocument();
  });

  it('applies variant styling and defaults to info', () => {
    const { container, rerender } = render(<Alert message="m" />);
    expect(container.firstChild).toHaveClass('text-brand-300');
    rerender(<Alert message="m" variant="success" />);
    expect(container.firstChild).toHaveClass('text-emerald-300');
  });

  it('merges a custom className', () => {
    const { container } = render(<Alert message="m" className="mt-8" />);
    expect(container.firstChild).toHaveClass('mt-8');
  });
});

describe('EmptyState', () => {
  it('renders title, description and action', () => {
    render(
      <EmptyState
        icon={Inbox}
        title="No orders"
        description="Place your first order"
        action={<button>Trade</button>}
      />
    );
    expect(screen.getByText('No orders')).toBeInTheDocument();
    expect(screen.getByText('Place your first order')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Trade' })).toBeInTheDocument();
  });

  it('omits description when not provided', () => {
    render(<EmptyState icon={Inbox} title="Nothing" />);
    expect(screen.queryByText('Place your first order')).not.toBeInTheDocument();
  });
});

describe('Spinner', () => {
  it('uses the size class', () => {
    const { container } = render(<Spinner size="sm" />);
    expect(container.firstChild).toHaveClass('w-4', 'h-4', 'animate-spin');
  });

  it('PageLoader shows loading text', () => {
    render(<PageLoader />);
    expect(screen.getByText('Loading…')).toBeInTheDocument();
  });
});

describe('StatusBadge', () => {
  it('shows the human label and badge class', () => {
    render(<StatusBadge status="PARTIALLY_FILLED" />);
    const el = screen.getByText('Partial');
    expect(el).toHaveClass('badge-partial');
  });
});
