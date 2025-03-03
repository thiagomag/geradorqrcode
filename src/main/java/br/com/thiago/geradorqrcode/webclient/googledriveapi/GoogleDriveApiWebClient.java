package br.com.thiago.geradorqrcode.webclient.googledriveapi;

import br.com.thiago.geradorqrcode.webclient.googledriveapi.dto.GoogleDriveApiResponse;
import br.com.thiago.geradorqrcode.webclient.googledriveapi.dto.UploadFileRequest;
import reactor.core.publisher.Mono;

import java.io.File;

public interface GoogleDriveApiWebClient {

    Mono<GoogleDriveApiResponse> uploadFile(File file, UploadFileRequest uploadFileRequest);

    Mono<Void> deleteFile(String projectId, String fileId);
}
