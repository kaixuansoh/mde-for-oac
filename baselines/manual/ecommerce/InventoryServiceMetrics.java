package com.acme.inventory;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongGauge;
import io.opentelemetry.api.metrics.Meter;

public final class InventoryServiceMetrics {

    public final LongCounter stockQueriesTotal;
    public final LongGauge stockLevel;
    public final LongCounter inventoryErrorsTotal;

    public InventoryServiceMetrics() {
        Meter m = OpenTelemetry.getGlobalOpenTelemetry().getMeter("com.acme.inventory");
        stockQueriesTotal = m.counterBuilder("stock.queries.total")
                .setUnit("1")
                .setDescription("Total stock-check queries")
                .build();
        stockLevel = m.gaugeBuilder("stock.level")
                .ofLongs()
                .setUnit("1")
                .setDescription("Current stock units in inventory")
                .build();
        inventoryErrorsTotal = m.counterBuilder("inventory.errors.total")
                .setUnit("1")
                .setDescription("Inventory query failures")
                .build();
    }
}
