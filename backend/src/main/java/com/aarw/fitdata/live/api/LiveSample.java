package com.aarw.fitdata.live.api;

public record LiveSample(
        long ts,
        Double hr,
        Long steps,
        Double distance_m,
        Double calories
) {}
