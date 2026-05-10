package com.acme.inventory;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

/**
 * Auto-generated OpenTelemetry instrumentation for InventoryService.
 * Generated from observability model — do not edit by hand.
 */
public final class InventoryServiceInstrumentation {

    private static final Tracer COM_ACME_INVENTORY_TRACER =
        OpenTelemetry.getGlobalOpenTelemetry()
            .getTracer("com.acme.inventory", "1.0.0");

    public Span startCheckStock() {
        SpanBuilder builder = COM_ACME_INVENTORY_TRACER
            .spanBuilder("CheckStock")
            .setSpanKind(SpanKind.SERVER);
        builder.setAttribute("rpc.system", "grpc");
        builder.setAttribute("rpc.service", "Inventory");
        Span span = builder.startSpan();
        return span;
    }

    public Span startQueryStockDB(Context parentContext) {
        SpanBuilder builder = COM_ACME_INVENTORY_TRACER
            .spanBuilder("QueryStockDB")
            .setSpanKind(SpanKind.CLIENT)
            .setParent(parentContext);
        builder.setAttribute("db.system", "postgresql");
        builder.setAttribute("db.operation", "SELECT");
        Span span = builder.startSpan();
        return span;
    }

}