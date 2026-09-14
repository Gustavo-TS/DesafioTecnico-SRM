package com.srm.creditengine.dto;

import com.srm.creditengine.model.Moeda;
import jakarta.validation.constraints.NotNull;

public record LiquidacaoRequest(

        @NotNull
        Moeda moedaPagamento

) {
}