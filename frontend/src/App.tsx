import { lazy } from 'react';
import { createBrowserRouter, RouterProvider } from 'react-router';
import { AppLayout } from './components/layout/AppLayout';
import { GuestRoute, ProtectedRoute } from './components/layout/ProtectedRoute';
import { AuthProvider } from './context/AuthProvider';
import { LoginPage } from './pages/LoginPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { RegisterPage } from './pages/RegisterPage';

// Each page below is its own chunk (Recharts and large forms load only when needed).
// Login and registration stay in the main bundle so the first screen paints immediately.
const page = <T,>(load: () => Promise<T>, name: keyof T) =>
  lazy(() => load().then((m) => ({ default: m[name] as unknown as React.ComponentType })));

const DashboardPage = page(() => import('./pages/DashboardPage'), 'DashboardPage');
const AccountsPage = page(() => import('./pages/AccountsPage'), 'AccountsPage');
const AccountDetailPage = page(() => import('./pages/AccountDetailPage'), 'AccountDetailPage');
const TransactionsPage = page(() => import('./pages/TransactionsPage'), 'TransactionsPage');
const CalendarPage = page(() => import('./pages/CalendarPage'), 'CalendarPage');
const BudgetPage = page(() => import('./pages/BudgetPage'), 'BudgetPage');
const GoalsPage = page(() => import('./pages/GoalsPage'), 'GoalsPage');
const CategoriesPage = page(() => import('./pages/CategoriesPage'), 'CategoriesPage');
const RecurringPage = page(() => import('./pages/RecurringPage'), 'RecurringPage');
const InsightsPage = page(() => import('./pages/InsightsPage'), 'InsightsPage');
const RemindersPage = page(() => import('./pages/RemindersPage'), 'RemindersPage');
const NotificationsPage = page(() => import('./pages/NotificationsPage'), 'NotificationsPage');
const SharedExpensesPage = page(() => import('./pages/SharedExpensesPage'), 'SharedExpensesPage');
const ReportsPage = page(() => import('./pages/ReportsPage'), 'ReportsPage');
const SettingsPage = page(() => import('./pages/SettingsPage'), 'SettingsPage');

export const routes = [
  {
    element: <GuestRoute />,
    children: [
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
    ],
  },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { path: '/', element: <DashboardPage /> },
          { path: '/accounts', element: <AccountsPage /> },
          { path: '/accounts/:id', element: <AccountDetailPage /> },
          { path: '/transactions', element: <TransactionsPage /> },
          { path: '/calendar', element: <CalendarPage /> },
          { path: '/budget', element: <BudgetPage /> },
          { path: '/goals', element: <GoalsPage /> },
          { path: '/categories', element: <CategoriesPage /> },
          { path: '/recurring', element: <RecurringPage /> },
          { path: '/insights', element: <InsightsPage /> },
          { path: '/reminders', element: <RemindersPage /> },
          { path: '/notifications', element: <NotificationsPage /> },
          { path: '/shared', element: <SharedExpensesPage /> },
          { path: '/reports', element: <ReportsPage /> },
          { path: '/settings', element: <SettingsPage /> },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
];

const router = createBrowserRouter(routes);

export function App() {
  return (
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>
  );
}
