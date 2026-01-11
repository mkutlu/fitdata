package com.aarw.fitdata.dto;

import java.time.LocalDate;
import java.util.List;

public record HeartRateIntradayDto(
        LocalDate date,
        Integer restingHr,
        int minBpm,
        int maxBpm,
        Integer caloriesOut,
        Integer activityCalories,
        List<Zone> zones,
        List<Point> points
) {
    public record Zone(
            String name,
            Integer min,
            Integer max,
            Integer minutes
    ) {}

    public record Point(
            String time,
            int bpm
    ) {}
}
