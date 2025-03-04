package br.com.thiago.geradorqrcode.service;

import br.com.thiago.geradorqrcode.controller.dto.GenerateQRCodeRequest;
import br.com.thiago.geradorqrcode.controller.dto.GenerateQrCodeResponse;
import br.com.thiago.geradorqrcode.model.QrCode;
import br.com.thiago.geradorqrcode.repository.QrCodeRepository;
import br.com.thiago.geradorqrcode.webclient.googledriveapi.GoogleDriveApiWebClient;
import br.com.thiago.geradorqrcode.webclient.googledriveapi.dto.GoogleDriveApiResponse;
import br.com.thiago.geradorqrcode.webclient.googledriveapi.dto.UploadFileRequest;
import br.com.thiago.geradorqrcode.webclient.urlshortener.UrlShortenerWebClient;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
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
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class QRCodeService {

    private final GoogleDriveApiWebClient googleDriveApiWebClient;
    private final UrlShortenerWebClient urlShortenerWebClient;
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

                        final var bitMatrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 300, 300, Map.of(EncodeHintType.MARGIN, 1));

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
            logo = ImageIO.read(new URL(logoPath));
        } else {
            logo = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(logoPath)));
        }

        // Definição do tamanho do círculo e do logo
        final int qrSize = qrImage.getWidth();
        final int circleSize = qrSize / 5; // O círculo terá 1/5 do tamanho do QR Code
        final int logoSize = qrSize / 6;   // O logo terá 1/6 do tamanho do QR Code
        final int centerX = (qrSize - circleSize) / 2;
        final int centerY = (qrSize - circleSize) / 2;

        // Criar um logo redimensionado e circular
        BufferedImage resizedLogo = new BufferedImage(logoSize, logoSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2dLogo = resizedLogo.createGraphics();
        g2dLogo.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2dLogo.setClip(new Ellipse2D.Float(0, 0, logoSize, logoSize)); // Recorte circular
        g2dLogo.drawImage(logo.getScaledInstance(logoSize, logoSize, Image.SCALE_SMOOTH), 0, 0, null);
        g2dLogo.dispose();

        // Criar o círculo de fundo com borda
        BufferedImage circleBackground = new BufferedImage(circleSize, circleSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2dCircle = circleBackground.createGraphics();
        g2dCircle.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Desenha o círculo branco com borda preta
        g2dCircle.setColor(Color.WHITE);
        g2dCircle.fillOval(0, 0, circleSize, circleSize);
        g2dCircle.drawOval(0, 0, circleSize, circleSize);

        // Desenha o logo dentro do círculo
        int logoX = (circleSize - logoSize) / 2;
        int logoY = (circleSize - logoSize) / 2;
        g2dCircle.drawImage(resizedLogo, logoX, logoY, null);
        g2dCircle.dispose();

        // Sobrepor o círculo ao QR Code
        Graphics2D g2d = qrImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(circleBackground, centerX, centerY, null);
        g2d.dispose();

        return qrImage;
    }

    public Mono<GenerateQrCodeResponse> generateQRCodeLink(GenerateQRCodeRequest request) {
        final var googleApiUploadRequest = buildGoogleApiUploadRequest();
        final var text = request.getText();
        return generateQRCodeToFile(text, 300, 300)
                .flatMap(file -> googleDriveApiWebClient.uploadFile(file, googleApiUploadRequest)
                        .flatMap(googleDriveApiResponse -> urlShortenerWebClient.shortenUrl(googleDriveApiResponse.getUrl())
                                .map(shortUrl -> (buildQrCode(googleDriveApiResponse, shortUrl)))
                                .flatMap(qrCodeRepository::save)
                                .publishOn(Schedulers.boundedElastic())
                                .map(qrCode -> {
                                    try {
                                        return new GenerateQrCodeResponse(Files.readAllBytes(file.toPath()), qrCode.getUrl());
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                })))
                .onErrorResume(error -> Mono.error(new RuntimeException("Error generating QR Code link", error)));
    }

    private QrCode buildQrCode(GoogleDriveApiResponse googleDriveApiResponse, String shortUrl) {
        return QrCode.builder()
                .url(shortUrl)
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

    @Scheduled(cron = "0 0 0 * * ?")
    public Flux<Void> deleteQRCodeJob() {
        log.info("Deleting QR Codes expired");
        return qrCodeRepository.findToExpire()
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