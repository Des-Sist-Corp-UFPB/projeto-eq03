package com.cristiane.salon.models.ai.service;

import com.cristiane.salon.exception.BusinessException;
import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.models.ai.client.LiteLlmClient;
import com.cristiane.salon.models.ai.client.LiteLlmCompletionResult;
import com.cristiane.salon.models.ai.dto.RecommendationResult;
import com.cristiane.salon.models.ai.entity.AiCallLog;
import com.cristiane.salon.models.ai.entity.AiConfig;
import com.cristiane.salon.models.ai.entity.AiRecommendation;
import com.cristiane.salon.models.ai.entity.RecommendationPriority;
import com.cristiane.salon.models.ai.entity.RecommendationType;
import com.cristiane.salon.models.ai.repository.AiCallLogRepository;
import com.cristiane.salon.models.ai.repository.AiRecommendationRepository;
import com.cristiane.salon.models.appointment.dto.AppointmentResponse;
import com.cristiane.salon.models.appointment.service.AppointmentService;
import com.cristiane.salon.models.report.dto.AppointmentReportResponse;
import com.cristiane.salon.models.report.dto.FinancialReportResponse;
import com.cristiane.salon.models.report.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock private AiConfigService aiConfigService;
    @Mock private LiteLlmClient liteLlmClient;
    @Mock private ReportService reportService;
    @Mock private AppointmentService appointmentService;
    @Mock private AiRecommendationRepository recommendationRepository;
    @Mock private AiCallLogRepository callLogRepository;

    private RecommendationService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationService(
                aiConfigService, liteLlmClient, reportService, appointmentService,
                recommendationRepository, callLogRepository, new ObjectMapper()
        );
    }

    private AiConfig enabledConfig() {
        return AiConfig.builder()
                .id(1L)
                .baseUrl("https://llm.rodrigor.com")
                .model("gpt-4o-mini")
                .apiKeyEncrypted("encrypted")
                .temperature(new BigDecimal("0.3"))
                .maxTokens(500)
                .enabled(true)
                .dailyCallBudget(200)
                .build();
    }

    @Test
    void generate_whenAiDisabled_throwsBusinessException() {
        AiConfig config = enabledConfig();
        config.setEnabled(false);
        when(aiConfigService.getDecryptedForInternalUse()).thenReturn(config);

        assertThatThrownBy(() -> service.generate(RecommendationType.FINANCEIRO, "USER", "1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("desativadas");
        verifyNoInteractions(liteLlmClient);
    }

    @Test
    void generate_whenNoApiKeyConfigured_throwsBusinessException() {
        AiConfig config = enabledConfig();
        when(aiConfigService.getDecryptedForInternalUse()).thenReturn(config);
        when(aiConfigService.getDecryptedApiKey(config)).thenReturn(null);

        assertThatThrownBy(() -> service.generate(RecommendationType.FINANCEIRO, "USER", "1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("API key");
        verifyNoInteractions(liteLlmClient);
    }

    @Test
    void generate_whenDailyBudgetReached_throwsBusinessException() {
        AiConfig config = enabledConfig();
        when(aiConfigService.getDecryptedForInternalUse()).thenReturn(config);
        when(aiConfigService.getDecryptedApiKey(config)).thenReturn("sk-test");
        when(callLogRepository.countSuccessfulSince(any())).thenReturn(200L);

        assertThatThrownBy(() -> service.generate(RecommendationType.FINANCEIRO, "USER", "1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Orçamento");
        verifyNoInteractions(liteLlmClient);
    }

    @Test
    void generate_financeiro_happyPath_persistsCacheAndLogsSuccess() {
        AiConfig config = enabledConfig();
        when(aiConfigService.getDecryptedForInternalUse()).thenReturn(config);
        when(aiConfigService.getDecryptedApiKey(config)).thenReturn("sk-test");
        when(callLogRepository.countSuccessfulSince(any())).thenReturn(0L);
        when(reportService.generateFinancialReport(any(), any())).thenReturn(
                new FinancialReportResponse(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, List.of(), "mock")
        );
        when(reportService.generateAppointmentReport(any(), any())).thenReturn(
                new AppointmentReportResponse(10, 2, 3, 4, 1, Map.of(), Map.of(), "mock")
        );

        String llmJson = """
                {"recommendations":[{"title":"Ociosidade nas terças","description":"40% de vagas livres.","suggestedAction":"Criar promoção","priority":"ALTA"}]}
                """;
        when(liteLlmClient.complete(anyString(), anyString(), anyString(), any(), anyInt(), anyString(), anyString()))
                .thenReturn(new LiteLlmCompletionResult(llmJson, 123));

        RecommendationResult result = service.generate(RecommendationType.FINANCEIRO, "USER", "5");

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).title()).isEqualTo("Ociosidade nas terças");
        assertThat(result.items().get(0).priority()).isEqualTo(RecommendationPriority.ALTA);
        assertThat(result.fromCache()).isFalse();

        verify(recommendationRepository).save(any(AiRecommendation.class));

        ArgumentCaptor<AiCallLog> logCaptor = ArgumentCaptor.forClass(AiCallLog.class);
        verify(callLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getSuccess()).isTrue();
        assertThat(logCaptor.getValue().getCallerType()).isEqualTo("USER");
        assertThat(logCaptor.getValue().getTokensUsed()).isEqualTo(123);
    }

    @Test
    void generate_whenLlmReturnsMalformedJson_throwsBusinessExceptionAndLogsFailure() {
        AiConfig config = enabledConfig();
        when(aiConfigService.getDecryptedForInternalUse()).thenReturn(config);
        when(aiConfigService.getDecryptedApiKey(config)).thenReturn("sk-test");
        when(callLogRepository.countSuccessfulSince(any())).thenReturn(0L);
        when(appointmentService.findAll()).thenReturn(List.of());
        when(liteLlmClient.complete(anyString(), anyString(), anyString(), any(), anyInt(), anyString(), anyString()))
                .thenReturn(new LiteLlmCompletionResult("isto não é json", null));

        assertThatThrownBy(() -> service.generate(RecommendationType.RETENCAO, "USER", "5"))
                .isInstanceOf(BusinessException.class);

        verify(recommendationRepository, never()).save(any());
        ArgumentCaptor<AiCallLog> logCaptor = ArgumentCaptor.forClass(AiCallLog.class);
        verify(callLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getSuccess()).isFalse();
    }

    @Test
    void generate_whenLlmOmitsRequiredField_throwsBusinessException() {
        AiConfig config = enabledConfig();
        when(aiConfigService.getDecryptedForInternalUse()).thenReturn(config);
        when(aiConfigService.getDecryptedApiKey(config)).thenReturn("sk-test");
        when(callLogRepository.countSuccessfulSince(any())).thenReturn(0L);
        when(appointmentService.findAll()).thenReturn(List.of());

        String llmJson = """
                {"recommendations":[{"title":"","description":"desc","suggestedAction":"acao","priority":"ALTA"}]}
                """;
        when(liteLlmClient.complete(anyString(), anyString(), anyString(), any(), anyInt(), anyString(), anyString()))
                .thenReturn(new LiteLlmCompletionResult(llmJson, null));

        assertThatThrownBy(() -> service.generate(RecommendationType.RETENCAO, "USER", "5"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getLatestCached_whenNoCacheExists_throwsResourceNotFound() {
        when(recommendationRepository.findFirstByTypeOrderByGeneratedAtDesc(RecommendationType.FINANCEIRO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLatestCached(RecommendationType.FINANCEIRO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getLatestCached_whenCacheExists_returnsDeserializedItemsFromCache() {
        AiRecommendation cached = AiRecommendation.builder()
                .id(1L)
                .type(RecommendationType.FINANCEIRO)
                .payload("[{\"title\":\"X\",\"description\":\"Y\",\"suggestedAction\":\"Z\",\"priority\":\"BAIXA\"}]")
                .generatedAt(LocalDateTime.now())
                .build();
        when(recommendationRepository.findFirstByTypeOrderByGeneratedAtDesc(RecommendationType.FINANCEIRO))
                .thenReturn(Optional.of(cached));

        RecommendationResult result = service.getLatestCached(RecommendationType.FINANCEIRO);

        assertThat(result.fromCache()).isTrue();
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).title()).isEqualTo("X");
    }
}
