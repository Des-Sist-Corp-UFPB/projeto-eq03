package com.cristiane.salon.models.service.dto;

import com.cristiane.salon.models.service.entity.SalonService;

import java.math.BigDecimal;

public record SalonServiceResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer durationMin,
        Boolean active
) {
    public static SalonServiceResponse fromEntity(Service service) {
        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getPrice(),
                service.getDurationMin(),
                service.getActive()
        );
    }
}
