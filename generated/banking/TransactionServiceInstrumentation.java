package com.acme.banking.transactions;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

/**
 * Auto-generated OpenTelemetry instrumentation for TransactionService.
 * Generated from observability model — do not edit by hand.
 */
public final class TransactionServiceInstrumentation {

    private static final Tracer COM_ACME_BANKING_TRANSACTIONS_TRACER =
        OpenTelemetry.getGlobalOpenTelemetry()
            .getTracer("com.acme.banking.transactions", "4.0.0");

    public Span startInitiateTransfer() {
        SpanBuilder builder = COM_ACME_BANKING_TRANSACTIONS_TRACER
            .spanBuilder("InitiateTransfer")
            .setSpanKind(SpanKind.SERVER);
        builder.setAttribute("http.request.method", "POST");
        builder.setAttribute("http.route", "/transfers");
        Span span = builder.startSpan();
        return span;
    }

    public Span startCheckFraud(Context parentContext) {
        SpanBuilder builder = COM_ACME_BANKING_TRANSACTIONS_TRACER
            .spanBuilder("CheckFraud")
            .setSpanKind(SpanKind.CLIENT)
            .setParent(parentContext);
        builder.setAttribute("net.peer.name", "fraud-service");
        builder.setAttribute("rpc.system", "grpc");
        Span span = builder.startSpan();
        return span;
    }

    public Span startDebitSourceAccount(Context parentContext) {
        SpanBuilder builder = COM_ACME_BANKING_TRANSACTIONS_TRACER
            .spanBuilder("DebitSourceAccount")
            .setSpanKind(SpanKind.CLIENT)
            .setParent(parentContext);
        builder.setAttribute("net.peer.name", "account-service");
        Span span = builder.startSpan();
        return span;
    }

    public Span startRecordLedgerEntry(Context parentContext) {
        SpanBuilder builder = COM_ACME_BANKING_TRANSACTIONS_TRACER
            .spanBuilder("RecordLedgerEntry")
            .setSpanKind(SpanKind.CLIENT)
            .setParent(parentContext);
        builder.setAttribute("net.peer.name", "ledger-service");
        Span span = builder.startSpan();
        return span;
    }

    public Span startSendNotification(Context parentContext) {
        SpanBuilder builder = COM_ACME_BANKING_TRANSACTIONS_TRACER
            .spanBuilder("SendNotification")
            .setSpanKind(SpanKind.PRODUCER)
            .setParent(parentContext);
        builder.setAttribute("messaging.system", "kafka");
        builder.setAttribute("messaging.destination", "transaction.completed");
        Span span = builder.startSpan();
        return span;
    }

}