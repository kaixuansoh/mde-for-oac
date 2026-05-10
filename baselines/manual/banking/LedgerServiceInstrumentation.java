package com.acme.banking.ledger;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

public final class LedgerServiceInstrumentation {

    private final Tracer tracer = OpenTelemetry.getGlobalOpenTelemetry()
            .getTracer("com.acme.banking.ledger", "2.0.0");

    public Span startRecordEntry() {
        return tracer.spanBuilder("RecordEntry")
                .setSpanKind(SpanKind.SERVER)
                .setAttribute("rpc.system", "grpc")
                .startSpan();
    }

    public Span startPersistLedgerEntry(Context parent) {
        return tracer.spanBuilder("PersistLedgerEntry")
                .setSpanKind(SpanKind.CLIENT)
                .setParent(parent)
                .setAttribute("db.system", "postgresql")
                .setAttribute("db.operation", "INSERT")
                .startSpan();
    }
}
