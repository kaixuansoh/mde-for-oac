package com.acme.banking.transactions;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

/**
 * Tracer for the TransactionService.
 *
 * Single root span (InitiateTransfer) with four child spans covering
 * the fan-out to fraud detection, account debit, ledger persistence,
 * and downstream notification dispatch.
 */
public final class TransactionServiceInstrumentation {

    private final Tracer tracer = OpenTelemetry.getGlobalOpenTelemetry()
            .getTracer("com.acme.banking.transactions", "4.0.0");

    public Span startInitiateTransfer() {
        return tracer.spanBuilder("InitiateTransfer")
                .setSpanKind(SpanKind.SERVER)
                .setAttribute("http.request.method", "POST")
                .setAttribute("http.route", "/transfers")
                .startSpan();
    }

    public Span startCheckFraud(Context parent) {
        return clientChild("CheckFraud", parent)
                .setAttribute("net.peer.name", "fraud-service")
                .setAttribute("rpc.system", "grpc")
                .startSpan();
    }

    public Span startDebitSourceAccount(Context parent) {
        return clientChild("DebitSourceAccount", parent)
                .setAttribute("net.peer.name", "account-service")
                .startSpan();
    }

    public Span startRecordLedgerEntry(Context parent) {
        return clientChild("RecordLedgerEntry", parent)
                .setAttribute("net.peer.name", "ledger-service")
                .startSpan();
    }

    public Span startSendNotification(Context parent) {
        return tracer.spanBuilder("SendNotification")
                .setSpanKind(SpanKind.PRODUCER)
                .setParent(parent)
                .setAttribute("messaging.system", "kafka")
                .setAttribute("messaging.destination", "transaction.completed")
                .startSpan();
    }

    private SpanBuilder clientChild(String name, Context parent) {
        return tracer.spanBuilder(name).setSpanKind(SpanKind.CLIENT).setParent(parent);
    }
}
