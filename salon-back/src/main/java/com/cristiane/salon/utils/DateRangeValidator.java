package com.cristiane.salon.utils;

import com.cristiane.salon.exception.BusinessException;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;

public class DateRangeValidator {

    private DateRangeValidator() {
        throw new IllegalStateException("Utility class");
    }

    /** Nulos são permitidos (filtro aberto de um dos lados) — só rejeita from > to quando ambos vêm preenchidos. */
    public static void validate(ChronoLocalDate from, ChronoLocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("A data inicial não pode ser posterior à data final");
        }
    }

    public static void validate(ChronoLocalDateTime<?> from, ChronoLocalDateTime<?> to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("A data/hora inicial não pode ser posterior à data/hora final");
        }
    }
}
