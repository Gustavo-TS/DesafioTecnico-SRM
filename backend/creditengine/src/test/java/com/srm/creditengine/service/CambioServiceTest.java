package com.srm.creditengine.service;

import com.srm.creditengine.exception.TaxaCambioNaoEncontradaException;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.srm.creditengine.model.Moeda;
import com.srm.creditengine.model.TaxaCambio;
import com.srm.creditengine.repository.TaxaCambioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CambioServiceTest {

    private TaxaCambioRepository taxaCambioRepository;
    private CambioService cambioService;

    @BeforeEach
    void setup() {
        taxaCambioRepository = mock(TaxaCambioRepository.class);
        cambioService = new CambioService(taxaCambioRepository);
    }

    @Test
    void deveBuscarTaxaCambioVigente() {
        OffsetDateTime instanteReferencia = OffsetDateTime.now();

        TaxaCambio taxa = new TaxaCambio();
        taxa.setMoedaOrigem(Moeda.BRL);
        taxa.setMoedaDestino(Moeda.USD);
        taxa.setTaxa(new BigDecimal("5.4321"));
        taxa.setVigenteEm(instanteReferencia.minusMinutes(10));

        when(taxaCambioRepository
                .findFirstByMoedaOrigemAndMoedaDestinoAndVigenteEmLessThanEqualOrderByVigenteEmDesc(
                        Moeda.BRL,
                        Moeda.USD,
                        instanteReferencia
                ))
                .thenReturn(Optional.of(taxa));

        TaxaCambio resultado = cambioService.buscarTaxaVigente(
                Moeda.BRL,
                Moeda.USD,
                instanteReferencia
        );

        assertEquals(new BigDecimal("5.4321"), resultado.getTaxa());

        verify(taxaCambioRepository, times(1))
                .findFirstByMoedaOrigemAndMoedaDestinoAndVigenteEmLessThanEqualOrderByVigenteEmDesc(
                        Moeda.BRL,
                        Moeda.USD,
                        instanteReferencia
                );
    }

    @Test
    void deveConverterBrlParaUsd() {
        BigDecimal resultado = cambioService.converterBrlParaUsd(
                new BigDecimal("92859.94"),
                new BigDecimal("5.4321")
        );

        assertEquals(new BigDecimal("17094.67"), resultado);
    }
    
    @Test
    void deveLancarExcecaoQuandoNaoExistirTaxaVigente() {
        OffsetDateTime instanteReferencia = OffsetDateTime.now();

        when(taxaCambioRepository
                .findFirstByMoedaOrigemAndMoedaDestinoAndVigenteEmLessThanEqualOrderByVigenteEmDesc(
                        Moeda.BRL,
                        Moeda.USD,
                        instanteReferencia
                ))
                .thenReturn(Optional.empty());

        assertThrows(
                TaxaCambioNaoEncontradaException.class,
                () -> cambioService.buscarTaxaVigente(
                        Moeda.BRL,
                        Moeda.USD,
                        instanteReferencia
                )
        );
    }
}