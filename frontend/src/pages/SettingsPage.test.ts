import { validatePasswordChange } from './SettingsPage';

describe('password change validation', () => {
  it('mirrors the server rules', () => {
    expect(validatePasswordChange('Old1pass', 'N3wPassword', 'N3wPassword')).toEqual({});
    expect(validatePasswordChange('', 'N3wPassword', 'N3wPassword').currentPassword).toBeDefined();
    expect(validatePasswordChange('Old1pass', 'short1', 'short1').newPassword).toMatch(/8 to 72/);
    expect(validatePasswordChange('Old1pass', 'Old1pass', 'Old1pass').newPassword).toMatch(/different/);
    expect(validatePasswordChange('Old1pass', 'N3wPassword', 'N3wPasswordX').confirm).toMatch(/match/);
  });
});
