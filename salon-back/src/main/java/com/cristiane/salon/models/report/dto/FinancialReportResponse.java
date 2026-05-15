package com.cristiane.salon.models.report.dto;

import java.math.BigDecimal;

public record FinancialReportResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netProfit,
        String period
) {}
