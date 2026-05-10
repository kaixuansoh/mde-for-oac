package com.acme.banking.accounts;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

public final class AccountServiceInstrumentation {

    private final Tracer tracer = OpenTelemetry.getGlobalOpenTelemetry()
            .getTracer("com.acme.banking.accounts", "2.5.0");

    public Span startGetAccountBalance() {
        return tracer.spanBuilder("GetAccountBalance")
                .setSpanKind(SpanKind.SERVER)
                .setAttribute("http.request.method", "GET")
                .setAttribute("http.route", "/accounts/{id}")
                .startSpan();
    }

    public Span startQueryAccountDB(Context parent) {
        return tracer.spanBuilder("QueryAccountDB")
                .setSpanKind(SpanKind.CLIENT)
                .setParent(parent)
                .setAttribute("db.system", "postgresql")
                .setAttribute("db.operation", "SELECT")
                .startSpan();
    }

    public Span startApplyDebit() {
        return tracer.spanBuilder("ApplyDebit")
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("component", "ledger")
                .startSpan();
    }
}
