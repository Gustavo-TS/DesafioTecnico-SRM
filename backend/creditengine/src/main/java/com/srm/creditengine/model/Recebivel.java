package com.srm.creditengine.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "recebiveis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recebivel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cedente_id", nullable = false)
    private Cedente cedente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoRecebivel tipo;

    @Column(
        name = "valor_face",
        nullable = false,
        precision = 19,
        scale = 2
    )
    private BigDecimal valorFace;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusRecebivel status;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = StatusRecebivel.PENDENTE;
        }

        if (criadoEm == null) {
            criadoEm = OffsetDateTime.now();
        }
    }
}