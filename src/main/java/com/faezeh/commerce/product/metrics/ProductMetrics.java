package com.faezeh.commerce.product.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ProductMetrics {

    private final Counter productCreatedCounter;
    private final Counter productUpdatedCounter;
    private final Counter productDeletedCounter;
    private final Counter productLookupCounter;
    private final Counter productListCounter;

    public ProductMetrics(MeterRegistry meterRegistry) {
        this.productCreatedCounter = meterRegistry.counter("product.created.count");
        this.productUpdatedCounter = meterRegistry.counter("product.updated.count");
        this.productDeletedCounter = meterRegistry.counter("product.deleted.count");
        this.productLookupCounter = meterRegistry.counter("product.lookup.count");
        this.productListCounter = meterRegistry.counter("product.list.count");
    }

    public void productCreated() {
        productCreatedCounter.increment();
    }

    public void productUpdated() {
        productUpdatedCounter.increment();
    }

    public void productDeleted() {
        productDeletedCounter.increment();
    }

    public void productLookup() {
        productLookupCounter.increment();
    }

    public void productList() {
        productListCounter.increment();
    }
}
