package com.cristiane.salon.integrations.push.config;

import jakarta.annotation.PostConstruct;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.GeneralSecurityException;
import java.security.Security;

/**
 * Registra o {@code nl.martijndwars.webpush.PushService} da biblioteca como bean único,
 * reaproveitado por toda notificação — em vez de instanciar um novo a cada envio (como o
 * exemplo original da issue #110 sugeria, o que também tornaria o código difícil de testar).
 */
@Configuration
public class WebPushConfig {

    @Value("${vapid.public-key}")
    private String vapidPublicKey;

    @Value("${vapid.private-key}")
    private String vapidPrivateKey;

    @Value("${vapid.subject}")
    private String vapidSubject;

    @PostConstruct
    public void registerBouncyCastleProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Bean
    public nl.martijndwars.webpush.PushService webPushService() throws GeneralSecurityException {
        return new nl.martijndwars.webpush.PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
    }
}
