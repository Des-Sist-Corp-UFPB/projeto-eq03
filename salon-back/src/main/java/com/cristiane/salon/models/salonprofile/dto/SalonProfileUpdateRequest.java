package com.cristiane.salon.models.salonprofile.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SalonProfileUpdateRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        @Size(max = 300) String address,
        @Size(max = 20) String phone,
        @Size(max = 150) String instagram,
        @Size(max = 20) String whatsapp,
        @Size(max = 500) String logoUrl,
        @NotEmpty @Valid List<BusinessHourDto> businessHours
) {
}
