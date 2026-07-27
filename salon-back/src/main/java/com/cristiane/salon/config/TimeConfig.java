package com.cristiane.salon.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Política de datas e horas da aplicação.
 *
 * <p>Existem aqui dois tipos de data com naturezas diferentes, e tratá-los como se fossem a
 * mesma coisa foi a origem de um bug em produção (horário escolhido às 22h aparecia como 19h):
 *
 * <ul>
 *   <li><b>Hora local do negócio</b> ({@code Appointment.scheduledAt}, {@code preferredDate}):
 *       é hora de relógio de parede no endereço do salão. Um corte marcado para as 14h é "14h
 *       no relógio de Recife", não um ponto na linha do tempo universal — se a cliente estiver
 *       viajando, ela ainda deve ver 14h, porque é a essa hora que ela estará fisicamente lá.
 *       Guardar isso como instante UTC seria pior: mudança na regra de fuso do país (o Brasil
 *       aboliu o horário de verão em 2019) faria o horário "andar" sozinho no relógio. Por isso
 *       usa {@code LocalDateTime}/{@code LocalDate} e é serializado literalmente.</li>
 *   <li><b>Instante de máquina</b> ({@code createdAt}, {@code updatedAt}, auditoria, outbox):
 *       é "quando isso aconteceu", um ponto real na linha do tempo. Usa {@code Instant}, que
 *       não tem fuso nenhum, e é serializado em ISO-8601 UTC para o cliente exibir no fuso de
 *       quem está olhando.</li>
 * </ul>
 *
 * <p>A JVM roda em UTC de propósito (ver {@code SalonApplication.init()}). Isso é intencional e
 * não deve ser revertido: nenhuma regra de negócio pode depender do fuso do sistema operacional,
 * senão o mesmo .jar se comporta diferente numa VPS em Recife e numa em Frankfurt. Onde o fuso
 * do salão importa, ele é pedido explicitamente através do bean {@link #salonZone(String)}.
 */
@Configuration
public class TimeConfig {

    /**
     * Fuso em que o salão opera. Configurável ({@code APP_TIMEZONE}) em vez de constante no
     * código para que abrir uma unidade em outro estado seja mudança de ambiente, não de código.
     */
    @Bean
    public ZoneId salonZone(@Value("${app.timezone:America/Recife}") String timezone) {
        return ZoneId.of(timezone);
    }

    /** Serializadores de tempo para o ObjectMapper Jackson 2.x (usado pelo AuditAspect e pelo push). */
    @Bean
    public com.fasterxml.jackson.databind.Module salonJackson2TimeModule() {
        com.fasterxml.jackson.databind.module.SimpleModule module =
                new com.fasterxml.jackson.databind.module.SimpleModule();

        module.addSerializer(LocalDateTime.class, new com.fasterxml.jackson.databind.JsonSerializer<>() {
            @Override
            public void serialize(LocalDateTime value, com.fasterxml.jackson.core.JsonGenerator gen,
                                  com.fasterxml.jackson.databind.SerializerProvider provider) throws java.io.IOException {
                gen.writeString(value.format(LOCAL_DATE_TIME_FORMAT));
            }
        });

        module.addSerializer(Instant.class, new com.fasterxml.jackson.databind.JsonSerializer<>() {
            @Override
            public void serialize(Instant value, com.fasterxml.jackson.core.JsonGenerator gen,
                                  com.fasterxml.jackson.databind.SerializerProvider provider) throws java.io.IOException {
                gen.writeString(value.toString());
            }
        });

        return module;
    }

    /** Mesma política acima para o Jackson 3.x, que é quem serializa as respostas HTTP no Spring Boot 4. */
    @Bean
    public tools.jackson.databind.JacksonModule salonJackson3TimeModule() {
        tools.jackson.databind.module.SimpleModule module = new tools.jackson.databind.module.SimpleModule();

        module.addSerializer(LocalDateTime.class, new tools.jackson.databind.ValueSerializer<>() {
            @Override
            public void serialize(LocalDateTime value, tools.jackson.core.JsonGenerator gen,
                                  tools.jackson.databind.SerializationContext context) {
                gen.writeString(value.format(LOCAL_DATE_TIME_FORMAT));
            }
        });

        module.addSerializer(Instant.class, new tools.jackson.databind.ValueSerializer<>() {
            @Override
            public void serialize(Instant value, tools.jackson.core.JsonGenerator gen,
                                  tools.jackson.databind.SerializationContext context) {
                gen.writeString(value.toString());
            }
        });

        return module;
    }

    /**
     * Sem deslocamento de fuso, de propósito. O valor sai exatamente como foi digitado e gravado:
     * o front envia "2026-07-28T22:00:00" e recebe "2026-07-28T22:00:00" de volta. A versão
     * anterior fingia que esse valor era UTC e o convertia para Recife na leitura (−3h), sem
     * fazer a conversão inversa na escrita — era essa assimetria que virava 22h em 19h.
     */
    private static final DateTimeFormatter LOCAL_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
}
