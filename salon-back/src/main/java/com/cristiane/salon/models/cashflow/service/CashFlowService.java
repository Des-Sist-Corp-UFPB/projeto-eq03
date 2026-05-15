package com.cristiane.salon.models.cashflow.service;

import com.cristiane.salon.exception.BadRequestException;
import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.models.appointment.entity.Appointment;
import com.cristiane.salon.models.appointment.repository.AppointmentRepository;
import com.cristiane.salon.models.cashflow.dto.CashFlowRequest;
import com.cristiane.salon.models.cashflow.dto.CashFlowResponse;
import com.cristiane.salon.models.cashflow.entity.CashFlow;
import com.cristiane.salon.models.cashflow.enums.CashFlowType;
import com.cristiane.salon.models.cashflow.repository.CashFlowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CashFlowService {

    private final CashFlowRepository cashFlowRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public List<CashFlowResponse> findByPeriod(LocalDate from, LocalDate to) {
        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null) to = LocalDate.now();

        return cashFlowRepository.findByDateBetween(from, to).stream()
                .map(CashFlowResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public CashFlowResponse create(CashFlowRequest request) {
        CashFlow cashFlow = new CashFlow();
        
        try {
            cashFlow.setType(CashFlowType.valueOf(request.type().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Tipo de fluxo de caixa inválido. Use INCOME ou EXPENSE.");
        }
        
        cashFlow.setAmount(request.amount());
        cashFlow.setDescription(request.description());
        cashFlow.setDate(request.date());

        if (request.appointmentId() != null) {
            Appointment appointment = appointmentRepository.findById(request.appointmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));
            cashFlow.setAppointment(appointment);
        }

        return CashFlowResponse.fromEntity(cashFlowRepository.save(cashFlow));
    }

    @Transactional
    public void delete(Long id) {
        if (!cashFlowRepository.existsById(id)) {
            throw new ResourceNotFoundException("Registro não encontrado");
        }
        cashFlowRepository.deleteById(id);
    }
}
