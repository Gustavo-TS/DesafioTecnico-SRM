package com.srm.creditengine.service;

import com.srm.creditengine.model.TipoRecebivel;
import com.srm.creditengine.strategy.ChequeStrategy;
import com.srm.creditengine.strategy.DuplicataStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        BigDecimal valorFace = new BigDecimal("100000.00");

        BigDecimal valorPresente = precificacaoService.calcularValorPresente(
                TipoRecebivel.DUPLICATA,
                valorFace,
                3
        );

        BigDecimal desagio = precificacaoService.calcularDesagio(
                valorFace,
                valorPresente
        );

        assertEquals(new BigDecimal("92859.94"), valorPresente);
        assertEquals(new BigDecimal("7140.06"), desagio);
    }

    @Test
    void deveCalcularGoldenCaseC2Cheque() {
        BigDecimal valorFace = new BigDecimal("25000.00");

        BigDecimal valorPresente = precificacaoService.calcularValorPresente(
                TipoRecebivel.CHEQUE,
                valorFace,
                2
        );

        BigDecimal desagio = precificacaoService.calcularDesagio(
                valorFace,
                valorPresente
        );

        assertEquals(new BigDecimal("23337.77"), valorPresente);
        assertEquals(new BigDecimal("1662.23"), desagio);
    }

    @Test
    void deveCalcularGoldenCaseC3DuplicataEmUsd() {
        BigDecimal valorFace = new BigDecimal("100000.00");

        BigDecimal valorPresenteBrl = precificacaoService.calcularValorPresente(
                TipoRecebivel.DUPLICATA,
                valorFace,
                3
        );

        BigDecimal desagio = precificacaoService.calcularDesagio(
                valorFace,
                valorPresenteBrl
        );

        BigDecimal valorFinalUsd = cambioService.converterBrlParaUsd(
                valorPresenteBrl,
                new BigDecimal("5.4321")
        );

        assertEquals(new BigDecimal("92859.94"), valorPresenteBrl);
        assertEquals(new BigDecimal("7140.06"), desagio);
        assertEquals(new BigDecimal("17094.67"), valorFinalUsd);
    }
}