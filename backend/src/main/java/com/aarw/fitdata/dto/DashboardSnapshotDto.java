package com.aarw.fitdata.dto;

import com.aarw.fitdata.fitbit.dto.FitbitProfileResponse;
import com.aarw.fitdata.fitbit.util.StepsRange;
import java.time.LocalDate;

public record DashboardSnapshotDto(
        LocalDate selectedDate,
        StepsRange stepsRange,
        StepsRange weightRange,
        FitbitProfileResponse profile,
        ReadinessCardDto readiness,
        StepsSeriesDto steps,
        WeightSeriesDto weight,
        HeartRateIntradayDto heartRate,
        SleepDto sleep
) {}
