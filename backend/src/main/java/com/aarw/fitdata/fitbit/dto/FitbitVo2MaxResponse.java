package com.aarw.fitdata.fitbit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FitbitVo2MaxResponse(
        List<CardioScore> cardioscore
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CardioScore(
            String dateTime,
            Value value
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(
            String vo2Max
    ) {}
}
