package com.cristiane.salon.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class BeanConfigTest {

    /**
     * Regressão: sem JavaTimeModule registrado, este mapper falhava ao serializar qualquer
     * LocalDate (ex.: StaffProfileRequest.birthDate) — o que fazia o AuditAspect descartar
     * silenciosamente os detalhes de auditoria de qualquer @Auditable(captureArgs=true) cujo
     * argumento tivesse um campo LocalDate.
     */
    @Test
    void objectMapper_shouldSerializeLocalDateWithoutThrowing() {
        ObjectMapper mapper = new BeanConfig(null).objectMapper(new TimeConfig().salonJackson2TimeModule());

        assertThatCode(() -> mapper.writeValueAsString(LocalDate.of(1990, 1, 1)))
                .doesNotThrowAnyException();
    }

    @Test
    void objectMapper_shouldRoundTripLocalDate() throws Exception {
        ObjectMapper mapper = new BeanConfig(null).objectMapper(new TimeConfig().salonJackson2TimeModule());
        LocalDate original = LocalDate.of(1990, 1, 1);

        String json = mapper.writeValueAsString(original);

        assertThat(mapper.readValue(json, LocalDate.class)).isEqualTo(original);
    }
}
