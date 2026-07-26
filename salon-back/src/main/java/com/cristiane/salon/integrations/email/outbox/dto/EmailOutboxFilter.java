package com.cristiane.salon.integrations.email.outbox.dto;

import com.cristiane.salon.integrations.email.outbox.enums.EmailOutboxStatus;

import java.util.List;

/**
 * {@code statuses} vazio/nulo = sem filtro (o botão "TODOS" da tela de admin). O botão "FALHOU"
 * manda {@code [FAILED, DEAD_LETTER]} — do ponto de vista de quem está olhando a tela, os dois
 * significam "não foi entregue", a distinção entre "ainda tentando" e "desistiu" é só detalhe.
 */
public record EmailOutboxFilter(List<EmailOutboxStatus> statuses) {
}
