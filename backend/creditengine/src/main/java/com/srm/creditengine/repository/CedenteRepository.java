package com.srm.creditengine.repository;

import com.srm.creditengine.model.Cedente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CedenteRepository extends JpaRepository<Cedente, UUID> {

    Optional<Cedente> findByDocumento(String documento);
}