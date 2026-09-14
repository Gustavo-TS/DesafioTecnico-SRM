package com.srm.creditengine.dto;

import java.math.BigDecimal;

import com.srm.creditengine.model.Moeda;

public record SimulacaoResponse(

        BigDecimal valorFace,
        BigDecimal taxaBase,
        BigDecimal spread,
        int prazoMeses,
        BigDecimal valorPresente,
        BigDecimal valorDesagio,
        Moeda moedaPagamento,
        BigDecimal taxaCambioUtilizada,
        BigDecimal valorFinal

) {
}