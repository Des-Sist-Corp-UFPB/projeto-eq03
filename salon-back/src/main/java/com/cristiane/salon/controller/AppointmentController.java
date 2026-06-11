package com.cristiane.salon.controller;

import com.cristiane.salon.annotation.Auditable;
import com.cristiane.salon.models.appointment.dto.AppointmentRequest;
import com.cristiane.salon.models.appointment.dto.AppointmentResponse;
import com.cristiane.salon.models.appointment.dto.ConfirmAppointmentRequest;
import com.cristiane.salon.models.appointment.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Endpoints para gerenciamento de agendamentos")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Auditable(action = "APPOINTMENT_CREATED", entityType = "Appointment", captureArgs = true)
    @Operation(summary = "Cliente solicita agenda ou equipe cria agendamento com horário")
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.create(request));
    }

    @PatchMapping("/{id}/confirm")
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Auditable(action = "APPOINTMENT_CONFIRMED", entityType = "Appointment", captureArgs = true)
    @Operation(summary = "Confirma solicitação do cliente definindo data e hora")
    public ResponseEntity<AppointmentResponse> confirm(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmAppointmentRequest body) {
        return ResponseEntity.ok(appointmentService.confirm(id, body.scheduledAt()));
    }

    @PatchMapping("/{id}/decline")
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Auditable(action = "APPOINTMENT_DECLINED", entityType = "Appointment", captureArgs = true)
    @Operation(summary = "Recusa solicitação de agendamento do cliente")
    public ResponseEntity<AppointmentResponse> decline(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.decline(id));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista os agendamentos do usuário logado (Cliente)")
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments() {
        return ResponseEntity.ok(appointmentService.getMyAppointments());
    }

    @GetMapping
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Operation(summary = "Lista todos os agendamentos (Admin/Gerente)")
    public ResponseEntity<List<AppointmentResponse>> findAll() {
        return ResponseEntity.ok(appointmentService.findAll());
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Auditable(action = "APPOINTMENT_CANCELLED", entityType = "Appointment", captureArgs = true)
    @Operation(summary = "Cancela um agendamento (Dono ou Admin)")
    public ResponseEntity<AppointmentResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.cancel(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Auditable(action = "APPOINTMENT_STATUS_CHANGED", entityType = "Appointment", captureArgs = true)
    @Operation(summary = "Atualiza o status de um agendamento (Admin/Gerente/Funcionária)")
    public ResponseEntity<AppointmentResponse> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(appointmentService.updateStatus(id, status));
    }
}
