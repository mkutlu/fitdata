package com.aarw.fitdata.fitbit.service;

import com.aarw.fitdata.fitbit.FitbitApiClient;
import com.aarw.fitdata.fitbit.dto.FitbitStepsSeriesResponse;
import com.aarw.fitdata.dto.StepsSeriesDto;
import com.aarw.fitdata.fitbit.util.StepsRangeCalculator;
import com.aarw.fitdata.fitbit.util.StepsRange;
import com.aarw.fitdata.oauth.token.FitbitTokenService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class StepsService {

    private final FitbitTokenService tokenService;
    private final FitbitApiClient apiClient;

    public StepsService(FitbitTokenService tokenService, FitbitApiClient apiClient) {
        this.tokenService = tokenService;
        this.apiClient = apiClient;
    }

    public StepsSeriesDto getSteps(StepsRange range) {
        var token = tokenService.getValidTokenOrThrow();

        LocalDate today = LocalDate.now();
        LocalDate start = StepsRangeCalculator.startDate(range, today);

        FitbitStepsSeriesResponse raw = apiClient.getDailyStepsSeries(
                token,
                start.toString(),
                today.toString()
        );

        List<StepsSeriesDto.Point> points = raw.activitiesSteps().stream()
                .map(it -> new StepsSeriesDto.Point(
                        LocalDate.parse(it.dateTime()),
                        parseSteps(it.value())
                ))
                .toList();

        return new StepsSeriesDto(range.name(), start, today, points);
    }

    private int parseSteps(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }
}