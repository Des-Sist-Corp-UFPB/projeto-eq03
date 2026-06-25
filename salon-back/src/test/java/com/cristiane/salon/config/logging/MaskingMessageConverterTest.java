package com.cristiane.salon.config.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class MaskingMessageConverterTest {

    private final MaskingMessageConverter converter = new MaskingMessageConverter();

    @Test
    void shouldMaskEmailsDirectly() {
        assertThat(converter.maskMessage("Email: elksandro@salao.com")).isEqualTo("Email: el***o@salao.com");
        assertThat(converter.maskMessage("Email: user@domain.info")).isEqualTo("Email: us***r@domain.info");
    }

    @Test
    void shouldMaskCpfsDirectly() {
        assertThat(converter.maskMessage("CPF: 123.456.789-09")).isEqualTo("CPF: ***.***.***-09");
        assertThat(converter.maskMessage("CPF: 12345678909")).isEqualTo("CPF: ***.***.***-09");
    }

    @Test
    void shouldMaskPhonesDirectly() {
        assertThat(converter.maskMessage("Telefone: (83) 99999-9999")).isEqualTo("Telefone: () *****-9999");
        assertThat(converter.maskMessage("Telefone: 83999999999")).isEqualTo("Telefone: *****9999");
    }

    @Test
    void shouldMaskNamesInContextDirectly() {
        assertThat(converter.maskMessage("Cliente: José Silva")).isEqualTo("Cliente: J*** S****");
        assertThat(converter.maskMessage("Profissional: Ana Maria Costa")).isEqualTo("Profissional: A** M**** C****");
        assertThat(converter.maskMessage("{\"clientName\":\"Maria Oliveira\"}")).isEqualTo("{\"clientName\":\"M**** O*******\"}");
    }

    @Test
    void shouldIntegrateWithLogbackLayout() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        PatternLayout layout = new PatternLayout();
        layout.setContext(context);
        layout.setPattern("%mask");
        layout.getInstanceConverterMap().put("mask", MaskingMessageConverter::new);
        layout.start();

        LoggingEvent event = new LoggingEvent();
        event.setMessage("O cliente: José Silva com CPF 12345678909 fez login");
        event.setLoggerContext(context);

        String formatted = layout.doLayout(event);
        assertThat(formatted).contains("J*** S****");
        assertThat(formatted).contains("***.***.***-09");
    }
}
