package com.srm.creditengine.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srm.creditengine.exception.TaxaCambioNaoEncontradaException;
import com.srm.creditengine.model.Moeda;
import com.srm.creditengine.model.TaxaCambio;
import com.srm.creditengine.repository.TaxaCambioRepository;

@Service
public class CambioService {

    private final TaxaCambioRepository taxaCambioRepository;

    public CambioService(
            TaxaCambioRepository taxaCambioRepository
    ) {
        this.taxaCambioRepository = taxaCambioRepository;
    }

    @Transactional
    public TaxaCambio cadastrar(
            Moeda moedaOrigem,
            Moeda moedaDestino,
            BigDecimal taxa,
            OffsetDateTime vigenteEm
    ) {

        if (moedaOrigem == moedaDestino) {
            throw new IllegalArgumentException(
                    "Moeda de origem e destino devem ser diferentes"
            );
        }

        TaxaCambio taxaCambio = new TaxaCambio();

        taxaCambio.setMoedaOrigem(moedaOrigem);
        taxaCambio.setMoedaDestino(moedaDestino);
        taxaCambio.setTaxa(taxa);
        taxaCambio.setVigenteEm(vigenteEm);

        return taxaCambioRepository.save(taxaCambio);
    }

    @Transactional(readOnly = true)
    public TaxaCambio buscarTaxaVigente(
            Moeda origem,
            Moeda destino,
            OffsetDateTime instanteReferencia
    ) {

        return taxaCambioRepository
                .findFirstByMoedaOrigemAndMoedaDestinoAndVigenteEmLessThanEqualOrderByVigenteEmDesc(
                        origem,
                        destino,
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
                .divide(
                        taxaCambio,
                        MathContext.DECIMAL128
                )
                .setScale(
                        2,
                        RoundingMode.HALF_EVEN
                );
    }
}