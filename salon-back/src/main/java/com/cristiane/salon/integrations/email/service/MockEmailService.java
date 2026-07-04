package com.cristiane.salon.integrations.email.service;

import com.cristiane.salon.models.appointment.entity.Appointment;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("performance")
public class MockEmailService extends EmailService {

    public MockEmailService() {
        super(null, null, null);
    }

    @Override
    public void sendRequestNotificationToStaff(Appointment appointment) {}

    @Override
    public void sendConfirmationNotificationToClient(Appointment appointment) {}

    @Override
    public void sendPaymentConfirmationNotificationToClient(Appointment appointment) {}

    @Override
    public void sendCancellationNotification(Appointment appointment) {}
}
