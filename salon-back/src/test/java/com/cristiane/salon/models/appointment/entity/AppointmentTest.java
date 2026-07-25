package com.cristiane.salon.models.appointment.entity;

import com.cristiane.salon.models.service.entity.SalonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentTest {

    private Appointment appointment;
    private SalonService salonService;

    @BeforeEach
    void setUp() {
        salonService = new SalonService();
        salonService.setPrice(new BigDecimal("100.00"));
        salonService.setDurationMin(45);

        appointment = new Appointment();
        appointment.setSalonService(salonService);
    }

    @Test
    void getEffectivePrice_whenNoCustomPrice_shouldReturnCatalogPrice() {
        assertThat(appointment.getEffectivePrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void getEffectivePrice_whenCustomPriceSet_shouldReturnCustomPriceAndLeaveCatalogUntouched() {
        appointment.setCustomPrice(new BigDecimal("200.00"));

        assertThat(appointment.getEffectivePrice()).isEqualByComparingTo("200.00");
        assertThat(salonService.getPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void getEffectiveDurationMin_whenNoCustomDuration_shouldReturnCatalogDuration() {
        assertThat(appointment.getEffectiveDurationMin()).isEqualTo(45);
    }

    @Test
    void getEffectiveDurationMin_whenCustomDurationSet_shouldReturnCustomDurationAndLeaveCatalogUntouched() {
        appointment.setCustomDurationMin(90);

        assertThat(appointment.getEffectiveDurationMin()).isEqualTo(90);
        assertThat(salonService.getDurationMin()).isEqualTo(45);
    }
}
