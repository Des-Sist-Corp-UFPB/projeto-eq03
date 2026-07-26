package com.cristiane.salon.models.salonprofile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Horário de funcionamento de um dia da semana (issue #116). Sempre existem exatamente 7 linhas
 * (uma por {@link DayOfWeek}), semeadas na migration — a aplicação nunca insere/remove linha,
 * só atualiza {@code open}/{@code openTime}/{@code closeTime}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tb_business_hours")
public class BusinessHour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, unique = true, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "is_open", nullable = false)
    private boolean open;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;
}
