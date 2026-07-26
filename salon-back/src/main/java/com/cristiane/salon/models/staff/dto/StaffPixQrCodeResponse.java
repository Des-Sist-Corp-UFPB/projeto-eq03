package com.cristiane.salon.models.staff.dto;

import java.math.BigDecimal;

/**
 * Payload do QR de pagamento PIX. Contém o "Copia e Cola" pronto para o app do banco ler —
 * mas <strong>não contém a chave PIX em nenhum campo</strong>. Quem gera isso não fica
 * sabendo qual é a chave da pessoa que vai receber.
 */
public record StaffPixQrCodeResponse(
        String brCodePayload,
        BigDecimal amount,
        String recipientName
) {
}
