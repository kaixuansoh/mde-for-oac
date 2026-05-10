package com.acme.banking.fraud;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;

import java.util.List;

public final class FraudDetectionServiceMetrics {

    // Risk-score buckets (dimensionless, 0..1)
    private static final List<Double> SCORE_BUCKETS = List.of(
            0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9);

    public final LongCounter scoresComputed;
    public final DoubleHistogram score;
    public final LongCounter flaggedTotal;

    public FraudDetectionServiceMetrics() {
        Meter m = OpenTelemetry.getGlobalOpenTelemetry().getMeter("com.acme.banking.fraud");
        scoresComputed = m.counterBuilder("fraud.scores.computed")
                .setUnit("1")
                .setDescription("Risk scores computed")
                .build();
        score = m.histogramBuilder("fraud.score")
                .setUnit("1")
                .setDescription("Distribution of risk scores")
                .setExplicitBucketBoundariesAdvice(SCORE_BUCKETS)
                .build();
        flaggedTotal = m.counterBuilder("fraud.flagged.total")
                .setUnit("1")
                .setDescription("Transactions flagged as high-risk")
                .build();
    }
}
