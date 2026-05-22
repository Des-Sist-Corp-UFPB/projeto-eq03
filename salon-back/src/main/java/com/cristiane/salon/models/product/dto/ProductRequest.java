package com.cristiane.salon.models.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres")
        String name,

        @Min(value = 0, message = "O estoque não pode ser negativo")
        Integer stock,

        @NotNull(message = "O preço é obrigatório")
        @DecimalMin(value = "0.00", message = "O preço não pode ser negativo")
        BigDecimal price,

        Boolean active
) {}
