package com.cristiane.salon.models.report.dto;

import com.cristiane.salon.models.employee.entity.CommissionScope;
import com.cristiane.salon.models.employee.entity.RemunerationType;
import java.math.BigDecimal;
import java.util.List;

public record PayrollReportResponse(
        List<PayrollItem> items,
        String period
) {
    public record PayrollItem(
            Long employeeId,
            String employeeName,
            RemunerationType remunerationType,
            CommissionScope commissionScope,
            BigDecimal baseAmount,
            BigDecimal calculatedPay
    ) {}
}
