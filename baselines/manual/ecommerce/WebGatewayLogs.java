package com.acme.gateway;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

public final class WebGatewayLogs {

    private static final AttributeKey<String> UPSTREAM = AttributeKey.stringKey("upstream");

    private final Logger logger = OpenTelemetry.getGlobalOpenTelemetry()
            .getLogsBridge().get("com.acme.gateway");

    public void upstreamUnavailable(String upstream) {
        logger.logRecordBuilder()
                .setSeverity(Severity.ERROR)
                .setSeverityText("ERROR")
                .setBody("Upstream service unavailable")
                .setAttribute(UPSTREAM, upstream)
                .emit();
    }

    public void requestHandledSuccessfully() {
        logger.logRecordBuilder()
                .setSeverity(Severity.INFO)
                .setSeverityText("INFO")
                .setBody("Request handled successfully")
                .emit();
    }
}
