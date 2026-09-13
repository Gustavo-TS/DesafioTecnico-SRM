package com.srm.creditengine.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Service
public class CambioService {

    public BigDecimal converterBrlParaUsd(
            BigDecimal valorEmBrl,
            BigDecimal taxaCambio
    ) {
        return valorEmBrl
                .divide(taxaCambio, MathContext.DECIMAL128)
                .setScale(2, RoundingMode.HALF_EVEN);
    }
}