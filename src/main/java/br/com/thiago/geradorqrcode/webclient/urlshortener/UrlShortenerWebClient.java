package br.com.thiago.geradorqrcode.webclient.urlshortener;

import reactor.core.publisher.Mono;

public interface UrlShortenerWebClient {

    Mono<String> shortenUrl(String url);
}
