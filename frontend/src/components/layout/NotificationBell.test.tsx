import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { NotificationBell } from './NotificationBell';
import { notificationService } from '../../services/reminderService';

vi.mock('../../services/reminderService', () => ({ notificationService: { unreadCount: vi.fn() } }));

describe('NotificationBell', () => {
  it('announces the unread count', async () => {
    vi.mocked(notificationService.unreadCount).mockResolvedValue(3);
    render(<MemoryRouter><NotificationBell /></MemoryRouter>);

    expect(await screen.findByRole('link', { name: 'Notifications, 3 unread' })).toHaveAttribute('href', '/notifications');
  });

  it('shows no badge when everything is read', async () => {
    vi.mocked(notificationService.unreadCount).mockResolvedValue(0);
    render(<MemoryRouter><NotificationBell /></MemoryRouter>);

    expect(await screen.findByRole('link', { name: 'Notifications' })).toBeInTheDocument();
  });
});
