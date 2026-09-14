package com.srm.creditengine.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srm.creditengine.model.Liquidacao;
import com.srm.creditengine.model.Moeda;
import com.srm.creditengine.model.Recebivel;
import com.srm.creditengine.model.ResultadoPrecificacao;
import com.srm.creditengine.model.StatusRecebivel;
import com.srm.creditengine.model.TaxaCambio;
import com.srm.creditengine.repository.LiquidacaoRepository;
import com.srm.creditengine.repository.RecebivelRepository;

@Service
public class LiquidacaoService {

    private final LiquidacaoRepository liquidacaoRepository;
    private final RecebivelRepository recebivelRepository;
    private final PrecificacaoService precificacaoService;
    private final CambioService cambioService;

    public LiquidacaoService(
            LiquidacaoRepository liquidacaoRepository,
            RecebivelRepository recebivelRepository,
            PrecificacaoService precificacaoService,
            CambioService cambioService
    ) {
        this.liquidacaoRepository = liquidacaoRepository;
        this.recebivelRepository = recebivelRepository;
        this.precificacaoService = precificacaoService;
        this.cambioService = cambioService;
    }

    private int calcularPrazoMeses(
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

    @Transactional(readOnly = true)
    public List<Liquidacao> listar(
            LocalDate dataInicio,
            LocalDate dataFim,
            UUID cedenteId,
            Moeda moeda
    ) {

        if (dataInicio != null
                && dataFim != null
                && dataInicio.isAfter(dataFim)) {

            throw new IllegalArgumentException(
                    "Data inicial não pode ser posterior à data final"
            );
        }

        OffsetDateTime inicio = dataInicio != null
                ? dataInicio
                    .atStartOfDay()
                    .atOffset(OffsetDateTime.now().getOffset())
                : null;

        OffsetDateTime fim = dataFim != null
                ? dataFim
                    .plusDays(1)
                    .atStartOfDay()
                    .atOffset(OffsetDateTime.now().getOffset())
                    .minusNanos(1)
                : null;

        return liquidacaoRepository.buscarComFiltros(
                inicio,
                fim,
                cedenteId,
                moeda
        );
    }

    @Transactional
    public Liquidacao liquidar(
            UUID recebivelId,
            Moeda moedaPagamento,
            String idempotencyKey
    ) {

    	var liquidacaoExistente =
    	        liquidacaoRepository.findByIdempotencyKey(idempotencyKey);

    	if (liquidacaoExistente.isPresent()) {

    	    Liquidacao existente = liquidacaoExistente.get();

    	    boolean mesmoRecebivel =
    	            existente.getRecebivel()
    	                    .getId()
    	                    .equals(recebivelId);

    	    boolean mesmaMoeda =
    	            existente.getMoedaPagamento() == moedaPagamento;

    	    if (!mesmoRecebivel || !mesmaMoeda) {
    	        throw new IllegalStateException(
    	                "Idempotency-Key já utilizada para outra operação"
    	        );
    	    }

    	    return existente;
    	}

        Recebivel recebivel = recebivelRepository
                .findById(recebivelId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Recebível não encontrado"
                        )
                );

        if (recebivel.getStatus() == StatusRecebivel.LIQUIDADO) {
            throw new IllegalStateException(
                    "Recebível já liquidado"
            );
        }

        if (liquidacaoRepository.existsByRecebivelId(recebivelId)) {
            throw new IllegalStateException(
                    "Já existe liquidação para este recebível"
            );
        }

        OffsetDateTime instanteOperacao = OffsetDateTime.now();

        int prazoMeses = calcularPrazoMeses(
                instanteOperacao.toLocalDate(),
                recebivel.getDataVencimento()
        );

        ResultadoPrecificacao precificacao =
                precificacaoService.calcular(
                        recebivel.getTipo(),
                        recebivel.getValorFace(),
                        prazoMeses
                );

        BigDecimal valorFinal;
        BigDecimal taxaCambioUtilizada = null;
        OffsetDateTime taxaCambioVigenteEm = null;

        if (moedaPagamento == Moeda.BRL) {

            valorFinal = precificacao.valorPresente();

        } else if (moedaPagamento == Moeda.USD) {

            TaxaCambio taxaCambio =
                    cambioService.buscarTaxaVigente(
                            Moeda.BRL,
                            Moeda.USD,
                            instanteOperacao
                    );

            taxaCambioUtilizada = taxaCambio.getTaxa();
            taxaCambioVigenteEm = taxaCambio.getVigenteEm();

            valorFinal = cambioService.converterBrlParaUsd(
                    precificacao.valorPresente(),
                    taxaCambioUtilizada
            );

        } else {

            throw new IllegalArgumentException(
                    "Moeda de pagamento não suportada"
            );
        }

        Liquidacao liquidacao = new Liquidacao();

        liquidacao.setRecebivel(recebivel);
        liquidacao.setIdempotencyKey(idempotencyKey);

        liquidacao.setValorFace(recebivel.getValorFace());
        liquidacao.setTaxaBase(precificacao.taxaBase());
        liquidacao.setSpread(precificacao.spread());
        liquidacao.setPrazoMeses(precificacao.prazoMeses());

        liquidacao.setValorPresente(precificacao.valorPresente());
        liquidacao.setValorDesagio(precificacao.valorDesagio());

        liquidacao.setMoedaPagamento(moedaPagamento);

        liquidacao.setTaxaCambioUtilizada(taxaCambioUtilizada);
        liquidacao.setTaxaCambioVigenteEm(taxaCambioVigenteEm);

        liquidacao.setValorFinal(valorFinal);
        liquidacao.setLiquidadoEm(instanteOperacao);

        recebivel.setStatus(StatusRecebivel.LIQUIDADO);

        recebivelRepository.save(recebivel);

        return liquidacaoRepository.save(liquidacao);
    }
}