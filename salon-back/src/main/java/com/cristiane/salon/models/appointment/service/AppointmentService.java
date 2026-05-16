package com.cristiane.salon.models.appointment.service;

import com.cristiane.salon.exception.BadRequestException;
import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.exception.UnauthorizedException;
import com.cristiane.salon.models.appointment.dto.AppointmentRequest;
import com.cristiane.salon.models.appointment.dto.AppointmentResponse;
import com.cristiane.salon.models.appointment.dto.TimeSlotResponse;
import com.cristiane.salon.models.appointment.entity.Appointment;
import com.cristiane.salon.models.appointment.enums.AppointmentStatus;
import com.cristiane.salon.models.appointment.repository.AppointmentRepository;
import com.cristiane.salon.models.employee.entity.Employee;
import com.cristiane.salon.models.employee.repository.EmployeeRepository;
import com.cristiane.salon.models.service.repository.SalonServiceRepository;
import com.cristiane.salon.models.cashflow.entity.CashFlow;
import com.cristiane.salon.models.cashflow.enums.CashFlowType;
import com.cristiane.salon.models.cashflow.repository.CashFlowRepository;
import com.cristiane.salon.models.user.entity.User;
import com.cristiane.salon.models.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final EmployeeRepository employeeRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final UserRepository userRepository;
    private final CashFlowRepository cashFlowRepository;

    // Horário de funcionamento fixo (ex: 09:00 as 18:00)
    private static final LocalTime START_TIME = LocalTime.of(9, 0);
    private static final LocalTime END_TIME = LocalTime.of(18, 0);
    private static final int SLOT_MINUTES = 30; // Intervalos de 30 min para agendamento

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Usuário não autenticado"));
    }

    @Transactional(readOnly = true)
    public List<TimeSlotResponse> getAvailableSlots(LocalDate date, Long employeeId) {
        if (date.isBefore(LocalDate.now())) {
            throw new BadRequestException("A data não pode ser no passado");
        }

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Appointment> existingAppointments = appointmentRepository
                .findActiveAppointmentsByEmployeeAndDate(employeeId, startOfDay, endOfDay);

        List<TimeSlotResponse> slots = new ArrayList<>();
        LocalTime currentTime = START_TIME;

        while (currentTime.isBefore(END_TIME)) {
            final LocalTime slotTime = currentTime;
            
            // Check if slot overlaps with any existing appointment
            boolean isAvailable = true;
            for (Appointment apt : existingAppointments) {
                LocalTime aptStart = apt.getScheduledAt().toLocalTime();
                LocalTime aptEnd = aptStart.plusMinutes(apt.getSalonService().getDurationMin());

                // Se o slot estiver dentro do horário de um agendamento existente
                if ((slotTime.equals(aptStart) || slotTime.isAfter(aptStart)) && slotTime.isBefore(aptEnd)) {
                    isAvailable = false;
                    break;
                }
            }

            // Não permitir agendamentos no passado para o dia atual
            if (date.equals(LocalDate.now()) && slotTime.isBefore(LocalTime.now())) {
                isAvailable = false;
            }

            slots.add(new TimeSlotResponse(currentTime, isAvailable));
            currentTime = currentTime.plusMinutes(SLOT_MINUTES);
        }

        return slots;
    }

    @Transactional
    public AppointmentResponse create(AppointmentRequest request) {
        User currentUser = getAuthenticatedUser();
        User client;

        if ("ADMIN".equals(currentUser.getRoleName()) && request.clientId() != null) {
            client = userRepository.findById(request.clientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        } else {
            client = currentUser;
        }
        
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado"));
                
        com.cristiane.salon.models.service.entity.SalonService service = salonServiceRepository.findById(request.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado"));

        if (!service.getActive()) {
            throw new BadRequestException("Este serviço não está disponível");
        }

        if (request.scheduledAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Não é possível agendar no passado");
        }

        // Validação simples de disponibilidade
        List<Appointment> existingAppointments = appointmentRepository.findActiveAppointmentsByEmployeeAndDate(
                employee.getId(),
                request.scheduledAt().toLocalDate().atStartOfDay(),
                request.scheduledAt().toLocalDate().atTime(LocalTime.MAX)
        );

        LocalDateTime requestEnd = request.scheduledAt().plusMinutes(service.getDurationMin());

        for (Appointment apt : existingAppointments) {
            LocalDateTime aptStart = apt.getScheduledAt();
            LocalDateTime aptEnd = aptStart.plusMinutes(apt.getSalonService().getDurationMin());

            if ((request.scheduledAt().isEqual(aptStart) || request.scheduledAt().isAfter(aptStart)) && request.scheduledAt().isBefore(aptEnd) ||
                (requestEnd.isAfter(aptStart) && (requestEnd.isEqual(aptEnd) || requestEnd.isBefore(aptEnd)))) {
                throw new BadRequestException("O horário selecionado não está disponível para este funcionário");
            }
        }

        Appointment appointment = new Appointment();
        appointment.setClient(client);
        appointment.setEmployee(employee);
        appointment.setSalonService(service);
        appointment.setScheduledAt(request.scheduledAt());
        appointment.setStatus(AppointmentStatus.PENDING);

        return AppointmentResponse.fromEntity(appointmentRepository.save(appointment));
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getMyAppointments() {
        User client = getAuthenticatedUser();
        return appointmentRepository.findByClientId(client.getId()).stream()
                .map(AppointmentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findAll() {
        return appointmentRepository.findAll().stream()
                .map(AppointmentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public AppointmentResponse cancel(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        User currentUser = getAuthenticatedUser();
        boolean isAdmin = "ADMIN".equals(currentUser.getRoleName());
        
        if (!isAdmin && !appointment.getClient().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Você não tem permissão para cancelar este agendamento");
        }

        if (appointment.getStatus() == AppointmentStatus.DONE) {
            throw new BadRequestException("Não é possível cancelar um agendamento já concluído");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        return AppointmentResponse.fromEntity(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse updateStatus(Long id, String statusStr) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        try {
            AppointmentStatus status = AppointmentStatus.valueOf(statusStr.toUpperCase());
            appointment.setStatus(status);
            
            // Auto-create INCOME cash flow entry when appointment status -> DONE
            if (status == AppointmentStatus.DONE) {
                boolean alreadyBilled = cashFlowRepository.findAll().stream()
                        .anyMatch(cf -> cf.getAppointment() != null && cf.getAppointment().getId().equals(id));
                
                if (!alreadyBilled) {
                    CashFlow cashFlow = new CashFlow();
                    cashFlow.setType(CashFlowType.INCOME);
                    cashFlow.setAmount(appointment.getSalonService().getPrice());
                    cashFlow.setDescription("Pagamento do agendamento #" + appointment.getId() + " - " + appointment.getSalonService().getName());
                    cashFlow.setDate(LocalDate.now());
                    cashFlow.setAppointment(appointment);
                    cashFlowRepository.save(cashFlow);
                }
            }
            
            return AppointmentResponse.fromEntity(appointmentRepository.save(appointment));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Status inválido");
        }
    }
}
