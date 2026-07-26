package com.cristiane.salon.security.crypto;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PiiHashUtilTest {

    private String randomBase64Pepper() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Test
    void hash_isDeterministic_sameInputAlwaysProducesSameOutput() {
        PiiHashUtil util = new PiiHashUtil(randomBase64Pepper());

        String hash1 = util.hash("12345678901");
        String hash2 = util.hash("12345678901");

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void hash_differentInputs_produceDifferentHashes() {
        PiiHashUtil util = new PiiHashUtil(randomBase64Pepper());

        assertThat(util.hash("12345678901")).isNotEqualTo(util.hash("10987654321"));
    }

    @Test
    void hash_withDifferentPepper_producesDifferentHashForSameInput() {
        // Confirma que o pepper realmente participa do cálculo — sem ele o hash
        // seria reversível por força bruta (CPF tem só ~10^11 combinações).
        PiiHashUtil util1 = new PiiHashUtil(randomBase64Pepper());
        PiiHashUtil util2 = new PiiHashUtil(randomBase64Pepper());

        assertThat(util1.hash("12345678901")).isNotEqualTo(util2.hash("12345678901"));
    }

    @Test
    void hash_whenNullOrBlank_returnsNull() {
        PiiHashUtil util = new PiiHashUtil(randomBase64Pepper());

        assertThat(util.hash(null)).isNull();
        assertThat(util.hash("")).isNull();
        assertThat(util.hash("   ")).isNull();
    }

    @Test
    void hash_shouldBeHexEncodedSha256Length() {
        PiiHashUtil util = new PiiHashUtil(randomBase64Pepper());
        String hash = util.hash("12345678901");

        assertThat(hash).hasSize(64); // SHA-256 = 32 bytes = 64 chars hex
        assertThat(hash).matches("^[0-9a-f]{64}$");
    }

    @Test
    void constructor_whenPepperTooShort_shouldThrow() {
        String shortPepper = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> new PiiHashUtil(shortPepper))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
