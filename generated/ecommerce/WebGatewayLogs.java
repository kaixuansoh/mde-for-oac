package com.acme.gateway;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

/**
 * Auto-generated OpenTelemetry log emitters for WebGateway.
 * Generated from observability model — do not edit by hand.
 */
public final class WebGatewayLogs {

    private static final Logger COM_ACME_GATEWAY_LOGGER =
        GlobalOpenTelemetry.get()
            .getLogsBridge()
            .get("com.acme.gateway");

    public void logUpstreamServiceUnavailable() {
        COM_ACME_GATEWAY_LOGGER.logRecordBuilder()
            .setSeverity(Severity.ERROR)
            .setSeverityText("ERROR")
            .setBody("Upstream service unavailable")
            .setAttribute(AttributeKey.stringKey("upstream"), "order-service")
            .emit();
    }

    public void logRequestHandledSuccessfully() {
        COM_ACME_GATEWAY_LOGGER.logRecordBuilder()
            .setSeverity(Severity.INFO)
            .setSeverityText("INFO")
            .setBody("Request handled successfully")
            .emit();
    }

}