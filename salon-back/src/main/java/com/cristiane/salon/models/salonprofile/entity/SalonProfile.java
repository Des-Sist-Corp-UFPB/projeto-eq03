package com.cristiane.salon.models.salonprofile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Perfil público do salão (issue #117) — tabela singleton, só existe UM registro (a aplicação
 * sempre usa o de menor id). Nada aqui é dado sensível: tudo é exibido publicamente na página
 * inicial via {@code GET /v1/salon/profile}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tb_salon_profile")
public class SalonProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 300)
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(length = 150)
    private String instagram;

    @Column(length = 20)
    private String whatsapp;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
