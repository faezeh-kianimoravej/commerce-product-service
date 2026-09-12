package com.faezeh.commerce.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Product availability response")
public record ProductAvailabilityResponse(
        @Schema(description = "Product identifier", example = "1")
        Long productId,

        @Schema(description = "Whether the product exists and is active", example = "true")
        Boolean existsAndActive,

        @Schema(description = "Whether the requested quantity is available", example = "true")
        Boolean available,

        @Schema(description = "Current available inventory quantity", example = "20")
        Integer availableQuantity
) {
}
