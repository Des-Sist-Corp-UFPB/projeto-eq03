package com.cristiane.salon.models.appointment.entity;

import com.cristiane.salon.models.service.entity.SalonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentTest {

    private Appointment appointment;
    private SalonService haircut;
    private SalonService coloring;
    private AppointmentServiceItem haircutItem;
    private AppointmentServiceItem coloringItem;

    @BeforeEach
    void setUp() {
        haircut = new SalonService();
        haircut.setName("Corte");
        haircut.setPrice(new BigDecimal("100.00"));
        haircut.setDurationMin(45);

        coloring = new SalonService();
        coloring.setName("Coloração");
        coloring.setPrice(new BigDecimal("150.00"));
        coloring.setDurationMin(60);

        appointment = new Appointment();

        haircutItem = new AppointmentServiceItem();
        haircutItem.setAppointment(appointment);
        haircutItem.setSalonService(haircut);

        coloringItem = new AppointmentServiceItem();
        coloringItem.setAppointment(appointment);
        coloringItem.setSalonService(coloring);
    }

    @Test
    void getEffectivePrice_whenNoCustomPrice_shouldReturnCatalogPrice() {
        assertThat(haircutItem.getEffectivePrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void getEffectivePrice_whenCustomPriceSet_shouldReturnCustomPriceAndLeaveCatalogUntouched() {
        haircutItem.setCustomPrice(new BigDecimal("200.00"));

        assertThat(haircutItem.getEffectivePrice()).isEqualByComparingTo("200.00");
        assertThat(haircut.getPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void getEffectiveDurationMin_whenNoCustomDuration_shouldReturnCatalogDuration() {
        assertThat(haircutItem.getEffectiveDurationMin()).isEqualTo(45);
    }

    @Test
    void getEffectiveDurationMin_whenCustomDurationSet_shouldReturnCustomDurationAndLeaveCatalogUntouched() {
        haircutItem.setCustomDurationMin(90);

        assertThat(haircutItem.getEffectiveDurationMin()).isEqualTo(90);
        assertThat(haircut.getDurationMin()).isEqualTo(45);
    }

    @Test
    void getTotalEffectivePrice_withMultipleServices_shouldSumEffectivePrices() {
        haircutItem.setCustomPrice(new BigDecimal("120.00"));
        appointment.setServices(List.of(haircutItem, coloringItem));

        assertThat(appointment.getTotalEffectivePrice()).isEqualByComparingTo("270.00");
    }

    @Test
    void getTotalEffectiveDurationMin_withMultipleServices_shouldSumEffectiveDurations() {
        coloringItem.setCustomDurationMin(90);
        appointment.setServices(List.of(haircutItem, coloringItem));

        assertThat(appointment.getTotalEffectiveDurationMin()).isEqualTo(135);
    }

    @Test
    void getServiceNames_withMultipleServices_shouldJoinNamesWithComma() {
        appointment.setServices(List.of(haircutItem, coloringItem));

        assertThat(appointment.getServiceNames()).isEqualTo("Corte, Coloração");
    }
}
