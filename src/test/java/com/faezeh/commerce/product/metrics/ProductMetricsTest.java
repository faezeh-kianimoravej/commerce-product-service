package com.faezeh.commerce.product.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductMetricsTest {

    private SimpleMeterRegistry meterRegistry;
    private ProductMetrics productMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        productMetrics = new ProductMetrics(meterRegistry);
    }

    @Test
    void incrementsProductCountersWithExpectedMetricNames() {
        productMetrics.productCreated();
        productMetrics.productUpdated();
        productMetrics.productDeleted();
        productMetrics.productLookup();
        productMetrics.productList();

        assertThat(counterCount("product.created.count")).isEqualTo(1.0);
        assertThat(counterCount("product.updated.count")).isEqualTo(1.0);
        assertThat(counterCount("product.deleted.count")).isEqualTo(1.0);
        assertThat(counterCount("product.lookup.count")).isEqualTo(1.0);
        assertThat(counterCount("product.list.count")).isEqualTo(1.0);
    }

    private double counterCount(String counterName) {
        return meterRegistry.counter(counterName).count();
    }
}
