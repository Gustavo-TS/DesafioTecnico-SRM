package com.srm.creditengine.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.srm.creditengine.model.Moeda;

public record TaxaCambioResponse(

        UUID id,
        Moeda moedaOrigem,
        Moeda moedaDestino,
        BigDecimal taxa,
        OffsetDateTime vigenteEm,
        OffsetDateTime criadoEm

) {
}