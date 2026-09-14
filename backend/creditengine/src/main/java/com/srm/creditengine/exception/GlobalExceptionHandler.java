package com.srm.creditengine.exception;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> tratarBadRequest(
            IllegalArgumentException exception
    ) {
        return criarResposta(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    @ExceptionHandler(TaxaCambioNaoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> tratarTaxaCambioNaoEncontrada(
            TaxaCambioNaoEncontradaException exception
    ) {
        return criarResposta(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> tratarConflito(
            IllegalStateException exception
    ) {
        return criarResposta(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> tratarValidacao(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> campos = new HashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        campos.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        Map<String, Object> body = new HashMap<>();

        body.put("timestamp", OffsetDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("erro", "Dados inválidos");
        body.put("campos", campos);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }

    private ResponseEntity<Map<String, Object>> criarResposta(
            HttpStatus status,
            String mensagem
    ) {
        Map<String, Object> body = new HashMap<>();

        body.put("timestamp", OffsetDateTime.now());
        body.put("status", status.value());
        body.put("erro", mensagem);

        return ResponseEntity
                .status(status)
                .body(body);
    }
}