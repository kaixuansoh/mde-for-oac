package com.acme.banking.ledger;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

/**
 * Auto-generated OpenTelemetry instrumentation for LedgerService.
 * Generated from observability model — do not edit by hand.
 */
public final class LedgerServiceInstrumentation {

    private static final Tracer COM_ACME_BANKING_LEDGER_TRACER =
        OpenTelemetry.getGlobalOpenTelemetry()
            .getTracer("com.acme.banking.ledger", "2.0.0");

    public Span startRecordEntry() {
        SpanBuilder builder = COM_ACME_BANKING_LEDGER_TRACER
            .spanBuilder("RecordEntry")
            .setSpanKind(SpanKind.SERVER);
        builder.setAttribute("rpc.system", "grpc");
        Span span = builder.startSpan();
        return span;
    }

    public Span startPersistLedgerEntry(Context parentContext) {
        SpanBuilder builder = COM_ACME_BANKING_LEDGER_TRACER
            .spanBuilder("PersistLedgerEntry")
            .setSpanKind(SpanKind.CLIENT)
            .setParent(parentContext);
        builder.setAttribute("db.system", "postgresql");
        builder.setAttribute("db.operation", "INSERT");
        Span span = builder.startSpan();
        return span;
    }

}