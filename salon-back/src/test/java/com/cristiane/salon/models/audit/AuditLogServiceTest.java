package com.cristiane.salon.models.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        RequestContextHolder.resetRequestAttributes();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void logAction_whenNoRequestContext_shouldLogWithDefaultIPAndUA() {
        // Arrange
        AuditLog expected = AuditLog.builder().id(1L).build();
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(expected);

        // Act
        AuditLog result = auditLogService.logAction(
                10L, "test@example.com", "CREATE", "User", 10L, "Created user", "SUCCESS"
        );

        // Assert
        assertThat(result).isNotNull();
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getIpAddress()).isEqualTo("N/A");
        assertThat(saved.getUserAgent()).isEqualTo("N/A");
        assertThat(saved.getUserId()).isEqualTo(10L);
        assertThat(saved.getUserEmail()).isEqualTo("test@example.com");
        assertThat(saved.getAction()).isEqualTo("CREATE");
        assertThat(saved.getEntityType()).isEqualTo("User");
        assertThat(saved.getEntityId()).isEqualTo(10L);
        assertThat(saved.getDetails()).isEqualTo("Created user");
        assertThat(saved.getStatus()).isEqualTo("SUCCESS");
        assertThat(saved.getErrorMessage()).isNull();
    }

    @Test
    void logAction_withXForwardedForHeader_shouldExtractFirstIp() {
        // Arrange
        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.0.1, 10.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(attributes.getRequest()).thenReturn(request);
        RequestContextHolder.setRequestAttributes(attributes);

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        AuditLog result = auditLogService.logAction(
                10L, "test@example.com", "CREATE", "User", 10L, "Created user", "SUCCESS", "Error message"
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getIpAddress()).isEqualTo("192.168.0.1");
        assertThat(result.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(result.getErrorMessage()).isEqualTo("Error message");
    }

    @Test
    void logAction_withXRealIpHeader_shouldUseRealIp() {
        // Arrange
        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("172.16.0.5");
        when(attributes.getRequest()).thenReturn(request);
        RequestContextHolder.setRequestAttributes(attributes);

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        AuditLog result = auditLogService.logAction(
                10L, "test@example.com", "CREATE", "User", 10L, "Created user", "SUCCESS"
        );

        // Assert
        assertThat(result.getIpAddress()).isEqualTo("172.16.0.5");
    }

    @Test
    void logAction_withNoIpHeaders_shouldUseRemoteAddr() {
        // Arrange
        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("");
        when(request.getHeader("X-Real-IP")).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("200.100.50.25");
        when(attributes.getRequest()).thenReturn(request);
        RequestContextHolder.setRequestAttributes(attributes);

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        AuditLog result = auditLogService.logAction(
                10L, "test@example.com", "CREATE", "User", 10L, "Created user", "SUCCESS"
        );

        // Assert
        assertThat(result.getIpAddress()).isEqualTo("200.100.50.25");
    }

    @Test
    void logAction_whenSaveThrowsException_shouldCatchAndReturnNull() {
        // Arrange
        when(auditLogRepository.save(any(AuditLog.class))).thenThrow(new RuntimeException("Database down"));

        // Act
        AuditLog result = auditLogService.logAction(
                10L, "test@example.com", "CREATE", "User", 10L, "Created user", "SUCCESS"
        );

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void getAllAuditLogs_withPageableOnly_shouldCallFindAll() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> page = new PageImpl<>(Collections.emptyList());
        when(auditLogRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<AuditLog> result = auditLogService.getAllAuditLogs(pageable);

        // Assert
        assertThat(result).isNotNull();
        verify(auditLogRepository).findAll(pageable);
    }

    @Test
    void getAllAuditLogs_withFilters_shouldTrimParamsAndFormatEmail() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> page = new PageImpl<>(Collections.emptyList());
        when(auditLogRepository.findWithFilters("LOGIN", "User", "%test@example.com%", pageable)).thenReturn(page);

        // Act
        Page<AuditLog> result = auditLogService.getAllAuditLogs(" LOGIN ", " User ", " Test@Example.com ", pageable);

        // Assert
        assertThat(result).isNotNull();
        verify(auditLogRepository).findWithFilters("LOGIN", "User", "%test@example.com%", pageable);
    }

    @Test
    void getAllAuditLogs_withEmptyFilters_shouldPassNull() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> page = new PageImpl<>(Collections.emptyList());
        when(auditLogRepository.findWithFilters(null, null, null, pageable)).thenReturn(page);

        // Act
        Page<AuditLog> result = auditLogService.getAllAuditLogs("  ", " ", "   ", pageable);

        // Assert
        assertThat(result).isNotNull();
        verify(auditLogRepository).findWithFilters(null, null, null, pageable);
    }

    @Test
    void getAuditLogsWithCombinedFilters_shouldConstructRecifeDatesAndQuery() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> page = new PageImpl<>(Collections.emptyList());

        LocalDate startDate = LocalDate.of(2026, 6, 1);
        LocalDate endDate = LocalDate.of(2026, 6, 15);

        ZonedDateTime zStart = startDate.atTime(LocalTime.of(0, 0, 0)).atZone(ZoneId.of("America/Recife"));
        LocalDateTime expectedStart = zStart.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();

        ZonedDateTime zEnd = endDate.atTime(LocalTime.of(23, 59, 59)).atZone(ZoneId.of("America/Recife"));
        LocalDateTime expectedEnd = zEnd.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();

        when(auditLogRepository.findWithFiltersCombined(
                eq(10L), eq("User"), eq("UPDATE"), eq(expectedStart), eq(expectedEnd), eq(pageable)
        )).thenReturn(page);

        // Act
        Page<AuditLog> result = auditLogService.getAuditLogsWithCombinedFilters(
                10L, " User ", " UPDATE ", startDate, endDate, pageable
        );

        // Assert
        assertThat(result).isNotNull();
        verify(auditLogRepository).findWithFiltersCombined(
                eq(10L), eq("User"), eq("UPDATE"), eq(expectedStart), eq(expectedEnd), eq(pageable)
        );
    }

    @Test
    void getAuditLogsWithCombinedFilters_whenDatesAreNull_shouldQueryWithNullDates() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> page = new PageImpl<>(Collections.emptyList());
        when(auditLogRepository.findWithFiltersCombined(
                eq(null), eq(null), eq(null), eq(null), eq(null), eq(pageable)
        )).thenReturn(page);

        // Act
        Page<AuditLog> result = auditLogService.getAuditLogsWithCombinedFilters(
                null, null, null, null, null, pageable
        );

        // Assert
        assertThat(result).isNotNull();
        verify(auditLogRepository).findWithFiltersCombined(
                eq(null), eq(null), eq(null), eq(null), eq(null), eq(pageable)
        );
    }

    @Test
    void getAuditLogsByUser_shouldQueryRepository() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(auditLogRepository.findByUserId(10L, pageable)).thenReturn(Page.empty());

        // Act
        Page<AuditLog> result = auditLogService.getAuditLogsByUser(10L, pageable);

        // Assert
        verify(auditLogRepository).findByUserId(10L, pageable);
    }

    @Test
    void getAuditLogsByAction_shouldQueryRepository() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(auditLogRepository.findByAction("LOGIN", pageable)).thenReturn(Page.empty());

        // Act
        Page<AuditLog> result = auditLogService.getAuditLogsByAction("LOGIN", pageable);

        // Assert
        verify(auditLogRepository).findByAction("LOGIN", pageable);
    }

    @Test
    void getAuditLogsByEntityType_shouldQueryRepository() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(auditLogRepository.findByEntityType("User", pageable)).thenReturn(Page.empty());

        // Act
        Page<AuditLog> result = auditLogService.getAuditLogsByEntityType("User", pageable);

        // Assert
        verify(auditLogRepository).findByEntityType("User", pageable);
    }

    @Test
    void getAuditLogsByDateRange_shouldQueryRepository() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime from = LocalDateTime.now().minusDays(5);
        LocalDateTime to = LocalDateTime.now();
        when(auditLogRepository.findByCreatedAtBetween(from, to, pageable)).thenReturn(Page.empty());

        // Act
        Page<AuditLog> result = auditLogService.getAuditLogsByDateRange(from, to, pageable);

        // Assert
        verify(auditLogRepository).findByCreatedAtBetween(from, to, pageable);
    }

    @Test
    void getEntityHistory_shouldQueryRepository() {
        // Arrange
        when(auditLogRepository.findByEntityTypeAndEntityId("User", 10L)).thenReturn(Collections.emptyList());

        // Act
        List<AuditLog> result = auditLogService.getEntityHistory("User", 10L);

        // Assert
        verify(auditLogRepository).findByEntityTypeAndEntityId("User", 10L);
    }
}
