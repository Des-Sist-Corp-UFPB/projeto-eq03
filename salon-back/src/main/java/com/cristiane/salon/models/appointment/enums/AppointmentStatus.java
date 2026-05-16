package com.cristiane.salon.models.appointment.enums;

public enum AppointmentStatus {
    /** Legado: agendamento criado antes do fluxo de solicitação */
    PENDING,
    /** Cliente enviou pedido; salão ainda não definiu horário */
    REQUESTED,
    CONFIRMED,
    /** Salão recusou a solicitação */
    DECLINED,
    DONE,
    CANCELLED
}
