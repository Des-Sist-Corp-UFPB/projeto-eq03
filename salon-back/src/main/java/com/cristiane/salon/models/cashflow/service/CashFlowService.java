package com.cristiane.salon.models.cashflow.service;

import com.cristiane.salon.exception.BadRequestException;
import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.models.appointment.entity.Appointment;
import com.cristiane.salon.models.appointment.repository.AppointmentRepository;
import com.cristiane.salon.models.cashflow.dto.CashFlowRequest;
import com.cristiane.salon.models.cashflow.dto.CashFlowResponse;
import com.cristiane.salon.models.cashflow.dto.CashFlowItemRequest;
import com.cristiane.salon.models.cashflow.entity.CashFlow;
import com.cristiane.salon.models.cashflow.enums.CashFlowType;
import com.cristiane.salon.models.cashflow.repository.CashFlowRepository;
import com.cristiane.salon.models.product.entity.Product;
import com.cristiane.salon.models.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CashFlowService {

    private final CashFlowRepository cashFlowRepository;
    private final AppointmentRepository appointmentRepository;
    private final ProductRepository productRepository;

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

        if (request.items() != null && !request.items().isEmpty()) {
            if (!"INCOME".equalsIgnoreCase(request.type())) {
                throw new BadRequestException("Venda de produtos deve ser um registro de entrada (INCOME).");
            }
            cashFlow.setType(CashFlowType.INCOME);

            BigDecimal totalAmount = BigDecimal.ZERO;
            List<String> itemDescriptions = new ArrayList<>();

            for (CashFlowItemRequest item : request.items()) {
                Product product = productRepository.findById(item.productId())
                        .orElseThrow(() -> new ResourceNotFoundException("Produto com ID " + item.productId() + " não encontrado."));

                if (product.getActive() == null || !product.getActive()) {
                    throw new BadRequestException("Produto '" + product.getName() + "' não está ativo.");
                }

                if (product.getStock() == null || product.getStock() < item.quantity()) {
                    throw new BadRequestException("Estoque insuficiente para o produto: " + product.getName() +
                            " (Solicitado: " + item.quantity() + ", Disponível: " + (product.getStock() != null ? product.getStock() : 0) + ")");
                }

                product.setStock(product.getStock() - item.quantity());
                productRepository.save(product);

                BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(item.quantity()));
                totalAmount = totalAmount.add(itemTotal);

                itemDescriptions.add(item.quantity() + "x " + product.getName());
            }

            cashFlow.setAmount(totalAmount);

            String itemsSummary = String.join(", ", itemDescriptions);
            String desc = request.description();
            if (desc == null || desc.trim().isEmpty() || desc.equalsIgnoreCase("Venda de Produtos") || desc.equalsIgnoreCase("Venda de Produto")) {
                cashFlow.setDescription("Venda de Produtos: " + itemsSummary);
            } else {
                cashFlow.setDescription(desc + " (" + itemsSummary + ")");
            }

            cashFlow.setDate(request.date());
        } else {
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
