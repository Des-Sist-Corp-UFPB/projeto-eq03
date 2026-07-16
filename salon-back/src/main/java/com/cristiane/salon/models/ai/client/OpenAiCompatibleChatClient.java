package com.cristiane.salon.models.ai.client;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Casca fina sobre o contrato OpenAI-compatible de chat/completions — não é específica de
 * nenhum provedor. {@code baseUrl}/{@code model}/{@code apiKey} vêm da Central de IA e podem
 * apontar pra qualquer backend que fale esse formato (OpenAI, Azure OpenAI, Groq, OpenRouter,
 * Ollama, vLLM, ou um proxy como o LiteLLM na frente de outra coisa qualquer, incluindo
 * provedores que nativamente não falam esse formato). Nenhuma regra de negócio aqui; quem
 * decide o que perguntar é o {@link com.cristiane.salon.models.ai.service.RecommendationService}.
 */
@Component
public class OpenAiCompatibleChatClient {

    @SuppressWarnings("unchecked")
    public ChatCompletionResult complete(
            String baseUrl,
            String apiKey,
            String model,
            BigDecimal temperature,
            int maxTokens,
            String systemPrompt,
            String userPrompt
    ) {
        RestClient restClient = RestClient.create(baseUrl);

        Map<String, Object> payload = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", temperature,
                "max_tokens", maxTokens,
                "response_format", Map.of("type", "json_object")
        );

        Map<String, Object> response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("Resposta vazia do provedor de IA");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("Resposta do provedor de IA sem 'choices'");
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = message != null ? (String) message.get("content") : null;
        if (content == null) {
            throw new IllegalStateException("Resposta do provedor de IA sem conteúdo");
        }

        Integer totalTokens = null;
        Map<String, Object> usage = (Map<String, Object>) response.get("usage");
        if (usage != null && usage.get("total_tokens") instanceof Number number) {
            totalTokens = number.intValue();
        }

        return new ChatCompletionResult(content, totalTokens);
    }
}
