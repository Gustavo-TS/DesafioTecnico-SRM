package com.srm.creditengine.dto;

import java.math.BigDecimal;

import com.srm.creditengine.model.Moeda;
import com.srm.creditengine.model.TipoRecebivel;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SimulacaoRequest(

        @NotNull
        TipoRecebivel tipo,

        @NotNull
        @Positive
        BigDecimal valorFace,

        @Positive
        int prazoMeses,

        @NotNull
        Moeda moedaPagamento

) {
}