package com.cristiane.salon.aspect;

import com.cristiane.salon.annotation.Auditable;
import com.cristiane.salon.models.appointment.dto.AppointmentResponse;
import com.cristiane.salon.models.audit.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.reflect.Method;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private AuditLogService auditLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AuditAspect auditAspect;

    @BeforeEach
    void setUp() {
        auditAspect = new AuditAspect(auditLogService, objectMapper);
    }

    // A dummy method to simulate Controller method with PathVariable
    public void dummyUpdateMethod(@PathVariable Long id, String status) {}

    // A dummy method for create
    public void dummyCreateMethod() {}

    @Test
    void testExtractEntityIdFromPathVariable() throws Exception {
        JoinPoint joinPoint = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        
        Method method = this.getClass().getMethod("dummyUpdateMethod", Long.class, String.class);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(signature);
        
        Object[] args = new Object[]{ 42L, "PENDING" };
        when(joinPoint.getArgs()).thenReturn(args);

        Auditable auditable = mock(Auditable.class);
        when(auditable.action()).thenReturn("UPDATE");
        when(auditable.entityType()).thenReturn("Dummy");
        when(auditable.captureArgs()).thenReturn(false);

        auditAspect.logSuccessfulAction(joinPoint, auditable, null);

        verify(auditLogService).logAction(
                any(), any(), eq("UPDATE"), eq("Dummy"), eq(42L), any(), eq("SUCCESS")
        );
    }

    @Test
    void testExtractEntityIdFromReturnValue() throws Exception {
        JoinPoint joinPoint = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        
        Method method = this.getClass().getMethod("dummyCreateMethod");
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);

        AppointmentResponse response = new AppointmentResponse(
                99L, 1L, "Client", 2L, "Employee", 3L, "Service",
                null, null, "Notes", "PENDING"
        );
        ResponseEntity<AppointmentResponse> result = ResponseEntity.ok(response);

        Auditable auditable = mock(Auditable.class);
        when(auditable.action()).thenReturn("CREATE");
        when(auditable.entityType()).thenReturn("Dummy");
        
        auditAspect.logSuccessfulAction(joinPoint, auditable, result);

        verify(auditLogService).logAction(
                any(), any(), eq("CREATE"), eq("Dummy"), eq(99L), any(), eq("SUCCESS")
        );
    }

    @Test
    void testStatusOverrideToCompleted() throws Exception {
        JoinPoint joinPoint = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        
        Method method = this.getClass().getMethod("dummyUpdateMethod", Long.class, String.class);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(signature);
        
        when(signature.getParameterNames()).thenReturn(new String[]{"id", "status"});
        Object[] args = new Object[]{ 10L, "DONE" };
        when(joinPoint.getArgs()).thenReturn(args);

        Auditable auditable = mock(Auditable.class);
        when(auditable.action()).thenReturn("APPOINTMENT_STATUS_CHANGED");
        when(auditable.entityType()).thenReturn("Appointment");

        auditAspect.logSuccessfulAction(joinPoint, auditable, null);

        verify(auditLogService).logAction(
                any(), any(), eq("APPOINTMENT_COMPLETED"), eq("Appointment"), eq(10L), any(), eq("SUCCESS")
        );
    }

    @Test
    void testParameterMasking() throws Exception {
        JoinPoint joinPoint = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        
        Method method = this.getClass().getMethod("dummyCreateMethod");
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(signature);
        
        class SensitiveDto {
            public String password = "mySecretPassword";
            public String clientNotes = "some private client notes";
            public String name = "Public Name";
        }
        
        Object[] args = new Object[]{ new SensitiveDto() };
        when(joinPoint.getArgs()).thenReturn(args);

        Auditable auditable = mock(Auditable.class);
        when(auditable.action()).thenReturn("CREATE");
        when(auditable.entityType()).thenReturn("Dummy");
        when(auditable.captureArgs()).thenReturn(true);

        auditAspect.logSuccessfulAction(joinPoint, auditable, null);

        verify(auditLogService).logAction(
                any(), any(), any(), any(), any(),
                argThat(details -> details != null && details.contains("\"password\":\"***\"") && details.contains("\"clientNotes\":\"***\"") && details.contains("\"name\":\"Public Name\"")),
                eq("SUCCESS")
        );
    }
}
