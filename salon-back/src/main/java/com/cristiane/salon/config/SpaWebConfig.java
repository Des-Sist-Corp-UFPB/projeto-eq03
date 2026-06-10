package com.cristiane.salon.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/", "classpath:/public/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        
                        // Se for uma rota de API, não deve retornar o index.html (retorna null para resultar em 404)
                        if (resourcePath != null && (resourcePath.startsWith("v1/") || resourcePath.startsWith("/v1/") || resourcePath.equals("v1") || resourcePath.equals("/v1"))) {
                            return null;
                        }
                        
                        // Retorna o index.html se ele existir, caso contrário retorna null (evita 500)
                        Resource indexHtml = new ClassPathResource("/static/index.html");
                        if (indexHtml.exists() && indexHtml.isReadable()) {
                            return indexHtml;
                        }
                        
                        return null;
                    }
                });
    }
}
