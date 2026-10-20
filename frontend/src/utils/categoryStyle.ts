import type { IconName } from '../components/ui/Icon';

// Visual identity for categories and goals: a line icon picked from the name, and the stored
// colour softened into a pastel so every category reads well on the midnight background.

const categoryIcons: [RegExp, IconName][] = [
  [/grocer|supermarket/i, 'cart'],
  [/dining|restaurant|food|coffee|take ?out/i, 'utensils'],
  [/transport|transit|bus\b/i, 'bus'],
  [/gas|fuel|car\b|parking/i, 'car'],
  [/shop|cloth/i, 'bag'],
  [/hous|rent|mortgage|home/i, 'home'],
  [/subscri|stream/i, 'tv'],
  [/entertain|movie|game/i, 'film'],
  [/health|medical|pharm|fitness/i, 'heart'],
  [/educat|tuition|school|course/i, 'graduation'],
  [/util|hydro|electric|internet|phone/i, 'bolt'],
  [/insur/i, 'shield'],
  [/travel|trip|vacation/i, 'plane'],
  [/personal|care|beauty/i, 'sparkle'],
  [/salary|pay|wage/i, 'briefcase'],
  [/freelance|contract/i, 'laptop'],
  [/invest|dividend|interest/i, 'trendUp'],
  [/gift/i, 'gift'],
  [/refund/i, 'repeat'],
];

export function categoryIcon(name: string | null | undefined): IconName {
  if (!name) return 'tag';
  return categoryIcons.find(([pattern]) => pattern.test(name))?.[1] ?? 'tag';
}

const goalIcons: [RegExp, IconName][] = [
  [/emergency|rainy|safety/i, 'shield'],
  [/laptop|computer|phone|tech/i, 'laptop'],
  [/vacation|trip|travel|holiday/i, 'plane'],
  [/tuition|school|educat|college|universit/i, 'graduation'],
  [/car|vehicle/i, 'car'],
  [/home|house|down ?payment|rent/i, 'home'],
  [/wedding|gift/i, 'gift'],
];

export function goalIcon(name: string): IconName {
  return goalIcons.find(([pattern]) => pattern.test(name))?.[1] ?? 'piggy';
}

function parseHex(hex: string): [number, number, number] | null {
  const match = /^#?([0-9a-f]{6})$/i.exec(hex.trim());
  if (!match) return null;
  const n = parseInt(match[1]!, 16);
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
}

/** The colour mixed 40 % toward warm white: a pastel that keeps the category's hue. */
export function pastel(hex: string | null | undefined): string {
  const rgb = parseHex(hex ?? '');
  if (!rgb) return '#a9b6cc';
  const mix = (c: number) => Math.round(c + (248 - c) * 0.4);
  return `rgb(${mix(rgb[0])} ${mix(rgb[1])} ${mix(rgb[2])})`;
}

/** A translucent tint of the colour for icon tiles and selected cards. */
export function tint(hex: string | null | undefined, alpha = 0.18): string {
  const rgb = parseHex(hex ?? '') ?? [169, 182, 204];
  return `rgb(${rgb[0]} ${rgb[1]} ${rgb[2]} / ${alpha})`;
}

/** Pastel swatches offered when creating a category (stored as hex). */
export const CATEGORY_SWATCHES = ['#F28B88', '#83C9A5', '#90BFF2', '#F2C27B', '#B9A3F0', '#F5A3C7', '#7FD1D8', '#C9D97E', '#F4A877', '#A9B6CC'];
