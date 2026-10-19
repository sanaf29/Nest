import { validateRegistration } from './RegisterPage';

const valid = { firstName: 'Sanae', lastName: 'F', email: 'sanae@example.com', password: 'Str0ngPass', preferredCurrency: 'CAD' };

describe('registration validation', () => {
  it('accepts a valid form', () => {
    expect(validateRegistration(valid)).toEqual({});
  });

  it('mirrors the server password policy', () => {
    expect(validateRegistration({ ...valid, password: 'short1' }).password).toMatch(/8 to 72/);
    expect(validateRegistration({ ...valid, password: 'onlyletters' }).password).toMatch(/letter and one number/);
    expect(validateRegistration({ ...valid, password: '12345678' }).password).toMatch(/letter and one number/);
    expect(validateRegistration({ ...valid, password: 'x1'.repeat(37) }).password).toMatch(/8 to 72/);
  });

  it('requires names and a plausible email', () => {
    const errors = validateRegistration({ ...valid, firstName: ' ', email: 'not-an-email' });
    expect(Object.keys(errors).sort()).toEqual(['email', 'firstName']);
  });
});
