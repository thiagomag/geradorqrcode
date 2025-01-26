package br.com.thiago.geradorqrcode.controller;

import br.com.thiago.geradorqrcode.service.QRCodeService;
import br.com.thiago.geradorqrcode.webclient.dto.GoogleDriveApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/qrcode")
@RequiredArgsConstructor
public class QRCodeController {

    private final QRCodeService qrCodeService;

    @GetMapping(value = "/generate", produces = MediaType.IMAGE_PNG_VALUE)
    public Mono<byte[]> generateQRCode(@RequestParam String text) {
        return qrCodeService.generateQRCode(text);
    }

    @GetMapping(value = "/generate-link", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<GoogleDriveApiResponse> generateQRCodeLink(@RequestParam String text) {
        return qrCodeService.generateQRCodeLink(text);
    }

    @GetMapping("/download")
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
