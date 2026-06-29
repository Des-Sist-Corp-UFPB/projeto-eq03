package com.cristiane.salon.utils;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CpfValidatorTest {

    @Test
    void isValid_whenCpfIsNull_shouldReturnFalse() {
        assertThat(CpfValidator.isValid(null)).isFalse();
    }

    @Test
    void isValid_whenCpfIsInvalidLength_shouldReturnFalse() {
        assertThat(CpfValidator.isValid("123")).isFalse();
        assertThat(CpfValidator.isValid("123456789012")).isFalse();
    }

    @Test
    void isValid_whenCpfHasAllEqualDigits_shouldReturnFalse() {
        assertThat(CpfValidator.isValid("00000000000")).isFalse();
        assertThat(CpfValidator.isValid("11111111111")).isFalse();
        assertThat(CpfValidator.isValid("22222222222")).isFalse();
        assertThat(CpfValidator.isValid("33333333333")).isFalse();
        assertThat(CpfValidator.isValid("44444444444")).isFalse();
        assertThat(CpfValidator.isValid("55555555555")).isFalse();
        assertThat(CpfValidator.isValid("66666666666")).isFalse();
        assertThat(CpfValidator.isValid("77777777777")).isFalse();
        assertThat(CpfValidator.isValid("88888888888")).isFalse();
        assertThat(CpfValidator.isValid("99999999999")).isFalse();
    }

    @Test
    void isValid_whenCpfIsValid_shouldReturnTrue() {
        assertThat(CpfValidator.isValid("09123456752")).isTrue();
        assertThat(CpfValidator.isValid("12345678909")).isTrue();
    }

    @Test
    void isValid_whenCpfIsInvalid_shouldReturnFalse() {
        assertThat(CpfValidator.isValid("11111111112")).isFalse();
        assertThat(CpfValidator.isValid("09123456753")).isFalse();
        assertThat(CpfValidator.isValid("12345678908")).isFalse();
    }
}
