package com.aarw.fitdata.fitbit.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ReadinessScoreEstimator {

    private static final Logger log =
            LoggerFactory.getLogger(ReadinessScoreEstimator.class);

    private ReadinessScoreEstimator() {}

    /* ============================================================
       Public API
       ============================================================ */

    public static int estimate(ReadinessInputs in) {

        int ceiling = readinessCeiling(in.hrvPercentChange());
        int base = baseScore(in.hrvPercentChange());

        int sleepAdj = sleepAdjustment(in.sleepTrend());
        int rhrAdj = rhrAdjustment(in.rhrDeltaBpm());
        int strainAdj = strainPenalty(in.activityLoad());

        int raw = base + sleepAdj + rhrAdj + strainAdj;

        // Fitbit behaviour: HRV defines the maximum possible score
        int capped = Math.min(raw, ceiling);
        int finalScore = clamp(capped);

        log.info("""
            Readiness v3 calculation
            -------------------------
            HRV % change      : {}
            HRV ceiling       : {}
            Base score        : {}
            Sleep adjustment  : {}
            RHR adjustment    : {}
            Strain penalty    : {}
            Raw score         : {}
            Final score       : {}
            """,
                in.hrvPercentChange(),
                ceiling,
                base,
                sleepAdj,
                rhrAdj,
                strainAdj,
                raw,
                finalScore
        );

        return finalScore;
    }

    /* ============================================================
       HRV → score ceiling (most important rule)
       ============================================================ */

    private static int readinessCeiling(double hrvPct) {
        if (hrvPct >= 20) return 100;
        if (hrvPct >= 10) return 85;
        if (hrvPct >= 5)  return 75;
        if (hrvPct >= -5) return 70;   // "About usual"
        return 60;
    }

    /* ============================================================
       HRV → base score
       ============================================================ */

    private static int baseScore(double hrvPct) {
        if (hrvPct >= 20) return 88;
        if (hrvPct >= 10) return 75;
        if (hrvPct >= 5)  return 68;
        if (hrvPct >= -5) return 65;
        return 55;
    }

    /* ============================================================
       Sleep trend (minor trim, never dominant)
       ============================================================ */

    private static int sleepAdjustment(SleepTrend trend) {
        if (trend == null) return 0;
        return switch (trend) {
            case EXCELLENT -> +4;
            case GOOD -> +2;
            case FAIR -> -3;
            case POOR -> -7;
        };
    }

    /* ============================================================
       Resting heart rate (fine tuning)
       ============================================================ */

    private static int rhrAdjustment(int delta) {
        if (delta <= -3) return +4;
        if (delta == -2) return +3;
        if (delta == -1) return +1;
        if (delta == 0) return 0;
        return -3;
    }

    /* ============================================================
       Activity load / strain (strong penalty)
       ============================================================ */

    private static int strainPenalty(ActivityLoad load) {
        if (load == null) return 0;
        return switch (load) {
            case REST -> 0;
            case LOW -> -3;
            case MODERATE -> -8;
            case HIGH -> -15;
            case VERY_HIGH -> -22;
        };
    }

    /* ============================================================
       Utilities
       ============================================================ */

    private static int clamp(int v) {
        return Math.max(1, Math.min(100, v));
    }

    /* ============================================================
       Supporting types
       ============================================================ */

    public record ReadinessInputs(
            double hrvPercentChange,
            int rhrDeltaBpm,
            SleepTrend sleepTrend,
            ActivityLoad activityLoad
    ) {}

    public enum SleepTrend {
        EXCELLENT,
        GOOD,
        FAIR,
        POOR
    }

    public enum ActivityLoad {
        REST,
        LOW,
        MODERATE,
        HIGH,
        VERY_HIGH
    }
}
