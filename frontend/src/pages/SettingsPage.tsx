import { useState, type FormEvent } from 'react';
import { Link } from 'react-router';
import { Avatar } from '../components/ui/Avatar';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { SelectField, TextField } from '../components/ui/Field';
import { Icon, type IconName } from '../components/ui/Icon';
import { PageHeader } from '../components/ui/PageHeader';
import { SegmentedControl } from '../components/ui/SegmentedControl';
import { FormError } from '../components/ui/States';
import { useAuth } from '../hooks/useAuth';
import { useFormSubmit } from '../hooks/useFormSubmit';
import { authService } from '../services/authService';
import { reportService } from '../services/reportService';
import { currentYearMonth } from '../utils/dates';
import { toApiError, type FieldErrors } from '../utils/errors';
import { readThemePreference, setThemePreference, type ThemePreference } from '../utils/theme';
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

function ThemeSetting() {
  const [preference, setPreference] = useState<ThemePreference>(readThemePreference);
  return (
    <SegmentedControl
      label="Theme"
      value={preference}
      onChange={(next) => {
        setPreference(next);
        setThemePreference(next);
      }}
      options={[
        { value: 'dark', label: 'Dark' },
        { value: 'light', label: 'Light' },
        { value: 'system', label: 'System' },
      ]}
    />
  );
}

function SettingLink({ to, icon, label, hint }: { to: string; icon: IconName; label: string; hint: string }) {
  return (
    <Link to={to} className="flex items-center gap-3 rounded-2xl px-3 py-3 transition-colors hover:bg-ink-100/60">
      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-accent-100 text-accent-600" aria-hidden="true">
        <Icon name={icon} className="h-4.5 w-4.5" />
      </span>
      <span className="min-w-0 flex-1">
        <span className="block text-sm font-semibold text-ink-900">{label}</span>
        <span className="block truncate text-xs text-ink-500">{hint}</span>
      </span>
      <Icon name="chevronRight" className="h-4 w-4 shrink-0 text-ink-400" />
    </Link>
  );
}

function ExportData({ currency }: { currency: string }) {
  const [busy, setBusy] = useState<'all' | 'month' | null>(null);
  const [error, setError] = useState<string | null>(null);
  async function run(kind: 'all' | 'month') {
    setBusy(kind);
    setError(null);
    try {
      const { year, month } = currentYearMonth();
      if (kind === 'all') await reportService.downloadTransactionsCsv({});
      else await reportService.downloadMonthlyCsv(year, month, currency);
    } catch (e) {
      setError(toApiError(e).message);
    } finally {
      setBusy(null);
    }
  }
  return (
    <div className="flex flex-col gap-3">
      <FormError message={error} />
      <p className="text-sm text-ink-500">Download a spreadsheet-ready CSV. Files are generated on demand and never stored.</p>
      <div className="flex flex-wrap gap-2">
        <Button variant="secondary" icon={<Icon name="download" className="h-4 w-4" />} loading={busy === 'all'} onClick={() => run('all')}>
          All transactions
        </Button>
        <Button variant="secondary" icon={<Icon name="download" className="h-4 w-4" />} loading={busy === 'month'} onClick={() => run('month')}>
          This month’s report
        </Button>
      </div>
    </div>
  );
}

export function SettingsPage() {
  const { user, logout } = useAuth();
  return (
    <>
      <PageHeader title="Profile & settings" />
      <div className="flex flex-col gap-6">
        {user && (
          <section aria-label="Your profile" className="card-hero flex animate-rise items-center gap-4 rounded-3xl p-5 shadow-card">
            <Avatar firstName={user.firstName} lastName={user.lastName} className="h-16 w-16 text-xl ring-4 ring-white/20" />
            <div className="min-w-0 flex-1">
              <p className="truncate text-xl font-bold">
                {user.firstName} {user.lastName}
              </p>
              <p className="truncate text-sm text-white/75">{user.email}</p>
              <p className="mt-1 text-xs text-white/60">
                Member since {new Date(user.createdAt).toLocaleDateString(undefined, { month: 'long', year: 'numeric' })} · {user.preferredCurrency}
              </p>
            </div>
          </section>
        )}

        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <Card title="Profile & currency" description="Your name and the currency Nest reports in.">
            <ProfileForm />
          </Card>
          <div className="flex flex-col gap-6">
            <Card title="Appearance" description="Dark is easiest on the eyes at night.">
              <ThemeSetting />
            </Card>
            <Card title="Budget preferences" bodyClassName="p-2">
              <SettingLink to="/budget" icon="pie" label="Monthly budget" hint="Income, savings rule, and category limits" />
              <SettingLink to="/categories" icon="tag" label="Categories" hint="Names and colours for your spending" />
              <SettingLink to="/recurring" icon="repeat" label="Recurring transactions" hint="Rent, pay, and subscriptions on autopilot" />
            </Card>
            <Card title="Notifications" bodyClassName="p-2">
              <p className="px-3 pt-1 pb-2 text-sm text-ink-500">Nest notifies you in the app (the bell) for reminders, budget limits, recurring bills, and goals.</p>
              <SettingLink to="/notifications" icon="bell" label="Notification inbox" hint="Everything Nest has told you" />
              <SettingLink to="/reminders" icon="calendar" label="Reminders" hint="Bills, renewals, parking, and more" />
            </Card>
          </div>
          <Card title="Security" description="Change your password. You’ll stay signed in on this device.">
            <PasswordForm />
          </Card>
          <div className="flex flex-col gap-6">
            <Card title="Export your data">
              <ExportData currency={user?.preferredCurrency ?? 'CAD'} />
            </Card>
            <Card title="More tools" bodyClassName="p-2">
              <SettingLink to="/accounts" icon="wallet" label="Accounts" hint="Chequing, savings, cash, and cards" />
              <SettingLink to="/goals" icon="target" label="Savings goals" hint="Emergency fund, trips, tuition" />
              <SettingLink to="/shared" icon="users" label="Split expenses" hint="Share costs and settle up" />
              <SettingLink to="/reports" icon="file" label="Reports" hint="Monthly summaries and CSV exports" />
            </Card>
          </div>
        </div>

        <Button variant="secondary" size="lg" icon={<Icon name="logout" className="h-5 w-5" />} onClick={logout} className="w-full text-negative-600 lg:w-auto lg:self-start">
          Sign out
        </Button>
      </div>
    </>
  );
}
