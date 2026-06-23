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
import com.cristiane.salon.integrations.payment.service.MercadoPagoPaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private SalonServiceRepository salonServiceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CashFlowRepository cashFlowRepository;

    @Mock
    private FeatureFlagService featureFlagService;

    @Mock
    private EmailService emailService;

    @Mock
    private MercadoPagoPaymentService mercadoPagoPaymentService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AppointmentService appointmentService;

    private User clientUser;
    private User staffUser;
    private Employee employee;
    private SalonService salonService;

    @BeforeEach
    void setUp() {
        clientUser = new User();
        clientUser.setId(10L);
        clientUser.setName("Cliente");
        clientUser.setEmail("client@example.com");
        clientUser.setRole(new com.cristiane.salon.models.user.entity.Role(1L, "CLIENTE", null));

        staffUser = new User();
        staffUser.setId(11L);
        staffUser.setName("Admin");
        staffUser.setEmail("admin@example.com");
        staffUser.setRole(new com.cristiane.salon.models.user.entity.Role(2L, "ADMIN", null));

        employee = new Employee();
        employee.setId(5L);
        employee.setUser(new User());
        employee.getUser().setId(12L);

        salonService = new SalonService();
        salonService.setId(8L);
        salonService.setName("Corte");
        salonService.setPrice(BigDecimal.valueOf(100.00));
        salonService.setDurationMin(45);
        salonService.setActive(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockAuthenticatedUser(User user) {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(user.getEmail());
        SecurityContext secCtx = mock(SecurityContext.class);
        lenient().when(secCtx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(secCtx);
        lenient().when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @Test
    void create_whenUserNotAuthenticated_shouldThrowUnauthorizedException() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("unknown@example.com");
        SecurityContext secCtx = mock(SecurityContext.class);
        when(secCtx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(secCtx);
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        AppointmentRequest request = new AppointmentRequest(5L, 8L, null, null, null, null);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.create(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Usuário não autenticado");
    }

    @Test
    void create_whenClientAndPortalDisabled_shouldThrowAccessDeniedException() {
        // Arrange
        mockAuthenticatedUser(clientUser);
        when(featureFlagService.isEnabled("ENABLE_CUSTOMER_PORTAL")).thenReturn(false);
        AppointmentRequest request = new AppointmentRequest(5L, 8L, null, null, null, null);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.create(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("O portal do cliente está temporariamente desativado.");
    }

    @Test
    void create_whenClientAndBookingDisabled_shouldThrowBadRequestException() {
        // Arrange
        mockAuthenticatedUser(clientUser);
        when(featureFlagService.isEnabled("ENABLE_CUSTOMER_PORTAL")).thenReturn(true);
        when(featureFlagService.isEnabled("CLIENT_BOOKING")).thenReturn(false);
        AppointmentRequest request = new AppointmentRequest(5L, 8L, null, null, null, null);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Agendamentos online para clientes estão temporariamente desativados.");
    }

    @Test
    void create_whenStaffAndClientNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        mockAuthenticatedUser(staffUser);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        AppointmentRequest request = new AppointmentRequest(5L, 8L, null, null, null, 99L);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Cliente não encontrado");
    }

    @Test
    void create_whenEmployeeNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        mockAuthenticatedUser(clientUser);
        when(featureFlagService.isEnabled("ENABLE_CUSTOMER_PORTAL")).thenReturn(true);
        when(featureFlagService.isEnabled("CLIENT_BOOKING")).thenReturn(true);
        when(employeeRepository.findById(5L)).thenReturn(Optional.empty());

        AppointmentRequest request = new AppointmentRequest(5L, 8L, null, null, null, null);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Profissional não encontrado");
    }

    @Test
    void create_whenServiceNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        mockAuthenticatedUser(clientUser);
        when(featureFlagService.isEnabled("ENABLE_CUSTOMER_PORTAL")).thenReturn(true);
        when(featureFlagService.isEnabled("CLIENT_BOOKING")).thenReturn(true);
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        when(salonServiceRepository.findById(8L)).thenReturn(Optional.empty());

        AppointmentRequest request = new AppointmentRequest(5L, 8L, null, null, null, null);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Serviço não encontrado");
    }

    @Test
    void create_whenServiceInactive_shouldThrowBadRequestException() {
        // Arrange
        mockAuthenticatedUser(clientUser);
        when(featureFlagService.isEnabled("ENABLE_CUSTOMER_PORTAL")).thenReturn(true);
        when(featureFlagService.isEnabled("CLIENT_BOOKING")).thenReturn(true);
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        salonService.setActive(false);
        when(salonServiceRepository.findById(8L)).thenReturn(Optional.of(salonService));

        AppointmentRequest request = new AppointmentRequest(5L, 8L, null, null, null, null);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Este serviço não está disponível");
    }

    // --- Staff create flow tests ---

    @Test
    void create_whenStaffFlowAndScheduledAtNull_shouldThrowBadRequestException() {
        // Arrange
        mockAuthenticatedUser(staffUser);
        when(userRepository.findById(10L)).thenReturn(Optional.of(clientUser));
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        when(salonServiceRepository.findById(8L)).thenReturn(Optional.of(salonService));

        AppointmentRequest request = new AppointmentRequest(5L, 8L, null, null, null, 10L);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Informe data e hora do agendamento");
    }

    @Test
    void create_whenStaffFlowAndScheduledAtInPast_shouldThrowBadRequestException() {
        // Arrange
        mockAuthenticatedUser(staffUser);
        when(userRepository.findById(10L)).thenReturn(Optional.of(clientUser));
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        when(salonServiceRepository.findById(8L)).thenReturn(Optional.of(salonService));

        AppointmentRequest request = new AppointmentRequest(5L, 8L, LocalDateTime.now().minusDays(1), null, null, 10L);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Não é possível agendar no passado");
    }

    @Test
    void create_whenStaffFlowAndPreferredDateInPast_shouldThrowBadRequestException() {
        // Arrange
        mockAuthenticatedUser(staffUser);
        when(userRepository.findById(10L)).thenReturn(Optional.of(clientUser));
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        when(salonServiceRepository.findById(8L)).thenReturn(Optional.of(salonService));

        AppointmentRequest request = new AppointmentRequest(5L, 8L, LocalDateTime.now().plusDays(1), LocalDate.now().minusDays(1), null, 10L);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("A data preferida deve ser hoje ou uma data futura");
    }

    @Test
    void create_whenStaffFlowAndHasOverlap_shouldThrowBadRequestException() {
        // Arrange
        mockAuthenticatedUser(staffUser);
        when(userRepository.findById(10L)).thenReturn(Optional.of(clientUser));
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        when(salonServiceRepository.findById(8L)).thenReturn(Optional.of(salonService));

        LocalDateTime targetTime = LocalDateTime.now().plusDays(1);
        Appointment conflicting = new Appointment();
        conflicting.setId(20L);
        conflicting.setScheduledAt(targetTime.plusMinutes(10));
        conflicting.setSalonService(salonService);

        when(appointmentRepository.findActiveAppointmentsByEmployeeAndDate(eq(5L), any(), any()))
                .thenReturn(List.of(conflicting));

        AppointmentRequest request = new AppointmentRequest(5L, 8L, targetTime, null, null, 10L);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Este horário já está ocupado para esta profissional");
    }

    @Test
    void create_whenStaffFlowSuccess_shouldSaveAndSendConfirmation() {
        // Arrange
        mockAuthenticatedUser(staffUser);
        when(userRepository.findById(10L)).thenReturn(Optional.of(clientUser));
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        when(salonServiceRepository.findById(8L)).thenReturn(Optional.of(salonService));

        LocalDateTime targetTime = LocalDateTime.now().plusDays(1);
        when(appointmentRepository.findActiveAppointmentsByEmployeeAndDate(eq(5L), any(), any()))
                .thenReturn(List.of());

        Appointment saved = new Appointment();
        saved.setId(100L);
        saved.setClient(clientUser);
        saved.setEmployee(employee);
        saved.setSalonService(salonService);
        saved.setScheduledAt(targetTime);
        saved.setStatus(AppointmentStatus.CONFIRMED);

        when(appointmentRepository.save(any(Appointment.class))).thenReturn(saved);

        AppointmentRequest request = new AppointmentRequest(5L, 8L, targetTime, LocalDate.now().plusDays(1), "notes", 10L);

        // Act
        AppointmentResponse result = appointmentService.create(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.status()).isEqualTo(AppointmentStatus.CONFIRMED.name());
        verify(emailService).sendConfirmationNotificationToClient(saved);
    }

    // --- Client create flow tests ---

    @Test
    void create_whenClientFlowAndScheduledAtNotNull_shouldThrowBadRequestException() {
        // Arrange
        mockAuthenticatedUser(clientUser);
        when(featureFlagService.isEnabled("ENABLE_CUSTOMER_PORTAL")).thenReturn(true);
        when(featureFlagService.isEnabled("CLIENT_BOOKING")).thenReturn(true);
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        when(salonServiceRepository.findById(8L)).thenReturn(Optional.of(salonService));

        AppointmentRequest request = new AppointmentRequest(5L, 8L, LocalDateTime.now().plusDays(1), null, null, null);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("O horário será definido pelo salão após aceitar seu pedido");
    }

    @Test
    void create_whenClientFlowAndPreferredDateInPast_shouldThrowBadRequestException() {
        // Arrange
        mockAuthenticatedUser(clientUser);
        when(featureFlagService.isEnabled("ENABLE_CUSTOMER_PORTAL")).thenReturn(true);
        when(featureFlagService.isEnabled("CLIENT_BOOKING")).thenReturn(true);
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        when(salonServiceRepository.findById(8L)).thenReturn(Optional.of(salonService));

        AppointmentRequest request = new AppointmentRequest(5L, 8L, null, LocalDate.now().minusDays(1), null, null);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("A data preferida deve ser hoje ou uma data futura");
    }

    @Test
    void create_whenClientFlowAndNotesTooLong_shouldThrowBadRequestException() {
        // Arrange
        mockAuthenticatedUser(clientUser);
        when(featureFlagService.isEnabled("ENABLE_CUSTOMER_PORTAL")).thenReturn(true);
        when(featureFlagService.isEnabled("CLIENT_BOOKING")).thenReturn(true);
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        when(salonServiceRepository.findById(8L)).thenReturn(Optional.of(salonService));

        String longNotes = "a".repeat(4001);
        AppointmentRequest request = new AppointmentRequest(5L, 8L, null, null, longNotes, null);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Observações muito longas (máx. 4000 caracteres)");
    }

    @Test
    void create_whenClientFlowSuccess_shouldSaveAndSendRequestNotification() {
        // Arrange
        mockAuthenticatedUser(clientUser);
        when(featureFlagService.isEnabled("ENABLE_CUSTOMER_PORTAL")).thenReturn(true);
        when(featureFlagService.isEnabled("CLIENT_BOOKING")).thenReturn(true);
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        when(salonServiceRepository.findById(8L)).thenReturn(Optional.of(salonService));

        Appointment saved = new Appointment();
        saved.setId(101L);
        saved.setClient(clientUser);
        saved.setEmployee(employee);
        saved.setSalonService(salonService);
        saved.setStatus(AppointmentStatus.REQUESTED);

        when(appointmentRepository.save(any(Appointment.class))).thenReturn(saved);

        AppointmentRequest request = new AppointmentRequest(5L, 8L, null, LocalDate.now().plusDays(2), "my notes", null);

        // Act
        AppointmentResponse result = appointmentService.create(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(101L);
        assertThat(result.status()).isEqualTo(AppointmentStatus.REQUESTED.name());
        verify(emailService).sendRequestNotificationToStaff(saved);
    }

    // --- confirm tests ---

    @Test
    void confirm_whenNotStaff_shouldThrowUnauthorizedException() {
        // Arrange
        mockAuthenticatedUser(clientUser);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.confirm(1L, LocalDateTime.now()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Apenas a equipe pode confirmar horários");
    }

    @Test
    void confirm_whenAppointmentNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        mockAuthenticatedUser(staffUser);
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.confirm(99L, LocalDateTime.now()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Agendamento não encontrado");
    }

    @Test
    void confirm_whenStatusNotRequested_shouldThrowBadRequestException() {
        // Arrange
        mockAuthenticatedUser(staffUser);
        Appointment apt = new Appointment();
        apt.setId(1L);
        apt.setStatus(AppointmentStatus.CONFIRMED);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.confirm(1L, LocalDateTime.now()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Apenas solicitações pendentes de confirmação podem ser aprovadas");
    }

    @Test
    void confirm_whenScheduledAtInPast_shouldThrowBadRequestException() {
        // Arrange
        mockAuthenticatedUser(staffUser);
        Appointment apt = new Appointment();
        apt.setId(1L);
        apt.setStatus(AppointmentStatus.REQUESTED);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.confirm(1L, LocalDateTime.now().minusHours(1)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Não é possível confirmar um horário no passado");
    }

    @Test
    void confirm_whenSuccess_shouldSetScheduledAtAndStatusConfirmed() {
        // Arrange
        mockAuthenticatedUser(staffUser);
        Appointment apt = new Appointment();
        apt.setId(1L);
        apt.setStatus(AppointmentStatus.REQUESTED);
        apt.setEmployee(employee);
        apt.setSalonService(salonService);
        apt.setClient(clientUser);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));

        LocalDateTime targetTime = LocalDateTime.now().plusHours(2);
        when(appointmentRepository.findActiveAppointmentsByEmployeeAndDate(eq(5L), any(), any()))
                .thenReturn(List.of());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AppointmentResponse result = appointmentService.confirm(1L, targetTime);

        // Assert
        assertThat(result.status()).isEqualTo(AppointmentStatus.CONFIRMED.name());
        verify(emailService).sendConfirmationNotificationToClient(apt);
    }

    // --- decline tests ---

    @Test
    void decline_whenNotStaff_shouldThrowUnauthorizedException() {
        // Arrange
        mockAuthenticatedUser(clientUser);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.decline(1L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Apenas a equipe pode recusar solicitações");
    }

    @Test
    void decline_whenAppointmentNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        mockAuthenticatedUser(staffUser);
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.decline(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Agendamento não encontrado");
    }

    @Test
    void decline_whenStatusNotRequested_shouldThrowBadRequestException() {
        // Arrange
        mockAuthenticatedUser(staffUser);
        Appointment apt = new Appointment();
        apt.setId(1L);
        apt.setStatus(AppointmentStatus.CONFIRMED);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.decline(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Apenas solicitações em análise podem ser recusadas");
    }

    @Test
    void decline_whenSuccess_shouldSetStatusDeclined() {
        // Arrange
        mockAuthenticatedUser(staffUser);
        Appointment apt = new Appointment();
        apt.setId(1L);
        apt.setStatus(AppointmentStatus.REQUESTED);
        apt.setClient(clientUser);
        apt.setEmployee(employee);
        apt.setSalonService(salonService);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AppointmentResponse result = appointmentService.decline(1L);

        // Assert
        assertThat(result.status()).isEqualTo(AppointmentStatus.DECLINED.name());
        verify(emailService).sendCancellationNotification(apt);
    }

    // --- getMyAppointments ---

    @Test
    void getMyAppointments_shouldReturnClientAppointments() {
        // Arrange
        mockAuthenticatedUser(clientUser);
        Appointment apt = new Appointment();
        apt.setId(1L);
        apt.setClient(clientUser);
        apt.setEmployee(employee);
        apt.setSalonService(salonService);
        when(appointmentRepository.findByClientId(10L)).thenReturn(List.of(apt));

        // Act
        List<AppointmentResponse> result = appointmentService.getMyAppointments();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
    }

    // --- findAll ---

    @Test
    void findAll_shouldReturnAllAppointments() {
        // Arrange
        Appointment apt = new Appointment();
        apt.setId(1L);
        apt.setClient(clientUser);
        apt.setEmployee(employee);
        apt.setSalonService(salonService);
        when(appointmentRepository.findAll()).thenReturn(List.of(apt));

        // Act
        List<AppointmentResponse> result = appointmentService.findAll();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
    }

    // --- cancel tests ---

    @Test
    void cancel_whenAppointmentNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        mockAuthenticatedUser(clientUser);
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.cancel(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Agendamento não encontrado");
    }

    @Test
    void cancel_whenNotOwnerAndNotStaff_shouldThrowUnauthorizedException() {
        // Arrange
        mockAuthenticatedUser(clientUser); // client ID is 10
        User otherClient = new User();
        otherClient.setId(99L);
        Appointment apt = new Appointment();
        apt.setId(1L);
        apt.setClient(otherClient); // Owned by 99
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.cancel(1L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Você não tem permissão para cancelar este agendamento");
    }

    @Test
    void cancel_whenStatusIsDone_shouldThrowBadRequestException() {
        // Arrange
        mockAuthenticatedUser(clientUser);
        Appointment apt = new Appointment();
        apt.setId(1L);
        apt.setClient(clientUser);
        apt.setStatus(AppointmentStatus.DONE);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.cancel(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Não é possível cancelar um agendamento já concluído");
    }

    @Test
    void cancel_whenStatusIsDeclined_shouldThrowBadRequestException() {
        // Arrange
        mockAuthenticatedUser(clientUser);
        Appointment apt = new Appointment();
        apt.setId(1L);
        apt.setClient(clientUser);
        apt.setStatus(AppointmentStatus.DECLINED);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.cancel(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Esta solicitação já foi recusada");
    }

    @Test
    void cancel_whenStatusIsCancelled_shouldThrowBadRequestException() {
        // Arrange
        mockAuthenticatedUser(clientUser);
        Appointment apt = new Appointment();
        apt.setId(1L);
        apt.setClient(clientUser);
        apt.setStatus(AppointmentStatus.CANCELLED);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.cancel(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Este agendamento já está cancelado");
    }

    @Test
    void cancel_whenSuccessByOwner_shouldSetStatusCancelled() {
        // Arrange
        mockAuthenticatedUser(clientUser);
        Appointment apt = new Appointment();
        apt.setId(1L);
        apt.setClient(clientUser);
        apt.setEmployee(employee);
        apt.setSalonService(salonService);
        apt.setStatus(AppointmentStatus.CONFIRMED);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AppointmentResponse result = appointmentService.cancel(1L);

        // Assert
        assertThat(result.status()).isEqualTo(AppointmentStatus.CANCELLED.name());
        verify(emailService).sendCancellationNotification(apt);
    }

    @Test
    void cancel_whenSuccessByStaff_shouldSetStatusCancelled() {
        // Arrange
        mockAuthenticatedUser(staffUser);
        Appointment apt = new Appointment();
        apt.setId(1L);
        apt.setClient(clientUser);
        apt.setEmployee(employee);
        apt.setSalonService(salonService);
        apt.setStatus(AppointmentStatus.REQUESTED);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AppointmentResponse result = appointmentService.cancel(1L);

        // Assert
        assertThat(result.status()).isEqualTo(AppointmentStatus.CANCELLED.name());
        verify(emailService).sendCancellationNotification(apt);
    }

    // --- updateStatus tests ---

    @Test
    void updateStatus_whenNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.updateStatus(99L, "CONFIRMED"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Agendamento não encontrado");
    }

    @Test
    void updateStatus_whenInvalidStatusString_shouldThrowBadRequestException() {
        // Arrange
        Appointment apt = new Appointment();
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.updateStatus(1L, "INVALID_STATE"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Status inválido");
    }

    @Test
    void updateStatus_whenStatusRequested_shouldThrowBadRequestException() {
        // Arrange
        Appointment apt = new Appointment();
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.updateStatus(1L, "REQUESTED"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Status inválido para esta operação");
    }

    @Test
    void updateStatus_whenStatusConfirmedOrDoneButScheduledAtNull_shouldThrowBadRequestException() {
        // Arrange
        Appointment apt = new Appointment();
        apt.setScheduledAt(null);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.updateStatus(1L, "CONFIRMED"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("É necessário ter data e hora definidas neste agendamento");

        assertThatThrownBy(() -> appointmentService.updateStatus(1L, "DONE"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("É necessário ter data e hora definidas neste agendamento");
    }

    @Test
    void updateStatus_whenStatusConfirmed_shouldSaveAndNotifyClient() {
        // Arrange
        Appointment apt = new Appointment();
        apt.setId(1L);
        apt.setScheduledAt(LocalDateTime.now().plusDays(1));
        apt.setClient(clientUser);
        apt.setEmployee(employee);
        apt.setSalonService(salonService);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AppointmentResponse result = appointmentService.updateStatus(1L, "CONFIRMED");

        // Assert
        assertThat(result.status()).isEqualTo(AppointmentStatus.CONFIRMED.name());
        verify(emailService).sendConfirmationNotificationToClient(apt);
    }

    @Test
    void updateStatus_whenStatusCancelled_shouldSaveAndNotifyCancellation() {
        // Arrange
        Appointment apt = new Appointment();
        apt.setId(1L);
        apt.setScheduledAt(LocalDateTime.now().plusDays(1));
        apt.setClient(clientUser);
        apt.setEmployee(employee);
        apt.setSalonService(salonService);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AppointmentResponse result = appointmentService.updateStatus(1L, "CANCELLED");

        // Assert
        assertThat(result.status()).isEqualTo(AppointmentStatus.CANCELLED.name());
        verify(emailService).sendCancellationNotification(apt);
    }

    @Test
    void updateStatus_whenStatusDoneAndNoPrice_shouldNotAutoBill() {
        // Arrange
        Appointment apt = new Appointment();
        apt.setId(1L);
        apt.setScheduledAt(LocalDateTime.now().plusDays(1));
        apt.setClient(clientUser);
        apt.setEmployee(employee);
        
        salonService.setPrice(null); // Price is null
        apt.setSalonService(salonService);
        
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AppointmentResponse result = appointmentService.updateStatus(1L, "DONE");

        // Assert
        assertThat(result.status()).isEqualTo(AppointmentStatus.DONE.name());
        verify(cashFlowRepository, never()).save(any());
    }

    @Test
    void updateStatus_whenStatusDoneAndPriceZero_shouldNotAutoBill() {
        // Arrange
        Appointment apt = new Appointment();
        apt.setId(1L);
        apt.setScheduledAt(LocalDateTime.now().plusDays(1));
        apt.setClient(clientUser);
        apt.setEmployee(employee);
        
        salonService.setPrice(BigDecimal.ZERO); // Price is zero
        apt.setSalonService(salonService);
        
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AppointmentResponse result = appointmentService.updateStatus(1L, "DONE");

        // Assert
        assertThat(result.status()).isEqualTo(AppointmentStatus.DONE.name());
        verify(cashFlowRepository, never()).save(any());
    }

    @Test
    void updateStatus_whenStatusDoneAndAlreadyBilled_shouldNotDuplicateBill() {
        // Arrange
        Appointment apt = new Appointment();
        apt.setId(1L);
        apt.setScheduledAt(LocalDateTime.now().plusDays(1));
        apt.setClient(clientUser);
        apt.setEmployee(employee);
        apt.setSalonService(salonService);
        
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CashFlow existingFlow = new CashFlow();
        existingFlow.setAppointment(apt);
        when(cashFlowRepository.findAll()).thenReturn(List.of(existingFlow));

        // Act
        AppointmentResponse result = appointmentService.updateStatus(1L, "DONE");

        // Assert
        assertThat(result.status()).isEqualTo(AppointmentStatus.DONE.name());
        verify(cashFlowRepository, never()).save(any());
    }

    @Test
    void updateStatus_whenStatusDoneAndNotYetBilled_shouldAutoBillSuccess() {
        // Arrange
        Appointment apt = new Appointment();
        apt.setId(1L);
        apt.setScheduledAt(LocalDateTime.now().plusDays(1));
        apt.setClient(clientUser);
        apt.setEmployee(employee);
        apt.setSalonService(salonService);
        
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cashFlowRepository.findAll()).thenReturn(new ArrayList<>()); // No bills yet

        // Act
        AppointmentResponse result = appointmentService.updateStatus(1L, "DONE");

        // Assert
        assertThat(result.status()).isEqualTo(AppointmentStatus.DONE.name());
        
        verify(cashFlowRepository).save(any(CashFlow.class));
    }

    // --- generatePixPayment tests ---

    @Test
    void generatePixPayment_whenClientHasNoCpf_shouldThrowBadRequestException() {
        // Arrange
        mockAuthenticatedUser(clientUser); // clientUser does NOT have CPF set

        Appointment apt = new Appointment();
        apt.setId(1L);
        apt.setClient(clientUser);
        apt.setEmployee(employee);
        apt.setSalonService(salonService);
        apt.setStatus(AppointmentStatus.CONFIRMED);

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.generatePixPayment(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("CPF é obrigatório para gerar o PIX");
    }

    @Test
    void generatePixPayment_whenAppointmentAlreadyPaid_shouldThrowBadRequestException() {
        // Arrange
        mockAuthenticatedUser(clientUser);

        Appointment apt = new Appointment();
        apt.setId(1L);
        apt.setClient(clientUser);
        apt.setEmployee(employee);
        apt.setSalonService(salonService);
        apt.setStatus(AppointmentStatus.CONFIRMED);
        apt.setPaymentStatus(com.cristiane.salon.models.appointment.enums.PaymentStatus.PAID);

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.generatePixPayment(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Este agendamento já está pago.");
    }

    @Test
    void generatePixPayment_whenClientNotOwner_shouldThrowUnauthorizedException() {
        // Arrange
        mockAuthenticatedUser(clientUser); // ID 10

        User otherClient = new User();
        otherClient.setId(99L);

        Appointment apt = new Appointment();
        apt.setId(1L);
        apt.setClient(otherClient); // Owned by ID 99
        apt.setStatus(AppointmentStatus.CONFIRMED);

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.generatePixPayment(1L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Você não tem permissão para gerar pagamento para este agendamento");
    }

    @Test
    void generatePixPayment_whenStatusIsRequested_shouldThrowBadRequestException() {
        // Arrange
        mockAuthenticatedUser(clientUser);

        Appointment apt = new Appointment();
        apt.setId(1L);
        apt.setClient(clientUser);
        apt.setStatus(AppointmentStatus.REQUESTED);

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.generatePixPayment(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("confirmados ou concluídos");
    }

    @Test
    void generatePixPayment_whenServiceHasNoPrice_shouldThrowBadRequestException() {
        // Arrange
        clientUser.setCpf("12345678901");
        mockAuthenticatedUser(clientUser);

        salonService.setPrice(null);
        Appointment apt = new Appointment();
        apt.setId(1L);
        apt.setClient(clientUser);
        apt.setSalonService(salonService);
        apt.setStatus(AppointmentStatus.CONFIRMED);

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(apt));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.generatePixPayment(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("não possui um valor configurado");
    }
}

