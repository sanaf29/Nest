import type { ReactNode } from 'react';

/** Centered card used by the login and registration pages. */
export function AuthLayout({ title, subtitle, children, footer }: { title: string; subtitle: string; children: ReactNode; footer: ReactNode }) {
  return (
    <main className="relative flex min-h-dvh items-center justify-center overflow-hidden px-4 py-10">
      <div className="pointer-events-none absolute -top-40 -right-32 h-96 w-96 rounded-full bg-hero-from/30 blur-3xl" aria-hidden="true" />
      <div className="pointer-events-none absolute -bottom-40 -left-32 h-96 w-96 rounded-full bg-brand-500/15 blur-3xl" aria-hidden="true" />
      <div className="relative w-full max-w-md animate-rise">
        <div className="mb-8 flex flex-col items-center gap-3 text-center">
          <img src="/nest.svg" alt="" className="h-14 w-14" />
          <h1 className="text-3xl font-bold tracking-tight text-ink-900">{title}</h1>
          <p className="text-sm text-ink-500">{subtitle}</p>
        </div>
        <div className="rounded-3xl bg-surface p-6 shadow-card ring-1 ring-line/40 sm:p-8">{children}</div>
        <p className="mt-6 text-center text-sm text-ink-500">{footer}</p>
      </div>
    </main>
  );
}
