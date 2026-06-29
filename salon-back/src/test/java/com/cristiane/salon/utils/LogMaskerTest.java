package com.cristiane.salon.utils;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LogMaskerTest {

    @Test
    void shouldMaskEmailCorrectly() {
        assertThat(LogMasker.maskEmail("elksandro@salao.com")).isEqualTo("el***o@salao.com");
        assertThat(LogMasker.maskEmail("ab@c.com")).isEqualTo("a***@c.com");
        assertThat(LogMasker.maskEmail("a@b.com")).isEqualTo("a***@b.com");
        assertThat(LogMasker.maskEmail("")).isEmpty();
        assertThat(LogMasker.maskEmail(null)).isNull();
        assertThat(LogMasker.maskEmail("   ")).isEqualTo("   ");
        assertThat(LogMasker.maskEmail("@b.com")).isEqualTo("***");
        assertThat(LogMasker.maskEmail("abc.com")).isEqualTo("***");
    }

    @Test
    void shouldMaskCpfCorrectly() {
        assertThat(LogMasker.maskCpf("12345678909")).isEqualTo("***.***.***-09");
        assertThat(LogMasker.maskCpf("123.456.789-09")).isEqualTo("***.***.***-09");
        assertThat(LogMasker.maskCpf("123")).isEqualTo("***");
        assertThat(LogMasker.maskCpf("")).isEmpty();
        assertThat(LogMasker.maskCpf(null)).isNull();
        assertThat(LogMasker.maskCpf("   ")).isEqualTo("   ");
    }

    @Test
    void shouldSanitizeJsonPayload() {
        String inputJson = "{\"email\":\"elksandro@salao.com\",\"identification\":{\"type\":\"CPF\",\"number\":\"12345678909\"},\"name\":\"José Silva\"}";
        String sanitized = LogMasker.sanitizeJson(inputJson);
        assertThat(sanitized).contains("\"email\":\"el***o@salao.com\"");
        assertThat(sanitized).contains("\"number\":\"***.***.***-09\"");

        // Edge cases
        assertThat(LogMasker.sanitizeJson(null)).isNull();
        assertThat(LogMasker.sanitizeJson("")).isEmpty();
        assertThat(LogMasker.sanitizeJson("   ")).isEqualTo("   ");

        // Non-11 length number pattern
        String otherJson = "{\"number\":\"123456\"}";
        assertThat(LogMasker.sanitizeJson(otherJson)).contains("\"number\":\"***\"");
    }

    @Test
    void shouldMaskRawEmailsAndCpfs() {
        assertThat(LogMasker.maskRawEmails(null)).isNull();
        assertThat(LogMasker.maskRawCpfs(null)).isNull();
    }
}
