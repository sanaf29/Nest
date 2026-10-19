import type { ReactNode } from 'react';

/** Centered card used by the login and registration pages. */
export function AuthLayout({ title, subtitle, children, footer }: { title: string; subtitle: string; children: ReactNode; footer: ReactNode }) {
  return (
    <main className="flex min-h-dvh items-center justify-center bg-gradient-to-br from-brand-50 via-ink-50 to-accent-100 px-4 py-10">
      <div className="w-full max-w-md">
        <div className="mb-8 flex flex-col items-center gap-3 text-center">
          <img src="/nest.svg" alt="" className="h-12 w-12" />
          <h1 className="text-2xl font-semibold tracking-tight text-ink-900">{title}</h1>
          <p className="text-sm text-ink-500">{subtitle}</p>
        </div>
        <div className="rounded-2xl bg-white p-6 shadow-sm ring-1 ring-ink-200/70 sm:p-8">{children}</div>
        <p className="mt-6 text-center text-sm text-ink-600">{footer}</p>
      </div>
    </main>
  );
}
