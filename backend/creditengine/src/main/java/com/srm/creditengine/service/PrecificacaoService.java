package com.srm.creditengine.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

import org.springframework.stereotype.Service;

import com.srm.creditengine.model.ResultadoPrecificacao;
import com.srm.creditengine.model.TipoRecebivel;
import com.srm.creditengine.strategy.ChequeStrategy;
import com.srm.creditengine.strategy.DuplicataStrategy;
import com.srm.creditengine.strategy.PrecificacaoStrategy;

@Service
public class PrecificacaoService {

    private static final BigDecimal TAXA_BASE =
            new BigDecimal("0.01");

    private final DuplicataStrategy duplicataStrategy;
    private final ChequeStrategy chequeStrategy;

    public PrecificacaoService(
            DuplicataStrategy duplicataStrategy,
            ChequeStrategy chequeStrategy
    ) {
        this.duplicataStrategy = duplicataStrategy;
        this.chequeStrategy = chequeStrategy;
    }

    public ResultadoPrecificacao calcular(
            TipoRecebivel tipo,
            BigDecimal valorFace,
            int prazoMeses
    ) {

        PrecificacaoStrategy strategy =
                obterStrategy(tipo);

        BigDecimal valorPresente = strategy
                .calcularValorPresente(
                        valorFace,
                        TAXA_BASE,
                        prazoMeses
                )
                .setScale(
                        2,
                        RoundingMode.HALF_EVEN
                );

        BigDecimal valorDesagio = valorFace
                .subtract(valorPresente)
                .setScale(
                        2,
                        RoundingMode.HALF_EVEN
                );

        return new ResultadoPrecificacao(
                TAXA_BASE,
                strategy.getSpread(),
                prazoMeses,
                valorPresente,
                valorDesagio
        );
    }

    public int calcularPrazoMeses(
            LocalDate dataReferencia,
            LocalDate dataVencimento
    ) {

        if (!dataVencimento.isAfter(dataReferencia)) {
            throw new IllegalArgumentException(
                    "Data de vencimento deve ser posterior à data atual"
            );
        }

        Period periodo = Period.between(
                dataReferencia,
                dataVencimento
        );

        int meses = periodo.getYears() * 12
                + periodo.getMonths();

        if (periodo.getDays() > 0) {
            meses++;
        }

        return meses;
    }

    private PrecificacaoStrategy obterStrategy(
            TipoRecebivel tipo
    ) {

        return switch (tipo) {
            case DUPLICATA -> duplicataStrategy;
            case CHEQUE -> chequeStrategy;
        };
    }
}