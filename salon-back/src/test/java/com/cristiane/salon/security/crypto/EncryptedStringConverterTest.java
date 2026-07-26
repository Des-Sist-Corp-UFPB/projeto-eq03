package com.cristiane.salon.security.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptedStringConverterTest {

    private EncryptedStringConverter converter;

    @BeforeEach
    void setUp() {
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);
        converter = new EncryptedStringConverter(new PiiEncryptionUtil(base64Key));
    }

    @Test
    void convertToDatabaseColumn_shouldEncryptTheValue() {
        String cipherText = converter.convertToDatabaseColumn("12345678901");

        assertThat(cipherText).isNotNull().isNotEqualTo("12345678901");
    }

    @Test
    void roundTrip_shouldRecoverOriginalValue() {
        String cipherText = converter.convertToDatabaseColumn("chave-pix@exemplo.com");

        assertThat(converter.convertToEntityAttribute(cipherText)).isEqualTo("chave-pix@exemplo.com");
    }

    @Test
    void convertToDatabaseColumn_whenNull_returnsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void convertToEntityAttribute_whenNull_returnsNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
