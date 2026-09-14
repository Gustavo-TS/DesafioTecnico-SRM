package com.srm.creditengine.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CedenteResponse(
        UUID id,
        String nome,
        String documento,
        OffsetDateTime criadoEm
) {
}