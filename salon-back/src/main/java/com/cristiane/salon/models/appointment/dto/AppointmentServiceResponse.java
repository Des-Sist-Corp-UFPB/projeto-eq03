package com.cristiane.salon.models.appointment.dto;

import com.cristiane.salon.models.appointment.entity.AppointmentServiceItem;

import java.math.BigDecimal;

public record AppointmentServiceResponse(
        Long serviceId,
        String serviceName,
        BigDecimal catalogPrice,
        Integer catalogDurationMin,
        /** Sobrescreve o preço do serviço só para este item (nulo = usa o valor do catálogo). */
        BigDecimal customPrice,
        Integer customDurationMin,
        String customServiceNotes,
        /** Valor realmente cobrado/considerado: customPrice se preenchido, senão o preço do serviço. */
        BigDecimal effectivePrice,
        Integer effectiveDurationMin
) {
    public static AppointmentServiceResponse fromEntity(AppointmentServiceItem item) {
        return new AppointmentServiceResponse(
                item.getSalonService().getId(),
                item.getSalonService().getName(),
                item.getSalonService().getPrice(),
                item.getSalonService().getDurationMin(),
                item.getCustomPrice(),
                item.getCustomDurationMin(),
                item.getCustomServiceNotes(),
                item.getEffectivePrice(),
                item.getEffectiveDurationMin()
        );
    }
}
