package com.acme.gateway;

import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.GlobalOpenTelemetry;

/**
 * Auto-generated OpenTelemetry metric registry for WebGateway.
 * Generated from observability model — do not edit by hand.
 */
public final class WebGatewayMetrics {

    private static final Meter COM_ACME_GATEWAY_METER =
        GlobalOpenTelemetry.get()
            .getMeter("com.acme.gateway");

    public final LongCounter gateway_requests_total = COM_ACME_GATEWAY_METER
        .counterBuilder("gateway.requests.total").setUnit("1").setDescription("Total gateway requests handled").build();

    public final DoubleHistogram gateway_duration = COM_ACME_GATEWAY_METER
        .histogramBuilder("gateway.duration").setUnit("ms").setDescription("Gateway request latency").setExplicitBucketBoundariesAdvice(java.util.List.of(5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0, 2500.0, 5000.0, 10000.0)).build();

    public final LongCounter gateway_5xx_total = COM_ACME_GATEWAY_METER
        .counterBuilder("gateway.5xx.total").setUnit("1").setDescription("Server-side error responses").build();

    public final LongCounter gateway_4xx_total = COM_ACME_GATEWAY_METER
        .counterBuilder("gateway.4xx.total").setUnit("1").setDescription("Client-side error responses").build();

}