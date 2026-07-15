package com.cristiane.salon.security.crypto;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class AiEncryptionUtilTest {

    private AiEncryptionUtil newUtil() {
        // 32 bytes exatos para AES-256
        String key32Bytes = Base64.getEncoder().encodeToString("unit-test-key-with-32-bytes-len!".getBytes());
        return new AiEncryptionUtil(key32Bytes);
    }

    @Test
    void encryptThenDecrypt_returnsOriginalPlainText() {
        AiEncryptionUtil util = newUtil();
        String plain = "sk-ivdkbnUwyFCPx5Cinwj_UBVz4ijT_S_YEqgZvWesymE";

        String encrypted = util.encrypt(plain);
        String decrypted = util.decrypt(encrypted);

        assertThat(encrypted).isNotEqualTo(plain);
        assertThat(decrypted).isEqualTo(plain);
    }

    @Test
    void encrypt_producesDifferentCiphertextEachTime_dueToRandomIv() {
        AiEncryptionUtil util = newUtil();
        String plain = "same-secret-value";

        String encrypted1 = util.encrypt(plain);
        String encrypted2 = util.encrypt(plain);

        assertThat(encrypted1).isNotEqualTo(encrypted2);
        assertThat(util.decrypt(encrypted1)).isEqualTo(plain);
        assertThat(util.decrypt(encrypted2)).isEqualTo(plain);
    }

    @Test
    void encrypt_withNull_returnsNull() {
        AiEncryptionUtil util = newUtil();
        assertThat(util.encrypt(null)).isNull();
    }

    @Test
    void decrypt_withNull_returnsNull() {
        AiEncryptionUtil util = newUtil();
        assertThat(util.decrypt(null)).isNull();
    }

    @Test
    void mask_showsOnlyPrefixAndSuffix() {
        String masked = AiEncryptionUtil.mask("sk-ivdkbnUwyFCPx5Cinwj_UBVz4ijT_S_YEqgZvWesymE");
        assertThat(masked).isEqualTo("sk-•••••WesymE");
        assertThat(masked).doesNotContain("ivdkbnUwyFCPx5Cinwj");
    }

    @Test
    void mask_withNullOrBlank_returnsNull() {
        assertThat(AiEncryptionUtil.mask(null)).isNull();
        assertThat(AiEncryptionUtil.mask("")).isNull();
    }
}
