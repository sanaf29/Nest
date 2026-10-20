import { categoryIcon, goalIcon, pastel, tint } from './categoryStyle';

describe('category style', () => {
  it('picks icons from names', () => {
    expect(categoryIcon('Groceries')).toBe('cart');
    expect(categoryIcon('Dining Out')).toBe('utensils');
    expect(categoryIcon('Something new')).toBe('tag');
    expect(categoryIcon(null)).toBe('tag');
    expect(goalIcon('Emergency Fund')).toBe('shield');
    expect(goalIcon('New Laptop')).toBe('laptop');
    expect(goalIcon('Bike')).toBe('piggy');
  });

  it('softens colours and tolerates bad input', () => {
    expect(pastel('#000000')).toBe('rgb(99 99 99)');
    expect(pastel('nope')).toBe('#a9b6cc');
    expect(tint('#ff0000', 0.5)).toBe('rgb(255 0 0 / 0.5)');
  });
});
