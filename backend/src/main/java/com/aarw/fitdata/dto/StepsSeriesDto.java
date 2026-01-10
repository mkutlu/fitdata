package com.aarw.fitdata.dto;

import java.time.LocalDate;
import java.util.List;

public record StepsSeriesDto(
        String range,
        LocalDate startDate,
        LocalDate endDate,
        List<Point> points
) {
    public record Point(LocalDate date, int steps) {}
}
