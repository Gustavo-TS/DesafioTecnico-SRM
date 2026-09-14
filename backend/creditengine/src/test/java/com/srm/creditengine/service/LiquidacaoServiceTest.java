package com.srm.creditengine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.srm.creditengine.model.Liquidacao;
import com.srm.creditengine.model.Moeda;
import com.srm.creditengine.model.Recebivel;
import com.srm.creditengine.model.ResultadoPrecificacao;
import com.srm.creditengine.model.StatusRecebivel;
import com.srm.creditengine.model.TaxaCambio;
import com.srm.creditengine.model.TipoRecebivel;
import com.srm.creditengine.repository.LiquidacaoRepository;
import com.srm.creditengine.repository.RecebivelRepository;

class LiquidacaoServiceTest {

    private LiquidacaoRepository liquidacaoRepository;
    private RecebivelRepository recebivelRepository;
    private PrecificacaoService precificacaoService;
    private CambioService cambioService;

    private LiquidacaoService liquidacaoService;

    @BeforeEach
    void setup() {
        liquidacaoRepository = mock(LiquidacaoRepository.class);
        recebivelRepository = mock(RecebivelRepository.class);
        precificacaoService = mock(PrecificacaoService.class);
        cambioService = mock(CambioService.class);

        liquidacaoService = new LiquidacaoService(
                liquidacaoRepository,
                recebivelRepository,
                precificacaoService,
                cambioService
        );
    }

