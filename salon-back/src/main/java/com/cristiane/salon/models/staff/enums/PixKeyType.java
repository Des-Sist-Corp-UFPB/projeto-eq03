package com.cristiane.salon.models.staff.enums;

import java.util.regex.Pattern;

/**
 * Tipos de chave PIX aceitos pelo Banco Central, cada um com o formato que a chave precisa ter.
 *
 * <p>A validação do formato mora aqui (e não espalhada em anotações) para que backend e
 * frontend possam divergir só na mensagem, nunca na regra.
 */
public enum PixKeyType {

    /** 11 dígitos. A validade dos dígitos verificadores é checada à parte por CpfValidator. */
    CPF(Pattern.compile("^\\d{11}$"), "O CPF deve ter 11 dígitos"),

    CNPJ(Pattern.compile("^\\d{14}$"), "O CNPJ deve ter 14 dígitos"),

    EMAIL(Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"),
            "O e-mail informado como chave PIX é inválido"),

    /** Formato E.164 exigido pelo PIX: +55 seguido de DDD e número. */
    TELEFONE(Pattern.compile("^\\+55\\d{10,11}$"),
            "O telefone deve estar no formato +55DDDNÚMERO (ex.: +5581999998888)"),

    /** Chave aleatória (EVP): UUID v4. */
    ALEATORIA(Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"),
            "A chave aleatória deve ser um UUID válido");

    private final Pattern pattern;
    private final String message;

    PixKeyType(Pattern pattern, String message) {
        this.pattern = pattern;
        this.message = message;
    }

    public boolean matches(String key) {
        return key != null && pattern.matcher(key).matches();
    }

    public String getMessage() {
        return message;
    }
}
