import { createBrowserRouter, RouterProvider } from 'react-router';
import { AppLayout } from './components/layout/AppLayout';
import { GuestRoute, ProtectedRoute } from './components/layout/ProtectedRoute';
import { AuthProvider } from './context/AuthProvider';
import { AccountDetailPage } from './pages/AccountDetailPage';
import { AccountsPage } from './pages/AccountsPage';
import { BudgetPage } from './pages/BudgetPage';
import { CalendarPage } from './pages/CalendarPage';
import { CategoriesPage } from './pages/CategoriesPage';
import { DashboardPage } from './pages/DashboardPage';
import { GoalsPage } from './pages/GoalsPage';
import { LoginPage } from './pages/LoginPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { RegisterPage } from './pages/RegisterPage';
import { TransactionsPage } from './pages/TransactionsPage';

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
