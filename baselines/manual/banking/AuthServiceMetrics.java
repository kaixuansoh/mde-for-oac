package com.acme.banking.auth;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;

import java.util.List;

/**
 * Metric registry for AuthService.
 */
public final class AuthServiceMetrics {

    private static final List<Double> LATENCY_BUCKETS = List.of(
            5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0,
            1000.0, 2500.0, 5000.0, 10000.0);

    public final LongCounter loginAttempts;
    public final LongCounter loginFailures;
    public final DoubleHistogram loginDuration;
    public final LongCounter tokensIssued;

    public AuthServiceMetrics() {
        Meter m = OpenTelemetry.getGlobalOpenTelemetry().getMeter("com.acme.banking.auth");
        loginAttempts = m.counterBuilder("auth.login.attempts")
                .setUnit("1")
                .setDescription("Total login attempts")
                .build();
        loginFailures = m.counterBuilder("auth.login.failures")
                .setUnit("1")
                .setDescription("Failed login attempts")
                .build();
        loginDuration = m.histogramBuilder("auth.login.duration")
                .setUnit("ms")
                .setDescription("Login latency")
                .setExplicitBucketBoundariesAdvice(LATENCY_BUCKETS)
                .build();
        tokensIssued = m.counterBuilder("auth.tokens.issued")
                .setUnit("1")
                .setDescription("Authentication tokens issued")
                .build();
    }
}
