package com.cristiane.salon.models.salonprofile.dto;

import com.cristiane.salon.models.salonprofile.entity.SalonProfile;

import java.time.LocalDateTime;
import java.util.List;

/** Público — servido sem autenticação em {@code GET /v1/salon/profile}. Nenhum campo aqui é
 * sensível: é literalmente o que já aparece hoje impresso no cartão de visita do salão. */
public record SalonProfileResponse(
        Long id,
        String name,
        String description,
        String address,
        String phone,
        String instagram,
        String whatsapp,
        String logoUrl,
        LocalDateTime updatedAt,
        List<BusinessHourDto> businessHours
) {
    public static SalonProfileResponse fromEntity(SalonProfile profile, List<BusinessHourDto> businessHours) {
        return new SalonProfileResponse(
                profile.getId(),
                profile.getName(),
                profile.getDescription(),
                profile.getAddress(),
                profile.getPhone(),
                profile.getInstagram(),
                profile.getWhatsapp(),
                profile.getLogoUrl(),
                profile.getUpdatedAt(),
                businessHours
        );
    }
}