    @Test
    void deveLiquidarRecebivelEmBrl() {

        UUID recebivelId = UUID.randomUUID();

        Recebivel recebivel = new Recebivel();
        recebivel.setId(recebivelId);
        recebivel.setTipo(TipoRecebivel.DUPLICATA);
        recebivel.setValorFace(new BigDecimal("100000.00"));
        recebivel.setDataVencimento(LocalDate.now().plusMonths(3));
        recebivel.setStatus(StatusRecebivel.PENDENTE);

        ResultadoPrecificacao resultadoPrecificacao =
                new ResultadoPrecificacao(
                        new BigDecimal("0.01"),
                        new BigDecimal("0.015"),
                        3,
                        new BigDecimal("92859.94"),
                        new BigDecimal("7140.06")
                );

        when(liquidacaoRepository.findByIdempotencyKey("abc-123"))
                .thenReturn(Optional.empty());

        when(recebivelRepository.findById(recebivelId))
                .thenReturn(Optional.of(recebivel));

        when(liquidacaoRepository.existsByRecebivelId(recebivelId))
                .thenReturn(false);

        when(precificacaoService.calcularPrazoMeses(
                any(LocalDate.class),
                eq(recebivel.getDataVencimento())
        )).thenReturn(3);

        when(precificacaoService.calcular(
                TipoRecebivel.DUPLICATA,
                new BigDecimal("100000.00"),
                3
        )).thenReturn(resultadoPrecificacao);

        when(liquidacaoRepository.save(any(Liquidacao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Liquidacao resultado = liquidacaoService.liquidar(
                recebivelId,
                Moeda.BRL,
                "abc-123"
        );

        assertEquals(
                new BigDecimal("92859.94"),
                resultado.getValorFinal()
        );

        assertEquals(
                Moeda.BRL,
                resultado.getMoedaPagamento()
        );

        assertEquals(
                StatusRecebivel.LIQUIDADO,
                recebivel.getStatus()
        );

        verify(cambioService, never())
                .buscarTaxaVigente(any(), any(), any());
    }

    @Test
    void deveRetornarLiquidacaoExistenteQuandoIdempotencyKeyJaExistir() {

        UUID recebivelId = UUID.randomUUID();
        String idempotencyKey = "abc-123";

        Recebivel recebivel = new Recebivel();
        recebivel.setId(recebivelId);

        Liquidacao liquidacaoExistente = new Liquidacao();
        liquidacaoExistente.setRecebivel(recebivel);
        liquidacaoExistente.setIdempotencyKey(idempotencyKey);
        liquidacaoExistente.setValorFinal(
                new BigDecimal("92859.94")
        );
        liquidacaoExistente.setMoedaPagamento(Moeda.BRL);

        when(liquidacaoRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(liquidacaoExistente));

        Liquidacao resultado = liquidacaoService.liquidar(
                recebivelId,
                Moeda.BRL,
                idempotencyKey
        );

        assertEquals(liquidacaoExistente, resultado);

        verify(liquidacaoRepository, never())
                .save(any(Liquidacao.class));

        verifyNoInteractions(
                recebivelRepository,
                precificacaoService,
                cambioService
        );
    }

    @Test
    void deveImpedirReutilizacaoDaIdempotencyKeyEmOutraOperacao() {

        UUID recebivelOriginalId = UUID.randomUUID();
        UUID outroRecebivelId = UUID.randomUUID();

        Recebivel recebivelOriginal = new Recebivel();
        recebivelOriginal.setId(recebivelOriginalId);

        Liquidacao liquidacaoExistente = new Liquidacao();
        liquidacaoExistente.setRecebivel(recebivelOriginal);
        liquidacaoExistente.setMoedaPagamento(Moeda.BRL);
        liquidacaoExistente.setIdempotencyKey("abc-123");

        when(liquidacaoRepository.findByIdempotencyKey("abc-123"))
                .thenReturn(Optional.of(liquidacaoExistente));

        assertThrows(
                IllegalStateException.class,
                () -> liquidacaoService.liquidar(
                        outroRecebivelId,
                        Moeda.BRL,
                        "abc-123"
                )
        );

        verifyNoInteractions(
                recebivelRepository,
                precificacaoService,
                cambioService
        );
    }

    @Test
    void deveLiquidarRecebivelEmUsd() {

        UUID recebivelId = UUID.randomUUID();

        Recebivel recebivel = new Recebivel();
        recebivel.setId(recebivelId);
        recebivel.setTipo(TipoRecebivel.DUPLICATA);
        recebivel.setValorFace(new BigDecimal("100000.00"));
        recebivel.setDataVencimento(LocalDate.now().plusMonths(3));
        recebivel.setStatus(StatusRecebivel.PENDENTE);

        ResultadoPrecificacao resultadoPrecificacao =
                new ResultadoPrecificacao(
                        new BigDecimal("0.01"),
                        new BigDecimal("0.015"),
                        3,
                        new BigDecimal("92859.94"),
                        new BigDecimal("7140.06")
                );

        TaxaCambio taxaCambio = new TaxaCambio();
        taxaCambio.setMoedaOrigem(Moeda.BRL);
        taxaCambio.setMoedaDestino(Moeda.USD);
        taxaCambio.setTaxa(new BigDecimal("5.4321"));
        taxaCambio.setVigenteEm(
                OffsetDateTime.now().minusMinutes(5)
        );

        when(liquidacaoRepository.findByIdempotencyKey("usd-123"))
                .thenReturn(Optional.empty());

        when(recebivelRepository.findById(recebivelId))
                .thenReturn(Optional.of(recebivel));

        when(liquidacaoRepository.existsByRecebivelId(recebivelId))
                .thenReturn(false);

        when(precificacaoService.calcularPrazoMeses(
                any(LocalDate.class),
                eq(recebivel.getDataVencimento())
        )).thenReturn(3);

        when(precificacaoService.calcular(
                TipoRecebivel.DUPLICATA,
                new BigDecimal("100000.00"),
                3
        )).thenReturn(resultadoPrecificacao);

        when(cambioService.buscarTaxaVigente(
                eq(Moeda.BRL),
                eq(Moeda.USD),
                any(OffsetDateTime.class)
        )).thenReturn(taxaCambio);

        when(cambioService.converterBrlParaUsd(
                new BigDecimal("92859.94"),
                new BigDecimal("5.4321")
        )).thenReturn(new BigDecimal("17094.67"));

        when(liquidacaoRepository.save(any(Liquidacao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Liquidacao resultado = liquidacaoService.liquidar(
                recebivelId,
                Moeda.USD,
                "usd-123"
        );

        assertEquals(
                new BigDecimal("17094.67"),
                resultado.getValorFinal()
        );

        assertEquals(
                Moeda.USD,
                resultado.getMoedaPagamento()
        );

        assertEquals(
                new BigDecimal("5.4321"),
                resultado.getTaxaCambioUtilizada()
        );

        assertEquals(
                taxaCambio.getVigenteEm(),
                resultado.getTaxaCambioVigenteEm()
        );

        assertEquals(
                StatusRecebivel.LIQUIDADO,
                recebivel.getStatus()
        );
    }

    @Test
    void deveImpedirNovaLiquidacaoQuandoRecebivelJaEstiverLiquidado() {

        UUID recebivelId = UUID.randomUUID();

        Recebivel recebivel = new Recebivel();
        recebivel.setId(recebivelId);
        recebivel.setStatus(StatusRecebivel.LIQUIDADO);

        when(liquidacaoRepository.findByIdempotencyKey("nova-chave"))
                .thenReturn(Optional.empty());

        when(recebivelRepository.findById(recebivelId))
                .thenReturn(Optional.of(recebivel));

        assertThrows(
                IllegalStateException.class,
                () -> liquidacaoService.liquidar(
                        recebivelId,
                        Moeda.BRL,
                        "nova-chave"
                )
        );

        verify(liquidacaoRepository, never())
                .save(any(Liquidacao.class));

        verifyNoInteractions(
                precificacaoService,
                cambioService
        );
    }
}