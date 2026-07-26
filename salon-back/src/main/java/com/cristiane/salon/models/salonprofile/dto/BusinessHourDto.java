package com.cristiane.salon.models.salonprofile.dto;

import com.cristiane.salon.models.salonprofile.entity.BusinessHour;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record BusinessHourDto(
        @NotNull DayOfWeek dayOfWeek,
        boolean open,
        LocalTime openTime,
        LocalTime closeTime
) {
    public static BusinessHourDto fromEntity(BusinessHour entity) {
        return new BusinessHourDto(entity.getDayOfWeek(), entity.isOpen(), entity.getOpenTime(), entity.getCloseTime());
    }
}
