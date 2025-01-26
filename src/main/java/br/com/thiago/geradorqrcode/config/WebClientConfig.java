package br.com.thiago.geradorqrcode.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    public static final int MAX_IN_MEMORY_SIZE = 10 * 1024 * 1024; // 10 MB

    @Bean
    public WebClient.Builder webClientConfiguration(ObjectMapper objectMapper) {
        return WebClient.builder()
                .exchangeStrategies(buildExchangeStrategies(objectMapper))
                .codecs(configurer -> {
                    // Configura o tamanho máximo da memória para arquivos
                    configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE);

                    // Habilita os codecs para multipart/form-data
                    configurer.defaultCodecs().multipartCodecs();
                });
    }

    private ExchangeStrategies buildExchangeStrategies(ObjectMapper objectMapper) {
        final var clientMapper = objectMapper.copy();
        return ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE);
                    configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(clientMapper));
                    configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(clientMapper));
                })
                .build();
    }
}