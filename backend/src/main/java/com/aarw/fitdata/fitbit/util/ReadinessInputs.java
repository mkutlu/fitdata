package com.aarw.fitdata.fitbit.util;

public record ReadinessInputs(
        double hrvPercentChange,
        int rhrDeltaBpm,
        SleepTrend sleepTrend,
        ActivityLoad activityLoad
) {}