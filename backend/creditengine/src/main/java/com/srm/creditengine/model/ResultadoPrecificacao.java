package com.srm.creditengine.model;

import java.math.BigDecimal;

public record ResultadoPrecificacao(
        BigDecimal taxaBase,
        BigDecimal spread,
        int prazoMeses,
        BigDecimal valorPresente,
        BigDecimal valorDesagio
) {
}