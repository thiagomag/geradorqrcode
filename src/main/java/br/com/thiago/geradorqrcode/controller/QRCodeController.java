package br.com.thiago.geradorqrcode.controller;

import br.com.thiago.geradorqrcode.controller.dto.GenerateQRCodeRequest;
import br.com.thiago.geradorqrcode.controller.dto.GenerateQrCodeResponse;
import br.com.thiago.geradorqrcode.service.QRCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/qrcode")
@Tag(name = "QR Code", description = "QR Code generation and download")
@RequiredArgsConstructor
public class QRCodeController {

    private final QRCodeService qrCodeService;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping(value = "/generate", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Generate QR Code", description = "Generate a QR Code image based on the text provided")
    @ApiResponse(responseCode = "200", description = "QR Code image generated successfully")
    public Mono<byte[]> generateQRCode(@RequestBody GenerateQRCodeRequest request) {
        return qrCodeService.generateQRCode(request);
    }

    @PostMapping(value = "/generate-link", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Generate QR Code Link", description = "Generate a QR Code image and upload it to Google Drive")
    @ApiResponse(responseCode = "200", description = "QR Code image uploaded successfully")
    public Mono<GenerateQrCodeResponse> generateQRCodeLink(@RequestBody GenerateQRCodeRequest request) {
        return qrCodeService.generateQRCodeLink(request);
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

    @DeleteMapping
    @Operation(summary = "Delete QR Code", description = "Delete a QR Code image from Google Drive")
    @ApiResponse(responseCode = "200", description = "QR Code image deleted successfully")
    public Mono<Void> deleteQRCode(@RequestParam String fileId) {
        return qrCodeService.deleteQRCode(fileId);
    }
}