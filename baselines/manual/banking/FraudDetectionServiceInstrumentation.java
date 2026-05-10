package com.acme.banking.fraud;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

public final class FraudDetectionServiceInstrumentation {

    private final Tracer tracer = OpenTelemetry.getGlobalOpenTelemetry()
            .getTracer("com.acme.banking.fraud", "1.8.0");

    public Span startScoreTransaction() {
        return tracer.spanBuilder("ScoreTransaction")
                .setSpanKind(SpanKind.SERVER)
                .setAttribute("rpc.system", "grpc")
                .startSpan();
    }

    public Span startLookupRiskRules(Context parent) {
        return tracer.spanBuilder("LookupRiskRules")
                .setSpanKind(SpanKind.INTERNAL)
                .setParent(parent)
                .setAttribute("component", "rules-engine")
                .startSpan();
    }

    public Span startQueryFraudHistory(Context parent) {
        return tracer.spanBuilder("QueryFraudHistory")
                .setSpanKind(SpanKind.CLIENT)
                .setParent(parent)
                .setAttribute("db.system", "cassandra")
                .startSpan();
    }
}
