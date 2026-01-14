package com.aarw.fitdata.fitbit.service;

import com.aarw.fitdata.dto.WeightSeriesDto;
import com.aarw.fitdata.fitbit.FitbitApiClient;
import com.aarw.fitdata.fitbit.dto.FitbitWeightResponse;
import com.aarw.fitdata.fitbit.util.StepsRange;
import com.aarw.fitdata.fitbit.util.StepsRangeCalculator;
import com.aarw.fitdata.oauth.token.FitbitTokenService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class WeightService {

    private final FitbitTokenService tokenService;
    private final FitbitApiClient apiClient;

    public WeightService(FitbitTokenService tokenService, FitbitApiClient apiClient) {
        this.tokenService = tokenService;
        this.apiClient = apiClient;
    }

    public WeightSeriesDto getWeight(StepsRange range, LocalDate baseDate) {
        var token = tokenService.getValidTokenOrThrow();

        LocalDate start = StepsRangeCalculator.startDate(range, baseDate);

        FitbitWeightResponse raw = apiClient.getWeightSeries(
                token,
                start.toString(),
                baseDate.toString()
        );

        List<WeightSeriesDto.Point> points = raw.weight().stream()
                .map(it -> new WeightSeriesDto.Point(
                        LocalDate.parse(it.date()),
                        it.weight()
                ))
                .toList();

        return new WeightSeriesDto(range.name(), start, baseDate, points);
    }
}
