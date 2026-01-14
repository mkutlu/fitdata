package com.aarw.fitdata.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SleepDto(
    String date,
    int totalMinutesAsleep,
    int totalTimeInBed,
    Integer sleepScore,
    String startTime,
    String endTime,
    LevelsSummary levelsSummary,
    List<SleepLevelSegment> segments
) {
    public record SleepLevelSegment(
        LocalDateTime startTime,
        String level,
        int durationSeconds
    ) {}

    public record LevelsSummary(
        int deep,
        int light,
        int rem,
        int awake
    ) {}
}
