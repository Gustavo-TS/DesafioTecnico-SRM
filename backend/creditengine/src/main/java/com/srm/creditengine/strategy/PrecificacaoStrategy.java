package com.srm.creditengine.strategy;

import java.math.BigDecimal;

public interface PrecificacaoStrategy {

    BigDecimal calcularValorPresente(
            BigDecimal valorFace,
            BigDecimal taxaBase,
            int prazoMeses
    );

    BigDecimal getSpread();
}