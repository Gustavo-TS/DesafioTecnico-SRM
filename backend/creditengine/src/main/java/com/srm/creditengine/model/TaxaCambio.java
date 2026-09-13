package com.srm.creditengine.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "taxas_cambio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxaCambio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "moeda_origem", nullable = false, length = 3)
    private Moeda moedaOrigem;

    @Enumerated(EnumType.STRING)
    @Column(name = "moeda_destino", nullable = false, length = 3)
    private Moeda moedaDestino;

    @Column(
        nullable = false,
        precision = 19,
        scale = 8
    )
    private BigDecimal taxa;

    @Column(name = "vigente_em", nullable = false)
    private OffsetDateTime vigenteEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        if (criadoEm == null) {
            criadoEm = OffsetDateTime.now();
        }
    }
}