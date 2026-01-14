package com.aarw.fitdata.fitbit.service;

import com.aarw.fitdata.dto.SleepDto;
import com.aarw.fitdata.fitbit.FitbitApiClient;
import com.aarw.fitdata.fitbit.dto.FitbitSleepResponse;
import com.aarw.fitdata.fitbit.util.SleepScoreEstimator;
import com.aarw.fitdata.oauth.token.FitbitTokenService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class SleepService {

    private final FitbitTokenService tokenService;
    private final FitbitApiClient apiClient;

    public SleepService(FitbitTokenService tokenService, FitbitApiClient apiClient) {
        this.tokenService = tokenService;
        this.apiClient = apiClient;
    }

    public SleepDto getSleep(LocalDate date) {
        var token = tokenService.getValidTokenOrThrow();
        FitbitSleepResponse raw = apiClient.getSleep(token, date.toString());

        if (raw.sleep() == null || raw.sleep().isEmpty()) {
            return new SleepDto(date.toString(), 0, 0, null, null, null, new SleepDto.LevelsSummary(0, 0, 0, 0), Collections.emptyList());
        }

        // Generally isMainSleep=true is the main sleep data.
        FitbitSleepResponse.SleepLog mainSleep = raw.sleep().stream()
                .filter(it -> it.isMainSleep() != null && it.isMainSleep())
                .findFirst()
                .orElse(raw.sleep().getFirst());

        List<SleepDto.SleepLevelSegment> segments = new ArrayList<>();
        if (mainSleep.levels() != null && mainSleep.levels().data() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
            for (FitbitSleepResponse.Levels.DataPoint dp : mainSleep.levels().data()) {
                LocalDateTime startTime = LocalDateTime.parse(dp.dateTime(), formatter);
                segments.add(new SleepDto.SleepLevelSegment(startTime, dp.level(), dp.seconds() != null ? dp.seconds() : 0));
            }
        }

        SleepDto.LevelsSummary summary = new SleepDto.LevelsSummary(0, 0, 0, 0);
        if (mainSleep.levels() != null && mainSleep.levels().summary() != null) {
            var s = mainSleep.levels().summary();
            summary = new SleepDto.LevelsSummary(
                    s.deep() != null && s.deep().minutes() != null ? s.deep().minutes() : 0,
                    s.light() != null && s.light().minutes() != null ? s.light().minutes() : 0,
                    s.rem() != null && s.rem().minutes() != null ? s.rem().minutes() : 0,
                    s.wake() != null && s.wake().minutes() != null ? s.wake().minutes() : 0
            );
        }

        // Estimate sleep score using SleepScoreEstimator
        int totalSleepMin = mainSleep.minutesAsleep() != null ? mainSleep.minutesAsleep().intValue() : 0;
        int remMin = summary.rem();
        int deepMin = summary.deep();
        int awakeMin = mainSleep.minutesAwake() != null ? mainSleep.minutesAwake().intValue() : 0;
        int sessionCount = raw.sleep().size();
        int longestSessionMin = mainSleep.duration() != null ? (int) (mainSleep.duration() / 60000) : 0;

        int finalScore = 0;
        try {
            if (totalSleepMin > 0) {
                var inputs = new SleepScoreEstimator.SleepInputs(
                        totalSleepMin, remMin, deepMin, awakeMin, sessionCount, longestSessionMin
                );
                finalScore = SleepScoreEstimator.estimate(inputs).score();
            }
        } catch (Exception e) {
            // Fallback to existing scores if estimation fails
            if (mainSleep.sleep_score() != null && mainSleep.sleep_score() > 0) {
                finalScore = mainSleep.sleep_score();
            } else if (mainSleep.efficiency_score() != null && mainSleep.efficiency_score() > 0) {
                finalScore = mainSleep.efficiency_score();
            } else if (mainSleep.efficiency() != null) {
                finalScore = mainSleep.efficiency().intValue();
            }
        }

        return new SleepDto(
                mainSleep.dateOfSleep(),
                totalSleepMin,
                mainSleep.timeInBed() != null ? mainSleep.timeInBed().intValue() : 0,
                finalScore,
                mainSleep.startTime(),
                mainSleep.endTime(),
                summary,
                segments
        );
    }
}
