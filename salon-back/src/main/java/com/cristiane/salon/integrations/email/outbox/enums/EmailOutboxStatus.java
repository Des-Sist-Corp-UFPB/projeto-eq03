package com.cristiane.salon.integrations.email.outbox.enums;

/**
 * PENDING é só um estado transitório em memória, entre criar a linha e a primeira tentativa —
 * na prática nunca fica persistido, porque {@code EmailOutboxService.sendNow} tenta enviar
 * imediatamente após gravar.
 */
public enum EmailOutboxStatus {
    PENDING,
    SENT,
    FAILED,
    DEAD_LETTER
}
