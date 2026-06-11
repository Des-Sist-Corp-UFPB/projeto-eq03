package com.cristiane.salon.config;

import com.cristiane.salon.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaRedirectController {

    // Essa expressão regular captura rotas que NÃO possuem ponto (ou seja, não são arquivos como .js, .css, .png)
    // e que NÃO começam com /v1 (as rotas da sua API)
    @RequestMapping(value = "{path:[^\\.]*}")
    public String redirectSpa(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri != null && (uri.startsWith("/v1/") || uri.equals("/v1"))) {
            throw new ResourceNotFoundException("Recurso não encontrado");
        }
        // O "forward:" faz um redirecionamento interno no servidor. 
        // A URL no navegador do usuário continua sendo /dashboard, mas o Spring entrega o index.html do React.
        return "forward:/index.html";
    }
}