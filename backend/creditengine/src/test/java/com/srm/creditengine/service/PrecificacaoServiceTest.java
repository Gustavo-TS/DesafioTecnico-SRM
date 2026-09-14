package com.srm.creditengine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.srm.creditengine.model.ResultadoPrecificacao;
import com.srm.creditengine.model.TipoRecebivel;
import com.srm.creditengine.strategy.ChequeStrategy;
import com.srm.creditengine.strategy.DuplicataStrategy;

class PrecificacaoServiceTest {

    private PrecificacaoService precificacaoService;
    private CambioService cambioService;

    @BeforeEach
    void setup() {
        precificacaoService = new PrecificacaoService(
                new DuplicataStrategy(),
                new ChequeStrategy()
        );

        cambioService = new CambioService(null);
    }

    @Test
    void deveCalcularGoldenCaseC1Duplicata() {

        ResultadoPrecificacao resultado =
                precificacaoService.calcular(
                        TipoRecebivel.DUPLICATA,
                        new BigDecimal("100000.00"),
                        3
                );

        assertEquals(
                new BigDecimal("92859.94"),
                resultado.valorPresente()
        );

        assertEquals(
                new BigDecimal("7140.06"),
                resultado.valorDesagio()
        );
    }

    @Test
    void deveCalcularGoldenCaseC2Cheque() {

        ResultadoPrecificacao resultado =
                precificacaoService.calcular(
                        TipoRecebivel.CHEQUE,
                        new BigDecimal("25000.00"),
                        2
                );

        assertEquals(
                new BigDecimal("23337.77"),
                resultado.valorPresente()
        );

        assertEquals(
                new BigDecimal("1662.23"),
                resultado.valorDesagio()
        );
    }

    @Test
    void deveCalcularGoldenCaseC3DuplicataEmUsd() {

        ResultadoPrecificacao resultado =
                precificacaoService.calcular(
                        TipoRecebivel.DUPLICATA,
                        new BigDecimal("100000.00"),
                        3
                );

        BigDecimal valorFinalUsd =
                cambioService.converterBrlParaUsd(
                        resultado.valorPresente(),
                        new BigDecimal("5.4321")
                );

        assertEquals(
                new BigDecimal("92859.94"),
                resultado.valorPresente()
        );

        assertEquals(
                new BigDecimal("7140.06"),
                resultado.valorDesagio()
        );

        assertEquals(
                new BigDecimal("17094.67"),
                valorFinalUsd
        );
    }
}