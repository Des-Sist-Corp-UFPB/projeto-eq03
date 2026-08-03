package com.cristiane.salon.utils;

import com.cristiane.salon.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateRangeValidatorTest {

    @Test
    void validate_localDate_fromAfterTo_throws() {
        assertThatThrownBy(() ->
                DateRangeValidator.validate(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 1))
        ).isInstanceOf(BusinessException.class);
    }

    @Test
    void validate_localDate_fromBeforeOrEqualTo_doesNotThrow() {
        assertThatCode(() ->
                DateRangeValidator.validate(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10))
        ).doesNotThrowAnyException();

        assertThatCode(() ->
                DateRangeValidator.validate(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 1))
        ).doesNotThrowAnyException();
    }

    @Test
    void validate_localDate_withNulls_doesNotThrow() {
        assertThatCode(() -> DateRangeValidator.validate((LocalDate) null, null)).doesNotThrowAnyException();
        assertThatCode(() -> DateRangeValidator.validate(LocalDate.now(), null)).doesNotThrowAnyException();
        assertThatCode(() -> DateRangeValidator.validate(null, LocalDate.now())).doesNotThrowAnyException();
    }

    @Test
    void validate_localDateTime_fromAfterTo_throws() {
        assertThatThrownBy(() ->
                DateRangeValidator.validate(
                        LocalDateTime.of(2026, 8, 10, 12, 0),
                        LocalDateTime.of(2026, 8, 10, 8, 0)
                )
        ).isInstanceOf(BusinessException.class);
    }

    @Test
    void validate_localDateTime_withNulls_doesNotThrow() {
        assertThatCode(() -> DateRangeValidator.validate((LocalDateTime) null, null)).doesNotThrowAnyException();
    }
}
