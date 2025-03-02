package br.com.thiago.geradorqrcode.service;

import br.com.thiago.geradorqrcode.controller.dto.GenerateQRCodeRequest;
import br.com.thiago.geradorqrcode.controller.dto.GenerateQrCodeResponse;
import br.com.thiago.geradorqrcode.webclient.GoogleDriveApiWebClient;
import br.com.thiago.geradorqrcode.webclient.dto.UploadFileRequest;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class QRCodeService {

    private final GoogleDriveApiWebClient googleDriveApiWebClient;

    public Mono<byte[]> generateQRCode(GenerateQRCodeRequest request) {
        return Mono.fromCallable(() -> {
                    try {
                        final var text = request.getText();
//                        final var backgroundColor = Integer.valueOf(Optional.ofNullable(request.getBackgroundColor())
//                                .orElse("0xFFFFFFFF"));
//                        final var foregroundColor = Integer.valueOf(Optional.ofNullable(request.getForegroundColor())
//                                .orElse("0xFF000000"));

                        final var qrCodeWriter = new QRCodeWriter();
                        final var bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 300, 300);

                        final var config = new MatrixToImageConfig(Color.BLACK.hashCode(), Color.WHITE.hashCode());

                        final var outputStream = new ByteArrayOutputStream();
                        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream, config);
                        return outputStream.toByteArray();
                    } catch (IOException | WriterException e) {
                        throw new RuntimeException(e);
                    }
                })
                .onErrorResume(error -> Mono.error(new RuntimeException("Error generating QR Code", error)));
    }

    public Mono<GenerateQrCodeResponse> generateQRCodeLink(GenerateQRCodeRequest request) {
        final var googleApiUploadRequest = buildGoogleApiUploadRequest();
        final var text = request.getText();
        return generateQRCodeToFile(text, 300, 300)
                .flatMap(file -> googleDriveApiWebClient.uploadFile(file, googleApiUploadRequest)
                        .publishOn(Schedulers.boundedElastic())
                        .map(googleDriveApiResponse -> {
                            try {
                                return new GenerateQrCodeResponse(Files.readAllBytes(file.toPath()), googleDriveApiResponse.getUrl());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }))
                .onErrorResume(error -> Mono.error(new RuntimeException("Error generating QR Code link", error)));
    }

    private UploadFileRequest buildGoogleApiUploadRequest() {
        return UploadFileRequest.builder()
                .projectId("feisty-bindery-441214-i3")
                .folderId("1V3XfeD5c6ztSIsTJclRK77fbTjZNU0ko")
                .build();
    }

    public Mono<File> generateQRCodeToFile(String text, int width, int height) {
        return Mono.fromCallable(() -> {
                    try {
                        QRCodeWriter qrCodeWriter = new QRCodeWriter();
                        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
                        File tempFile = File.createTempFile("qr-code", ".png");
                        Path path = tempFile.toPath();
                        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
                        return tempFile;
                    } catch (WriterException | IOException e) {
                        throw new RuntimeException("Failed to generate QR Code to file", e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(error -> Mono.error(new RuntimeException("Error generating QR Code to file", error)));
    }

}
