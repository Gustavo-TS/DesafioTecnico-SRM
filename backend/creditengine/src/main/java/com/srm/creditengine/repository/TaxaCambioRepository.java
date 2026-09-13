package com.srm.creditengine.repository;

import com.srm.creditengine.model.Moeda;
import com.srm.creditengine.model.TaxaCambio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface TaxaCambioRepository extends JpaRepository<TaxaCambio, UUID> {

    Optional<TaxaCambio>
    findFirstByMoedaOrigemAndMoedaDestinoAndVigenteEmLessThanEqualOrderByVigenteEmDesc(
            Moeda moedaOrigem,
            Moeda moedaDestino,
            OffsetDateTime instanteReferencia
    );
}