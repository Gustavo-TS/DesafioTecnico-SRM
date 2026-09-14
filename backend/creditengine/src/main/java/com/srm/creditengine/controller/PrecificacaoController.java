package com.srm.creditengine.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.srm.creditengine.dto.SimulacaoRequest;
import com.srm.creditengine.dto.SimulacaoResponse;
import com.srm.creditengine.model.Moeda;
import com.srm.creditengine.model.ResultadoPrecificacao;
import com.srm.creditengine.model.TaxaCambio;
import com.srm.creditengine.service.CambioService;
import com.srm.creditengine.service.PrecificacaoService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/precificacoes")
@Tag(name = "Precificação")
public class PrecificacaoController {

    private final PrecificacaoService precificacaoService;
    private final CambioService cambioService;

    public PrecificacaoController(
            PrecificacaoService precificacaoService,
            CambioService cambioService
    ) {
        this.precificacaoService = precificacaoService;
        this.cambioService = cambioService;
    }

    @PostMapping("/simular")
    public ResponseEntity<SimulacaoResponse> simular(
            @Valid @RequestBody SimulacaoRequest request
    ) {

        int prazoMeses = precificacaoService.calcularPrazoMeses(
                LocalDate.now(),
                request.dataVencimento()
        );

        ResultadoPrecificacao precificacao =
                precificacaoService.calcular(
                        request.tipo(),
                        request.valorFace(),
                        prazoMeses
                );

        BigDecimal taxaCambioUtilizada = null;
        BigDecimal valorFinal = precificacao.valorPresente();

        if (request.moedaPagamento() == Moeda.USD) {

            TaxaCambio taxaCambio =
                    cambioService.buscarTaxaVigente(
                            Moeda.BRL,
                            Moeda.USD,
                            OffsetDateTime.now()
                    );

            taxaCambioUtilizada = taxaCambio.getTaxa();

            valorFinal = cambioService.converterBrlParaUsd(
                    precificacao.valorPresente(),
                    taxaCambioUtilizada
            );
        }

        SimulacaoResponse response = new SimulacaoResponse(
                request.valorFace(),
                precificacao.taxaBase(),
                precificacao.spread(),
                precificacao.prazoMeses(),
                precificacao.valorPresente(),
                precificacao.valorDesagio(),
                request.moedaPagamento(),
                taxaCambioUtilizada,
                valorFinal
        );

        return ResponseEntity.ok(response);
    }
}