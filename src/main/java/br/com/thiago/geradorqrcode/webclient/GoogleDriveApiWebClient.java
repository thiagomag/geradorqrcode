package br.com.thiago.geradorqrcode.webclient;

import br.com.thiago.geradorqrcode.webclient.dto.GoogleDriveApiResponse;
import br.com.thiago.geradorqrcode.webclient.dto.UploadFileRequest;
import reactor.core.publisher.Mono;

import java.io.File;

public interface GoogleDriveApiWebClient {

    Mono<GoogleDriveApiResponse> uploadFile(File file, UploadFileRequest uploadFileRequest);

    Mono<Void> deleteFile(String projectId, String fileId);
}
