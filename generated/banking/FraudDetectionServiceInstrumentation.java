package com.acme.banking.fraud;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

/**
 * Auto-generated OpenTelemetry instrumentation for FraudDetectionService.
 * Generated from observability model — do not edit by hand.
 */
public final class FraudDetectionServiceInstrumentation {

    private static final Tracer COM_ACME_BANKING_FRAUD_TRACER =
        GlobalOpenTelemetry.get()
            .getTracer("com.acme.banking.fraud", "1.8.0");

    public Span startScoreTransaction() {
        SpanBuilder builder = COM_ACME_BANKING_FRAUD_TRACER
            .spanBuilder("ScoreTransaction")
            .setSpanKind(SpanKind.SERVER);
        builder.setAttribute("rpc.system", "grpc");
        Span span = builder.startSpan();
        return span;
    }

    public Span startLookupRiskRules(Context parentContext) {
        SpanBuilder builder = COM_ACME_BANKING_FRAUD_TRACER
            .spanBuilder("LookupRiskRules")
            .setSpanKind(SpanKind.INTERNAL)
            .setParent(parentContext);
        builder.setAttribute("component", "rules-engine");
        Span span = builder.startSpan();
        return span;
    }

    public Span startQueryFraudHistory(Context parentContext) {
        SpanBuilder builder = COM_ACME_BANKING_FRAUD_TRACER
            .spanBuilder("QueryFraudHistory")
            .setSpanKind(SpanKind.CLIENT)
            .setParent(parentContext);
        builder.setAttribute("db.system", "cassandra");
        Span span = builder.startSpan();
        return span;
    }

}