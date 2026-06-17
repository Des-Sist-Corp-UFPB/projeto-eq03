package com.cristiane.salon.models.audit;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class AuditLogTest {

    @Test
    void testGettersSettersAndBuilder() {
        LocalDateTime now = LocalDateTime.now();
        AuditLog log = AuditLog.builder()
                .id(1L)
                .userId(2L)
                .userEmail("user@test.com")
                .action("CREATE")
                .entityType("PRODUCT")
                .entityId(10L)
                .details("Some details")
                .ipAddress("127.0.0.1")
                .userAgent("Mozilla")
                .status("SUCCESS")
                .errorMessage("No error")
                .createdAt(now)
                .build();

        assertEquals(1L, log.getId());
        assertEquals(2L, log.getUserId());
        assertEquals("user@test.com", log.getUserEmail());
        assertEquals("CREATE", log.getAction());
        assertEquals("PRODUCT", log.getEntityType());
        assertEquals(10L, log.getEntityId());
        assertEquals("Some details", log.getDetails());
        assertEquals("127.0.0.1", log.getIpAddress());
        assertEquals("Mozilla", log.getUserAgent());
        assertEquals("SUCCESS", log.getStatus());
        assertEquals("No error", log.getErrorMessage());
        assertEquals(now, log.getCreatedAt());

        // Test NoArgsConstructor and setters
        AuditLog emptyLog = new AuditLog();
        emptyLog.setId(5L);
        emptyLog.setUserId(6L);
        emptyLog.setUserEmail("empty@test.com");
        emptyLog.setAction("UPDATE");
        emptyLog.setEntityType("USER");
        emptyLog.setEntityId(12L);
        emptyLog.setDetails("Empty details");
        emptyLog.setIpAddress("0.0.0.0");
        emptyLog.setUserAgent("Chrome");
        emptyLog.setStatus("FAILURE");
        emptyLog.setErrorMessage("Error occured");
        emptyLog.setCreatedAt(now);

        assertEquals(5L, emptyLog.getId());
        assertEquals(6L, emptyLog.getUserId());
        assertEquals("empty@test.com", emptyLog.getUserEmail());
        assertEquals("UPDATE", emptyLog.getAction());
        assertEquals("USER", emptyLog.getEntityType());
        assertEquals(12L, emptyLog.getEntityId());
        assertEquals("Empty details", emptyLog.getDetails());
        assertEquals("0.0.0.0", emptyLog.getIpAddress());
        assertEquals("Chrome", emptyLog.getUserAgent());
        assertEquals("FAILURE", emptyLog.getStatus());
        assertEquals("Error occured", emptyLog.getErrorMessage());
        assertEquals(now, emptyLog.getCreatedAt());
        
        // Test toString and equals/hashCode
        assertNotNull(log.toString());
        assertEquals(log, log);
        assertNotEquals(log, emptyLog);
        assertNotEquals(log.hashCode(), emptyLog.hashCode());
    }

    @Test
    void testPrePersistOnCreate() {
        AuditLog log = new AuditLog();
        assertNull(log.getCreatedAt());

        log.onCreate();
        assertNotNull(log.getCreatedAt());

        LocalDateTime specificTime = LocalDateTime.now().minusDays(1);
        AuditLog logWithTime = new AuditLog();
        logWithTime.setCreatedAt(specificTime);
        logWithTime.onCreate();
        assertEquals(specificTime, logWithTime.getCreatedAt());
    }
}
