package com.srm.creditengine.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "liquidacoes",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_liquidacao_recebivel",
            columnNames = "recebivel_id"
        ),
        @UniqueConstraint(
            name = "uk_liquidacao_idempotency_key",
            columnNames = "idempotency_key"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Liquidacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recebivel_id", nullable = false)
    private Recebivel recebivel;

    @Column(
        name = "idempotency_key",
        nullable = false,
        length = 100
    )
    private String idempotencyKey;

    @Column(
        name = "valor_face",
        nullable = false,
        precision = 19,
        scale = 2
    )
    private BigDecimal valorFace;

    @Column(
        name = "taxa_base",
        nullable = false,
        precision = 19,
        scale = 8
    )
    private BigDecimal taxaBase;

    @Column(
        nullable = false,
        precision = 19,
        scale = 8
    )
    private BigDecimal spread;

    @Column(name = "prazo_meses", nullable = false)
    private Integer prazoMeses;

    @Column(
        name = "valor_presente",
        nullable = false,
        precision = 19,
        scale = 2
    )
    private BigDecimal valorPresente;

    @Column(
        name = "valor_desagio",
        nullable = false,
        precision = 19,
        scale = 2
    )
    private BigDecimal valorDesagio;

    @Enumerated(EnumType.STRING)
    @Column(name = "moeda_pagamento", nullable = false, length = 3)
    private Moeda moedaPagamento;

    @Column(
        name = "taxa_cambio_utilizada",
        precision = 19,
        scale = 8
    )
    private BigDecimal taxaCambioUtilizada;

    @Column(name = "taxa_cambio_vigente_em")
    private OffsetDateTime taxaCambioVigenteEm;

    @Column(
        name = "valor_final",
        nullable = false,
        precision = 19,
        scale = 2
    )
    private BigDecimal valorFinal;

    @Column(name = "liquidado_em", nullable = false, updatable = false)
    private OffsetDateTime liquidadoEm;

    @PrePersist
    public void prePersist() {
        if (liquidadoEm == null) {
            liquidadoEm = OffsetDateTime.now();
        }
    }
}