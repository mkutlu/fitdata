package com.aarw.fitdata.fitbit.util;


import java.time.DayOfWeek;
import java.time.LocalDate;

public final class StepsRangeCalculator {

    private StepsRangeCalculator() {}

    public static LocalDate startDate(StepsRange range, LocalDate baseDate) {
        return switch (range) {
            case LAST_7_DAYS -> baseDate.minusDays(6);
            case LAST_14_DAYS -> baseDate.minusDays(13);
            case LAST_30_DAYS -> baseDate.minusDays(29);
            case CURRENT_WEEK -> baseDate.with(DayOfWeek.MONDAY);
        };
    }
}
