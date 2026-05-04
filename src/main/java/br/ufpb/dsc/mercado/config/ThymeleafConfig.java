package br.ufpb.dsc.mercado.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.dialect.IDialect;

/**
 * Configuração do Thymeleaf Layout.
 * 
 * NOTA: O Thymeleaf Layout Dialect do projeto nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect
 * pode não estar disponível em alguns ambientes Docker.
 * 
 * Alternativa: usar th:insert e th:replace para incluir templates sem o layout:decorate.
 */
@Configuration
public class ThymeleafConfig {

    // A configuração pode ser deixada vazia se a dependência não for encontrada.
    // O Spring irá usar o Thymeleaf padrão sem o Layout Dialect.

}

