package com.cristiane.salon.models.product.dto;

import com.cristiane.salon.models.product.entity.Product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        Integer stock,
        BigDecimal price
) {
    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getStock(),
                product.getPrice()
        );
    }
}
