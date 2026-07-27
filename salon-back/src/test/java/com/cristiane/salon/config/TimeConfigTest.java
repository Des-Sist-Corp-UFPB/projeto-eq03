package com.cristiane.salon.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Trava a política de datas descrita em {@link TimeConfig}.
 *
 * <p>O bug que originou estes testes: a tela administrativa escolhia 22h para um agendamento e a
 * listagem mostrava 19h. O serializador de {@code LocalDateTime} fingia que o valor gravado era
 * UTC e o convertia para America/Recife na leitura (−3h), mas não havia conversão nenhuma na
 * escrita. Gravava literal, lia deslocado.
 */
class TimeConfigTest {

    private final TimeConfig timeConfig = new TimeConfig();

    private ObjectMapper mapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.registerModule(timeConfig.salonJackson2TimeModule());
        return mapper;
    }

    @Test
    void localDateTime_shouldSerializeLiterally_withoutAnyTimezoneShift() throws Exception {
        // Hora de parede do salão: 22h escolhido tem que voltar como 22h, não 19h.
        LocalDateTime escolhido = LocalDateTime.of(2026, 7, 28, 22, 0, 0);

        String json = mapper().writeValueAsString(escolhido);

        assertThat(json).isEqualTo("\"2026-07-28T22:00:00\"");
        assertThat(json).doesNotContain("19:00");
    }

    @Test
    void localDateTime_shouldRoundTripUnchanged() throws Exception {
        // A ida e a volta precisam ser simétricas — era exatamente essa simetria que faltava.
        LocalDateTime original = LocalDateTime.of(2026, 12, 31, 23, 30, 0);
        ObjectMapper mapper = mapper();

        LocalDateTime roundTripped =
                mapper.readValue(mapper.writeValueAsString(original), LocalDateTime.class);

        assertThat(roundTripped).isEqualTo(original);
    }

    @Test
    void instant_shouldSerializeAsIso8601Utc() throws Exception {
        // Instante de máquina: sai em UTC e sem ambiguidade, o cliente decide como exibir.
        Instant momento = Instant.parse("2026-07-28T22:00:00Z");

        String json = mapper().writeValueAsString(momento);

        assertThat(json).isEqualTo("\"2026-07-28T22:00:00Z\"");
    }

    @Test
    void salonZone_shouldDefaultToRecifeAndBeConfigurable() {
        assertThat(timeConfig.salonZone("America/Recife")).isEqualTo(ZoneId.of("America/Recife"));
        // Configurável de propósito: abrir unidade em outro fuso é mudança de ambiente,
        // não de código.
        assertThat(timeConfig.salonZone("Europe/Lisbon")).isEqualTo(ZoneId.of("Europe/Lisbon"));
    }

    @Test
    void salonClock_shouldAnswerInTheSalonZoneRegardlessOfTheJvmZone() {
        // A JVM roda em UTC (SalonApplication.init()). Perto da meia-noite, "hoje" em UTC e
        // "hoje" em Recife são dias diferentes — e o negócio segue o calendário do salão.
        SalonClock clock = new SalonClock(ZoneId.of("America/Recife"));

        Instant momento = Instant.parse("2026-07-27T01:00:00Z"); // 22h do dia 26 em Recife
        assertThat(clock.toLocalDateTime(momento))
                .isEqualTo(LocalDateTime.of(2026, 7, 26, 22, 0, 0));
    }

    @Test
    void salonClock_toLocalDateTime_shouldTolerateNull() {
        SalonClock clock = new SalonClock(ZoneId.of("America/Recife"));
        assertThat(clock.toLocalDateTime(null)).isNull();
    }
}
