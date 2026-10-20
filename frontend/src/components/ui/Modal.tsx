import { useEffect, useId, useRef, type ReactNode } from 'react';
import { Icon } from './Icon';

interface ModalProps {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
}

/**
 * Accessible dialog: a bottom sheet on phones, a centred card on larger screens. Labelled by its
 * title, closes on Escape or backdrop click, moves focus in when opened and back when closed.
 */
export function Modal({ open, title, onClose, children }: ModalProps) {
  const titleId = useId();
  const panel = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const previouslyFocused = document.activeElement as HTMLElement | null;
    const first = panel.current?.querySelector<HTMLElement>('input, select, textarea, button:not([data-close])');
    first?.focus();
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('keydown', onKey);
      previouslyFocused?.focus();
    };
  }, [open, onClose]);

  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center sm:p-4">
      <div className="absolute inset-0 bg-black/55 backdrop-blur-[2px]" onClick={onClose} aria-hidden="true" />
      <div
        ref={panel}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className="relative max-h-[90dvh] w-full animate-sheet overflow-y-auto rounded-t-3xl bg-surface p-6 pb-[max(1.5rem,env(safe-area-inset-bottom))] shadow-lift ring-1 ring-line/50 sm:max-w-lg sm:rounded-3xl sm:pb-6"
      >
        <div className="mx-auto -mt-2 mb-3 h-1 w-10 rounded-full bg-ink-200 sm:hidden" aria-hidden="true" />
        <div className="mb-5 flex items-center justify-between">
          <h2 id={titleId} className="text-lg font-bold text-ink-900">
            {title}
          </h2>
          <button type="button" data-close onClick={onClose} className="rounded-full p-1.5 text-ink-500 hover:bg-ink-100 hover:text-ink-900" aria-label="Close">
            <Icon name="close" />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}
