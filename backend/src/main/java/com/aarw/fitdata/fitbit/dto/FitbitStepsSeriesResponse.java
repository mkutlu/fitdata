package com.aarw.fitdata.fitbit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record FitbitStepsSeriesResponse(
        @JsonProperty("activities-steps") List<Item> activitiesSteps
) {
    public record Item(
            String dateTime,
            String value
    ) {}
}