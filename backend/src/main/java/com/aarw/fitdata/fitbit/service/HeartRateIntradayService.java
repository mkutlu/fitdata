package com.aarw.fitdata.fitbit.service;

import com.aarw.fitdata.dto.HeartRateIntradayDto;
import com.aarw.fitdata.fitbit.FitbitApiClient;
import com.aarw.fitdata.fitbit.FitbitRateLimitException;
import com.aarw.fitdata.fitbit.dto.FitbitActivitiesSummaryResponse;
import com.aarw.fitdata.fitbit.dto.FitbitHeartDailyRangeResponse;
import com.aarw.fitdata.fitbit.dto.FitbitHeartIntradayResponse;
import com.aarw.fitdata.oauth.token.FitbitTokenEntity;
import com.aarw.fitdata.oauth.token.FitbitTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HeartRateIntradayService {

    private static final Logger log = LoggerFactory.getLogger(HeartRateIntradayService.class);

    private final FitbitTokenService tokenService;
    private final FitbitApiClient apiClient;

    private final Map<String, FitbitHeartIntradayResponse> intradayCache = new ConcurrentHashMap<>();
    private final Map<String, String> bestDetailLevel = new ConcurrentHashMap<>();

    public HeartRateIntradayService(FitbitTokenService tokenService, FitbitApiClient apiClient) {
        this.tokenService = tokenService;
        this.apiClient = apiClient;
    }

    public HeartRateIntradayDto get(LocalDate baseDate) {
        var token = tokenService.getValidTokenOrThrow();
        String dateIso = baseDate.toString();

        FitbitHeartIntradayResponse intraday = fetchIntradayWithFallback(token, dateIso);
        List<FitbitHeartIntradayResponse.DataPoint> dataset =
                intraday == null || intraday.intraday() == null || intraday.intraday().dataset() == null
                        ? List.of()
                        : intraday.intraday().dataset();

        int minBpm = 0;
        int maxBpm = 0;

        List<HeartRateIntradayDto.Point> points = new ArrayList<>();
        if (!dataset.isEmpty()) {
            minBpm = dataset.stream().min(Comparator.comparingInt(FitbitHeartIntradayResponse.DataPoint::value)).get().value();
            maxBpm = dataset.stream().max(Comparator.comparingInt(FitbitHeartIntradayResponse.DataPoint::value)).get().value();
            points = dataset.stream().map(p -> new HeartRateIntradayDto.Point(p.time(), p.value())).toList();
        }

        FitbitHeartDailyRangeResponse day = apiClient.getHeartForDay(token, dateIso);
        FitbitHeartDailyRangeResponse.ActivityHeart item =
                day == null || day.activitiesHeart() == null || day.activitiesHeart().isEmpty()
                        ? null
                        : day.activitiesHeart().getFirst();

        Integer restingHr = item == null || item.value() == null ? null : item.value().restingHeartRate();

        List<HeartRateIntradayDto.Zone> zones = List.of();
        if (item != null && item.value() != null && item.value().heartRateZones() != null) {
            zones = item.value().heartRateZones().stream()
                    .map(z -> new HeartRateIntradayDto.Zone(z.name(), z.min(), z.max(), z.minutes()))
                    .toList();
        }

        FitbitActivitiesSummaryResponse activity = apiClient.getActivitiesSummaryForDay(token, dateIso);
        Integer caloriesOut = activity == null || activity.summary() == null ? null : activity.summary().caloriesOut();
        Integer activityCalories = activity == null || activity.summary() == null ? null : activity.summary().activityCalories();

        return new HeartRateIntradayDto(baseDate, restingHr, minBpm, maxBpm, caloriesOut, activityCalories, zones, points);
    }

    private FitbitHeartIntradayResponse fetchIntradayWithFallback(FitbitTokenEntity token, String dateIso) {
        if (intradayCache.containsKey(dateIso)) {
            return intradayCache.get(dateIso);
        }

        String knownLevel = bestDetailLevel.get(dateIso);
        if (knownLevel != null) {
            try {
                FitbitHeartIntradayResponse r = apiClient.getHeartIntraday(token, dateIso, knownLevel);
                intradayCache.put(dateIso, r);
                return r;
            } catch (FitbitRateLimitException e) {
                log.warn("Rate limit hit while fetching intraday for {} with level {}. Retry-after: {}", dateIso, knownLevel, e.getRetryAfter());
                throw e;
            } catch (Exception e) {
                log.warn("Failed to fetch intraday for {} with known level {}. Falling back to full probe.", dateIso, knownLevel);
            }
        }

        List<String> levels = List.of("1min", "5min", "15min");
        for (String level : levels) {
            try {
                FitbitHeartIntradayResponse r = apiClient.getHeartIntraday(token, dateIso, level);
                if (hasData(r)) {
                    bestDetailLevel.put(dateIso, level);
                    intradayCache.put(dateIso, r);
                    return r;
                }
            } catch (FitbitRateLimitException e) {
                log.warn("Rate limit hit during probe for {} at level {}.", dateIso, level);
                throw e;
            } catch (Exception e) {
                log.debug("Probe failed for {} at level {}: {}", dateIso, level, e.getMessage());
            }
        }

        return null;
    }

    private boolean hasData(FitbitHeartIntradayResponse r) {
        return r != null
                && r.intraday() != null
                && r.intraday().dataset() != null
                && !r.intraday().dataset().isEmpty();
    }

}
