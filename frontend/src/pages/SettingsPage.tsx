import { useState, type FormEvent } from 'react';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { SelectField, TextField } from '../components/ui/Field';
import { PageHeader } from '../components/ui/PageHeader';
import { FormError } from '../components/ui/States';
import { useAuth } from '../hooks/useAuth';
import { useFormSubmit } from '../hooks/useFormSubmit';
import { authService } from '../services/authService';
import type { FieldErrors } from '../utils/errors';
import { COMMON_CURRENCIES } from '../utils/money';

function ProfileForm() {
  const { user, updateUser } = useAuth();
  const [firstName, setFirstName] = useState(user?.firstName ?? '');
  const [lastName, setLastName] = useState(user?.lastName ?? '');
  const [currency, setCurrency] = useState(user?.preferredCurrency ?? 'CAD');
  const [saved, setSaved] = useState(false);
  const { submitting, formError, fieldErrors, submit } = useFormSubmit();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSaved(false);
    const errors: FieldErrors = {};
    if (!firstName.trim()) errors.firstName = 'Required.';
    if (!lastName.trim()) errors.lastName = 'Required.';
    const ok = await submit(errors, async () => {
      updateUser(await authService.updateProfile({ firstName: firstName.trim(), lastName: lastName.trim(), preferredCurrency: currency }));
    });
    setSaved(ok);
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      <FormError message={formError} />
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <TextField label="First name" value={firstName} onChange={(e) => setFirstName(e.target.value)} error={fieldErrors.firstName} />
        <TextField label="Last name" value={lastName} onChange={(e) => setLastName(e.target.value)} error={fieldErrors.lastName} />
      </div>
      <TextField label="Email" value={user?.email ?? ''} disabled hint="Your sign-in email can't be changed yet." />
      <SelectField label="Preferred currency" value={currency} onChange={(e) => setCurrency(e.target.value)}
        hint="Dashboards, budgets, and new accounts use this currency. Existing accounts keep theirs.">
        {[...new Set([currency, ...COMMON_CURRENCIES])].map((c) => <option key={c}>{c}</option>)}
      </SelectField>
      <div className="flex items-center justify-end gap-3">
        {saved && <span role="status" className="text-sm text-positive-600">Saved</span>}
        <Button type="submit" loading={submitting}>Save profile</Button>
      </div>
    </form>
  );
}

export function validatePasswordChange(current: string, next: string, confirm: string): FieldErrors {
  const e: FieldErrors = {};
  if (!current) e.currentPassword = 'Enter your current password.';
  if (next.length < 8 || next.length > 72) e.newPassword = 'Use 8 to 72 characters.';
  else if (!/\p{L}/u.test(next) || !/\d/.test(next)) e.newPassword = 'Include at least one letter and one number.';
  else if (next === current) e.newPassword = 'Choose a different password.';
  if (confirm !== next) e.confirm = 'Passwords don’t match.';
  return e;
}

function PasswordForm() {
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [confirm, setConfirm] = useState('');
  const [done, setDone] = useState(false);
  const { submitting, formError, fieldErrors, submit } = useFormSubmit();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setDone(false);
    const ok = await submit(validatePasswordChange(current, next, confirm), () =>
      authService.changePassword({ currentPassword: current, newPassword: next }));
    if (ok) {
      setCurrent('');
      setNext('');
      setConfirm('');
      setDone(true);
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      <FormError message={formError} />
      <TextField label="Current password" type="password" autoComplete="current-password" value={current} onChange={(e) => setCurrent(e.target.value)} error={fieldErrors.currentPassword} />
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <TextField label="New password" type="password" autoComplete="new-password" value={next} onChange={(e) => setNext(e.target.value)} error={fieldErrors.newPassword} />
        <TextField label="Confirm new password" type="password" autoComplete="new-password" value={confirm} onChange={(e) => setConfirm(e.target.value)} error={fieldErrors.confirm} />
      </div>
      <div className="flex items-center justify-end gap-3">
        {done && <span role="status" className="text-sm text-positive-600">Password changed</span>}
        <Button type="submit" loading={submitting}>Change password</Button>
      </div>
    </form>
  );
}

export function SettingsPage() {
  return (
    <>
      <PageHeader title="Settings" />
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card title="Profile"><ProfileForm /></Card>
        <Card title="Password" description="You'll stay signed in on this device."><PasswordForm /></Card>
      </div>
    </>
  );
}
