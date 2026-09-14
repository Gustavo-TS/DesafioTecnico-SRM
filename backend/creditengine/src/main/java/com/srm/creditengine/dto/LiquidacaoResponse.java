package com.srm.creditengine.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.srm.creditengine.model.Moeda;

public record LiquidacaoResponse(

        UUID id,
        UUID recebivelId,
        BigDecimal valorFace,
        BigDecimal taxaBase,
        BigDecimal spread,
        Integer prazoMeses,
        BigDecimal valorPresente,
        BigDecimal valorDesagio,
        Moeda moedaPagamento,
        BigDecimal taxaCambioUtilizada,
        OffsetDateTime taxaCambioVigenteEm,
        BigDecimal valorFinal,
        OffsetDateTime liquidadoEm

) {
}