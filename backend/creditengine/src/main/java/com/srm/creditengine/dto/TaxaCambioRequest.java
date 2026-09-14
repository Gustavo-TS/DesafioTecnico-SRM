package com.srm.creditengine.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.srm.creditengine.model.Moeda;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TaxaCambioRequest(

        @NotNull
        Moeda moedaOrigem,

        @NotNull
        Moeda moedaDestino,

        @NotNull
        @Positive
        BigDecimal taxa,

        @NotNull
        OffsetDateTime vigenteEm

) {
}