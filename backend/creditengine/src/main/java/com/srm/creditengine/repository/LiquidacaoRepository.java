package com.srm.creditengine.repository;

import com.srm.creditengine.model.Liquidacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LiquidacaoRepository extends JpaRepository<Liquidacao, UUID> {

    Optional<Liquidacao> findByIdempotencyKey(String idempotencyKey);

    boolean existsByRecebivelId(UUID recebivelId);
}