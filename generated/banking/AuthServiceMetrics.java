package com.acme.banking.auth;

import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.OpenTelemetry;

/**
 * Auto-generated OpenTelemetry metric registry for AuthService.
 * Generated from observability model — do not edit by hand.
 */
public final class AuthServiceMetrics {

    private static final Meter COM_ACME_BANKING_AUTH_METER =
        OpenTelemetry.getGlobalOpenTelemetry()
            .getMeter("com.acme.banking.auth");

    public final LongCounter auth_login_attempts = COM_ACME_BANKING_AUTH_METER
        .counterBuilder("auth.login.attempts").setUnit("1").setDescription("Total login attempts").build();

    public final LongCounter auth_login_failures = COM_ACME_BANKING_AUTH_METER
        .counterBuilder("auth.login.failures").setUnit("1").setDescription("Failed login attempts").build();

    public final DoubleHistogram auth_login_duration = COM_ACME_BANKING_AUTH_METER
        .histogramBuilder("auth.login.duration").setUnit("ms").setDescription("Login latency").setExplicitBucketBoundariesAdvice(java.util.List.of(5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0, 2500.0, 5000.0, 10000.0)).build();

    public final LongCounter auth_tokens_issued = COM_ACME_BANKING_AUTH_METER
        .counterBuilder("auth.tokens.issued").setUnit("1").setDescription("Authentication tokens issued").build();

}