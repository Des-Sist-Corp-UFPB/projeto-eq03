package com.cristiane.salon.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class HttpClientConfigTest {

    @Test
    void externalRestClientBuilder_buildsAUsableRestClientBuilder() {
        HttpClientConfig config = new HttpClientConfig();
        ReflectionTestUtils.setField(config, "connectTimeoutMs", 1234);
        ReflectionTestUtils.setField(config, "readTimeoutMs", 5678);

        RestClient.Builder builder = config.externalRestClientBuilder();

        assertThat(builder).isNotNull();
        // clone() é o padrão recomendado pelo Spring pra derivar um builder por chamada, já
        // que RestClient.Builder é mutável e não é thread-safe.
        assertThat(builder.clone().baseUrl("http://localhost").build()).isNotNull();
    }

    @Test
    void aiRestClientBuilder_buildsAUsableRestClientBuilder_independentOfSharedTimeout() {
        HttpClientConfig config = new HttpClientConfig();
        ReflectionTestUtils.setField(config, "connectTimeoutMs", 1234);
        ReflectionTestUtils.setField(config, "readTimeoutMs", 5678);
        ReflectionTestUtils.setField(config, "aiReadTimeoutMs", 30000);

        RestClient.Builder builder = config.aiRestClientBuilder();

        assertThat(builder).isNotNull();
        assertThat(builder.clone().baseUrl("http://localhost").build()).isNotNull();
    }
}
