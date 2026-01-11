package com.aarw.fitdata.fitbit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FitbitActivitiesSummaryResponse(
        Summary summary
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Summary(
            Integer caloriesOut,
            Integer activityCalories
    ) {}
}
