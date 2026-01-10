package com.aarw.fitdata.fitbit.util;


import java.time.DayOfWeek;
import java.time.LocalDate;

public final class StepsRangeCalculator {
    private StepsRangeCalculator() {}

    public static LocalDate startDate(StepsRange range, LocalDate today) {
        return switch (range) {
            case LAST_7_DAYS -> today.minusDays(6);
            case LAST_14_DAYS -> today.minusDays(13);
            case LAST_30_DAYS -> today.minusDays(29);
            case CURRENT_WEEK -> today.with(DayOfWeek.MONDAY);
        };
    }
}
