package br.com.thiago.geradorqrcode.webclient.urlshortener.impl;

import br.com.thiago.geradorqrcode.webclient.urlshortener.UrlShortenerWebClient;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class UrlShortenerWebClientImpl implements UrlShortenerWebClient {

    private final WebClient webClient;

    public UrlShortenerWebClientImpl(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("http://localhost:8082").build();
    }

    @Override
    public Mono<String> shortenUrl(String url) {
        return webClient.post()
                .uri("/shorten")
                .bodyValue(url)
                .retrieve()
                .bodyToMono(String.class);
    }
}