package com.cristiane.salon.integrations.email.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailGatewayTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicReference<String> lastAuthHeader = new AtomicReference<>();
    private final AtomicReference<String> lastRequestBody = new AtomicReference<>();
    private int statusToSend = 200;

    private EmailGateway gateway;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/emails", exchange -> {
            lastAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            lastRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusToSend, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();

        gateway = new EmailGateway(RestClient.builder());
        ReflectionTestUtils.setField(gateway, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(gateway, "fromEmail", "notificacoes@elksandro.com");
        ReflectionTestUtils.setField(gateway, "apiUrl", baseUrl);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void send_whenSuccessful_postsExpectedPayloadAndAuthHeader() {
        gateway.send("cliente@example.com", "Assunto", "<p>Html</p>", "reply@example.com");

        assertThat(lastAuthHeader.get()).isEqualTo("Bearer test-api-key");
        assertThat(lastRequestBody.get())
                .contains("cliente@example.com")
                .contains("Assunto")
                .contains("notificacoes@elksandro.com")
                .contains("reply@example.com");
    }

    @Test
    void send_whenProviderReturnsError_propagatesException() {
        statusToSend = 500;

        assertThatThrownBy(() -> gateway.send("cliente@example.com", "Assunto", "<p>Html</p>", "reply@example.com"))
                .isInstanceOf(RestClientException.class);
    }
}
