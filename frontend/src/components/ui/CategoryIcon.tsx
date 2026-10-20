import { Icon, type IconName } from './Icon';
import { categoryIcon, pastel, tint } from '../../utils/categoryStyle';
import { cn } from '../../utils/cn';

/** A rounded tile with the category's icon in its pastel colour. */
export function CategoryIcon({ name, color, icon, className }: { name: string | null; color?: string | null; icon?: IconName; className?: string }) {
  return (
    <span
      className={cn('flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl', className)}
      style={{ backgroundColor: tint(color), color: pastel(color) }}
      aria-hidden="true"
    >
      <Icon name={icon ?? categoryIcon(name)} className="h-5 w-5" />
    </span>
  );
}
