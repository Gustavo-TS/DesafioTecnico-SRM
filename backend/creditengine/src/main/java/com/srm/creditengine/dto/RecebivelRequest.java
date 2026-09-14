package com.srm.creditengine.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.srm.creditengine.model.TipoRecebivel;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RecebivelRequest(

        @NotNull
        UUID cedenteId,

        @NotNull
        TipoRecebivel tipo,

        @NotNull
        @Positive
        BigDecimal valorFace,

        @NotNull
        @Future
        LocalDate dataVencimento

) {
}