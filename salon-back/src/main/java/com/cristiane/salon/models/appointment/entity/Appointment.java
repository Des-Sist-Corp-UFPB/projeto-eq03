package com.cristiane.salon.models.appointment.entity;

import com.cristiane.salon.models.appointment.enums.AppointmentStatus;
import com.cristiane.salon.models.appointment.enums.PaymentStatus;
import com.cristiane.salon.models.employee.entity.Employee;
import com.cristiane.salon.models.service.entity.SalonService;
import com.cristiane.salon.models.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_appointment")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_service_id", nullable = false)
    private SalonService salonService;

    /** Definido pela equipe ao confirmar o pedido do cliente */
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "preferred_date")
    private LocalDate preferredDate;

    @Column(name = "client_notes", columnDefinition = "TEXT")
    private String clientNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status = AppointmentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    /** ID gerado pelo Mercado Pago para rastrear o pagamento via Webhook */
    @Column(name = "payment_id")
    private Long paymentId;

    /** A string "Copia e Cola" do PIX gerada pela API */
    @Column(name = "pix_qr_code", columnDefinition = "TEXT")
    private String pixQrCode;

    /**
     * O serviço cadastrado funciona como um template: estes três campos sobrescrevem
     * preço/duração/observações só para este agendamento, sem alterar o cadastro do serviço.
     * Nulo = usa o valor do serviço (ver {@link #getEffectivePrice()}/{@link #getEffectiveDurationMin()}).
     */
    @Column(name = "custom_price", precision = 10, scale = 2)
    private BigDecimal customPrice;

    @Column(name = "custom_duration_min")
    private Integer customDurationMin;

    @Column(name = "custom_service_notes", columnDefinition = "TEXT")
    private String customServiceNotes;

    public BigDecimal getEffectivePrice() {
        return customPrice != null ? customPrice : salonService.getPrice();
    }

    public Integer getEffectiveDurationMin() {
        return customDurationMin != null ? customDurationMin : salonService.getDurationMin();
    }
}
