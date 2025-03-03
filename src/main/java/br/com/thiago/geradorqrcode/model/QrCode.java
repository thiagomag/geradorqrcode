package br.com.thiago.geradorqrcode.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@ToString
@Builder(toBuilder = true)
@Table("qr_code")
public class QrCode {

    @Id
    private Long id;
    private String url;
    private String fileId;
    private Boolean isActive;
    private LocalDateTime expirationDate;
    @CreatedDate
    private LocalDateTime createdAt;
    private LocalDateTime deletedTmsp;

    public QrCode delete() {
        this.deletedTmsp = LocalDateTime.now();
        this.isActive = Boolean.FALSE;
        return this;
    }
}
