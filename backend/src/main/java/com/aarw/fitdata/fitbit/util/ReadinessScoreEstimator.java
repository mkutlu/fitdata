package com.aarw.fitdata.fitbit.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ReadinessScoreEstimator {

    private static final Logger log =
            LoggerFactory.getLogger(ReadinessScoreEstimator.class);

    private ReadinessScoreEstimator() {}

    public static int estimate(ReadinessInputs in) {

        int hrvScore = scoreHrv(in.hrvPercentChange());
        int rhrScore = scoreRhr(in.rhrDeltaBpm());
        int sleepScore = scoreSleepTrend(in.sleepTrend());
        int strainPenalty = scoreStrain(in.activityLoad());

        int raw = hrvScore + rhrScore + sleepScore + strainPenalty + 10;
        int finalScore = clamp(raw);

        log.info("""
            Readiness calculated:
              HRV score       = {}
              RHR score       = {}
              Sleep score     = {}
              Strain penalty  = {}
              Raw score       = {}
              Final score     = {}
            """,
                hrvScore, rhrScore, sleepScore, strainPenalty, raw, finalScore
        );

        return finalScore;
    }

    /* ---------------- Components ---------------- */

    private static int scoreHrv(double percent) {
        return percent >= 25 ? 40 :
                percent >= 15 ? 34 :
                        percent >= 5  ? 28 :
                                percent >= -5 ? 22 : 15;
    }

    private static int scoreRhr(int delta) {
        return delta <= -4 ? 25 :
                delta == -3 ? 23 :
                        delta == -2 ? 21 :
                                delta == -1 ? 18 :
                                        delta == 0  ? 15 : 10;
    }

    private static int scoreSleepTrend(SleepTrend trend) {
        return switch (trend) {
            case EXCELLENT -> 20;
            case GOOD -> 17;
            case FAIR -> 13;
            case POOR -> 8;
        };
    }

    private static int scoreStrain(ActivityLoad load) {
        return switch (load) {
            case REST -> 0;
            case LOW -> -3;
            case MODERATE -> -5;
            case HIGH -> -10;
            case VERY_HIGH -> -15;
        };
    }

    private static int clamp(int v) {
        return Math.max(1, Math.min(100, v));
    }
}
