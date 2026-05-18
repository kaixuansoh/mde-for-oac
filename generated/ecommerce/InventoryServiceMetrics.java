package com.acme.inventory;

import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongGauge;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.GlobalOpenTelemetry;

/**
 * Auto-generated OpenTelemetry metric registry for InventoryService.
 * Generated from observability model — do not edit by hand.
 */
public final class InventoryServiceMetrics {

    private static final Meter COM_ACME_INVENTORY_METER =
        GlobalOpenTelemetry.get()
            .getMeter("com.acme.inventory");

    public final LongCounter stock_queries_total = COM_ACME_INVENTORY_METER
        .counterBuilder("stock.queries.total").setUnit("1").setDescription("Total stock-check queries").build();

    public final LongGauge stock_level = COM_ACME_INVENTORY_METER
        .gaugeBuilder("stock.level").ofLongs().setUnit("1").setDescription("Current stock units in inventory").build();

    public final LongCounter inventory_errors_total = COM_ACME_INVENTORY_METER
        .counterBuilder("inventory.errors.total").setUnit("1").setDescription("Inventory query failures").build();

}