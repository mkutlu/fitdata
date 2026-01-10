package com.aarw.fitdata.dto;


import java.time.LocalDate;

public record HeartRateDayDto(
        LocalDate date,
        Integer restingHr,
        Zones zones
) {
    public record Zones(
            int outOfRangeMin,
            int fatBurnMin,
            int cardioMin,
            int peakMin
    ) {}
}