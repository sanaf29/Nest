import { useState } from 'react';
import { CategoryForm } from '../components/forms/CategoryForm';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { Icon } from '../components/ui/Icon';
import { Modal } from '../components/ui/Modal';
import { PageHeader } from '../components/ui/PageHeader';
import { ErrorState, LoadingState } from '../components/ui/States';
import { useAsync } from '../hooks/useAsync';
import { categoryService } from '../services/categoryService';
import type { CategoryResponse, CategoryType } from '../types/api';

export function CategoriesPage() {
  const [showArchived, setShowArchived] = useState(false);
  const categories = useAsync(() => categoryService.list(undefined, showArchived), [showArchived]);
  const [editing, setEditing] = useState<CategoryResponse | CategoryType | null>(null);
  const [archiving, setArchiving] = useState<CategoryResponse | null>(null);

  const section = (type: CategoryType, title: string) => {
    const items = (categories.data ?? []).filter((c) => c.categoryType === type);
    return (
      <Card
        title={title}
        bodyClassName="p-0"
        action={
          <Button size="sm" variant="secondary" icon={<Icon name="plus" className="h-4 w-4" />} onClick={() => setEditing(type)}>
            Add
          </Button>
        }
      >
        <ul className="divide-y divide-ink-100">
          {items.map((c) => (
            <li key={c.id} className="flex items-center justify-between gap-3 px-5 py-3">
              <span className="flex min-w-0 items-center gap-3">
                <span className="h-3 w-3 shrink-0 rounded-full" style={{ backgroundColor: c.color }} aria-hidden="true" />
                <span className="truncate text-sm text-ink-800">{c.name}</span>
                {c.isDefault && <Badge>Default</Badge>}
                {c.archived && <Badge tone="warning">Archived</Badge>}
              </span>
              {!c.isDefault && !c.archived && (
                <span className="flex gap-1">
                  <button type="button" onClick={() => setEditing(c)} className="rounded-lg p-1.5 text-ink-500 hover:bg-ink-100" aria-label={`Edit ${c.name}`}>
                    <Icon name="edit" className="h-4 w-4" />
                  </button>
                  <button type="button" onClick={() => setArchiving(c)} className="rounded-lg p-1.5 text-ink-500 hover:bg-ink-100" aria-label={`Archive ${c.name}`}>
                    <Icon name="trash" className="h-4 w-4" />
                  </button>
                </span>
              )}
            </li>
          ))}
        </ul>
      </Card>
    );
  };

  return (
    <>
      <PageHeader
        title="Categories"
        description="Default categories are shared by everyone and can't be changed. Add your own for anything else."
        actions={
          <label className="flex items-center gap-2 text-sm text-ink-600">
            <input type="checkbox" checked={showArchived} onChange={(e) => setShowArchived(e.target.checked)} className="rounded border-ink-300" />
            Show archived
          </label>
        }
      />
      {categories.loading && !categories.data ? (
        <LoadingState />
      ) : categories.error ? (
        <ErrorState error={categories.error} onRetry={categories.reload} />
      ) : (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          {section('EXPENSE', 'Expense categories')}
          {section('INCOME', 'Income categories')}
        </div>
      )}

      <Modal open={editing !== null} title={typeof editing === 'object' && editing ? 'Edit category' : 'Add category'} onClose={() => setEditing(null)}>
        {editing !== null && (
          <CategoryForm
            category={typeof editing === 'object' ? editing : undefined}
            defaultType={typeof editing === 'string' ? editing : 'EXPENSE'}
            onSaved={() => {
              setEditing(null);
              categories.reload();
            }}
            onCancel={() => setEditing(null)}
          />
        )}
      </Modal>
      <ConfirmDialog
        open={archiving !== null}
        title="Archive category?"
        message={`"${archiving?.name}" won't be offered for new transactions. Existing transactions keep it.`}
        confirmLabel="Archive"
        onConfirm={async () => {
          await categoryService.archive(archiving!.id);
          categories.reload();
        }}
        onClose={() => setArchiving(null)}
      />
    </>
  );
}
