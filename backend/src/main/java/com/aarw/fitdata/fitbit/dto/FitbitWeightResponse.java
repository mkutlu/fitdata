package com.aarw.fitdata.fitbit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record FitbitWeightResponse(
        @JsonProperty("weight") List<WeightLog> weight
) {
    public record WeightLog(
            String date,
            Double weight,
            Long logId
    ) {}
}
