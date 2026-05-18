package com.acme.banking.accounts;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

/**
 * Auto-generated OpenTelemetry instrumentation for AccountService.
 * Generated from observability model — do not edit by hand.
 */
public final class AccountServiceInstrumentation {

    private static final Tracer COM_ACME_BANKING_ACCOUNTS_TRACER =
        GlobalOpenTelemetry.get()
            .getTracer("com.acme.banking.accounts", "2.5.0");

    public Span startGetAccountBalance() {
        SpanBuilder builder = COM_ACME_BANKING_ACCOUNTS_TRACER
            .spanBuilder("GetAccountBalance")
            .setSpanKind(SpanKind.SERVER);
        builder.setAttribute("http.request.method", "GET");
        builder.setAttribute("http.route", "/accounts/{id}");
        Span span = builder.startSpan();
        return span;
    }

    public Span startQueryAccountDB(Context parentContext) {
        SpanBuilder builder = COM_ACME_BANKING_ACCOUNTS_TRACER
            .spanBuilder("QueryAccountDB")
            .setSpanKind(SpanKind.CLIENT)
            .setParent(parentContext);
        builder.setAttribute("db.system", "postgresql");
        builder.setAttribute("db.operation", "SELECT");
        Span span = builder.startSpan();
        return span;
    }

    public Span startApplyDebit() {
        SpanBuilder builder = COM_ACME_BANKING_ACCOUNTS_TRACER
            .spanBuilder("ApplyDebit")
            .setSpanKind(SpanKind.INTERNAL);
        builder.setAttribute("component", "ledger");
        Span span = builder.startSpan();
        return span;
    }

}