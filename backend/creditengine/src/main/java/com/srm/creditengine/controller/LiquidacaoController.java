package com.srm.creditengine.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.srm.creditengine.dto.LiquidacaoRequest;
import com.srm.creditengine.dto.LiquidacaoResponse;
import com.srm.creditengine.model.Liquidacao;
import com.srm.creditengine.model.Moeda;
import com.srm.creditengine.model.ResultadoLiquidacao;
import com.srm.creditengine.service.LiquidacaoService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
@Tag(name = "Liquidações")
public class LiquidacaoController {

    private final LiquidacaoService liquidacaoService;

    public LiquidacaoController(
            LiquidacaoService liquidacaoService
    ) {
        this.liquidacaoService = liquidacaoService;
    }

    @PostMapping("/recebiveis/{recebivelId}/liquidacoes")
    public ResponseEntity<LiquidacaoResponse> liquidar(
            @PathVariable UUID recebivelId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody LiquidacaoRequest request
    ) {

        ResultadoLiquidacao resultado =
                liquidacaoService.liquidar(
                        recebivelId,
                        request.moedaPagamento(),
                        idempotencyKey
                );

        HttpStatus status = resultado.criada()
                ? HttpStatus.CREATED
                : HttpStatus.OK;

        return ResponseEntity
                .status(status)
                .body(toResponse(resultado.liquidacao()));
    }

    @GetMapping("/liquidacoes")
    public ResponseEntity<List<LiquidacaoResponse>> listar(

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataInicio,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataFim,

            @RequestParam(required = false)
            UUID cedenteId,

            @RequestParam(required = false)
            Moeda moeda
    ) {

        List<LiquidacaoResponse> response =
                liquidacaoService
                        .listar(
                                dataInicio,
                                dataFim,
                                cedenteId,
                                moeda
                        )
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    private LiquidacaoResponse toResponse(
            Liquidacao liquidacao
    ) {

        return new LiquidacaoResponse(
                liquidacao.getId(),
                liquidacao.getRecebivel().getId(),
                liquidacao.getValorFace(),
                liquidacao.getTaxaBase(),
                liquidacao.getSpread(),
                liquidacao.getPrazoMeses(),
                liquidacao.getValorPresente(),
                liquidacao.getValorDesagio(),
                liquidacao.getMoedaPagamento(),
                liquidacao.getTaxaCambioUtilizada(),
                liquidacao.getTaxaCambioVigenteEm(),
                liquidacao.getValorFinal(),
                liquidacao.getLiquidadoEm()
        );
    }
}