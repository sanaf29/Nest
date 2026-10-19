import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router';
import { AuthLayout } from '../components/layout/AuthLayout';
import { Button } from '../components/ui/Button';
import { SelectField, TextField } from '../components/ui/Field';
import { FormError } from '../components/ui/States';
import { useAuth } from '../hooks/useAuth';
import { toApiError, type FieldErrors } from '../utils/errors';
import { COMMON_CURRENCIES } from '../utils/money';

interface FormState {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  preferredCurrency: string;
}

/** Mirrors the server rules so most mistakes are caught before a round trip. The server still validates. */
export function validateRegistration(form: FormState): FieldErrors {
  const errors: FieldErrors = {};
  if (!form.firstName.trim()) errors.firstName = 'Enter your first name.';
  if (!form.lastName.trim()) errors.lastName = 'Enter your last name.';
  if (!/^\S+@\S+\.\S+$/.test(form.email.trim())) errors.email = 'Enter a valid email address.';
  if (form.password.length < 8 || form.password.length > 72) {
    errors.password = 'Use 8 to 72 characters.';
  } else if (!/\p{L}/u.test(form.password) || !/\d/.test(form.password)) {
    errors.password = 'Include at least one letter and one number.';
  }
  return errors;
}

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState<FormState>({ firstName: '', lastName: '', email: '', password: '', preferredCurrency: 'CAD' });
  const [errors, setErrors] = useState<FieldErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const set = (field: keyof FormState) => (value: string) => setForm((f) => ({ ...f, [field]: value }));

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const nextErrors = validateRegistration(form);
    setErrors(nextErrors);
    setFormError(null);
    if (Object.keys(nextErrors).length > 0) return;

    setSubmitting(true);
    try {
      await register({ ...form, email: form.email.trim() });
      navigate('/', { replace: true });
    } catch (error) {
      const apiError = toApiError(error);
      setErrors(apiError.fieldErrors);
      setFormError(apiError.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout
      title="Create your Nest"
      subtitle="Track accounts, plan budgets, and grow your savings."
      footer={
        <>
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-brand-700 hover:underline">
            Log in
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
        <FormError message={formError} />
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <TextField label="First name" autoComplete="given-name" value={form.firstName} onChange={(e) => set('firstName')(e.target.value)} error={errors.firstName} />
          <TextField label="Last name" autoComplete="family-name" value={form.lastName} onChange={(e) => set('lastName')(e.target.value)} error={errors.lastName} />
        </div>
        <TextField label="Email" type="email" autoComplete="email" value={form.email} onChange={(e) => set('email')(e.target.value)} error={errors.email} />
        <TextField
          label="Password"
          type="password"
          autoComplete="new-password"
          value={form.password}
          onChange={(e) => set('password')(e.target.value)}
          error={errors.password}
          hint="At least 8 characters, with a letter and a number."
        />
        <SelectField
          label="Preferred currency"
          value={form.preferredCurrency}
          onChange={(e) => set('preferredCurrency')(e.target.value)}
          error={errors.preferredCurrency}
          hint="New accounts and budgets default to this currency."
        >
          {COMMON_CURRENCIES.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </SelectField>
        <Button type="submit" loading={submitting} className="mt-2 w-full">
          Create account
        </Button>
      </form>
    </AuthLayout>
  );
}
