package br.com.thiago.geradorqrcode.controller;

import br.com.thiago.geradorqrcode.controller.dto.GenerateQRCodeRequest;
import br.com.thiago.geradorqrcode.service.QRCodeService;
import br.com.thiago.geradorqrcode.webclient.dto.GoogleDriveApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/qrcode")
@Tag(name = "QR Code", description = "QR Code generation and download")
@RequiredArgsConstructor
public class QRCodeController {

    private final QRCodeService qrCodeService;

    @PostMapping(value = "/generate", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Generate QR Code", description = "Generate a QR Code image based on the text provided")
    @ApiResponse(responseCode = "200", description = "QR Code image generated successfully")
    public Mono<byte[]> generateQRCode(@RequestBody GenerateQRCodeRequest request) {
        return qrCodeService.generateQRCode(request);
    }

    @GetMapping(value = "/generate-link", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Generate QR Code Link", description = "Generate a QR Code image and upload it to Google Drive")
    @ApiResponse(responseCode = "200", description = "QR Code image uploaded successfully")
    public Mono<GoogleDriveApiResponse> generateQRCodeLink(@RequestParam String text) {
        return qrCodeService.generateQRCodeLink(text);
    }

    @GetMapping("/download")
    @Operation(summary = "Download QR Code", description = "Download a QR Code image based on the text provided")
    @ApiResponse(responseCode = "200", description = "QR Code image downloaded successfully")
    public Mono<ResponseEntity<FileSystemResource>> downloadQRCode(@RequestParam String text) {
        return qrCodeService.generateQRCodeToFile(text, 300, 300)
                .map(file -> {
                    FileSystemResource resource = new FileSystemResource(file);

                    HttpHeaders headers = new HttpHeaders();
                    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"qr-code.png\"");
                    headers.setContentType(MediaType.IMAGE_PNG);

                    return ResponseEntity.ok()
                            .headers(headers)
                            .contentLength(file.length())
                            .body(resource);
                });
    }
}