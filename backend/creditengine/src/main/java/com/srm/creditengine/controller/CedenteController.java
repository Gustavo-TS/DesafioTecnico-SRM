package com.srm.creditengine.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.srm.creditengine.dto.CedenteRequest;
import com.srm.creditengine.dto.CedenteResponse;
import com.srm.creditengine.model.Cedente;
import com.srm.creditengine.service.CedenteService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cedentes")
@Tag(name = "Cedentes")
public class CedenteController {

    private final CedenteService cedenteService;

    public CedenteController(CedenteService cedenteService) {
        this.cedenteService = cedenteService;
    }

    @PostMapping
    public ResponseEntity<CedenteResponse> criar(
            @Valid @RequestBody CedenteRequest request
    ) {

        Cedente cedente = cedenteService.criar(
                request.nome(),
                request.documento()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(cedente));
    }

    @GetMapping
    public ResponseEntity<List<CedenteResponse>> listar() {

        List<CedenteResponse> response = cedenteService
                .listar()
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    private CedenteResponse toResponse(Cedente cedente) {
        return new CedenteResponse(
                cedente.getId(),
                cedente.getNome(),
                cedente.getDocumento(),
                cedente.getCriadoEm()
        );
    }
}