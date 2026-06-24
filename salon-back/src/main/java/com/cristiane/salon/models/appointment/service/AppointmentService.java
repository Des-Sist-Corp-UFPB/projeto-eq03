package com.cristiane.salon.models.appointment.service;

import com.cristiane.salon.exception.BadRequestException;
import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.exception.UnauthorizedException;
import com.cristiane.salon.exception.BusinessException;
import com.cristiane.salon.models.appointment.dto.GeneratePixRequest;
import com.cristiane.salon.utils.CpfValidator;
import com.cristiane.salon.integrations.payment.service.MercadoPagoPaymentService;
import com.cristiane.salon.models.appointment.dto.AppointmentRequest;
import com.cristiane.salon.models.appointment.dto.AppointmentResponse;
import com.cristiane.salon.models.appointment.entity.Appointment;
import com.cristiane.salon.models.appointment.enums.AppointmentStatus;
import com.cristiane.salon.models.appointment.enums.PaymentStatus;
import com.cristiane.salon.models.appointment.repository.AppointmentRepository;
import com.cristiane.salon.models.cashflow.entity.CashFlow;
import com.cristiane.salon.models.cashflow.enums.CashFlowType;
import com.cristiane.salon.models.cashflow.repository.CashFlowRepository;
import com.cristiane.salon.models.employee.entity.Employee;
import com.cristiane.salon.models.employee.repository.EmployeeRepository;
import com.cristiane.salon.models.service.entity.SalonService;
import com.cristiane.salon.models.service.repository.SalonServiceRepository;
import com.cristiane.salon.integrations.email.service.EmailService;
import com.cristiane.salon.models.featureflag.service.FeatureFlagService;
import com.cristiane.salon.models.user.entity.User;
import com.cristiane.salon.models.user.repository.UserRepository;
import com.cristiane.salon.models.audit.AuditLogService;
import com.mercadopago.resources.payment.Payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final EmployeeRepository employeeRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final UserRepository userRepository;
    private final CashFlowRepository cashFlowRepository;
    private final FeatureFlagService featureFlagService;
    private final EmailService emailService;
    private final MercadoPagoPaymentService mercadoPagoPaymentService;
    private final AuditLogService auditLogService;

    private static int blockingMinutes(SalonService service) {
        if (service.getDurationMin() != null && service.getDurationMin() > 0) {
            return service.getDurationMin();
        }
        return 60;
    }

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

        LocalDateTime requestEnd = scheduledAt.plusMinutes(blockingMinutes(service));

        for (Appointment apt : existing) {
            if (ignoreAppointmentId != null && apt.getId().equals(ignoreAppointmentId)) {
                continue;
            }
            LocalDateTime aptStart = apt.getScheduledAt();
            LocalDateTime aptEnd = aptStart.plusMinutes(blockingMinutes(apt.getSalonService()));

            boolean overlaps = scheduledAt.isBefore(aptEnd) && aptStart.isBefore(requestEnd);
            if (overlaps) {
                throw new BadRequestException("Este horário já está ocupado para esta profissional");
            }
        }
    }

    @Transactional
    public AppointmentResponse create(AppointmentRequest request) {
        User currentUser = getAuthenticatedUser();

        if ("CLIENTE".equals(currentUser.getRoleName()) && !featureFlagService.isEnabled("ENABLE_CUSTOMER_PORTAL")) {
            throw new AccessDeniedException("O portal do cliente está temporariamente desativado.");
        }

        boolean staffCreatesForClient = isStaff(currentUser) && request.clientId() != null;

        if (!staffCreatesForClient && !featureFlagService.isEnabled("CLIENT_BOOKING")) {
            throw new BadRequestException("Agendamentos online para clientes estão temporariamente desativados.");
        }

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

            if (request.preferredDate() != null && request.preferredDate().isBefore(LocalDate.now())) {
                throw new BadRequestException("A data preferida deve ser hoje ou uma data futura");
            }

            Appointment appointment = new Appointment();
            appointment.setClient(client);
            appointment.setEmployee(employee);
            appointment.setSalonService(service);
            appointment.setScheduledAt(request.scheduledAt());
            appointment.setPreferredDate(request.preferredDate());
            appointment.setClientNotes(request.clientNotes());
            appointment.setStatus(AppointmentStatus.CONFIRMED);

            Appointment saved = appointmentRepository.save(appointment);
            emailService.sendConfirmationNotificationToClient(saved);
            return AppointmentResponse.fromEntity(saved);
        }

        if (request.scheduledAt() != null) {
            throw new BadRequestException("O horário será definido pelo salão após aceitar seu pedido");
        }

        if (request.preferredDate() != null && request.preferredDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("A data preferida deve ser hoje ou uma data futura");
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

        Appointment saved = appointmentRepository.save(appointment);
        emailService.sendRequestNotificationToStaff(saved);
        return AppointmentResponse.fromEntity(saved);
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

        Appointment saved = appointmentRepository.save(appointment);
        emailService.sendConfirmationNotificationToClient(saved);
        return AppointmentResponse.fromEntity(saved);
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
        Appointment saved = appointmentRepository.save(appointment);
        // Notify client and staff that their request was declined
        emailService.sendCancellationNotification(saved);
        return AppointmentResponse.fromEntity(saved);
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

    @Transactional(readOnly = true)
    public AppointmentResponse findById(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        User currentUser = getAuthenticatedUser();
        boolean isAdmin = isStaff(currentUser);

        if (!isAdmin && !appointment.getClient().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Você não tem permissão para visualizar este agendamento");
        }

        return AppointmentResponse.fromEntity(appointment);
    }

    @Transactional
    public AppointmentResponse generatePixPayment(Long id, GeneratePixRequest request) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        User currentUser = getAuthenticatedUser();
        boolean isAdmin = isStaff(currentUser);

        // Regra 1: Apenas o dono do agendamento ou a equipe do salão podem gerar o PIX
        if (!isAdmin && !appointment.getClient().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Você não tem permissão para gerar pagamento para este agendamento");
        }

        // Regra 2: Não gerar se já estiver pago
        if (appointment.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Este agendamento já está pago.");
        }

        // Regra 3: Não gerar para agendamentos cancelados (terminal state)
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BadRequestException("Não é possível gerar PIX para um agendamento cancelado.");
        }

        BigDecimal amount = appointment.getSalonService().getPrice();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Este serviço não possui um valor configurado para cobrança.");
        }

        // Idempotency: Check if there's already a pending PIX payment
        if (appointment.getPaymentStatus() == PaymentStatus.PENDING &&
                appointment.getPixQrCode() != null &&
                appointment.getPaymentId() != null) {
            return AppointmentResponse.fromEntity(appointment);
        }

        String clientCpf;
        if (request != null && Boolean.TRUE.equals(request.useSavedCpf())) {
            clientCpf = appointment.getClient().getCpf();
            if (clientCpf == null || clientCpf.isBlank()) {
                throw new BadRequestException("CPF é obrigatório para gerar o PIX. Por favor, cadastre seu CPF antes de continuar.");
            }
        } else {
            if (request == null || request.cpf() == null || request.cpf().isBlank()) {
                throw new BadRequestException("CPF é obrigatório para gerar o PIX. Por favor, cadastre seu CPF antes de continuar.");
            }
            String cleanCpf = request.cpf().replaceAll("\\D", "");
            if (!CpfValidator.isValid(cleanCpf)) {
                throw new BadRequestException("CPF inválido. Por favor, insira um CPF válido.");
            }

            // Persist the updated CPF to the database
            User client = appointment.getClient();
            client.setCpf(cleanCpf);
            userRepository.save(client);
            clientCpf = cleanCpf;
        }

        // Gera a cobrança na API do Mercado Pago com dados reais do cliente
        String description = "Pagamento do agendamento #" + appointment.getId() + " - " + appointment.getSalonService().getName();
        String payerEmail = appointment.getClient().getEmail();
        String payerName = appointment.getClient().getName();

        Payment payment = mercadoPagoPaymentService.createPixPayment(amount, description, payerEmail, payerName, clientCpf, appointment.getId());

        // Extrai o "Copia e Cola" de dentro da resposta complexa da API
        String qrCodeCopiaECola = payment.getPointOfInteraction().getTransactionData().getQrCode();

        // Salva os dados no banco e marca que está aguardando o cliente pagar
        appointment.setPaymentId(payment.getId());
        appointment.setPixQrCode(qrCodeCopiaECola);
        appointment.setPaymentStatus(PaymentStatus.PENDING);

        Appointment saved = appointmentRepository.save(appointment);
        return AppointmentResponse.fromEntity(saved);
    }

    @Transactional
    public void processPixPaymentWebhook(Long paymentId) {
        // 1. Double-Check: Consulta o Mercado Pago
        Payment payment = mercadoPagoPaymentService.getPayment(paymentId);
        
        // 2. Verifica se é um pagamento real e se foi aprovado
        if (payment == null || !"approved".equals(payment.getStatus())) {
            log.warn("Webhook ignorado. Pagamento {} não existe ou não está aprovado.", paymentId);
            return; 
        }

        // 3. Pega aquele external_reference que enviamos na hora de criar o PIX
        Long appointmentId = Long.valueOf(payment.getExternalReference());
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);

        // 4. Se não achar o agendamento ou se já estiver pago, não faz nada (Idempotência)
        if (appointment == null || appointment.getPaymentStatus() == PaymentStatus.PAID) {
            return; 
        }

        // 5. MARCA COMO PAGO!
        appointment.setPaymentStatus(PaymentStatus.PAID);
        appointmentRepository.save(appointment);

        // 6. Lança a receita no Fluxo de Caixa financeiro do salão
        CashFlow cashFlow = new CashFlow();
        cashFlow.setType(CashFlowType.INCOME);
        cashFlow.setAmount(payment.getTransactionAmount());
        cashFlow.setDescription("Pagamento PIX do agendamento #" + appointment.getId() + " - " + appointment.getSalonService().getName());
        cashFlow.setDate(java.time.LocalDate.now());
        cashFlow.setAppointment(appointment);
        cashFlowRepository.save(cashFlow);
        
        try {
            emailService.sendPaymentConfirmationNotificationToClient(appointment);
        } catch (Exception e) {
            log.error("Erro ao enviar e-mail de confirmação de pagamento (efeito colateral): {}", e.getMessage());
        }

        // 7. Registra no log de auditoria
        auditLogService.logAction(
                appointment.getClient().getId(),
                appointment.getClient().getEmail(),
                "PIX_PAYMENT_CONFIRMED",
                "Appointment",
                appointment.getId(),
                "Pagamento PIX do agendamento #" + appointment.getId() + " recebido com sucesso via webhook.",
                "SUCCESS"
        );
        
        log.info("✅ SUCESSO! Agendamento {} marcado como PAGO e dinheiro lançado no Caixa.", appointmentId);
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

        // Guard clause: estado terminal — já cancelado não pode voltar atrás
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Agendamentos pagos ou cancelados não podem ter seu status alterado.");
        }

        // Guard clause: não é possível cancelar um agendamento com pagamento confirmado sem estorno prévio
        if (appointment.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessException("Não é possível cancelar um agendamento que já foi pago. Realize o estorno antes de cancelar.");
        }

        if (appointment.getStatus() == AppointmentStatus.DONE) {
            throw new BadRequestException("Não é possível cancelar um agendamento já concluído");
        }

        if (appointment.getStatus() == AppointmentStatus.DECLINED) {
            throw new BadRequestException("Esta solicitação já foi recusada");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment saved = appointmentRepository.save(appointment);
        emailService.sendCancellationNotification(saved);
        return AppointmentResponse.fromEntity(saved);
    }

    @Transactional
    public AppointmentResponse updateStatus(Long id, String statusStr) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        // Guard clause: estado terminal — agendamento cancelado não permite mais alterações de status
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Agendamentos pagos ou cancelados não podem ter seu status alterado.");
        }

        try {
            AppointmentStatus status = AppointmentStatus.valueOf(statusStr.toUpperCase());
            if (status == AppointmentStatus.REQUESTED) {
                throw new BadRequestException("Status inválido para esta operação");
            }
            if ((status == AppointmentStatus.CONFIRMED || status == AppointmentStatus.DONE)
                    && appointment.getScheduledAt() == null) {
                throw new BadRequestException("É necessário ter data e hora definidas neste agendamento");
            }

            // Guard clause desacoplado: bloqueia alterações de status para não-DONE quando o pagamento está finalizado.
            // Permite DONE mesmo se já pago (ex: concluir serviço após pagamento PIX confirmado).
            if (status != AppointmentStatus.DONE &&
                    (appointment.getPaymentStatus() == PaymentStatus.PAID ||
                     appointment.getPaymentStatus() == PaymentStatus.CANCELLED)) {
                throw new BusinessException("Agendamentos pagos ou cancelados não podem ter seu status alterado.");
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

            Appointment saved = appointmentRepository.save(appointment);

            // Trigger email notifications based on resulting status
            if (status == AppointmentStatus.CONFIRMED) {
                emailService.sendConfirmationNotificationToClient(saved);
            } else if (status == AppointmentStatus.CANCELLED || status == AppointmentStatus.DECLINED) {
                emailService.sendCancellationNotification(saved);
            }

            return AppointmentResponse.fromEntity(saved);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Status inválido");
        }
    }

    @Transactional
    public AppointmentResponse updatePaymentStatus(Long id, String paymentStatusStr) {
        if (!isStaff(getAuthenticatedUser())) {
            throw new UnauthorizedException("Apenas a equipe pode atualizar o status de pagamento");
        }

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        // Guard clause: agendamento cancelado não permite mais alterações de pagamento
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Agendamentos pagos ou cancelados não podem ter seu status alterado.");
        }

        // Guard clause: status de pagamento em estado terminal
        if (appointment.getPaymentStatus() == PaymentStatus.PAID ||
                appointment.getPaymentStatus() == PaymentStatus.CANCELLED) {
            throw new BusinessException("Agendamentos pagos ou cancelados não podem ter seu status alterado.");
        }

        try {
            PaymentStatus paymentStatus = PaymentStatus.valueOf(paymentStatusStr.toUpperCase());

            // Apenas o Webhook do Mercado Pago tem permissão de sistema para transitar um agendamento de PENDING para PAID.
            // O endpoint manual do admin não deve permitir essa transição sem um ID de pagamento válido.
            if (paymentStatus == PaymentStatus.PAID && appointment.getPaymentStatus() == PaymentStatus.PENDING) {
                if (appointment.getPaymentId() == null) {
                    throw new BusinessException("Transição manual para PAGO não permitida para agendamentos pendentes sem um ID de pagamento válido.");
                }
            }

            appointment.setPaymentStatus(paymentStatus);

            if (paymentStatus == PaymentStatus.PAID) {
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
                        cashFlow.setDescription("Pagamento (Confirmado Admin) do agendamento #" + appointment.getId() + " - " + svc.getName());
                        cashFlow.setDate(java.time.LocalDate.now());
                        cashFlow.setAppointment(appointment);
                        cashFlowRepository.save(cashFlow);
                    }
                }
            }

            Appointment saved = appointmentRepository.save(appointment);
            return AppointmentResponse.fromEntity(saved);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Status de pagamento inválido");
        }
    }
}
