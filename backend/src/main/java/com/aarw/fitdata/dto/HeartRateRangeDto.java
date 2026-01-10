package com.aarw.fitdata.dto;

import java.time.LocalDate;
import java.util.List;

public record HeartRateRangeDto(
        String range,
        LocalDate startDate,
        LocalDate endDate,
        List<Point> points
) {
    public record Point(
            LocalDate date,
            Integer restingHr,
            HeartRateDayDto.Zones zones
    ) {}
}