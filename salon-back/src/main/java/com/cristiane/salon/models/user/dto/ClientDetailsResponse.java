package com.cristiane.salon.models.user.dto;

import com.cristiane.salon.models.appointment.dto.AppointmentResponse;
import java.time.LocalDateTime;
import java.util.List;

public record ClientDetailsResponse(
    Long id,
    String name,
    String email,
    String phone,
    String cpf,
    String role,
    Boolean active,
    LocalDateTime createdAt,
    Long totalAppointments,
    LocalDateTime lastAppointmentDate,
    List<AppointmentResponse> appointments
) {}
