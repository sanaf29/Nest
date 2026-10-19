import { Link } from 'react-router';
import { EmptyState } from '../components/ui/States';

export function NotFoundPage() {
  return (
    <EmptyState icon="search" title="Page not found" action={<Link to="/" className="text-sm font-medium text-brand-700 hover:underline">Go to the dashboard</Link>}>
      The page you are looking for does not exist.
    </EmptyState>
  );
}
