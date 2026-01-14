package com.aarw.fitdata.fitbit.dto;

import java.util.List;

public record FitbitSleepResponse(
    List<SleepLog> sleep,
    Summary summary
) {
    public record SleepLog(
        String dateOfSleep,
        Long duration,
        Long efficiency,
        Boolean isMainSleep,
        Levels levels,
        Long logId,
        Long minutesAfterWakeup,
        Long minutesAsleep,
        Long minutesAwake,
        Long minutesToFallAsleep,
        String startTime,
        String endTime,
        Long timeInBed,
        String type,
        Integer efficiency_score, // Some versions use efficiency_score
        Integer sleep_score,
        Integer infoCode
    ) {}

    public record Levels(
        Summary summary,
        List<DataPoint> data,
        List<DataPoint> shortData
    ) {
        public record Summary(
            Deep deep,
            Light light,
            Rem rem,
            Wake wake
        ) {
            public record Deep(Integer count, Integer minutes, Integer thirtyDayAvgMinutes) {}
            public record Light(Integer count, Integer minutes, Integer thirtyDayAvgMinutes) {}
            public record Rem(Integer count, Integer minutes, Integer thirtyDayAvgMinutes) {}
            public record Wake(Integer count, Integer minutes, Integer thirtyDayAvgMinutes) {}
        }

        public record DataPoint(
            String dateTime,
            String level,
            Integer seconds
        ) {}
    }

    public record Summary(
        Integer totalMinutesAsleep,
        Integer totalSleepRecords,
        Integer totalTimeInBed
    ) {}
}
