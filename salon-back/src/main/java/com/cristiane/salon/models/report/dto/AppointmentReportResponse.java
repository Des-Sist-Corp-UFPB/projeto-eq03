package com.cristiane.salon.models.report.dto;

import java.util.Map;

public record AppointmentReportResponse(
        long totalAppointments,
        long pending,
        long confirmed,
        long done,
        long cancelled,
        Map<String, Long> byEmployee,
        Map<String, Long> byService,
        String period
) {}
