package com.aarw.fitdata.fitbit.service;

import com.aarw.fitdata.dto.ReadinessCardDto;
import com.aarw.fitdata.dto.HeartRateDayDto;
import com.aarw.fitdata.dto.HeartRateRangeDto;
import com.aarw.fitdata.dto.SleepDto;
import com.aarw.fitdata.fitbit.FitbitApiClient;
import com.aarw.fitdata.fitbit.dto.FitbitActivitiesSummaryResponse;
import com.aarw.fitdata.fitbit.dto.FitbitHrvResponse;
import com.aarw.fitdata.fitbit.dto.FitbitVo2MaxResponse;
import com.aarw.fitdata.fitbit.util.ReadinessScoreEstimator;
import com.aarw.fitdata.fitbit.util.StepsRange;
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
                return new FitbitVo2MaxResponse(java.util.Collections.emptyList());
            }
        }).exceptionally(e -> {
            log.error("Unexpected error fetching VO2 Max for {}: {}", dateStr, e.getMessage());
            return new FitbitVo2MaxResponse(java.util.Collections.emptyList());
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
                            Integer calories = summary.summary().activityCalories();
                            if (calories != null && calories > 250) {
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
        }).exceptionally(e -> {
            log.error("Unexpected error calculating exercise days for {}: {}", dateStr, e.getMessage());
            return 0;
        });

        // 3. Estimate Readiness Score - ASYNC
        CompletableFuture<Integer> readinessScoreFuture = CompletableFuture.supplyAsync(() -> estimateReadiness(date))
                .exceptionally(e -> {
                    log.error("Unexpected error estimating readiness for {}: {}", dateStr, e.getMessage());
                    return 0;
                });

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
            CompletableFuture<HeartRateDayDto> todayHrFuture = CompletableFuture.supplyAsync(() -> heartRateService.getDay(date))
                    .exceptionally(e -> {
                        log.error("Error fetching today's HR for {}: {}", date, e.getMessage());
                        return new HeartRateDayDto(date, null, null);
                    });
            CompletableFuture<HeartRateRangeDto> last7DaysHrFuture = CompletableFuture.supplyAsync(() ->
                    heartRateService.getRange(StepsRange.LAST_7_DAYS, date))
                    .exceptionally(e -> {
                        log.error("Error fetching 7-day HR range for {}: {}", date, e.getMessage());
                        return new HeartRateRangeDto("LAST_7_DAYS", date.minusDays(7), date, List.of());
                    });
            CompletableFuture<SleepDto> sleepFuture = CompletableFuture.supplyAsync(() -> sleepService.getSleep(date))
                    .exceptionally(e -> {
                        log.error("Error fetching sleep for {}: {}", date, e.getMessage());
                        return new SleepDto(date.toString(), 0, 0, null, null, null, null, List.of());
                    });
            CompletableFuture<FitbitActivitiesSummaryResponse> activityFuture = CompletableFuture.supplyAsync(() ->
                    apiClient.getActivitiesSummaryForDay(token, date.toString()))
                    .exceptionally(e -> {
                        log.error("Error fetching activities for {}: {}", date, e.getMessage());
                        return null;
                    });
            CompletableFuture<FitbitHrvResponse> hrvTodayFuture = CompletableFuture.supplyAsync(() ->
                    apiClient.getHrv(token, date.toString()))
                    .exceptionally(e -> {
                        log.error("Error fetching today's HRV for {}: {}", date, e.getMessage());
                        return new FitbitHrvResponse(List.of());
                    });
            CompletableFuture<FitbitHrvResponse> hrvRangeFuture = CompletableFuture.supplyAsync(() -> {
                LocalDate start = date.minusDays(14);
                return apiClient.getHrvRange(token, start.toString(), date.minusDays(1).toString());
            }).exceptionally(e -> {
                log.error("Error fetching HRV range for {}: {}", date, e.getMessage());
                return new FitbitHrvResponse(List.of());
            });

            CompletableFuture.allOf(todayHrFuture, last7DaysHrFuture, sleepFuture, activityFuture, hrvTodayFuture, hrvRangeFuture).join();

            // 1. RHR Delta
            HeartRateDayDto todayHr = todayHrFuture.join();
            Integer todayRhr = todayHr.restingHr();

            HeartRateRangeDto rangeHr = last7DaysHrFuture.join();
            List<HeartRateRangeDto.Point> rhrPoints = rangeHr.points().stream()
                    .filter(p -> p.restingHr() != null && p.restingHr() > 0)
                    .toList();

            if (todayRhr == null || todayRhr == 0) {
                // Try to find the most recent RHR if today's is missing
                log.info("Today's RHR missing for {}, looking for recent data...", date);
                todayRhr = rhrPoints.isEmpty() ? null : rhrPoints.getLast().restingHr();
            }

            double avgRhr = rhrPoints.stream()
                    .mapToInt(HeartRateRangeDto.Point::restingHr)
                    .average()
                    .orElse(todayRhr != null && todayRhr > 0 ? todayRhr : 60);

            if (todayRhr == null || todayRhr == 0) {
                log.warn("Readiness estimation: No recent resting heart rate found for {}, using average as today's RHR", date);
                todayRhr = (int) avgRhr;
            }

            int rhrDelta = todayRhr - (int) avgRhr;

            // 2. Sleep Trend
            SleepDto sleep = sleepFuture.join();
            Integer sleepScore = sleep.sleepScore();
            ReadinessScoreEstimator.SleepTrend sleepTrend = null;
            if (sleepScore != null) {
                if (sleepScore >= 80) sleepTrend = ReadinessScoreEstimator.SleepTrend.EXCELLENT;
                else if (sleepScore >= 70) sleepTrend = ReadinessScoreEstimator.SleepTrend.GOOD;
                else if (sleepScore >= 60) sleepTrend = ReadinessScoreEstimator.SleepTrend.FAIR;
                else sleepTrend = ReadinessScoreEstimator.SleepTrend.POOR;
            }

            // 3. Activity Load
            FitbitActivitiesSummaryResponse activity = activityFuture.join();
            int activeCals = (activity != null && activity.summary() != null && activity.summary().activityCalories() != null) 
                    ? activity.summary().activityCalories() : 0;
            ReadinessScoreEstimator.ActivityLoad activityLoad = ReadinessScoreEstimator.ActivityLoad.REST;

            if (activeCals > 1500) activityLoad = ReadinessScoreEstimator.ActivityLoad.VERY_HIGH;
            else if (activeCals > 1000) activityLoad = ReadinessScoreEstimator.ActivityLoad.HIGH;
            else if (activeCals > 500) activityLoad = ReadinessScoreEstimator.ActivityLoad.MODERATE;
            else if (activeCals > 200) activityLoad = ReadinessScoreEstimator.ActivityLoad.LOW;

            // 4. HRV
            FitbitHrvResponse hrvToday = hrvTodayFuture.join();
            if (hrvToday != null && hrvToday.hrv() != null) {
                log.debug("Today's HRV records count: {}", hrvToday.hrv().size());
                hrvToday.hrv().forEach(h -> log.debug("HRV Record: date={}, value={}", h.dateTime(), h.value()));
            }

            Double todayHrvValue = (hrvToday != null && hrvToday.hrv() != null && !hrvToday.hrv().isEmpty())
                    ? hrvToday.hrv().stream()
                        .filter(h -> h.value() != null)
                        .map(h -> h.value().dailySample())
                        .filter(v -> v != null && v > 0)
                        .findFirst()
                        .orElse(null)
                    : null;

            FitbitHrvResponse hrvRange = hrvRangeFuture.join();
            if (hrvRange != null && hrvRange.hrv() != null) {
                log.debug("HRV Range records count: {}", hrvRange.hrv().size());
            }

            List<Double> hrvPoints = (hrvRange != null && hrvRange.hrv() != null)
                    ? hrvRange.hrv().stream()
                        .filter(r -> r.value() != null && r.value().dailySample() != null && r.value().dailySample() > 0)
                        .map(r -> r.value().dailySample())
                        .toList()
                    : List.of();

            if (todayHrvValue == null) {
                log.info("Today's HRV missing for {}, looking for recent data in {} records...", date, hrvPoints.size());
                todayHrvValue = hrvPoints.isEmpty() ? null : hrvPoints.getLast();
            }

            double avgHrv = hrvPoints.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(todayHrvValue != null ? todayHrvValue : 50.0);

            if (todayHrvValue == null) {
                log.warn("Readiness estimation: No recent HRV found for {}, using average", date);
                todayHrvValue = avgHrv;
            }

            double hrvPercentChange = ((todayHrvValue - avgHrv) / Math.max(1.0, avgHrv)) * 100.0;

            ReadinessScoreEstimator.ReadinessInputs inputs = new ReadinessScoreEstimator.ReadinessInputs(hrvPercentChange, rhrDelta, sleepTrend, activityLoad);
            Integer score = ReadinessScoreEstimator.estimate(inputs);
            log.debug("Readiness score estimated for {}: {}", date, score);
            return score;
        } catch (Exception e) {
            log.error("CRITICAL: Failed to estimate readiness for {}: {}", date, e.getMessage(), e);
            return 0; // Return 0 instead of null to avoid frontend issues, though 1 is the minimum in clamp
        }
    }
}
