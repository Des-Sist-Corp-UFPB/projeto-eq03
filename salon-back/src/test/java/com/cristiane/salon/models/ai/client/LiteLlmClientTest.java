package com.cristiane.salon.models.ai.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LiteLlmClientTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicReference<String> lastAuthHeader = new AtomicReference<>();
    private final AtomicReference<String> lastRequestBody = new AtomicReference<>();
    private String responseToSend;
    private int statusToSend = 200;

    private final LiteLlmClient client = new LiteLlmClient();

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            lastAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            lastRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

            byte[] body = responseToSend.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusToSend, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void complete_parsesContentAndUsageFromOpenAiShapedResponse() {
        responseToSend = """
                {
                  "choices": [{"message": {"role": "assistant", "content": "{\\"recommendations\\":[]}"}}],
                  "usage": {"total_tokens": 321}
                }
                """;

        LiteLlmCompletionResult result = client.complete(
                baseUrl, "sk-test-key", "gpt-4o-mini", new BigDecimal("0.3"), 500,
                "system prompt", "user prompt"
        );

        assertThat(result.content()).isEqualTo("{\"recommendations\":[]}");
        assertThat(result.totalTokens()).isEqualTo(321);
        assertThat(lastAuthHeader.get()).isEqualTo("Bearer sk-test-key");
        assertThat(lastRequestBody.get()).contains("gpt-4o-mini").contains("system prompt").contains("user prompt");
    }

    @Test
    void complete_withMissingChoices_throwsIllegalState() {
        responseToSend = "{\"choices\": []}";

        assertThatThrownBy(() -> client.complete(baseUrl, "sk", "gpt-4o-mini", new BigDecimal("0.3"), 500, "s", "u"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void complete_whenProviderReturnsError_propagatesAsException() {
        statusToSend = 401;
        responseToSend = "{\"error\":{\"message\":\"Incorrect API key provided: COLOCAR_AQUI\"}}";

        assertThatThrownBy(() -> client.complete(baseUrl, "sk", "gpt-4o-mini", new BigDecimal("0.3"), 500, "s", "u"))
                .isInstanceOf(Exception.class);
    }
}
