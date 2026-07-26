package com.cristiane.salon.security.crypto;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PiiEncryptionUtilTest {

    private String randomBase64Key() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Test
    void encryptThenDecrypt_shouldReturnOriginalValue() {
        PiiEncryptionUtil util = new PiiEncryptionUtil(randomBase64Key());

        String plain = "12345678901";
        String encrypted = util.encrypt(plain);

        assertThat(encrypted).isNotEqualTo(plain);
        assertThat(util.decrypt(encrypted)).isEqualTo(plain);
    }

    @Test
    void encrypt_whenCalledTwiceWithSameInput_shouldProduceDifferentCiphertext() {
        // IV aleatório por chamada: mesmo texto não pode virar o mesmo ciphertext,
        // senão um atacante com o banco poderia notar CPFs repetidos entre linhas.
        PiiEncryptionUtil util = new PiiEncryptionUtil(randomBase64Key());

        String encrypted1 = util.encrypt("12345678901");
        String encrypted2 = util.encrypt("12345678901");

        assertThat(encrypted1).isNotEqualTo(encrypted2);
        assertThat(util.decrypt(encrypted1)).isEqualTo(util.decrypt(encrypted2));
    }

    @Test
    void encrypt_whenNull_shouldReturnNull() {
        PiiEncryptionUtil util = new PiiEncryptionUtil(randomBase64Key());
        assertThat(util.encrypt(null)).isNull();
    }

    @Test
    void decrypt_whenNull_shouldReturnNull() {
        PiiEncryptionUtil util = new PiiEncryptionUtil(randomBase64Key());
        assertThat(util.decrypt(null)).isNull();
    }

    @Test
    void decrypt_whenCiphertextTamperedWith_shouldFailInsteadOfReturningGarbageSilently() {
        // GCM é autenticado: qualquer alteração no ciphertext precisa estourar, nunca
        // decifrar "algo" silenciosamente.
        PiiEncryptionUtil util = new PiiEncryptionUtil(randomBase64Key());
        String encrypted = util.encrypt("12345678901");

        byte[] bytes = Base64.getDecoder().decode(encrypted);
        bytes[bytes.length - 1] ^= 0xFF; // flip do último byte (parte do tag/ciphertext)
        String tampered = Base64.getEncoder().encodeToString(bytes);

        assertThatThrownBy(() -> util.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void decrypt_whenDifferentKeyThanUsedToEncrypt_shouldFail() {
        PiiEncryptionUtil encryptor = new PiiEncryptionUtil(randomBase64Key());
        PiiEncryptionUtil otherUtil = new PiiEncryptionUtil(randomBase64Key());

        String encrypted = encryptor.encrypt("12345678901");

        assertThatThrownBy(() -> otherUtil.decrypt(encrypted)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constructor_whenKeyIsNot32Bytes_shouldThrow() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> new PiiEncryptionUtil(shortKey))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void constructor_whenKeyIsNotValidBase64_shouldThrow() {
        assertThatThrownBy(() -> new PiiEncryptionUtil("not-valid-base64!!!"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void encrypt_shouldHandleUnicodeCharactersCorrectly() {
        PiiEncryptionUtil util = new PiiEncryptionUtil(randomBase64Key());
        String plain = "chave-pix-com-acentuação-é-çãü";

        String encrypted = util.encrypt(plain);

        assertThat(util.decrypt(encrypted)).isEqualTo(plain);
    }
}
