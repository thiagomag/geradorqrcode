package br.com.thiago.geradorqrcode.webclient.googledriveapi.impl;

import br.com.thiago.geradorqrcode.webclient.googledriveapi.GoogleDriveApiWebClient;
import br.com.thiago.geradorqrcode.webclient.googledriveapi.dto.GoogleDriveApiResponse;
import br.com.thiago.geradorqrcode.webclient.googledriveapi.dto.UploadFileRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.File;

@Component
public class GoogleDriveApiWebClientImpl implements GoogleDriveApiWebClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GoogleDriveApiWebClientImpl(WebClient.Builder builder, ObjectMapper objectMapper) {
        this.webClient = builder.baseUrl("http://localhost:8081").build();
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<GoogleDriveApiResponse> uploadFile(File file, UploadFileRequest request) {
        return webClient.post()
                .uri("/v1/google-drive/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(createMultipartBody(file, request)))
                .retrieve()
                .bodyToMono(GoogleDriveApiResponse.class);
    }

    @Override
    public Mono<Void> deleteFile(String projectId, String fileId) {
        return webClient.delete()
                .uri(String.format("/v1/google-drive/resources/%s/delete/%s", projectId, fileId))
                .retrieve()
                .bodyToMono(Void.class);
    }

    private MultiValueMap<String, HttpEntity<?>> createMultipartBody(File file, UploadFileRequest request) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        // Adiciona o arquivo com o tipo de conteúdo correto
        builder.part("file", new FileSystemResource(file))
                .contentType(MediaType.APPLICATION_OCTET_STREAM);

        // Converte o objeto UploadFileRequest para JSON e define o tipo de conteúdo como application/json
        try {
            String uploadFileRequestJson = objectMapper.writeValueAsString(request);
            builder.part("uploadFileRequest", uploadFileRequestJson)
                    .contentType(MediaType.APPLICATION_JSON);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao converter UploadFileRequest para JSON", e);
        }

        return builder.build();
    }
}