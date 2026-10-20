import type { IconName } from '../ui/Icon';

export interface NavItem {
  to: string;
  label: string;
  icon: IconName;
}

export interface NavSection {
  label?: string;
  items: NavItem[];
}

/** The app's sections. Adding a page means adding a route in App.tsx and an entry here. */
export const navigation: NavSection[] = [
  {
    items: [
      { to: '/', label: 'Dashboard', icon: 'dashboard' },
      { to: '/accounts', label: 'Accounts', icon: 'wallet' },
      { to: '/transactions', label: 'Transactions', icon: 'list' },
      { to: '/calendar', label: 'Calendar', icon: 'calendar' },
    ],
  },
  {
    label: 'Planning',
    items: [
      { to: '/budget', label: 'Budget', icon: 'pie' },
      { to: '/goals', label: 'Savings goals', icon: 'target' },
      { to: '/recurring', label: 'Recurring', icon: 'repeat' },
      { to: '/categories', label: 'Categories', icon: 'tag' },
    ],
  },
  {
    label: 'Tools',
    items: [
      { to: '/insights', label: 'Insights', icon: 'bulb' },
      { to: '/reminders', label: 'Reminders', icon: 'bell' },
      { to: '/shared', label: 'Split expenses', icon: 'users' },
      { to: '/reports', label: 'Reports', icon: 'file' },
    ],
  },
];
