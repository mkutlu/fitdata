package com.aarw.fitdata.dto;

import java.time.LocalDate;

public record ReadinessCardDto(
        LocalDate date,
        Integer readinessScore,
        Integer cardioLoadScore,
        Integer cardioLoadTargetMin,
        Integer cardioLoadTargetMax,
        String readinessStatus,
        String cardioLoadStatus,
        String vo2Max
) {}
