package com.cristiane.salon.models.report.service;

import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.models.appointment.entity.Appointment;
import com.cristiane.salon.models.appointment.enums.AppointmentStatus;
import com.cristiane.salon.models.appointment.repository.AppointmentRepository;
import com.cristiane.salon.models.cashflow.entity.CashFlow;
import com.cristiane.salon.models.cashflow.enums.CashFlowType;
import com.cristiane.salon.models.cashflow.repository.CashFlowRepository;
import com.cristiane.salon.models.employee.entity.Employee;
import com.cristiane.salon.models.employee.entity.RemunerationType;
import com.cristiane.salon.models.employee.entity.CommissionScope;
import com.cristiane.salon.models.employee.repository.EmployeeRepository;
import com.cristiane.salon.models.report.dto.AppointmentFinancialResponse;
import com.cristiane.salon.models.report.dto.AppointmentReportResponse;
import com.cristiane.salon.models.report.dto.FinancialReportResponse;
import com.cristiane.salon.models.report.dto.EmployeeFinanceResponse;
import com.cristiane.salon.models.report.dto.PayrollReportResponse;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final CashFlowRepository cashFlowRepository;
    private final AppointmentRepository appointmentRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public Page<AppointmentFinancialResponse> getEmployeeFinancialHistory(
            Long employeeId, LocalDate from, LocalDate to, Pageable pageable) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Funcionária não encontrada");
        }

        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? to.atTime(LocalTime.MAX) : null;

        return appointmentRepository
                .findByEmployeeIdForFinancialHistory(employeeId, fromDateTime, toDateTime, pageable)
                .map(AppointmentFinancialResponse::fromEntity);
    }

    @WithSpan("gerar-relatorio-financeiro")
    @Transactional(readOnly = true)
    public FinancialReportResponse generateFinancialReport(
            @SpanAttribute("relatorio.data_inicio") LocalDate from,
            @SpanAttribute("relatorio.data_fim") LocalDate to) {
        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null) to = LocalDate.now().plusDays(30);

        List<CashFlow> cashFlows = cashFlowRepository.findByDateBetween(from, to);

        BigDecimal income = cashFlows.stream()
                .filter(cf -> cf.getType() == CashFlowType.INCOME)
                .map(CashFlow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expense = cashFlows.stream()
                .filter(cf -> cf.getType() == CashFlowType.EXPENSE)
                .map(CashFlow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        final LocalDate finalFrom = from;
        final LocalDate finalTo = to;
        List<Appointment> doneAppointments = appointmentRepository.findAll().stream()
                .filter(a -> a.getStatus() == AppointmentStatus.DONE && isAppointmentInReportPeriod(a, finalFrom, finalTo))
                .collect(Collectors.toList());

        BigDecimal globalDoneAppointmentsValue = doneAppointments.stream()
                .map(a -> a.getSalonService().getPrice() != null ? a.getSalonService().getPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Employee> employees = employeeRepository.findAll();
        List<EmployeeFinanceResponse> employeeFinanceDetails = new ArrayList<>();

        BigDecimal totalSalaryPaid = BigDecimal.ZERO;
        BigDecimal totalCommissionPaid = BigDecimal.ZERO;

        for (Employee employee : employees) {
            List<Appointment> empDoneAppointments = doneAppointments.stream()
                    .filter(a -> a.getEmployee().getId().equals(employee.getId()))
                    .collect(Collectors.toList());

            long doneCount = empDoneAppointments.size();
            BigDecimal empDoneValue = empDoneAppointments.stream()
                    .map(a -> a.getSalonService().getPrice() != null ? a.getSalonService().getPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            PayoutBreakdown breakdown = calcularPagamentoFuncionaria(employee, empDoneValue, globalDoneAppointmentsValue);
            totalSalaryPaid = totalSalaryPaid.add(breakdown.salaryPart());
            totalCommissionPaid = totalCommissionPaid.add(breakdown.commissionPart());

            employeeFinanceDetails.add(new EmployeeFinanceResponse(
                    employee.getId(),
                    employee.getUser().getName(),
                    employee.getRemunerationType() != null ? employee.getRemunerationType().name() : null,
                    employee.getRemunerationValue(),
                    employee.getCommissionScope() != null ? employee.getCommissionScope().name() : null,
                    employee.getCommissionValue(),
                    doneCount,
                    empDoneValue,
                    breakdown.totalPayout()
            ));
        }

        BigDecimal netProfit = income.subtract(expense).subtract(totalSalaryPaid).subtract(totalCommissionPaid);
        String period = from + " a " + to;

        Span.current().setAttribute("relatorio.lucro_liquido", netProfit.doubleValue());
        Span.current().setAttribute("relatorio.funcionarias.total", employees.size());

        return new FinancialReportResponse(income, expense, totalSalaryPaid, totalCommissionPaid, netProfit, employeeFinanceDetails, period);
    }

    /**
     * Calcula o pagamento de uma funcionária no período do relatório (salário fixo,
     * comissão individual/global, ou a combinação dos dois). Span manual: fica aninhado
     * dentro de "gerar-relatorio-financeiro" no trace, um span por funcionária.
     */
    @WithSpan("calcular-pagamento-funcionaria")
    private PayoutBreakdown calcularPagamentoFuncionaria(
            Employee employee, BigDecimal empDoneValue, BigDecimal globalDoneAppointmentsValue) {

        Span span = Span.current();
        span.setAttribute("funcionaria.id", employee.getId());
        span.setAttribute("funcionaria.tipo_remuneracao",
                employee.getRemunerationType() != null ? employee.getRemunerationType().name() : "N/A");

        BigDecimal salaryPart = BigDecimal.ZERO;
        BigDecimal commissionPart = BigDecimal.ZERO;
        BigDecimal payout = BigDecimal.ZERO;

        if (employee.getRemunerationType() == RemunerationType.SALARIO_FIXO) {
            payout = employee.getRemunerationValue() != null ? employee.getRemunerationValue() : BigDecimal.ZERO;
            salaryPart = payout;
        } else if (employee.getRemunerationType() == RemunerationType.COMISSIONADO) {
            BigDecimal pct = employee.getRemunerationValue() != null ? employee.getRemunerationValue() : BigDecimal.ZERO;
            if (employee.getCommissionScope() == CommissionScope.INDIVIDUAL) {
                payout = empDoneValue.multiply(pct).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            } else if (employee.getCommissionScope() == CommissionScope.GLOBAL) {
                payout = globalDoneAppointmentsValue.multiply(pct).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            }
            commissionPart = payout;
        } else if (employee.getRemunerationType() == RemunerationType.FIXO_E_COMISSIONADO) {
            BigDecimal salary = employee.getRemunerationValue() != null ? employee.getRemunerationValue() : BigDecimal.ZERO;
            BigDecimal pct = employee.getCommissionValue() != null ? employee.getCommissionValue() : BigDecimal.ZERO;
            BigDecimal commission = BigDecimal.ZERO;
            if (employee.getCommissionScope() == CommissionScope.INDIVIDUAL) {
                commission = empDoneValue.multiply(pct).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            } else if (employee.getCommissionScope() == CommissionScope.GLOBAL) {
                commission = globalDoneAppointmentsValue.multiply(pct).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            }
            salaryPart = salary;
            commissionPart = commission;
            payout = salary.add(commission);
        }

        span.setAttribute("funcionaria.pagamento_calculado", payout.doubleValue());
        return new PayoutBreakdown(salaryPart, commissionPart, payout);
    }

    private record PayoutBreakdown(BigDecimal salaryPart, BigDecimal commissionPart, BigDecimal totalPayout) {}

    @Transactional(readOnly = true)
    public AppointmentReportResponse generateAppointmentReport(LocalDate from, LocalDate to) {
        final LocalDate fromDate = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        final LocalDate toDate = to == null ? LocalDate.now().plusDays(30) : to;

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

    @Transactional(readOnly = true)
    public PayrollReportResponse generatePayrollReport(LocalDate from, LocalDate to) {
        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null) to = LocalDate.now().plusDays(30);

        final LocalDate finalFrom = from;
        final LocalDate finalTo = to;

        List<Appointment> doneAppointments = appointmentRepository.findAll().stream()
                .filter(a -> a.getStatus() == AppointmentStatus.DONE && isAppointmentInReportPeriod(a, finalFrom, finalTo))
                .collect(Collectors.toList());

        BigDecimal globalDoneAppointmentsValue = doneAppointments.stream()
                .map(a -> a.getSalonService().getPrice() != null ? a.getSalonService().getPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Employee> employees = employeeRepository.findAll();
        List<PayrollReportResponse.PayrollItem> items = new ArrayList<>();

        for (Employee employee : employees) {
            List<Appointment> empDoneAppointments = doneAppointments.stream()
                    .filter(a -> a.getEmployee().getId().equals(employee.getId()))
                    .collect(Collectors.toList());

            BigDecimal empDoneValue = empDoneAppointments.stream()
                    .map(a -> a.getSalonService().getPrice() != null ? a.getSalonService().getPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal payout = BigDecimal.ZERO;
            BigDecimal baseAmount = BigDecimal.ZERO;

            if (employee.getRemunerationType() == RemunerationType.SALARIO_FIXO) {
                payout = employee.getRemunerationValue() != null ? employee.getRemunerationValue() : BigDecimal.ZERO;
                baseAmount = BigDecimal.ZERO;
            } else if (employee.getRemunerationType() == RemunerationType.COMISSIONADO) {
                BigDecimal pct = employee.getRemunerationValue() != null ? employee.getRemunerationValue() : BigDecimal.ZERO;
                if (employee.getCommissionScope() == CommissionScope.INDIVIDUAL) {
                    baseAmount = empDoneValue;
                } else if (employee.getCommissionScope() == CommissionScope.GLOBAL) {
                    baseAmount = globalDoneAppointmentsValue;
                }
                payout = baseAmount.multiply(pct).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            } else if (employee.getRemunerationType() == RemunerationType.FIXO_E_COMISSIONADO) {
                BigDecimal salary = employee.getRemunerationValue() != null ? employee.getRemunerationValue() : BigDecimal.ZERO;
                BigDecimal pct = employee.getCommissionValue() != null ? employee.getCommissionValue() : BigDecimal.ZERO;
                BigDecimal commissionPart = BigDecimal.ZERO;
                if (employee.getCommissionScope() == CommissionScope.INDIVIDUAL) {
                    baseAmount = empDoneValue;
                } else if (employee.getCommissionScope() == CommissionScope.GLOBAL) {
                    baseAmount = globalDoneAppointmentsValue;
                }
                commissionPart = baseAmount.multiply(pct).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
                payout = salary.add(commissionPart);
            }

            items.add(new PayrollReportResponse.PayrollItem(
                    employee.getId(),
                    employee.getUser().getName(),
                    employee.getRemunerationType(),
                    employee.getCommissionScope(),
                    baseAmount,
                    payout
            ));
        }

        String period = from + " a " + to;
        return new PayrollReportResponse(items, period);
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
