package com.srm.creditengine.model;

public record ResultadoLiquidacao(
        Liquidacao liquidacao,
        boolean criada
) {
}