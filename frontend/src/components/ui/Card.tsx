import type { ReactNode } from 'react';
import { cn } from '../../utils/cn';

interface CardProps {
  title?: string;
  description?: string;
  action?: ReactNode;
  children: ReactNode;
  className?: string;
  bodyClassName?: string;
}

/** The standard rounded surface. Feature cards use the `card-hero` gradient instead. */
export function Card({ title, description, action, children, className, bodyClassName }: CardProps) {
  return (
    <section className={cn('animate-rise rounded-3xl bg-surface shadow-card ring-1 ring-line/40', className)}>
      {(title || action) && (
        <header className="flex items-start justify-between gap-3 px-5 pt-5">
          <div className="min-w-0">
            {title && <h2 className="text-base font-semibold text-ink-900">{title}</h2>}
            {description && <p className="mt-0.5 text-sm text-ink-500">{description}</p>}
          </div>
          {action}
        </header>
      )}
      <div className={cn('p-5', bodyClassName)}>{children}</div>
    </section>
  );
}

/** "See all" style link-button for card headers. */
export const cardLinkClass = 'shrink-0 rounded-full px-2 py-1 text-sm font-semibold text-brand-700 hover:bg-brand-50';
