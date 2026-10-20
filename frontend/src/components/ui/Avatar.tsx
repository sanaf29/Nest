import { cn } from '../../utils/cn';

/** Initials in a soft coral-to-blue circle. */
export function Avatar({ firstName, lastName, className }: { firstName?: string; lastName?: string; className?: string }) {
  const initials = `${firstName?.[0] ?? ''}${lastName?.[0] ?? ''}`.toUpperCase() || '?';
  return (
    <span
      className={cn('flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-brand-500 to-accent-500 text-sm font-bold text-on-brand', className)}
      aria-hidden="true"
    >
      {initials}
    </span>
  );
}
