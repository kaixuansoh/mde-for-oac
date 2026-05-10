package com.acme.inventory;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

public final class InventoryServiceInstrumentation {

    private final Tracer tracer = OpenTelemetry.getGlobalOpenTelemetry()
            .getTracer("com.acme.inventory", "1.0.0");

    public Span startCheckStock() {
        return tracer.spanBuilder("CheckStock")
                .setSpanKind(SpanKind.SERVER)
                .setAttribute("rpc.system", "grpc")
                .setAttribute("rpc.service", "Inventory")
                .startSpan();
    }

    public Span startQueryStockDB(Context parent) {
        return tracer.spanBuilder("QueryStockDB")
                .setSpanKind(SpanKind.CLIENT)
                .setParent(parent)
                .setAttribute("db.system", "postgresql")
                .setAttribute("db.operation", "SELECT")
                .startSpan();
    }
}
