import { Suspense, useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router';
import { useAuth } from '../../hooks/useAuth';
import { Icon } from '../ui/Icon';
import { LoadingState } from '../ui/States';
import { NotificationBell } from './NotificationBell';
import { cn } from '../../utils/cn';
import { navigation } from './navigation';

function Logo() {
  return (
    <div className="flex items-center gap-2">
      <img src="/nest.svg" alt="" className="h-8 w-8" />
      <span className="text-lg font-semibold tracking-tight text-ink-900">Nest</span>
    </div>
  );
}

function NavLinks({ onNavigate }: { onNavigate?: () => void }) {
  return (
    <nav aria-label="Main" className="flex flex-col gap-6">
      {navigation.map((section, index) => (
        <div key={section.label ?? index}>
          {section.label && <p className="mb-2 px-3 text-xs font-semibold tracking-wide text-ink-400 uppercase">{section.label}</p>}
          <ul className="flex flex-col gap-0.5">
            {section.items.map((item) => (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  end={item.to === '/'}
                  onClick={onNavigate}
                  className={({ isActive }) =>
                    cn(
                      'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                      isActive ? 'bg-brand-50 text-brand-800' : 'text-ink-600 hover:bg-ink-100 hover:text-ink-900',
                    )
                  }
                >
                  <Icon name={item.icon} className="h-5 w-5 shrink-0" />
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </div>
      ))}
    </nav>
  );
}

function UserBox() {
  const { user, logout } = useAuth();
  if (!user) return null;
  return (
    <div className="flex items-center justify-between gap-2 rounded-xl bg-ink-50 p-3">
      <div className="min-w-0">
        <p className="truncate text-sm font-medium text-ink-900">
          {user.firstName} {user.lastName}
        </p>
        <p className="truncate text-xs text-ink-500">{user.email}</p>
      </div>
      <div className="flex shrink-0">
        <NavLink to="/settings" className="rounded-lg p-2 text-ink-500 hover:bg-white hover:text-ink-900" aria-label="Settings" title="Settings">
          <Icon name="settings" className="h-4 w-4" />
        </NavLink>
        <button type="button" onClick={logout} className="rounded-lg p-2 text-ink-500 hover:bg-white hover:text-ink-900" aria-label="Log out" title="Log out">
          <Icon name="logout" className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}

/** Sidebar on large screens; a top bar with a slide-in menu on phones. */
export function AppLayout() {
  const [menuOpen, setMenuOpen] = useState(false);
  const location = useLocation();

  useEffect(() => setMenuOpen(false), [location.pathname]);

  return (
    <div className="min-h-dvh lg:flex">
      <a href="#main" className="sr-only focus:not-sr-only focus:absolute focus:top-2 focus:left-2 focus:z-50 focus:rounded-lg focus:bg-white focus:px-3 focus:py-2">
        Skip to content
      </a>

      <aside className="no-print hidden w-64 shrink-0 flex-col justify-between overflow-y-auto border-r border-ink-200 bg-white px-4 py-6 lg:flex lg:sticky lg:top-0 lg:h-dvh">
        <div className="flex flex-col gap-8">
          <div className="flex items-center justify-between px-2">
            <Logo />
            <NotificationBell />
          </div>
          <NavLinks />
        </div>
        <UserBox />
      </aside>

      <header className="no-print sticky top-0 z-30 flex items-center justify-between border-b border-ink-200 bg-white/95 px-4 py-3 backdrop-blur lg:hidden">
        <Logo />
        <div className="flex items-center gap-1">
          <NotificationBell />
          <button type="button" onClick={() => setMenuOpen(true)} className="rounded-lg p-2 text-ink-600 hover:bg-ink-100" aria-label="Open menu" aria-expanded={menuOpen}>
            <Icon name="menu" />
          </button>
        </div>
      </header>

      {menuOpen && (
        <div className="fixed inset-0 z-40 lg:hidden" role="dialog" aria-modal="true" aria-label="Menu">
          <div className="absolute inset-0 bg-ink-900/40" onClick={() => setMenuOpen(false)} aria-hidden="true" />
          <div className="absolute inset-y-0 left-0 flex w-72 flex-col justify-between bg-white px-4 py-6 shadow-xl">
            <div className="flex flex-col gap-8">
              <div className="flex items-center justify-between px-2">
                <Logo />
                <button type="button" onClick={() => setMenuOpen(false)} className="rounded-lg p-1 text-ink-500 hover:bg-ink-100" aria-label="Close menu">
                  <Icon name="close" />
                </button>
              </div>
              <NavLinks onNavigate={() => setMenuOpen(false)} />
            </div>
            <UserBox />
          </div>
        </div>
      )}

      <main id="main" className="mx-auto w-full max-w-6xl flex-1 px-4 py-6 sm:px-6 lg:px-10 lg:py-10">
        <Suspense fallback={<LoadingState />}>
          <Outlet />
        </Suspense>
      </main>
    </div>
  );
}
