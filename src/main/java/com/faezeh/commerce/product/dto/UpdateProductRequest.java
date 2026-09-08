package com.faezeh.commerce.product.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload for updating an active product")
public record UpdateProductRequest(
        @Schema(description = "Product display name", example = "Black T-Shirt Large", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 255)
        String name,

        @Schema(description = "Product description", example = "Black cotton T-shirt, size large")
        @Size(max = 2000)
        String description,

        @Schema(description = "Product price", example = "29.99", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Positive
        BigDecimal price,

        @Schema(description = "Available inventory quantity", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @PositiveOrZero
        Integer availableQuantity,

        @Schema(description = "Whether the product is active", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Boolean active
) {
}
