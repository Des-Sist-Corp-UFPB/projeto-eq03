package com.cristiane.salon.models.appointment.service;

import com.cristiane.salon.integrations.email.service.EmailService;
import com.cristiane.salon.models.appointment.entity.Appointment;
import com.cristiane.salon.models.appointment.repository.AppointmentRepository;
import com.cristiane.salon.models.featureflag.service.FeatureFlagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentReminderServiceTest {

    private static final ZoneId RECIFE = ZoneId.of("America/Recife");

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private FeatureFlagService featureFlagService;

    @InjectMocks
    private AppointmentReminderService reminderService;

    private Appointment appointment(Long id) {
        Appointment appointment = new Appointment();
        appointment.setId(id);
        return appointment;
    }

    @BeforeEach
    void setUp() {
        lenient().when(featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")).thenReturn(true);
    }

    @Test
    void sendDailyReminders_whenFeatureFlagDisabled_doesNothing() {
        when(featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")).thenReturn(false);

        reminderService.sendDailyReminders();

        verifyNoInteractions(appointmentRepository, emailService);
    }

    @Test
    void sendDailyReminders_queriesTomorrowInBusinessTimeZone_notJvmDefault() {
        when(appointmentRepository.findConfirmedNotRemindedBetween(any(), any())).thenReturn(List.of());

        reminderService.sendDailyReminders();

        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(appointmentRepository).findConfirmedNotRemindedBetween(startCaptor.capture(), endCaptor.capture());

        LocalDate expectedTomorrow = LocalDate.now(RECIFE).plusDays(1);
        assertThat(startCaptor.getValue()).isEqualTo(expectedTomorrow.atStartOfDay());
        assertThat(endCaptor.getValue()).isEqualTo(expectedTomorrow.plusDays(1).atStartOfDay());
    }

    @Test
    void sendDailyReminders_forEachEligibleAppointment_sendsEmailAndMarksRemindedAtIndividually() {
        Appointment a1 = appointment(1L);
        Appointment a2 = appointment(2L);
        when(appointmentRepository.findConfirmedNotRemindedBetween(any(), any())).thenReturn(List.of(a1, a2));

        reminderService.sendDailyReminders();

        verify(emailService).sendAppointmentReminder(a1);
        verify(emailService).sendAppointmentReminder(a2);
        assertThat(a1.getRemindedAt()).isNotNull();
        assertThat(a2.getRemindedAt()).isNotNull();
        verify(appointmentRepository).save(a1);
        verify(appointmentRepository).save(a2);
    }

    @Test
    void sendDailyReminders_whenNoneEligible_sendsNothing() {
        when(appointmentRepository.findConfirmedNotRemindedBetween(any(), any())).thenReturn(List.of());

        reminderService.sendDailyReminders();

        verifyNoInteractions(emailService);
        verify(appointmentRepository, never()).save(any());
    }
}
