package br.com.thiago.geradorqrcode.service;

import br.com.thiago.geradorqrcode.controller.dto.GenerateQRCodeRequest;
import br.com.thiago.geradorqrcode.controller.dto.GenerateQrCodeResponse;
import br.com.thiago.geradorqrcode.model.QrCode;
import br.com.thiago.geradorqrcode.repository.QrCodeRepository;
import br.com.thiago.geradorqrcode.webclient.GoogleDriveApiWebClient;
import br.com.thiago.geradorqrcode.webclient.dto.GoogleDriveApiResponse;
import br.com.thiago.geradorqrcode.webclient.dto.UploadFileRequest;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class QRCodeService {

    private final GoogleDriveApiWebClient googleDriveApiWebClient;
    private final QrCodeRepository qrCodeRepository;

    @Value("${client.google-drive-api-service.project-id}")
    private String googleDriveProjectId;

    public Mono<byte[]> generateQRCode(GenerateQRCodeRequest request) {
        return Mono.fromCallable(() -> {
                    try {
                        final var text = request.getText();
                        final var backgroundColor = parseHexColor(Optional.ofNullable(request.getBackgroundColor())
                                .orElse("0xFFFFFFFF"));
                        final var foregroundColor = parseHexColor(Optional.ofNullable(request.getForegroundColor())
                                .orElse("0xFF000000"));

                        final var bitMatrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 300, 300);

                        final var config = new MatrixToImageConfig(foregroundColor, backgroundColor);

                        if (request.getLogoPath() != null) {
                            final var qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix, config);
                            final var qrWithLogo = addLogoToQRCode(qrImage, request.getLogoPath());
                            final var outputStream = new ByteArrayOutputStream();
                            ImageIO.write(qrWithLogo, "PNG", outputStream);
                            return outputStream.toByteArray();
                        }

                        final var outputStream = new ByteArrayOutputStream();
                        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream, config);
                        return outputStream.toByteArray();
                    } catch (IOException | WriterException e) {
                        throw new RuntimeException(e);
                    }
                })
                .onErrorResume(error -> Mono.error(new RuntimeException("Error generating QR Code", error)));
    }

    private int parseHexColor(String hexColor) {
        // Remove o prefixo "0x" se presente
        if (hexColor.startsWith("0x")) {
            hexColor = hexColor.substring(2);
        }

        // Converte o valor hexadecimal para int
        return (int) Long.parseLong(hexColor, 16);
    }

    private BufferedImage addLogoToQRCode(BufferedImage qrImage, String logoPath) throws IOException {
        BufferedImage logo;

        // Verifica se o caminho do logo é uma URL ou um caminho local
        if (logoPath.startsWith("http://") || logoPath.startsWith("https://")) {
            // Carrega o logo da URL
            logo = ImageIO.read(new URL(logoPath));
        } else {
            // Carrega o logo local
            logo = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(logoPath)));
        }

        // Calcula a posição e o tamanho do logo no QR Code
        final var logoWidth = qrImage.getWidth() / 5; // O logo será 1/5 da largura do QR
        final var logoHeight = qrImage.getHeight() / 5;
        final var logoX = (qrImage.getWidth() - logoWidth) / 2;
        final var logoY = (qrImage.getHeight() - logoHeight) / 2;

        // Redimensiona o logo para caber no QR Code
        final var resizedLogo = logo.getScaledInstance(logoWidth, logoHeight, Image.SCALE_SMOOTH);

        // Sobrepõe o logo ao QR Code
        final var g2d = qrImage.createGraphics();
        g2d.drawImage(resizedLogo, logoX, logoY, null);
        g2d.dispose();

        return qrImage;
    }


    public Mono<GenerateQrCodeResponse> generateQRCodeLink(GenerateQRCodeRequest request) {
        final var googleApiUploadRequest = buildGoogleApiUploadRequest();
        final var text = request.getText();
        return generateQRCodeToFile(text, 300, 300)
                .flatMap(file -> googleDriveApiWebClient.uploadFile(file, googleApiUploadRequest)
                        .flatMap(googleDriveApiResponse -> qrCodeRepository.save(buildQrCode(googleDriveApiResponse))
                                .publishOn(Schedulers.boundedElastic())
                                .map(qrCode -> {
                                    try {
                                        return new GenerateQrCodeResponse(Files.readAllBytes(file.toPath()), googleDriveApiResponse.getUrl());
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                })))
                .onErrorResume(error -> Mono.error(new RuntimeException("Error generating QR Code link", error)));
    }

    private QrCode buildQrCode(GoogleDriveApiResponse googleDriveApiResponse) {
        return QrCode.builder()
                .url(googleDriveApiResponse.getUrl())
                .expirationDate(LocalDateTime.now().plusMonths(1))
                .isActive(Boolean.TRUE)
                .fileId(googleDriveApiResponse.getFileId())
                .build();
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

    public Mono<Void> deleteQRCode(String fileId) {
        log.info("Deleting QR Code with fileId: {}", fileId);
        return qrCodeRepository.findByFileId(fileId)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException("QR Code not found"))))
                .flatMap(qrcode -> googleDriveApiWebClient.deleteFile(googleDriveProjectId, fileId)
                        .then(Mono.just(qrcode.delete()))
                        .flatMap(qrCodeRepository::save))
                .onErrorResume(error -> Mono.error(new RuntimeException("Error deleting QR Code", error)))
                .then();
    }

    @Scheduled(cron = "0 */5 * * * *")
    public Flux<Void> deleteQRCodeJob() {
        log.info("Deleting QR Codes expired");
        return qrCodeRepository.findToExpirate()
                .flatMap(qrcode -> googleDriveApiWebClient.deleteFile(googleDriveProjectId, qrcode.getFileId())
                        .then(Mono.just(qrcode.delete()))
                        .flatMap(qrCodeRepository::save)
                        .then())
                .onErrorResume(error -> {
                    log.error("Error deleting QR Code", error);
                    return Mono.error(new RuntimeException("Error deleting QR Code", error));
                });
    }
}
