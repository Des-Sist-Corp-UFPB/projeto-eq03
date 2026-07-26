package com.cristiane.salon.models.staff.service;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Monta o payload "BR Code" (EMV QRCPS-MPM) usado pelos apps de banco para ler um QR de PIX.
 *
 * <p>Formato TLV (tag-length-value): cada campo é {@code IDLLVALUE}, onde ID e LL têm 2
 * dígitos fixos. É o mesmo formato que qualquer maquininha/gateway PIX gera — não há nada
 * proprietário aqui, é a especificação pública do Banco Central.
 *
 * <p>Existe como classe separada (em vez de método no service) porque monta uma string com
 * regra própria e tem seu próprio teste de unidade (CRC16 é fácil de errar silenciosamente).
 */
public final class PixBrCodeBuilder {

    private PixBrCodeBuilder() {
    }

    /**
     * @param pixKey       chave PIX já decifrada — só usada em memória, nunca logada
     * @param merchantName nome do recebedor (máx. 25 caracteres, exigência do padrão)
     * @param merchantCity cidade do recebedor (máx. 15 caracteres)
     * @param amount       valor a cobrar, ou {@code null} para deixar o valor livre no app do pagador
     * @param txId         identificador da transação (máx. 25 caracteres alfanuméricos)
     */
    public static String build(String pixKey, String merchantName, String merchantCity,
                                BigDecimal amount, String txId) {
        StringBuilder payload = new StringBuilder();

        tlv(payload, "00", "01"); // Payload Format Indicator
        payload.append(merchantAccountInfo(pixKey));
        tlv(payload, "52", "0000"); // Merchant Category Code (não classificado)
        tlv(payload, "53", "986"); // Moeda: BRL
        if (amount != null) {
            tlv(payload, "54", amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        }
        tlv(payload, "58", "BR");
        tlv(payload, "59", truncate(normalize(merchantName), 25));
        tlv(payload, "60", truncate(normalize(merchantCity), 15));
        payload.append(additionalDataField(txId));

        // CRC é sempre o último campo, e o cálculo inclui o próprio "6304" (ID+tamanho) do CRC.
        payload.append("6304");
        String crc = crc16Ccitt(payload.toString());
        payload.append(crc);

        return payload.toString();
    }

    private static String merchantAccountInfo(String pixKey) {
        StringBuilder sub = new StringBuilder();
        tlv(sub, "00", "br.gov.bcb.pix");
        tlv(sub, "01", pixKey);
        return wrap("26", sub.toString());
    }

    private static String additionalDataField(String txId) {
        String safeTxId = (txId == null || txId.isBlank()) ? "***" : truncate(normalize(txId), 25);
        StringBuilder sub = new StringBuilder();
        tlv(sub, "05", safeTxId);
        return wrap("62", sub.toString());
    }

    private static void tlv(StringBuilder target, String id, String value) {
        target.append(wrap(id, value));
    }

    private static String wrap(String id, String value) {
        String length = String.format(Locale.ROOT, "%02d", value.length());
        return id + length + value;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        // O padrão exige ASCII sem acentos; troca fora do range básico por espaço.
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\x20-\\x7E]", " ")
                .trim();
    }

    private static String truncate(String value, int max) {
        return value.length() > max ? value.substring(0, max) : value;
    }

    /** CRC16-CCITT (polinômio 0x1021, valor inicial 0xFFFF) — o checksum exigido pelo padrão EMV. */
    static String crc16Ccitt(String data) {
        int crc = 0xFFFF;
        byte[] bytes = data.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for (byte b : bytes) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format(Locale.ROOT, "%04X", crc);
    }
}
