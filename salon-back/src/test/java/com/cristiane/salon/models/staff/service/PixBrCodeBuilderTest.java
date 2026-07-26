package com.cristiane.salon.models.staff.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PixBrCodeBuilderTest {

    @Test
    void build_shouldNeverContainThePixKeyInPlainSightAsASeparateLeakedField() {
        // A chave PIX PRECISA aparecer dentro do payload (é assim que o app do banco sabe
        // para quem pagar) — o que este teste garante é que o payload é só isso: não deve
        // haver nenhum campo adicional vazando a chave fora do bloco 26 (Merchant Account Info).
        String payload = PixBrCodeBuilder.build(
                "chave-secreta@exemplo.com", "Maria Silva", "Recife", new BigDecimal("70.00"), "TX123"
        );

        assertThat(payload).contains("chave-secreta@exemplo.com");
        // A chave deve aparecer exatamente uma vez (dentro do bloco 26), não duplicada em outro campo.
        assertThat(payload.split("chave-secreta@exemplo.com", -1)).hasSize(2);
    }

    @Test
    void build_shouldStartWithPayloadFormatIndicator() {
        String payload = PixBrCodeBuilder.build("11144477735", "Maria Silva", "Recife", null, null);

        assertThat(payload).startsWith("000201");
    }

    @Test
    void build_shouldEndWithValidCrc16() {
        String payload = PixBrCodeBuilder.build("11144477735", "Maria Silva", "Recife",
                new BigDecimal("100.00"), "TX1");

        // "withoutCrc" já inclui o "6304" (ID+tamanho do próprio campo do CRC) — o cálculo
        // do CRC no padrão EMV cobre até esse marcador, não incluindo o valor do CRC em si.
        String withoutCrc = payload.substring(0, payload.length() - 4);
        String crcInPayload = payload.substring(payload.length() - 4);
        String recalculated = PixBrCodeBuilder.crc16Ccitt(withoutCrc);

        assertThat(crcInPayload).isEqualTo(recalculated);
    }

    @Test
    void build_withoutAmount_shouldOmitTheAmountField() {
        String payload = PixBrCodeBuilder.build("11144477735", "Maria Silva", "Recife", null, "TX1");

        assertThat(payload).doesNotContain("5405"); // tag 54 (amount) com 5 chars de valor
    }

    @Test
    void build_withAmount_shouldIncludeItFormattedWithTwoDecimals() {
        String payload = PixBrCodeBuilder.build("11144477735", "Maria Silva", "Recife",
                new BigDecimal("70"), "TX1");

        assertThat(payload).contains("540570.00");
    }

    @Test
    void build_shouldTruncateMerchantNameLongerThan25Chars() {
        String longName = "Nome Extremamente Longo Que Excede O Limite";
        String payload = PixBrCodeBuilder.build("11144477735", longName, "Recife", null, "TX1");

        assertThat(payload).contains(longName.substring(0, 25));
        assertThat(payload).doesNotContain(longName);
    }

    @Test
    void build_shouldRemoveAccentsFromMerchantNameAndCity() {
        String payload = PixBrCodeBuilder.build("11144477735", "José António", "São Paulo", null, "TX1");

        assertThat(payload).contains("Jose Antonio");
        assertThat(payload).contains("Sao Paulo");
    }

    @Test
    void build_whenTxIdBlank_shouldUseDefaultPlaceholder() {
        String payload = PixBrCodeBuilder.build("11144477735", "Maria Silva", "Recife", null, "");

        assertThat(payload).contains("***");
    }

    @Test
    void crc16Ccitt_isDeterministic() {
        String a = PixBrCodeBuilder.crc16Ccitt("hello world");
        String b = PixBrCodeBuilder.crc16Ccitt("hello world");

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSize(4);
    }

    @Test
    void crc16Ccitt_differentInputs_produceDifferentCrc() {
        assertThat(PixBrCodeBuilder.crc16Ccitt("hello world"))
                .isNotEqualTo(PixBrCodeBuilder.crc16Ccitt("hello world!"));
    }
}
