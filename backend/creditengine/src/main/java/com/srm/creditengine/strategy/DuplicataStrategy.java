package com.srm.creditengine.strategy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

@Component
public class DuplicataStrategy implements PrecificacaoStrategy {

    private static final BigDecimal SPREAD = new BigDecimal("0.015");

    @Override
    public BigDecimal calcularValorPresente(
            BigDecimal valorFace,
            BigDecimal taxaBase,
            int prazoMeses
    ) {
        BigDecimal taxaTotal = BigDecimal.ONE
                .add(taxaBase)
                .add(SPREAD);

        BigDecimal fator = taxaTotal.pow(
                prazoMeses,
                MathContext.DECIMAL128
        );

        return valorFace.divide(
                fator,
                MathContext.DECIMAL128
        );
    }

    @Override
    public BigDecimal getSpread() {
        return SPREAD;
    }
}