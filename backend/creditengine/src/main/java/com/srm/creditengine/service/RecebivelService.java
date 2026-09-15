package com.srm.creditengine.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.srm.creditengine.exception.RecursoNaoEncontradoException;
import com.srm.creditengine.model.Cedente;
import com.srm.creditengine.model.Recebivel;
import com.srm.creditengine.model.StatusRecebivel;
import com.srm.creditengine.model.TipoRecebivel;
import com.srm.creditengine.repository.CedenteRepository;
import com.srm.creditengine.repository.RecebivelRepository;

@Service
public class RecebivelService {

    private final RecebivelRepository recebivelRepository;
    private final CedenteRepository cedenteRepository;

    public RecebivelService(
            RecebivelRepository recebivelRepository,
            CedenteRepository cedenteRepository
    ) {
        this.recebivelRepository = recebivelRepository;
        this.cedenteRepository = cedenteRepository;
    }

    public Recebivel criar(
            UUID cedenteId,
            TipoRecebivel tipo,
            BigDecimal valorFace,
            LocalDate dataVencimento
    ) {

        Cedente cedente = cedenteRepository
                .findById(cedenteId)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Cedente não encontrado"
                        )
                ); 

        Recebivel recebivel = new Recebivel();

        recebivel.setCedente(cedente);
        recebivel.setTipo(tipo);
        recebivel.setValorFace(valorFace);
        recebivel.setDataVencimento(dataVencimento);
        recebivel.setStatus(StatusRecebivel.PENDENTE);

        return recebivelRepository.save(recebivel);
    }

    public Recebivel buscarPorId(UUID id) {

        return recebivelRepository
                .findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Recebível não encontrado"
                        )
                );
    }

    public List<Recebivel> listar() {
        return recebivelRepository.findAll();
    }
}