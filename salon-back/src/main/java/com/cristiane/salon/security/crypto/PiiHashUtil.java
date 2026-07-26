package com.cristiane.salon.security.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Gera um hash determinístico de um dado sensível, para permitir busca por igualdade
 * (ex.: "já existe alguém com este CPF?") sem precisar decifrar nada.
 *
 * <p>Usa HMAC-SHA256 com um pepper secreto em vez de SHA-256 puro de propósito: o espaço de
 * CPFs válidos é pequeno o suficiente (~10^11) para ser varrido por força bruta, então um
 * hash sem segredo seria reversível na prática por quem obtivesse o banco. Com o pepper (que
 * vive só na env, fora do banco), o atacante precisaria também vazar a chave da aplicação.
 *
 * <p>Determinístico por construção — mesma entrada gera sempre a mesma saída, que é o que
 * permite o índice UNIQUE. Isso é um trade-off aceito: vale para checagem de duplicidade,
 * não para proteger contra análise de frequência.
 */
@Component
public class PiiHashUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final SecretKeySpec pepper;

    public PiiHashUtil(@Value("${app.pii-hash-pepper}") String base64Pepper) {
        byte[] pepperBytes;
        try {
            pepperBytes = Base64.getDecoder().decode(base64Pepper);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.pii-hash-pepper deve estar em Base64", e);
        }
        if (pepperBytes.length < 32) {
            throw new IllegalStateException(
                    "app.pii-hash-pepper deve ter ao menos 32 bytes após decodificar o Base64");
        }
        this.pepper = new SecretKeySpec(pepperBytes, HMAC_ALGORITHM);
    }

    /**
     * @param plainText valor já normalizado pelo chamador (ex.: CPF só com dígitos)
     * @return hash em hexadecimal, ou {@code null} se a entrada for nula/vazia
     */
    public String hash(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(pepper);
            byte[] digest = mac.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar hash de dado sensível", e);
        }
    }
}
