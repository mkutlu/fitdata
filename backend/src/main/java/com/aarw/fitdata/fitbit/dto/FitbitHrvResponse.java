package com.aarw.fitdata.fitbit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FitbitHrvResponse(
        List<Hrv> hrv
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Hrv(
            Value value,
            String dateTime
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(
            Double dailySample,
            Double deepSleep
    ) {}
}
