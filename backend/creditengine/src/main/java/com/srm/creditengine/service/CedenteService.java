package com.srm.creditengine.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.srm.creditengine.model.Cedente;
import com.srm.creditengine.repository.CedenteRepository;

@Service
public class CedenteService {

    private final CedenteRepository cedenteRepository;

    public CedenteService(CedenteRepository cedenteRepository) {
        this.cedenteRepository = cedenteRepository;
    }

    public Cedente criar(String nome, String documento) {

        if (cedenteRepository.findByDocumento(documento).isPresent()) {
            throw new IllegalStateException(
                    "Já existe cedente com este documento"
            );
        }

        Cedente cedente = new Cedente();
        cedente.setNome(nome);
        cedente.setDocumento(documento);

        return cedenteRepository.save(cedente);
    }

    public List<Cedente> listar() {
        return cedenteRepository.findAll();
    }
}