package com.acme.gateway;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;

import java.util.List;

/**
 * Metric registry for the WebGateway.
 */
public final class WebGatewayMetrics {

    private static final List<Double> DURATION_BUCKETS = List.of(
            5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0,
            1000.0, 2500.0, 5000.0, 10000.0);

    public final LongCounter requestsTotal;
    public final DoubleHistogram duration;
    public final LongCounter serverErrorsTotal;
    public final LongCounter clientErrorsTotal;

    public WebGatewayMetrics() {
        Meter m = OpenTelemetry.getGlobalOpenTelemetry().getMeter("com.acme.gateway");
        requestsTotal = m.counterBuilder("gateway.requests.total")
                .setUnit("1")
                .setDescription("Total gateway requests handled")
                .build();
        duration = m.histogramBuilder("gateway.duration")
                .setUnit("ms")
                .setDescription("Gateway request latency")
                .setExplicitBucketBoundariesAdvice(DURATION_BUCKETS)
                .build();
        serverErrorsTotal = m.counterBuilder("gateway.5xx.total")
                .setUnit("1")
                .setDescription("Server-side error responses")
                .build();
        clientErrorsTotal = m.counterBuilder("gateway.4xx.total")
                .setUnit("1")
                .setDescription("Client-side error responses")
                .build();
    }
}
