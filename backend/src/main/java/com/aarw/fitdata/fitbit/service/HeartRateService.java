package com.aarw.fitdata.fitbit.service;

import com.aarw.fitdata.dto.HeartRateDayDto;
import com.aarw.fitdata.dto.HeartRateRangeDto;
import com.aarw.fitdata.fitbit.FitbitApiClient;
import com.aarw.fitdata.fitbit.dto.FitbitHeartDailyRangeResponse;
import com.aarw.fitdata.fitbit.util.StepsRange;
import com.aarw.fitdata.fitbit.util.StepsRangeCalculator;
import com.aarw.fitdata.oauth.token.FitbitTokenService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class HeartRateService {

    private final FitbitTokenService tokenService;
    private final FitbitApiClient apiClient;

    public HeartRateService(FitbitTokenService tokenService, FitbitApiClient apiClient) {
        this.tokenService = tokenService;
        this.apiClient = apiClient;
    }

    public HeartRateDayDto getDay(LocalDate baseDate) {
        var token = tokenService.getValidTokenOrThrow();

        FitbitHeartDailyRangeResponse raw = apiClient.getHeartForDay(token, baseDate.toString());
        var item = firstOrNull(raw);

        if (item == null) {
            return new HeartRateDayDto(baseDate, null, new HeartRateDayDto.Zones(0, 0, 0, 0));
        }

        Integer resting = item.value() == null ? null : item.value().restingHeartRate();
        HeartRateDayDto.Zones zones = mapZones(item.value() == null ? null : item.value().heartRateZones());
        return new HeartRateDayDto(LocalDate.parse(item.dateTime()), resting, zones);
    }

    public HeartRateRangeDto getRange(StepsRange range, LocalDate baseDate) {
        var token = tokenService.getValidTokenOrThrow();

        LocalDate start = StepsRangeCalculator.startDate(range, baseDate);

        FitbitHeartDailyRangeResponse raw = apiClient.getHeartByDateRange(token, start.toString(), baseDate.toString());
        List<FitbitHeartDailyRangeResponse.ActivityHeart> items =
                raw == null || raw.activitiesHeart() == null ? List.of() : raw.activitiesHeart();

        List<HeartRateRangeDto.Point> points = items.stream()
                .map(it -> {
                    LocalDate d = LocalDate.parse(it.dateTime());
                    Integer resting = it.value() == null ? null : it.value().restingHeartRate();
                    HeartRateDayDto.Zones zones = mapZones(it.value() == null ? null : it.value().heartRateZones());
                    return new HeartRateRangeDto.Point(d, resting, zones);
                })
                .toList();

        return new HeartRateRangeDto(range.name(), start, baseDate, points);
    }

    private FitbitHeartDailyRangeResponse.ActivityHeart firstOrNull(FitbitHeartDailyRangeResponse raw) {
        if (raw == null || raw.activitiesHeart() == null || raw.activitiesHeart().isEmpty()) return null;
        return raw.activitiesHeart().getFirst();
    }

    private HeartRateDayDto.Zones mapZones(List<FitbitHeartDailyRangeResponse.Zone> zones) {
        if (zones == null || zones.isEmpty()) {
            return new HeartRateDayDto.Zones(0, 0, 0, 0);
        }

        int out = 0, fat = 0, cardio = 0, peak = 0;

        for (var z : zones) {
            String name = z.name() == null ? "" : z.name().toLowerCase();
            int minutes = z.minutes() == null ? 0 : z.minutes();

            if (name.contains("out")) out += minutes;
            else if (name.contains("fat")) fat += minutes;
            else if (name.contains("cardio")) cardio += minutes;
            else if (name.contains("peak")) peak += minutes;
        }

        return new HeartRateDayDto.Zones(out, fat, cardio, peak);
    }
}
