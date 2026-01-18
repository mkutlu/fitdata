package com.aarw.fitdata.fitbit.service;

import com.aarw.fitdata.dto.ReadinessCardDto;
import com.aarw.fitdata.dto.HeartRateDayDto;
import com.aarw.fitdata.dto.HeartRateRangeDto;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

        // 1. Fetch VO2 Max (Cardio Fitness Score) - ASYNC
        CompletableFuture<FitbitVo2MaxResponse> vo2MaxFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return apiClient.getVo2Max(token, dateStr);
            } catch (Exception e) {
                log.error("Error fetching VO2 Max for {}: {}", dateStr, e.getMessage());
                return null;
            }
        });

        // 2. Calculate Exercise Days (Current Week starting Monday) - ASYNC (partially)
        CompletableFuture<Integer> exerciseDaysFuture = CompletableFuture.supplyAsync(() -> {
            int exerciseDaysCount = 0;
            try {
                LocalDate current = date.with(java.time.DayOfWeek.MONDAY);
                List<CompletableFuture<FitbitActivitiesSummaryResponse>> activityFutures = new ArrayList<>();
                while (!current.isAfter(date)) {
                    final String d = current.toString();
                    activityFutures.add(CompletableFuture.supplyAsync(() -> apiClient.getActivitiesSummaryForDay(token, d)));
                    current = current.plusDays(1);
                }

                for (var f : activityFutures) {
                    try {
                        FitbitActivitiesSummaryResponse summary = f.join();
                        if (summary != null && summary.summary() != null) {
                            if (summary.summary().activityCalories() != null && summary.summary().activityCalories() > 250) {
                                exerciseDaysCount++;
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error fetching activity summary during exercise days calculation: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Error calculating exercise days for {}: {}", dateStr, e.getMessage());
            }
            return exerciseDaysCount;
        });

        // 3. Estimate Readiness Score - ASYNC
        CompletableFuture<Integer> readinessScoreFuture = CompletableFuture.supplyAsync(() -> estimateReadiness(date));

        // Wait for all to complete
        CompletableFuture.allOf(vo2MaxFuture, exerciseDaysFuture, readinessScoreFuture).join();

        Integer cardioScore = null;
        String vo2MaxText = null;
        try {
            FitbitVo2MaxResponse vo2MaxRaw = vo2MaxFuture.join();
            if (vo2MaxRaw != null && vo2MaxRaw.cardioscore() != null && !vo2MaxRaw.cardioscore().isEmpty()) {
                var scoreVal = vo2MaxRaw.cardioscore().getFirst().value();
                vo2MaxText = scoreVal.vo2Max();
                if (vo2MaxText != null) {
                    String[] parts = vo2MaxText.split("-");
                    if (parts.length > 0) {
                        try {
                            cardioScore = Integer.parseInt(parts[0].trim());
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}

        int exerciseDaysCount = exerciseDaysFuture.join();
        Integer readinessScore = readinessScoreFuture.join();
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
            var token = tokenService.getValidTokenOrThrow();

            // Parallelize estimation inputs
            CompletableFuture<HeartRateDayDto> todayHrFuture = CompletableFuture.supplyAsync(() -> heartRateService.getDay(date));
            CompletableFuture<HeartRateRangeDto> last7DaysHrFuture = CompletableFuture.supplyAsync(() ->
                    heartRateService.getRange(com.aarw.fitdata.fitbit.util.StepsRange.LAST_7_DAYS, date));
            CompletableFuture<SleepDto> sleepFuture = CompletableFuture.supplyAsync(() -> sleepService.getSleep(date));
            CompletableFuture<FitbitActivitiesSummaryResponse> activityFuture = CompletableFuture.supplyAsync(() ->
                    apiClient.getActivitiesSummaryForDay(token, date.toString()));
            CompletableFuture<FitbitHrvResponse> hrvTodayFuture = CompletableFuture.supplyAsync(() ->
                    apiClient.getHrv(token, date.toString()));
            CompletableFuture<FitbitHrvResponse> hrvRangeFuture = CompletableFuture.supplyAsync(() -> {
                LocalDate start = date.minusDays(7);
                return apiClient.getHrvRange(token, start.toString(), date.minusDays(1).toString());
            });

            CompletableFuture.allOf(todayHrFuture, last7DaysHrFuture, sleepFuture, activityFuture, hrvTodayFuture, hrvRangeFuture).join();

            // 1. RHR Delta
            HeartRateDayDto todayHr = todayHrFuture.join();
            Integer todayRhr = todayHr.restingHr();
            if (todayRhr == null || todayRhr == 0) return null;

            double avgRhr = last7DaysHrFuture.join().points().stream()
                    .map(HeartRateRangeDto.Point::restingHr)
                    .filter(r -> r != null && r > 0)
                    .mapToInt(r -> r)
                    .average()
                    .orElse(todayRhr);
            int rhrDelta = (int) (todayRhr - avgRhr);

            // 2. Sleep Trend
            SleepDto sleep = sleepFuture.join();
            Integer sleepScore = sleep.sleepScore();
            SleepTrend sleepTrend = null;
            if (sleepScore != null) {
                if (sleepScore >= 80) sleepTrend = SleepTrend.EXCELLENT;
                else if (sleepScore >= 70) sleepTrend = SleepTrend.GOOD;
                else if (sleepScore >= 60) sleepTrend = SleepTrend.FAIR;
                else sleepTrend = SleepTrend.POOR;
            }

            // 3. Activity Load
            FitbitActivitiesSummaryResponse activity = activityFuture.join();
            int activeCals = (activity != null && activity.summary() != null) ? activity.summary().activityCalories() : 0;
            ActivityLoad activityLoad = ActivityLoad.REST;

            if (activeCals > 1500) activityLoad = ActivityLoad.VERY_HIGH;
            else if (activeCals > 1000) activityLoad = ActivityLoad.HIGH;
            else if (activeCals > 500) activityLoad = ActivityLoad.MODERATE;
            else if (activeCals > 200) activityLoad = ActivityLoad.LOW;

            // 4. HRV
            FitbitHrvResponse hrvToday = hrvTodayFuture.join();
            Double todayHrvValue = (hrvToday != null && hrvToday.hrv() != null && !hrvToday.hrv().isEmpty())
                    ? hrvToday.hrv().getFirst().value().dailySample()
                    : null;

            double hrvPercentChange = 0.0;
            if (todayHrvValue != null) {
                FitbitHrvResponse hrvRange = hrvRangeFuture.join();
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
