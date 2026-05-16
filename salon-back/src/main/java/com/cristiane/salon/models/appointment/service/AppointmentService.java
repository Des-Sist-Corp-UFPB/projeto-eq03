package com.cristiane.salon.models.appointment.service;

import com.cristiane.salon.exception.BadRequestException;
import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.exception.UnauthorizedException;
import com.cristiane.salon.models.appointment.dto.AppointmentRequest;
import com.cristiane.salon.models.appointment.dto.AppointmentResponse;
import com.cristiane.salon.models.appointment.entity.Appointment;
import com.cristiane.salon.models.appointment.enums.AppointmentStatus;
import com.cristiane.salon.models.appointment.repository.AppointmentRepository;
import com.cristiane.salon.models.cashflow.entity.CashFlow;
import com.cristiane.salon.models.cashflow.enums.CashFlowType;
import com.cristiane.salon.models.cashflow.repository.CashFlowRepository;
import com.cristiane.salon.models.employee.entity.Employee;
import com.cristiane.salon.models.employee.repository.EmployeeRepository;
import com.cristiane.salon.models.service.entity.SalonService;
import com.cristiane.salon.models.service.repository.SalonServiceRepository;
import com.cristiane.salon.models.user.entity.User;
import com.cristiane.salon.models.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Usuário não autenticado"));
    }

    private boolean isStaff(User user) {
        String role = user.getRoleName();
        return "ADMIN".equals(role) || "GERENTE_DE_ATENDIMENTO".equals(role);
    }

    private void assertNoScheduleConflict(Long employeeId, LocalDateTime scheduledAt, SalonService service,
                                         Long ignoreAppointmentId) {
        List<Appointment> existing = appointmentRepository.findActiveAppointmentsByEmployeeAndDate(
                employeeId,
                scheduledAt.toLocalDate().atStartOfDay(),
                scheduledAt.toLocalDate().atTime(LocalTime.MAX)
        );

        LocalDateTime requestEnd = scheduledAt.plusMinutes(service.getDurationMin());

        for (Appointment apt : existing) {
            if (ignoreAppointmentId != null && apt.getId().equals(ignoreAppointmentId)) {
                continue;
            }
            LocalDateTime aptStart = apt.getScheduledAt();
            LocalDateTime aptEnd = aptStart.plusMinutes(apt.getSalonService().getDurationMin());

            boolean overlaps = scheduledAt.isBefore(aptEnd) && aptStart.isBefore(requestEnd);
            if (overlaps) {
                throw new BadRequestException("Este horário já está ocupado para esta profissional");
            }
        }
    }

    @Transactional
    public AppointmentResponse create(AppointmentRequest request) {
        User currentUser = getAuthenticatedUser();
        boolean staffCreatesForClient = isStaff(currentUser) && request.clientId() != null;

        User client;
        if (staffCreatesForClient) {
            client = userRepository.findById(request.clientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        } else {
            client = currentUser;
        }

        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Profissional não encontrado"));

        SalonService service = salonServiceRepository.findById(request.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado"));

        if (!service.getActive()) {
            throw new BadRequestException("Este serviço não está disponível");
        }

        if (staffCreatesForClient) {
            if (request.scheduledAt() == null) {
                throw new BadRequestException("Informe data e hora do agendamento");
            }
            if (request.scheduledAt().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("Não é possível agendar no passado");
            }
            assertNoScheduleConflict(employee.getId(), request.scheduledAt(), service, null);

            Appointment appointment = new Appointment();
            appointment.setClient(client);
            appointment.setEmployee(employee);
            appointment.setSalonService(service);
            appointment.setScheduledAt(request.scheduledAt());
            appointment.setPreferredDate(request.preferredDate());
            appointment.setClientNotes(request.clientNotes());
            appointment.setStatus(AppointmentStatus.CONFIRMED);

            return AppointmentResponse.fromEntity(appointmentRepository.save(appointment));
        }

        if (isStaff(currentUser)) {
            throw new BadRequestException("Para criar solicitação como equipe, informe o cliente");
        }

        if (!"CLIENTE".equals(currentUser.getRoleName())) {
            throw new BadRequestException("Apenas clientes podem enviar solicitação por este fluxo");
        }

        if (request.scheduledAt() != null) {
            throw new BadRequestException("O horário será definido pelo salão após aceitar seu pedido");
        }

        String notes = request.clientNotes();
        if (notes != null && notes.length() > 4000) {
            throw new BadRequestException("Observações muito longas (máx. 4000 caracteres)");
        }

        Appointment appointment = new Appointment();
        appointment.setClient(client);
        appointment.setEmployee(employee);
        appointment.setSalonService(service);
        appointment.setPreferredDate(request.preferredDate());
        appointment.setClientNotes(notes);
        appointment.setStatus(AppointmentStatus.REQUESTED);

        return AppointmentResponse.fromEntity(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse confirm(Long id, LocalDateTime scheduledAt) {
        if (!isStaff(getAuthenticatedUser())) {
            throw new UnauthorizedException("Apenas a equipe pode confirmar horários");
        }

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        if (appointment.getStatus() != AppointmentStatus.REQUESTED) {
            throw new BadRequestException("Apenas solicitações pendentes de confirmação podem ser aprovadas");
        }

        if (scheduledAt.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Não é possível confirmar um horário no passado");
        }

        assertNoScheduleConflict(appointment.getEmployee().getId(), scheduledAt, appointment.getSalonService(), null);

        appointment.setScheduledAt(scheduledAt);
        appointment.setStatus(AppointmentStatus.CONFIRMED);

        return AppointmentResponse.fromEntity(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse decline(Long id) {
        if (!isStaff(getAuthenticatedUser())) {
            throw new UnauthorizedException("Apenas a equipe pode recusar solicitações");
        }

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        if (appointment.getStatus() != AppointmentStatus.REQUESTED) {
            throw new BadRequestException("Apenas solicitações em análise podem ser recusadas");
        }

        appointment.setStatus(AppointmentStatus.DECLINED);
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
        boolean isAdmin = isStaff(currentUser);

        if (!isAdmin && !appointment.getClient().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Você não tem permissão para cancelar este agendamento");
        }

        if (appointment.getStatus() == AppointmentStatus.DONE) {
            throw new BadRequestException("Não é possível cancelar um agendamento já concluído");
        }

        if (appointment.getStatus() == AppointmentStatus.DECLINED) {
            throw new BadRequestException("Esta solicitação já foi recusada");
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BadRequestException("Este agendamento já está cancelado");
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
            if (status == AppointmentStatus.REQUESTED) {
                throw new BadRequestException("Status inválido para esta operação");
            }
            if ((status == AppointmentStatus.CONFIRMED || status == AppointmentStatus.DONE)
                    && appointment.getScheduledAt() == null) {
                throw new BadRequestException("É necessário ter data e hora definidas neste agendamento");
            }
            appointment.setStatus(status);

            if (status == AppointmentStatus.DONE) {
                SalonService svc = appointment.getSalonService();
                BigDecimal servicePrice = svc.getPrice();
                boolean shouldAutoBill = servicePrice != null && servicePrice.signum() > 0;

                if (shouldAutoBill) {
                    boolean alreadyBilled = cashFlowRepository.findAll().stream()
                            .anyMatch(cf -> cf.getAppointment() != null && cf.getAppointment().getId().equals(id));

                    if (!alreadyBilled) {
                        CashFlow cashFlow = new CashFlow();
                        cashFlow.setType(CashFlowType.INCOME);
                        cashFlow.setAmount(servicePrice);
                        cashFlow.setDescription("Pagamento do agendamento #" + appointment.getId() + " - " + svc.getName());
                        cashFlow.setDate(java.time.LocalDate.now());
                        cashFlow.setAppointment(appointment);
                        cashFlowRepository.save(cashFlow);
                    }
                }
            }

            return AppointmentResponse.fromEntity(appointmentRepository.save(appointment));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Status inválido");
        }
    }
}
