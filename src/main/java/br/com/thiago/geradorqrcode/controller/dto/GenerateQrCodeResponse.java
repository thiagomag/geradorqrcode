package br.com.thiago.geradorqrcode.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Base64;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "GenerateQrCodeResponse", description = "Response to generate a QR Code")
public class GenerateQrCodeResponse {

    private String qrCode;
    private String url;

    public GenerateQrCodeResponse(byte[] qrCode, String url) {
        this.qrCode = Base64.getEncoder().encodeToString(qrCode);
        this.url = url;
    }

    public String getQrCode() {
        return "data:image/png;base64," + qrCode;
    }
}
