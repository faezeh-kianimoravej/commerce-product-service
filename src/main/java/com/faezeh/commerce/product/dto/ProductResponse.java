package com.faezeh.commerce.product.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Product response")
public record ProductResponse(
        @Schema(description = "Product identifier", example = "1")
        Long id,

        @Schema(description = "Unique product SKU", example = "TSHIRT-BLACK-L")
        String sku,

        @Schema(description = "Product display name", example = "Black T-Shirt Large")
        String name,

        @Schema(description = "Product description", example = "Black cotton T-shirt, size large")
        String description,

        @Schema(description = "Product price", example = "29.99")
        BigDecimal price,

        @Schema(description = "Available inventory quantity", example = "20")
        Integer availableQuantity,

        @Schema(description = "Whether the product is active", example = "true")
        Boolean active
) {
}
