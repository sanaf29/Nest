import { Suspense, useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useLocation } from 'react-router';
import { useAuth } from '../../hooks/useAuth';
import { Avatar } from '../ui/Avatar';
import { Icon } from '../ui/Icon';
import { LoadingState } from '../ui/States';
import { NotificationBell } from './NotificationBell';
import { cn } from '../../utils/cn';
import { navigation, tabs, type NavItem } from './navigation';

function Logo() {
  return (
    <Link to="/" className="flex items-center gap-2 rounded-xl" aria-label="Nest home">
      <img src="/nest.svg" alt="" className="h-8 w-8" />
      <span className="text-lg font-bold tracking-tight text-ink-900">Nest</span>
    </Link>
  );
}

function NavLinks({ onNavigate }: { onNavigate?: () => void }) {
  return (
    <nav aria-label="Main" className="flex flex-col gap-6">
      {navigation.map((section, index) => (
        <div key={section.label ?? index}>
          {section.label && <p className="mb-2 px-3 text-[11px] font-bold tracking-widest text-ink-400 uppercase">{section.label}</p>}
          <ul className="flex flex-col gap-0.5">
            {section.items.map((item) => (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  end={item.to === '/'}
                  onClick={onNavigate}
                  className={({ isActive }) =>
                    cn(
                      'flex items-center gap-3 rounded-2xl px-3 py-2.5 text-sm font-semibold transition-colors',
                      isActive ? 'bg-brand-50 text-brand-700' : 'text-ink-500 hover:bg-ink-100 hover:text-ink-900',
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
    <div className="flex items-center gap-3 rounded-2xl bg-surface-2 p-3 ring-1 ring-line/40">
      <Avatar firstName={user.firstName} lastName={user.lastName} className="h-9 w-9" />
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-semibold text-ink-900">
          {user.firstName} {user.lastName}
        </p>
        <p className="truncate text-xs text-ink-500">{user.email}</p>
      </div>
      <div className="flex shrink-0">
        <NavLink to="/settings" className="rounded-xl p-2 text-ink-500 hover:bg-ink-100 hover:text-ink-900" aria-label="Settings" title="Settings">
          <Icon name="settings" className="h-4 w-4" />
        </NavLink>
        <button type="button" onClick={logout} className="rounded-xl p-2 text-ink-500 hover:bg-ink-100 hover:text-ink-900" aria-label="Log out" title="Log out">
          <Icon name="logout" className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}

function Tab({ item }: { item: NavItem }) {
  return (
    <NavLink
      to={item.to}
      end={item.to === '/'}
      className={({ isActive }) =>
        cn('flex flex-1 flex-col items-center gap-1 py-2 text-[11px] font-semibold transition-colors', isActive ? 'text-brand-600' : 'text-ink-400 hover:text-ink-700')
      }
    >
      {({ isActive }) => (
        <>
          <Icon name={item.icon} className={cn('h-6 w-6 transition-transform', isActive && 'scale-110')} />
          {item.label}
        </>
      )}
    </NavLink>
  );
}

/** Fixed bottom navigation for phones and tablets, with the Add action raised in the middle. */
function TabBar() {
  return (
    <nav aria-label="Tabs" className="no-print fixed inset-x-0 bottom-0 z-30 lg:hidden">
      <div className="mx-auto flex max-w-xl items-end rounded-t-3xl bg-nav/95 px-3 pb-[max(0.5rem,env(safe-area-inset-bottom))] shadow-lift ring-1 ring-line/50 backdrop-blur">
        {tabs.left.map((item) => <Tab key={item.to} item={item} />)}
        <div className="flex flex-1 justify-center">
          <NavLink
            to="/add"
            aria-label="Add transaction"
            className="-mt-6 flex h-15 w-15 items-center justify-center rounded-full bg-brand-600 text-on-brand shadow-glow ring-4 ring-canvas transition-transform active:scale-95"
          >
            <Icon name="plus" className="h-7 w-7" />
          </NavLink>
        </div>
        {tabs.right.map((item) => <Tab key={item.to} item={item} />)}
      </div>
    </nav>
  );
}

/** Sidebar on large screens; a top bar, slide-in menu, and bottom tabs on phones. */
export function AppLayout() {
  const [menuOpen, setMenuOpen] = useState(false);
  const location = useLocation();
  const { user } = useAuth();

  useEffect(() => setMenuOpen(false), [location.pathname]);

  return (
    <div className="min-h-dvh lg:flex">
      <a href="#main" className="sr-only focus:not-sr-only focus:absolute focus:top-2 focus:left-2 focus:z-50 focus:rounded-xl focus:bg-surface focus:px-3 focus:py-2">
        Skip to content
      </a>

      <aside className="no-print hidden w-68 shrink-0 flex-col justify-between overflow-y-auto border-r border-line/40 bg-surface px-4 py-6 lg:sticky lg:top-0 lg:flex lg:h-dvh">
        <div className="flex flex-col gap-7">
          <div className="flex items-center justify-between px-2">
            <Logo />
            <NotificationBell />
          </div>
          <Link
            to="/add"
            className="flex items-center justify-center gap-2 rounded-2xl bg-brand-600 px-4 py-3 text-sm font-bold text-on-brand shadow-glow transition-transform active:scale-[0.97]"
          >
            <Icon name="plus" className="h-4 w-4" />
            New transaction
          </Link>
          <NavLinks />
        </div>
        <div className="pt-6">
          <UserBox />
        </div>
      </aside>

      <header className="no-print sticky top-0 z-30 flex items-center justify-between bg-canvas/85 px-4 py-3 backdrop-blur lg:hidden">
        <Logo />
        <div className="flex items-center gap-1">
          <NotificationBell />
          <button type="button" onClick={() => setMenuOpen(true)} className="rounded-xl p-2 text-ink-500 hover:bg-ink-100 hover:text-ink-900" aria-label="Open menu" aria-expanded={menuOpen}>
            <Icon name="menu" />
          </button>
          {user && (
            <Link to="/settings" aria-label="Your profile" className="ml-1 rounded-full">
              <Avatar firstName={user.firstName} lastName={user.lastName} className="h-9 w-9" />
            </Link>
          )}
        </div>
      </header>

      {menuOpen && (
        <div className="fixed inset-0 z-40 lg:hidden" role="dialog" aria-modal="true" aria-label="Menu">
          <div className="absolute inset-0 bg-black/55 backdrop-blur-[2px]" onClick={() => setMenuOpen(false)} aria-hidden="true" />
          <div className="absolute inset-y-0 left-0 flex w-76 max-w-[85vw] animate-rise flex-col justify-between overflow-y-auto rounded-r-3xl bg-surface px-4 py-6 shadow-lift">
            <div className="flex flex-col gap-7">
              <div className="flex items-center justify-between px-2">
                <Logo />
                <button type="button" onClick={() => setMenuOpen(false)} className="rounded-xl p-1.5 text-ink-500 hover:bg-ink-100" aria-label="Close menu">
                  <Icon name="close" />
                </button>
              </div>
              <NavLinks onNavigate={() => setMenuOpen(false)} />
            </div>
            <div className="pt-6">
              <UserBox />
            </div>
          </div>
        </div>
      )}

      <main id="main" className="mx-auto w-full max-w-6xl min-w-0 flex-1 px-4 pt-2 pb-32 sm:px-6 lg:px-10 lg:py-10">
        <Suspense fallback={<LoadingState />}>
          <Outlet />
        </Suspense>
      </main>

      <TabBar />
    </div>
  );
}
