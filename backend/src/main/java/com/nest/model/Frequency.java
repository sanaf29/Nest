package com.nest.model;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * How often a recurring transaction repeats. Each constant computes the next date itself.
 *
 * <p>Month-based schedules keep their <em>anchor day</em>: a monthly rule on the 31st falls on
 * 30 April, 28/29 February, and back on 31 March. A yearly rule on 29 February falls on
 * 28 February in non-leap years. Clamping each step from the anchor (rather than from the
 * previous, possibly clamped, date) is what stops a rule from drifting to the 28th forever.
 */
public enum Frequency {
    WEEKLY {
        @Override
        public LocalDate next(LocalDate current, int anchorDay) {
            return current.plusWeeks(1);
        }
    },
    BIWEEKLY {
        @Override
        public LocalDate next(LocalDate current, int anchorDay) {
            return current.plusWeeks(2);
        }
    },
    MONTHLY {
        @Override
        public LocalDate next(LocalDate current, int anchorDay) {
            return addMonths(current, 1, anchorDay);
        }
    },
    QUARTERLY {
        @Override
        public LocalDate next(LocalDate current, int anchorDay) {
            return addMonths(current, 3, anchorDay);
        }
    },
    YEARLY {
        @Override
        public LocalDate next(LocalDate current, int anchorDay) {
            return addMonths(current, 12, anchorDay);
        }
    };

    /** The occurrence after {@code current}. */
    public abstract LocalDate next(LocalDate current, int anchorDay);

    private static LocalDate addMonths(LocalDate current, int months, int anchorDay) {
        YearMonth target = YearMonth.from(current).plusMonths(months);
        return target.atDay(Math.min(anchorDay, target.lengthOfMonth()));
    }
}
