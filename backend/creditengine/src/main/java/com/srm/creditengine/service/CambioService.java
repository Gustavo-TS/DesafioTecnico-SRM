package com.srm.creditengine.service;

import com.srm.creditengine.model.Moeda;
import com.srm.creditengine.model.TaxaCambio;
import com.srm.creditengine.repository.TaxaCambioRepository;
import org.springframework.stereotype.Service;
import com.srm.creditengine.exception.TaxaCambioNaoEncontradaException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

@Service
public class CambioService {

    private final TaxaCambioRepository taxaCambioRepository;

    public CambioService(TaxaCambioRepository taxaCambioRepository) {
        this.taxaCambioRepository = taxaCambioRepository;
    }

    public TaxaCambio buscarTaxaVigente(
            Moeda moedaOrigem,
            Moeda moedaDestino,
            OffsetDateTime instanteReferencia
    ) {
        return taxaCambioRepository
                .findFirstByMoedaOrigemAndMoedaDestinoAndVigenteEmLessThanEqualOrderByVigenteEmDesc(
                        moedaOrigem,
                        moedaDestino,
                        instanteReferencia
                )
                .orElseThrow(() ->
                new TaxaCambioNaoEncontradaException(
                        "Taxa de câmbio vigente não encontrada"
                )
        );
    }

    public BigDecimal converterBrlParaUsd(
            BigDecimal valorEmBrl,
            BigDecimal taxaCambio
    ) {
        return valorEmBrl
                .divide(taxaCambio, MathContext.DECIMAL128)
                .setScale(2, RoundingMode.HALF_EVEN);
    }
}