import { crossedMilestone } from './GoalsPage';

describe('goal milestones', () => {
  it('reports the highest milestone passed by a contribution', () => {
    expect(crossedMilestone(10, 30)).toBe(25);
    expect(crossedMilestone(20, 80)).toBe(75);
    expect(crossedMilestone(90, 100)).toBe(100);
    expect(crossedMilestone(30, 45)).toBeNull();
    expect(crossedMilestone(50, 50)).toBeNull();
  });
});
