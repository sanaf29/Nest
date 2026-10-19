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

export function Card({ title, description, action, children, className, bodyClassName }: CardProps) {
  return (
    <section className={cn('rounded-2xl bg-white shadow-sm ring-1 ring-ink-200/70', className)}>
      {(title || action) && (
        <header className="flex items-start justify-between gap-3 border-b border-ink-100 px-5 py-4">
          <div>
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
