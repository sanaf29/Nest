import { useState } from 'react';
import { toApiError, type FieldErrors } from '../utils/errors';

/**
 * Shared submit handling for forms: client errors first; if they pass, call the server and map
 * its RFC 7807 response onto field errors and a form-level message.
 */
export function useFormSubmit() {
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

  async function submit(clientErrors: FieldErrors, action: () => Promise<void>): Promise<boolean> {
    setFieldErrors(clientErrors);
    setFormError(null);
    if (Object.keys(clientErrors).length > 0) return false;
    setSubmitting(true);
    try {
      await action();
      return true;
    } catch (error) {
      const apiError = toApiError(error);
      setFieldErrors(apiError.fieldErrors);
      setFormError(apiError.message);
      return false;
    } finally {
      setSubmitting(false);
    }
  }

  return { submitting, formError, fieldErrors, submit };
}
