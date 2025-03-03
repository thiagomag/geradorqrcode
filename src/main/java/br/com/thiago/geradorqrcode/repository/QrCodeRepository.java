package br.com.thiago.geradorqrcode.repository;

import br.com.thiago.geradorqrcode.model.QrCode;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface QrCodeRepository extends ReactiveCrudRepository<QrCode, Long> {

    Mono<QrCode> findByFileId(String fileId);

    @Query("select q.* from qr_code q " +
            "where q.expiration_date <= now() " +
            "and q.is_active = true;")
    Flux<QrCode> findToExpirate();

}
