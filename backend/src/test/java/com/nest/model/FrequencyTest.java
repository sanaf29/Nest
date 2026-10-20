package com.nest.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FrequencyTest {

    private static List<LocalDate> schedule(Frequency frequency, LocalDate start, int count) {
        List<LocalDate> dates = new ArrayList<>(List.of(start));
        while (dates.size() < count) {
            dates.add(frequency.next(dates.getLast(), start.getDayOfMonth()));
        }
        return dates;
    }

    @Test
    void monthlyOnThe31stClampsToShortMonthsAndReturnsTo31() {
        assertThat(schedule(Frequency.MONTHLY, LocalDate.of(2027, 12, 31), 5)).containsExactly(
                LocalDate.of(2027, 12, 31), LocalDate.of(2028, 1, 31), LocalDate.of(2028, 2, 29), // leap year
                LocalDate.of(2028, 3, 31), LocalDate.of(2028, 4, 30));
    }

    @Test
    void monthlyAcrossANonLeapFebruary() {
        assertThat(schedule(Frequency.MONTHLY, LocalDate.of(2026, 1, 30), 3))
                .containsExactly(LocalDate.of(2026, 1, 30), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 30));
    }

    @Test
    void yearlyOnLeapDayFallsOnFeb28InOtherYears() {
        assertThat(schedule(Frequency.YEARLY, LocalDate.of(2028, 2, 29), 5)).containsExactly(
                LocalDate.of(2028, 2, 29), LocalDate.of(2029, 2, 28), LocalDate.of(2030, 2, 28),
                LocalDate.of(2031, 2, 28), LocalDate.of(2032, 2, 29));
    }

    @Test
    void weeklyBiweeklyAndQuarterly() {
        assertThat(Frequency.WEEKLY.next(LocalDate.of(2026, 12, 29), 29)).isEqualTo(LocalDate.of(2027, 1, 5));
        assertThat(Frequency.BIWEEKLY.next(LocalDate.of(2026, 10, 19), 19)).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(schedule(Frequency.QUARTERLY, LocalDate.of(2026, 11, 30), 3))
                .containsExactly(LocalDate.of(2026, 11, 30), LocalDate.of(2027, 2, 28), LocalDate.of(2027, 5, 30));
    }
}
