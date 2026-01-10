package com.aarw.fitdata.fitbit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record FitbitHeartDailyRangeResponse(
        @JsonProperty("activities-heart") List<ActivityHeart> activitiesHeart
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ActivityHeart(
            String dateTime,
            Value value
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(
            Integer restingHeartRate,
            List<Zone> heartRateZones
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Zone(
            String name,
            Integer min,
            Integer max,
            Integer minutes,
            Integer caloriesOut
    ) {}
}
