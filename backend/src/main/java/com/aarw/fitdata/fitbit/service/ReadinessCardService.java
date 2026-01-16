package com.aarw.fitdata.fitbit.service;

import com.aarw.fitdata.dto.ReadinessCardDto;
import com.aarw.fitdata.dto.HeartRateDayDto;
import com.aarw.fitdata.dto.SleepDto;
import com.aarw.fitdata.fitbit.FitbitApiClient;
import com.aarw.fitdata.fitbit.dto.FitbitActivitiesSummaryResponse;
import com.aarw.fitdata.fitbit.dto.FitbitHrvResponse;
import com.aarw.fitdata.fitbit.dto.FitbitVo2MaxResponse;
import com.aarw.fitdata.fitbit.util.ActivityLoad;
import com.aarw.fitdata.fitbit.util.ReadinessInputs;
import com.aarw.fitdata.fitbit.util.ReadinessScoreEstimator;
import com.aarw.fitdata.fitbit.util.SleepTrend;
import com.aarw.fitdata.oauth.token.FitbitTokenService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ReadinessCardService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReadinessCardService.class);

    private final FitbitTokenService tokenService;
    private final FitbitApiClient apiClient;
    private final HeartRateService heartRateService;
    private final SleepService sleepService;

    public ReadinessCardService(FitbitTokenService tokenService,
                                FitbitApiClient apiClient,
                                HeartRateService heartRateService,
                                SleepService sleepService) {
        this.tokenService = tokenService;
        this.apiClient = apiClient;
        this.heartRateService = heartRateService;
        this.sleepService = sleepService;
    }

    public ReadinessCardDto getReadinessCard(LocalDate date) {
        var token = tokenService.getValidTokenOrThrow();
        String dateStr = date.toString();

        // 1. Fetch VO2 Max (Cardio Fitness Score)
        Integer cardioScore = null;
        String vo2MaxText = null;
        try {
            FitbitVo2MaxResponse vo2MaxRaw = apiClient.getVo2Max(token, dateStr);
            if (vo2MaxRaw != null && vo2MaxRaw.cardioscore() != null && !vo2MaxRaw.cardioscore().isEmpty()) {
                var scoreVal = vo2MaxRaw.cardioscore().getFirst().value();
                vo2MaxText = scoreVal.vo2Max();
                try {
                    if (vo2MaxText != null) {
                        String[] parts = vo2MaxText.split("-");
                        if (parts.length > 0) {
                            cardioScore = Integer.parseInt(parts[0].trim());
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
        } catch (Exception e) {
            log.error("Error fetching VO2 Max for {}: {}", dateStr, e.getMessage());
        }

        // 2. Calculate Exercise Days (Current Week starting Monday)
        int exerciseDaysCount = 0;
        try {
            // Loop from Monday until the selected date
            LocalDate current = date.with(java.time.DayOfWeek.MONDAY);
            while (!current.isAfter(date)) {
                FitbitActivitiesSummaryResponse summary = apiClient.getActivitiesSummaryForDay(token, current.toString());
                if (summary != null && summary.summary() != null) {
                    // Consider a day as "Exercise Day" if activity calories > 250
                    if (summary.summary().activityCalories() != null && summary.summary().activityCalories() > 250) {
                        exerciseDaysCount++;
                    }
                }
                current = current.plusDays(1);
            }
        } catch (Exception e) {
            log.error("Error calculating exercise days for {}: {}", dateStr, e.getMessage());
        }

        // 3. Estimate Readiness Score
        Integer readinessScore = estimateReadiness(date);
        String readinessStatus = "ESTIMATED";

        return new ReadinessCardDto(
                date,
                readinessScore,
                cardioScore,
                null, // targetMin
                null, // targetMax
                readinessStatus,
                null, // cardioStatus
                vo2MaxText,
                exerciseDaysCount
        );
    }

    private Integer estimateReadiness(LocalDate date) {
        try {
            // 1. RHR Delta
            HeartRateDayDto todayHr = heartRateService.getDay(date);
            Integer todayRhr = todayHr.restingHr();
            if (todayRhr == null || todayRhr == 0) return null;

            // Get average RHR for last 7 days
            double avgRhr = heartRateService.getRange(com.aarw.fitdata.fitbit.util.StepsRange.LAST_7_DAYS, date)
                    .points().stream()
                    .map(com.aarw.fitdata.dto.HeartRateRangeDto.Point::restingHr)
                    .filter(r -> r != null && r > 0)
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(todayRhr);
            int rhrDelta = (int) (todayRhr - avgRhr);

            // 2. Sleep Trend
            SleepDto sleep = sleepService.getSleep(date);
            Integer sleepScore = sleep.sleepScore();
            SleepTrend sleepTrend = null;
            if (sleepScore != null) {
                if (sleepScore >= 80) sleepTrend = SleepTrend.EXCELLENT;
                else if (sleepScore >= 70) sleepTrend = SleepTrend.GOOD;
                else if (sleepScore >= 60) sleepTrend = SleepTrend.FAIR;
                else sleepTrend = SleepTrend.POOR;
            }

            // 3. Activity Load
            var token = tokenService.getValidTokenOrThrow();
            FitbitActivitiesSummaryResponse activity = apiClient.getActivitiesSummaryForDay(token, date.toString());
            int activeCals = (activity != null && activity.summary() != null) ? activity.summary().activityCalories() : 0;
            ActivityLoad activityLoad = ActivityLoad.REST;

            if (activeCals > 1500) activityLoad = ActivityLoad.VERY_HIGH;
            else if (activeCals > 1000) activityLoad = ActivityLoad.HIGH;
            else if (activeCals > 500) activityLoad = ActivityLoad.MODERATE;
            else if (activeCals > 200) activityLoad = ActivityLoad.LOW;

            // 4. HRV
            FitbitHrvResponse hrvToday = apiClient.getHrv(token, date.toString());
            Double todayHrvValue = (hrvToday != null && hrvToday.hrv() != null && !hrvToday.hrv().isEmpty())
                    ? hrvToday.hrv().getFirst().value().dailySample()
                    : null;

            double hrvPercentChange = 0.0;
            if (todayHrvValue != null) {
                LocalDate start = date.minusDays(7);
                FitbitHrvResponse hrvRange = apiClient.getHrvRange(token, start.toString(), date.minusDays(1).toString());
                double avgHrv = (hrvRange != null && hrvRange.hrv() != null && !hrvRange.hrv().isEmpty())
                        ? hrvRange.hrv().stream()
                        .map(r -> r.value().dailySample())
                        .filter(v -> v != null && v > 0)
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(todayHrvValue)
                        : todayHrvValue;

                hrvPercentChange = ((todayHrvValue - avgHrv) / avgHrv) * 100.0;
            }

            ReadinessInputs inputs = new ReadinessInputs(hrvPercentChange, rhrDelta, sleepTrend, activityLoad);
            return ReadinessScoreEstimator.estimate(inputs);
        } catch (Exception e) {
            log.error("Failed to estimate readiness for {}: {}", date, e.getMessage());
            return null;
        }
    }
}
