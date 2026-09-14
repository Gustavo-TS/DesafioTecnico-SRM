package com.srm.creditengine.controller;

import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.srm.creditengine.dto.TaxaCambioRequest;
import com.srm.creditengine.dto.TaxaCambioResponse;
import com.srm.creditengine.model.Moeda;
import com.srm.creditengine.model.TaxaCambio;
import com.srm.creditengine.service.CambioService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/taxas-cambio")
@Tag(name = "Taxas de câmbio")
public class TaxaCambioController {

    private final CambioService cambioService;

    public TaxaCambioController(
            CambioService cambioService
    ) {
        this.cambioService = cambioService;
    }

    @PostMapping
    public ResponseEntity<TaxaCambioResponse> cadastrar(
            @Valid @RequestBody TaxaCambioRequest request
    ) {

        TaxaCambio taxaCambio =
                cambioService.cadastrar(
                        request.moedaOrigem(),
                        request.moedaDestino(),
                        request.taxa(),
                        request.vigenteEm()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(taxaCambio));
    }

    @GetMapping("/vigente")
    public ResponseEntity<TaxaCambioResponse> buscarVigente(
            @RequestParam Moeda moedaOrigem,
            @RequestParam Moeda moedaDestino
    ) {

        TaxaCambio taxaCambio =
                cambioService.buscarTaxaVigente(
                        moedaOrigem,
                        moedaDestino,
                        OffsetDateTime.now()
                );

        return ResponseEntity.ok(
                toResponse(taxaCambio)
        );
    }

    private TaxaCambioResponse toResponse(
            TaxaCambio taxaCambio
    ) {

        return new TaxaCambioResponse(
                taxaCambio.getId(),
                taxaCambio.getMoedaOrigem(),
                taxaCambio.getMoedaDestino(),
                taxaCambio.getTaxa(),
                taxaCambio.getVigenteEm(),
                taxaCambio.getCriadoEm()
        );
    }
}