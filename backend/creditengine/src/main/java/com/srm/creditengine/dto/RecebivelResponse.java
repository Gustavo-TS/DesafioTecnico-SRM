package com.srm.creditengine.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.srm.creditengine.model.StatusRecebivel;
import com.srm.creditengine.model.TipoRecebivel;

public record RecebivelResponse(

        UUID id,
        UUID cedenteId,
        TipoRecebivel tipo,
        BigDecimal valorFace,
        LocalDate dataVencimento,
        StatusRecebivel status,
        OffsetDateTime criadoEm

) {
}