import { accountService } from '../services/accountService';
import { categoryService } from '../services/categoryService';
import { useAsync } from './useAsync';

/** Open accounts and visible categories, needed by most forms. */
export function useReferenceData() {
  const accounts = useAsync(() => accountService.list(false), []);
  const categories = useAsync(() => categoryService.list(), []);
  return {
    accounts: accounts.data ?? [],
    categories: categories.data ?? [],
    loading: accounts.loading || categories.loading,
    error: accounts.error ?? categories.error,
    reload: () => {
      accounts.reload();
      categories.reload();
    },
  };
}
