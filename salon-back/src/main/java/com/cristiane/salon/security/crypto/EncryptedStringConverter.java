package com.cristiane.salon.security.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * Cifra/decifra transparentemente um campo String na fronteira do JPA.
 *
 * <p>Aplicado com {@code @Convert(converter = EncryptedStringConverter.class)} no campo da
 * entidade. A vantagem sobre cifrar na mão no service é não ter como esquecer: qualquer
 * caminho que persista a entidade passa por aqui.
 *
 * <p>Consequência importante: a coluna vira opaca para o banco — não dá para indexar, buscar
 * com LIKE nem ordenar por ela. Para buscar por igualdade (ex.: CPF duplicado), use uma coluna
 * irmã com o hash determinístico de {@link PiiHashUtil}.
 */
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final PiiEncryptionUtil piiEncryptionUtil;

    public EncryptedStringConverter(PiiEncryptionUtil piiEncryptionUtil) {
        this.piiEncryptionUtil = piiEncryptionUtil;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return piiEncryptionUtil.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return piiEncryptionUtil.decrypt(dbData);
    }
}
