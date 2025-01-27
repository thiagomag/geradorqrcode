package br.com.thiago.geradorqrcode.service;

import br.com.thiago.geradorqrcode.controller.dto.GenerateQRCodeRequest;
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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QRCodeService {

    private final GoogleDriveApiWebClient googleDriveApiWebClient;

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
                .subscribeOn(Schedulers.boundedElastic())
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

    public Mono<GoogleDriveApiResponse> generateQRCodeLink(String text) {
        final var googleApiUploadRequest = buildGoogleApiUploadRequest();
        return generateQRCodeToFile(text, 300, 300)
                .flatMap(file -> googleDriveApiWebClient.uploadFile(file, googleApiUploadRequest))
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
