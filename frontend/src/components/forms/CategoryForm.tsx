import { useState, type FormEvent } from 'react';
import { Button } from '../ui/Button';
import { SelectField, TextField } from '../ui/Field';
import { CATEGORY_SWATCHES } from '../../utils/categoryStyle';
import { FormError } from '../ui/States';
import { useFormSubmit } from '../../hooks/useFormSubmit';
import { categoryService } from '../../services/categoryService';
import type { CategoryResponse, CategoryType } from '../../types/api';
import type { FieldErrors } from '../../utils/errors';

const palette = CATEGORY_SWATCHES;

export function CategoryForm({ category, defaultType = 'EXPENSE', onSaved, onCancel }: { category?: CategoryResponse; defaultType?: CategoryType; onSaved: () => void; onCancel: () => void }) {
  const [name, setName] = useState(category?.name ?? '');
  const [type, setType] = useState<CategoryType>(category?.categoryType ?? defaultType);
  const [color, setColor] = useState(category?.color ?? palette[0]!);
  const { submitting, formError, fieldErrors, submit } = useFormSubmit();

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const errors: FieldErrors = name.trim() ? {} : { name: 'Give the category a name.' };
    await submit(errors, async () => {
      if (category) await categoryService.update(category.id, { name: name.trim(), color });
      else await categoryService.create({ name: name.trim(), categoryType: type, color });
      onSaved();
    });
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      <FormError message={formError} />
      <TextField label="Name" value={name} onChange={(e) => setName(e.target.value)} error={fieldErrors.name} maxLength={50} placeholder="Pets" />
      <SelectField label="Type" value={type} onChange={(e) => setType(e.target.value as CategoryType)} disabled={!!category} hint={category ? 'Fixed once created' : undefined}>
        <option value="EXPENSE">Expense</option>
        <option value="INCOME">Income</option>
      </SelectField>
      <fieldset>
        <legend className="mb-2 text-sm font-medium text-ink-700">Colour</legend>
        <div className="flex flex-wrap gap-2">
          {palette.map((swatch) => (
            <button
              key={swatch}
              type="button"
              onClick={() => setColor(swatch)}
              aria-label={`Colour ${swatch}`}
              aria-pressed={color.toUpperCase() === swatch}
              className="h-8 w-8 rounded-full ring-offset-2 aria-pressed:ring-2 aria-pressed:ring-ink-800"
              style={{ backgroundColor: swatch }}
            />
          ))}
        </div>
      </fieldset>
      <div className="flex justify-end gap-2 pt-2">
        <Button variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" loading={submitting}>
          {category ? 'Save changes' : 'Add category'}
        </Button>
      </div>
    </form>
  );
}
