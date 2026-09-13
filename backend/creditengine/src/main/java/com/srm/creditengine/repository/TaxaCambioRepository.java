package com.srm.creditengine.repository;

import com.srm.creditengine.model.Moeda;
import com.srm.creditengine.model.TaxaCambio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TaxaCambioRepository extends JpaRepository<TaxaCambio, UUID> {

    Optional<TaxaCambio> findFirstByMoedaOrigemAndMoedaDestinoOrderByVigenteEmDesc(
        Moeda moedaOrigem,
        Moeda moedaDestino
    );
}