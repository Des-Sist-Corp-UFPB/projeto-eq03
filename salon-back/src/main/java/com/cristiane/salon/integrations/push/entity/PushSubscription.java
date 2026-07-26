package com.cristiane.salon.integrations.push.entity;

import com.cristiane.salon.models.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Uma subscription de Web Push por navegador/dispositivo autorizado — {@code endpoint} é a URL
 * do serviço de push do navegador (FCM, Mozilla autopush, etc.), {@code p256dh}/{@code auth} são
 * as chaves públicas que o backend usa para cifrar o payload conforme o protocolo Web Push
 * padrão (RFC 8291). Nenhuma delas, sozinha, entrega a chave privada VAPID nem permite forjar
 * notificação para outro destinatário.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_push_subscription")
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String endpoint;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String p256dh;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String auth;

    @Column(name = "user_agent")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
