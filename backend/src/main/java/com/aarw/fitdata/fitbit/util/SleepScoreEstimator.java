package com.aarw.fitdata.fitbit.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SleepScoreEstimator {

    private static final Logger log = LoggerFactory.getLogger(SleepScoreEstimator.class);

    private SleepScoreEstimator() {}

    public record SleepInputs(
            int totalSleepMin,
            int remMin,
            int deepMin,
            int awakeMin,
            int sessionCount,
            int longestSessionMin
    ) {}

    public record SleepScoreResult(
            int score,
            double durationScore,
            double qualityScore,
            double awakePenalty,
            double fragmentationPenalty,
            double remRatio,
            double deepRatio,
            double awakeRatio,
            double longestRatio
    ) {}

    public static SleepScoreResult estimate(SleepInputs in) {
        validate(in);

        final int T = in.totalSleepMin();
        final int REM = clampNonNegative(in.remMin());
        final int DEEP = clampNonNegative(in.deepMin());
        final int AWAKE = clampNonNegative(in.awakeMin());
        final int S = Math.max(1, in.sessionCount());
        final int L = Math.max(0, in.longestSessionMin());

        final double remRatio = REM / (double) T;
        final double deepRatio = DEEP / (double) T;
        final double awakeRatio = AWAKE / (double) T;

        final double longestRatio = (L <= 0) ? 1.0 : Math.min((double) L / (double) T, 1.0);

        // Duration (0–47)
        final double dur = Math.sqrt(Math.min(T, 480) / 480.0);
        final double durationScore = 47.0 * dur;

        // Quality (0–28)
        final double remTerm = 0.66 * cap01(remRatio / 0.23);
        final double deepTerm = 0.34 * cap01(deepRatio / 0.22);
        final double qualityScore = 28.0 * (remTerm + deepTerm);

        // Awake penalty (approx 0–14)
        final double awakePenalty = 11.2 * cap01(awakeRatio / 0.196) + 3.2 * cap01(AWAKE / 60.0);

        // Fragmentation penalty
        final boolean fragmented = (S > 1) || (longestRatio < 0.784);
        final double fragmentationPenalty;
        if (fragmented) {
            final double longestShortfall = Math.max(0.0, 0.784 - longestRatio);
            final double longestPenalty = 4.6 * cap01(longestShortfall / 0.401);
            final double sessionPenalty = 4.4 * Math.max(0, S - 1);
            fragmentationPenalty = longestPenalty + sessionPenalty;
        } else {
            fragmentationPenalty = 0.0;
        }

        final double raw = 20.0 + durationScore + qualityScore - awakePenalty - fragmentationPenalty;
        final int score = (int) Math.round(clamp(raw, 100.0));

        if (log.isDebugEnabled()) {
            log.debug(
                    "SleepScore estimate: score={} raw={} T={} REM={} DEEP={} AWAKE={} S={} L={} " +
                            "durationScore={} qualityScore={} awakePenalty={} fragPenalty={} " +
                            "ratios(rem={},deep={},awake={},longest={})",
                    score, round2(raw), T, REM, DEEP, AWAKE, S, L,
                    round2(durationScore), round2(qualityScore), round2(awakePenalty), round2(fragmentationPenalty),
                    round4(remRatio), round4(deepRatio), round4(awakeRatio), round4(longestRatio)
            );
        }

        return new SleepScoreResult(
                score,
                durationScore,
                qualityScore,
                awakePenalty,
                fragmentationPenalty,
                remRatio,
                deepRatio,
                awakeRatio,
                longestRatio
        );
    }

    private static void validate(SleepInputs in) {
        if (in == null) throw new IllegalArgumentException("inputs must not be null");
        if (in.totalSleepMin() <= 0) throw new IllegalArgumentException("totalSleepMin must be > 0");
        if (in.sessionCount() < 0) throw new IllegalArgumentException("sessionCount must be >= 0");
        if (in.longestSessionMin() < 0) throw new IllegalArgumentException("longestSessionMin must be >= 0");

        final int T = in.totalSleepMin();
        final int REM = clampNonNegative(in.remMin());
        final int DEEP = clampNonNegative(in.deepMin());
        final int AWAKE = clampNonNegative(in.awakeMin());

        if (REM > T || DEEP > T || AWAKE > T) {
            log.warn("SleepInputs look inconsistent: T={} REM={} DEEP={} AWAKE={}", T, REM, DEEP, AWAKE);
        }
    }

    private static int clampNonNegative(int v) {
        return Math.max(0, v);
    }

    private static double cap01(double v) {
        return clamp(v, 1.0);
    }

    private static double clamp(double v, double hi) {
        return Math.max(0.0, Math.min(hi, v));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
