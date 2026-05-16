package com.cristiane.salon.models.report.service;

import com.cristiane.salon.models.appointment.entity.Appointment;
import com.cristiane.salon.models.appointment.enums.AppointmentStatus;
import com.cristiane.salon.models.appointment.repository.AppointmentRepository;
import com.cristiane.salon.models.cashflow.entity.CashFlow;
import com.cristiane.salon.models.cashflow.enums.CashFlowType;
import com.cristiane.salon.models.cashflow.repository.CashFlowRepository;
import com.cristiane.salon.models.report.dto.AppointmentReportResponse;
import com.cristiane.salon.models.report.dto.FinancialReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final CashFlowRepository cashFlowRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public FinancialReportResponse generateFinancialReport(LocalDate from, LocalDate to) {
        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null) to = LocalDate.now();

        List<CashFlow> cashFlows = cashFlowRepository.findByDateBetween(from, to);

        BigDecimal income = cashFlows.stream()
                .filter(cf -> cf.getType() == CashFlowType.INCOME)
                .map(CashFlow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expense = cashFlows.stream()
                .filter(cf -> cf.getType() == CashFlowType.EXPENSE)
                .map(CashFlow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netProfit = income.subtract(expense);
        String period = from + " a " + to;

        return new FinancialReportResponse(income, expense, netProfit, period);
    }

    @Transactional(readOnly = true)
    public AppointmentReportResponse generateAppointmentReport(LocalDate from, LocalDate to) {
        final LocalDate fromDate = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        final LocalDate toDate = to == null ? LocalDate.now() : to;

        List<Appointment> appointments = appointmentRepository.findAll().stream()
                .filter(a -> isAppointmentInReportPeriod(a, fromDate, toDate))
                .collect(Collectors.toList());

        long pending = appointments.stream().filter(a ->
                a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.REQUESTED).count();
        long confirmed = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED).count();
        long done = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.DONE).count();
        long cancelled = appointments.stream().filter(a ->
                a.getStatus() == AppointmentStatus.CANCELLED || a.getStatus() == AppointmentStatus.DECLINED).count();

        Map<String, Long> byEmployee = appointments.stream()
                .collect(Collectors.groupingBy(a -> a.getEmployee().getUser().getName(), Collectors.counting()));

        Map<String, Long> byService = appointments.stream()
                .collect(Collectors.groupingBy(a -> a.getSalonService().getName(), Collectors.counting()));

        String period = fromDate + " a " + toDate;

        return new AppointmentReportResponse(
                appointments.size(),
                pending,
                confirmed,
                done,
                cancelled,
                byEmployee,
                byService,
                period
        );
    }

    private static boolean isAppointmentInReportPeriod(Appointment a, LocalDate from, LocalDate to) {
        LocalDateTime startOfDay = from.atStartOfDay();
        LocalDateTime endOfDay = to.atTime(LocalTime.MAX);
        if (a.getScheduledAt() != null) {
            return !a.getScheduledAt().isBefore(startOfDay) && !a.getScheduledAt().isAfter(endOfDay);
        }
        if (a.getPreferredDate() != null) {
            return !a.getPreferredDate().isBefore(from) && !a.getPreferredDate().isAfter(to);
        }
        if (a.getCreatedAt() != null) {
            LocalDate d = a.getCreatedAt().toLocalDate();
            return !d.isBefore(from) && !d.isAfter(to);
        }
        return false;
    }
}
