import type { ReactNode } from 'react';
import { Button } from './Button';
import { Icon, type IconName } from './Icon';
import { Spinner } from './Spinner';
import type { ApiError } from '../../utils/errors';

export function LoadingState({ label = 'Loading…' }: { label?: string }) {
  return (
    <div role="status" className="flex items-center justify-center gap-2 py-12 text-sm text-ink-500">
      <Spinner />
      {label}
    </div>
  );
}

export function ErrorState({ error, onRetry }: { error: ApiError; onRetry?: () => void }) {
  return (
    <div role="alert" className="flex flex-col items-center gap-3 rounded-xl bg-negative-50 px-6 py-8 text-center">
      <Icon name="alert" className="h-6 w-6 text-negative-600" />
      <p className="text-sm text-ink-700">{error.message}</p>
      {onRetry && (
        <Button variant="secondary" size="sm" onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  );
}

export function EmptyState({ icon = 'list', title, children, action }: { icon?: IconName; title: string; children?: ReactNode; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center gap-3 px-6 py-10 text-center">
      <div className="rounded-full bg-brand-50 p-3 text-brand-700">
        <Icon name={icon} className="h-6 w-6" />
      </div>
      <h3 className="text-sm font-semibold text-ink-900">{title}</h3>
      {children && <p className="max-w-sm text-sm text-ink-500">{children}</p>}
      {action}
    </div>
  );
}

/** Inline error for a form submission. */
export function FormError({ message }: { message: string | null }) {
  if (!message) return null;
  return (
    <div role="alert" className="flex items-start gap-2 rounded-lg bg-negative-50 px-3 py-2 text-sm text-negative-600">
      <Icon name="alert" className="mt-0.5 h-4 w-4 shrink-0" />
      <span>{message}</span>
    </div>
  );
}
