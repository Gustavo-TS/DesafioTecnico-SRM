package com.srm.creditengine.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.srm.creditengine.dto.RecebivelRequest;
import com.srm.creditengine.dto.RecebivelResponse;
import com.srm.creditengine.model.Recebivel;
import com.srm.creditengine.service.RecebivelService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/recebiveis")
@Tag(name = "Recebíveis")
public class RecebivelController {

    private final RecebivelService recebivelService;

    public RecebivelController(RecebivelService recebivelService) {
        this.recebivelService = recebivelService;
    }

    @PostMapping
    public ResponseEntity<RecebivelResponse> criar(
            @Valid @RequestBody RecebivelRequest request
    ) {
        Recebivel recebivel = recebivelService.criar(
                request.cedenteId(),
                request.tipo(),
                request.valorFace(),
                request.dataVencimento()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(recebivel));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecebivelResponse> buscarPorId(
            @PathVariable UUID id
    ) {
        Recebivel recebivel = recebivelService.buscarPorId(id);

        return ResponseEntity.ok(
                toResponse(recebivel)
        );
    }

    @GetMapping
    public ResponseEntity<List<RecebivelResponse>> listar() {
        List<RecebivelResponse> recebiveis = recebivelService
                .listar()
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(recebiveis);
    }

    private RecebivelResponse toResponse(Recebivel recebivel) {
        return new RecebivelResponse(
                recebivel.getId(),
                recebivel.getCedente().getId(),
                recebivel.getTipo(),
                recebivel.getValorFace(),
                recebivel.getDataVencimento(),
                recebivel.getStatus(),
                recebivel.getCriadoEm()
        );
    }
}