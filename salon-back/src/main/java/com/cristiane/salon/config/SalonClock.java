package com.cristiane.salon.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * "Que horas são no salão?" — ponto único para qualquer regra de negócio que precise do agora
 * ou do hoje.
 *
 * <p>Existe para que nenhum serviço chame {@code LocalDateTime.now()} direto. Aquela chamada lê
 * o fuso do sistema operacional, que aqui é UTC de propósito (ver {@code SalonApplication}), e
 * já causou um bug real: comparar um horário digitado pela tela (hora de Recife) com
 * {@code LocalDateTime.now()} (hora UTC) fazia o sistema recusar como "no passado" qualquer
 * agendamento marcado para menos de 3 horas à frente — encaixar alguém para daqui a uma hora
 * era impossível.
 *
 * <p>Também converte instante de máquina para hora de parede do salão, que é o único jeito
 * correto de comparar um {@code createdAt} (Instant) com um {@code scheduledAt} (LocalDateTime).
 */
@Component
@RequiredArgsConstructor
public class SalonClock {

    private final ZoneId salonZone;

    /** Agora, no relógio de parede do salão. */
    public LocalDateTime now() {
        return LocalDateTime.now(salonZone);
    }

    /** Hoje, no calendário do salão — perto da meia-noite isso difere do "hoje" em UTC. */
    public LocalDate today() {
        return LocalDate.now(salonZone);
    }

    /** Converte um instante de máquina para a hora de parede correspondente no salão. */
    public LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, salonZone);
    }

    public ZoneId zone() {
        return salonZone;
    }
}
