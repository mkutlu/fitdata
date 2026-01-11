package com.aarw.fitdata.fitbit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FitbitHeartIntradayResponse(
        @JsonProperty("activities-heart-intraday") Intraday intraday
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Intraday(
            List<DataPoint> dataset,
            Integer datasetInterval,
            String datasetType
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DataPoint(
            String time,
            int value
    ) {}
}
